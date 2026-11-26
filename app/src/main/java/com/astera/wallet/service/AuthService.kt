package com.astera.wallet.service

import android.util.Log
import com.astera.wallet.model.ServiceResult
import com.astera.wallet.model.UserProfile
import com.astera.wallet.util.OperationType
import com.astera.wallet.util.PinHasher
import com.astera.wallet.util.currentMonthKey
import com.astera.wallet.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.astera.wallet.util.generateWalletAccountNumber

class AuthService(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

  fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signInWithEmailPassword(email: String, password: String): ServiceResult<FirebaseUser> {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return ServiceResult.Error("Masukkan alamat email yang valid.")
    }
    if (password.isBlank()) return ServiceResult.Error("Kata sandi wajib diisi.")
    return try {
      val user = auth.signInWithEmailAndPassword(cleanEmail, password).await().user
        ?: return ServiceResult.Error("Sesi pengguna tidak ditemukan.")
      ServiceResult.Success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Email/password sign-in failed", e)
      ServiceResult.Error(e.localizedMessage ?: "Email atau kata sandi tidak cocok.")
    }
  }

  suspend fun registerWithEmailPassword(
    email: String,
    password: String,
    name: String,
    phone: String,
    pin: String,
  ): ServiceResult<UserProfile> {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return ServiceResult.Error("Masukkan alamat email yang valid.")
    }
    if (password.length < 8) return ServiceResult.Error("Kata sandi minimal 8 karakter.")
    return try {
      val user = auth.createUserWithEmailAndPassword(cleanEmail, password).await().user
        ?: return ServiceResult.Error("Gagal membuat akun.")
      completeProfileRegistration(user.uid, cleanEmail, name, phone, pin)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Email/password registration failed", e)
      ServiceResult.Error(e.localizedMessage ?: "Gagal membuat akun. Coba lagi.")
    }
  }

  suspend fun sendPasswordReset(email: String): ServiceResult<Unit> {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return ServiceResult.Error("Masukkan alamat email yang valid.")
    }
    return try {
      auth.sendPasswordResetEmail(cleanEmail).await()
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ServiceResult.Error(e.localizedMessage ?: "Email reset kata sandi gagal dikirim.")
    }
  }

  suspend fun completeProfileRegistration(
    uid: String,
    email: String,
    name: String,
    phone: String,
    pin: String,
  ): ServiceResult<UserProfile> {
    val cleanName = name.trim()
    val cleanPhone = phone.trim()
    if (cleanName.isEmpty()) return ServiceResult.Error("Nama lengkap wajib diisi.")
    if (!cleanPhone.matches(Regex("^08[0-9]{8,11}$"))) {
      return ServiceResult.Error("Nomor HP harus diawali 08 dan terdiri dari 10-13 digit.")
    }
    if (!pin.matches(Regex("^[0-9]{6}$"))) {
      return ServiceResult.Error("PIN harus terdiri dari 6 digit angka.")
    }

    val pinHash = PinHasher.hashPin(pin)
    val accountNumber = generateWalletAccountNumber()
    val userRef = db.collection("users").document(uid)
    val dirRef = db.collection("user_directory").document(uid)
    val secretRef = db.collection("user_secrets").document(uid)
    val budgetRef = db.collection("budgets").document(uid)
    val bankAlphaRef = db.collection("bank_accounts").document("${uid}_alpha")
    val bankBetaRef = db.collection("bank_accounts").document("${uid}_beta")

    val userData =
      mapOf(
        "uid" to uid,
        "name" to cleanName,
        "phone" to cleanPhone,
        "email" to email.ifBlank { "$uid@astera.sim" },
        "accountNumber" to accountNumber,
        "balance" to INITIAL_WALLET_BALANCE,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    val dirData =
      mapOf(
        "uid" to uid,
        "name" to cleanName,
        "phone" to cleanPhone,
        "accountNumber" to accountNumber,
        "searchable" to true,
      )
    val secretData =
      mapOf(
        "uid" to uid,
        "pinHash" to pinHash,
        "failedAttempts" to 0L,
        "lockUntilMs" to 0L,
        "blocked" to false,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    val budgetData =
      mapOf(
        "uid" to uid,
        "monthlyLimit" to DEFAULT_MONTHLY_LIMIT,
        "monthKey" to currentMonthKey(),
        "spentThisMonth" to 0L,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    val alphaData =
      mapOf(
        "ownerId" to uid,
        "bankName" to "Bank Alpha",
        "accountNumber" to "1001 4821 0901",
        "balance" to INITIAL_BANK_BALANCE,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    val betaData =
      mapOf(
        "ownerId" to uid,
        "bankName" to "Bank Beta",
        "accountNumber" to "2002 7734 5512",
        "balance" to INITIAL_BANK_BALANCE,
        "createdAt" to FieldValue.serverTimestamp(),
      )

    return try {
      db.runBatch { batch ->
          batch.set(userRef, userData)
          batch.set(dirRef, dirData)
          batch.set(secretRef, secretData)
          batch.set(budgetRef, budgetData)
          batch.set(bankAlphaRef, alphaData)
          batch.set(bankBetaRef, betaData)
        }
        .await()

      ServiceResult.Success(
        UserProfile(
          id = uid,
          uid = uid,
          name = cleanName,
          phone = cleanPhone,
          email = email.ifBlank { "$uid@astera.sim" },
          accountNumber = accountNumber,
          balance = INITIAL_WALLET_BALANCE,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, userRef.path)
      ServiceResult.Error("Gagal menyimpan profil akun. Coba lagi.")
    }
  }

  fun signOut(): ServiceResult<Unit> {
    auth.signOut()
    return ServiceResult.Success(Unit)
  }

  companion object {
    private const val TAG = "AuthService"
    const val INITIAL_WALLET_BALANCE = 10_000_000L
    const val INITIAL_BANK_BALANCE = 5_000_000L
    const val DEFAULT_MONTHLY_LIMIT = 1_000_000L
  }
}
