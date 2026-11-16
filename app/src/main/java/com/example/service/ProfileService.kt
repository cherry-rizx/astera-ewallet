package com.example.service

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ServiceResult
import com.example.model.UserSecret
import com.example.util.OperationType
import com.example.util.PinHasher
import com.example.util.friendlyFirestoreMessage
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.asteraPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "astera_prefs")

data class NotificationSettings(
  val transactionAlerts: Boolean = true,
  val budgetWarnings: Boolean = true,
  val securityAlerts: Boolean = true,
)

sealed interface PinVerifyOutcome {
  data object Verified : PinVerifyOutcome

  data class WrongPin(val remainingAttempts: Int) : PinVerifyOutcome

  data object Blocked : PinVerifyOutcome
}

class ProfileService(
  private val db: FirebaseFirestore,
  private val appContext: Context,
  private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {

  private val keyTxAlerts = booleanPreferencesKey("notif_tx_alerts")
  private val keyBudgetAlerts = booleanPreferencesKey("notif_budget_alerts")
  private val keySecurityAlerts = booleanPreferencesKey("notif_security_alerts")

  fun observeNotificationSettings(): Flow<NotificationSettings> =
    appContext.asteraPrefsDataStore.data.map { prefs ->
      NotificationSettings(
        transactionAlerts = prefs[keyTxAlerts] ?: true,
        budgetWarnings = prefs[keyBudgetAlerts] ?: true,
        securityAlerts = prefs[keySecurityAlerts] ?: true,
      )
    }

  suspend fun updateNotificationSetting(
    uid: String,
    transactionAlerts: Boolean,
    budgetWarnings: Boolean,
    securityAlerts: Boolean,
  ): ServiceResult<Unit> {
    return try {
      appContext.asteraPrefsDataStore.edit { prefs ->
        prefs[keyTxAlerts] = transactionAlerts
        prefs[keyBudgetAlerts] = budgetWarnings
        prefs[keySecurityAlerts] = securityAlerts
      }
      db.collection("notification_settings").document(uid)
        .set(
          mapOf(
            "uid" to uid,
            "transactionAlerts" to transactionAlerts,
            "budgetWarnings" to budgetWarnings,
            "securityAlerts" to securityAlerts,
          )
        )
        .await()
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ServiceResult.Error("Gagal menyimpan pengaturan notifikasi.")
    }
  }

  fun observeUserSecret(uid: String): Flow<UserSecret?> =
    db.collection("user_secrets")
      .document(uid)
      .snapshots()
      .map { snap ->
        if (!snap.exists()) null
        else snap.toObject(UserSecret::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

  suspend fun verifyPin(uid: String, pin: String): ServiceResult<PinVerifyOutcome> {
    val secretRef = db.collection("user_secrets").document(uid)
    return try {
      val snap = secretRef.get().await()
      if (!snap.exists()) {
        return ServiceResult.Error("PIN belum diatur untuk akun ini.")
      }
      val pinHash = snap.getString("pinHash") ?: ""
      val failedAttempts = (snap.getLong("failedAttempts") ?: 0L).toInt()
      if (snap.getBoolean("blocked") == true) return ServiceResult.Success(PinVerifyOutcome.Blocked)

      if (PinHasher.verifyPin(pin, pinHash)) {
        if (failedAttempts > 0) {
          secretRef
            .update(
              mapOf(
                "failedAttempts" to 0L,
                "lockUntilMs" to 0L,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
        ServiceResult.Success(PinVerifyOutcome.Verified)
      } else {
        val nextAttempts = failedAttempts + 1
        if (nextAttempts >= MAX_PIN_ATTEMPTS) {
          secretRef
            .update(
              mapOf(
                "failedAttempts" to MAX_PIN_ATTEMPTS.toLong(),
                "lockUntilMs" to 0L,
                "blocked" to true,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
          ServiceResult.Success(PinVerifyOutcome.Blocked)
        } else {
          secretRef
            .update(
              mapOf(
                "failedAttempts" to nextAttempts.toLong(),
                "lockUntilMs" to 0L,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
          ServiceResult.Success(PinVerifyOutcome.WrongPin(MAX_PIN_ATTEMPTS - nextAttempts))
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, secretRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal memverifikasi PIN."))
    }
  }

  suspend fun changePin(uid: String, currentPin: String, newPin: String): ServiceResult<Unit> {
    if (!newPin.matches(Regex("^[0-9]{6}$"))) {
      return ServiceResult.Error("PIN baru harus terdiri dari 6 digit angka.")
    }
    val verifyResult = verifyPin(uid, currentPin)
    if (verifyResult is ServiceResult.Error) return verifyResult
    when (val outcome = (verifyResult as ServiceResult.Success).data) {
      PinVerifyOutcome.Blocked -> return ServiceResult.Error("Akun diblokir. Hubungi petugas ASTERA untuk membuka blokir.")
      is PinVerifyOutcome.WrongPin ->
        return ServiceResult.Error("PIN lama salah. Sisa percobaan: ${outcome.remainingAttempts}")
      PinVerifyOutcome.Verified -> Unit
    }

    val secretRef = db.collection("user_secrets").document(uid)
    return try {
      secretRef
        .update(
          mapOf(
            "pinHash" to PinHasher.hashPin(newPin),
            "failedAttempts" to 0L,
            "lockUntilMs" to 0L,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, secretRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal mengubah PIN."))
    }
  }

  suspend fun updateName(uid: String, newName: String): ServiceResult<Unit> {
    val cleanName = newName.trim()
    if (cleanName.isEmpty() || cleanName.length > 80) {
      return ServiceResult.Error("Nama harus diisi (maksimal 80 karakter).")
    }
    val userRef = db.collection("users").document(uid)
    val dirRef = db.collection("user_directory").document(uid)
    return try {
      db.runBatch { batch ->
          batch.update(userRef, "name", cleanName)
          batch.update(dirRef, "name", cleanName)
        }
        .await()
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal memperbarui nama."))
    }
  }

  suspend fun updatePhotoUri(uid: String, photoUri: String): ServiceResult<Unit> {
    val uri = Uri.parse(photoUri)
    val mimeType = appContext.contentResolver.getType(uri)
    if (mimeType?.startsWith("image/") != true) return ServiceResult.Error("Pilih file gambar yang valid.")
    val size =
      try {
        appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
      } catch (e: Exception) {
        null
      }
    if (size == null || size <= 0L) return ServiceResult.Error("File foto tidak dapat dibaca.")
    if (size > MAX_PROFILE_PHOTO_BYTES) return ServiceResult.Error("Ukuran foto maksimal 2 MB.")

    val userRef = db.collection("users").document(uid)
    val photoRef = storage.reference.child("users/$uid/profile.jpg")
    return try {
      val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
      photoRef.putFile(uri, metadata).await()
      val downloadUrl = photoRef.downloadUrl.await().toString()
      userRef.update("photoUrl", downloadUrl).await()
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal menyimpan foto profil."))
    }
  }

  companion object {
    const val MAX_PIN_ATTEMPTS = 3
    const val MAX_PROFILE_PHOTO_BYTES = 2L * 1024L * 1024L
  }
}
