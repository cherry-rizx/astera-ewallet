package com.astera.wallet.service

import com.astera.wallet.model.ServiceResult
import com.astera.wallet.model.UserDirectoryEntry
import com.astera.wallet.model.UserProfile
import com.astera.wallet.util.OperationType
import com.astera.wallet.util.friendlyFirestoreMessage
import com.astera.wallet.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.astera.wallet.util.generateWalletAccountNumber

class WalletService(private val db: FirebaseFirestore) {

  fun observeUserProfile(uid: String): Flow<UserProfile?> =
    db.collection("users")
      .document(uid)
      .snapshots()
      .map { snapshot ->
        if (!snapshot.exists()) null
        else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

  suspend fun getUserProfile(uid: String): ServiceResult<UserProfile?> {
    val path = "users/$uid"
    return try {
      val snapshot = db.collection("users").document(uid).get().await()
      val profile =
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      ServiceResult.Success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal memuat profil dompet."))
    }
  }

  suspend fun searchRecipients(query: String, excludeUid: String): ServiceResult<List<UserDirectoryEntry>> {
    val path = "user_directory"
    return try {
      val snapshot =
        db.collection(path)
          .whereEqualTo("searchable", true)
          .limit(100)
          .get()
          .await()
      val cleanQuery = query.trim().lowercase()
      val cleanDigits = cleanQuery.filter { it.isDigit() }
      val entries =
        snapshot.toObjects(UserDirectoryEntry::class.java).filter { entry ->
          entry.uid != excludeUid &&
            (cleanQuery.isEmpty() ||
              entry.name.lowercase().contains(cleanQuery) ||
              (cleanDigits.length >= 3 && entry.accountNumber.contains(cleanDigits)) ||
              (cleanDigits.length >= 3 && entry.phone.contains(cleanDigits)))
        }
      ServiceResult.Success(entries)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal mencari penerima."))
    }
  }

  /**
   * Akun lama (dibuat sebelum ada nomor rekening) diberi nomor rekening acak 12 digit sekali saja.
   * Tidak melakukan apa pun jika nomor sudah ada.
   */
  suspend fun ensureAccountNumber(uid: String): ServiceResult<Unit> {
    return try {
      val userRef = db.collection("users").document(uid)
      val snap = userRef.get().await()
      if (!snap.exists() || !snap.getString("accountNumber").isNullOrBlank()) return ServiceResult.Success(Unit)
      val number = generateWalletAccountNumber()
      db.runBatch { batch ->
          batch.update(userRef, "accountNumber", number)
          batch.update(db.collection("user_directory").document(uid), "accountNumber", number)
        }
        .await()
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal membuat nomor rekening."))
    }
  }

  suspend fun getRecipientByUid(uid: String): ServiceResult<UserDirectoryEntry?> {
    val path = "user_directory/$uid"
    return try {
      val snapshot = db.collection("user_directory").document(uid).get().await()
      val entry = if (snapshot.exists()) snapshot.toObject(UserDirectoryEntry::class.java) else null
      ServiceResult.Success(entry)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal membaca kode QR penerima."))
    }
  }
}
