package com.astera.wallet.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface ServiceResult<out T> {
  data class Success<T>(val data: T) : ServiceResult<T>

  data class Error(val message: String) : ServiceResult<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val name: String = "",
  val phone: String = "",
  val email: String = "",
  /** Nomor rekening Dompet ASTERA, 12 digit acak (bukan nomor HP). */
  val accountNumber: String = "",
  val balance: Long = 0L,
  val photoUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserDirectoryEntry(
  @DocumentId val id: String = "",
  val uid: String = "",
  val name: String = "",
  val phone: String = "",
  val accountNumber: String = "",
  val searchable: Boolean = true,
)

@IgnoreExtraProperties
data class UserSecret(
  @DocumentId val id: String = "",
  val uid: String = "",
  val pinHash: String = "",
  val failedAttempts: Long = 0L,
  val lockUntilMs: Long = 0L,
  val blocked: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SplitDetailItem(
  val bankId: String = "",
  val bankName: String = "",
  val amount: Long = 0L,
)

@IgnoreExtraProperties
data class TransactionRecord(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val receiverId: String = "",
  val senderName: String = "",
  val receiverName: String = "",
  val type: String = "transfer",
  val amount: Long = 0L,
  val note: String = "",
  val outOfBudget: Boolean = false,
  val splitDetail: List<SplitDetailItem> = emptyList(),
  /** Hanya untuk transfer keluar: "bank" atau "ewallet". */
  val destinationType: String = "",
  /** Hanya untuk transfer keluar: nama bank / e-wallet tujuan, mis. "BCA". */
  val destinationProvider: String = "",
  /** Hanya untuk transfer keluar: nomor rekening / nomor e-wallet tujuan. */
  val destinationAccount: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MonthlyBudget(
  @DocumentId val id: String = "",
  val uid: String = "",
  val monthlyLimit: Long = 0L,
  val monthKey: String = "",
  val spentThisMonth: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BankAccount(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val bankName: String = "",
  val accountNumber: String = "",
  val balance: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)
