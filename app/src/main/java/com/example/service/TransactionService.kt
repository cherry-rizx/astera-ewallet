package com.example.service

import com.example.model.BankAccount
import com.example.model.ServiceResult
import com.example.model.SplitDetailItem
import com.example.model.TransactionRecord
import com.example.util.OperationType
import com.example.util.calculateMultiBankSplit
import com.example.util.currentMonthKey
import com.example.util.currentDayKey
import com.example.util.friendlyFirestoreMessage
import com.example.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TransactionService(private val db: FirebaseFirestore) {

  fun observeTransactions(uid: String): Flow<List<TransactionRecord>> {
    val sentFlow =
      db.collection("transactions")
        .whereEqualTo("senderId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(TransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
    val receivedFlow =
      db.collection("transactions")
        .whereEqualTo("receiverId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(TransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    return combine(sentFlow, receivedFlow) { sent, received ->
      (sent + received)
        .distinctBy { it.id }
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }
  }

  suspend fun executeTransfer(
    senderId: String,
    senderName: String,
    receiverId: String,
    receiverName: String,
    amount: Long,
    note: String,
    outOfBudget: Boolean,
  ): ServiceResult<TransactionRecord> {
    if (amount <= 0L) return ServiceResult.Error("Nominal harus lebih dari Rp 0.")
    if (amount > MAX_TRANSFER_AMOUNT) {
      return ServiceResult.Error("Batas maksimal per transaksi adalah Rp 5.000.000.")
    }
    if (senderId == receiverId) {
      return ServiceResult.Error("Tidak dapat mentransfer ke akun sendiri.")
    }

    val senderRef = db.collection("users").document(senderId)
    val receiverRef = db.collection("users").document(receiverId)
    val budgetRef = db.collection("budgets").document(senderId)
    val dayKeyNow = currentDayKey()
    val dailyRef = db.collection("daily_transfer_limits").document("${senderId}_$dayKeyNow")
    val txRef = db.collection("transactions").document()
    val monthKeyNow = currentMonthKey()
    val cleanNote = note.trim().take(200)

    return try {
      db.runTransaction { transaction ->
          val senderSnap = transaction.get(senderRef)
          if (!senderSnap.exists()) {
            throw IllegalStateException("Profil pengirim tidak ditemukan.")
          }
          val senderBalance = senderSnap.getLong("balance") ?: 0L
          if (senderBalance < amount) {
            throw IllegalStateException("Saldo dompet tidak mencukupi.")
          }

          val budgetSnap = transaction.get(budgetRef)
          val dailySnap = transaction.get(dailyRef)
          val dailyTransferred = dailySnap.getLong("transferredAmount") ?: 0L
          if (dailyTransferred + amount > MAX_DAILY_TRANSFER_AMOUNT) {
            throw IllegalStateException("Batas transfer harian Rp 5.000.000 telah tercapai.")
          }
          val prevMonth = budgetSnap.getString("monthKey") ?: ""
          val prevSpent = if (prevMonth == monthKeyNow) (budgetSnap.getLong("spentThisMonth") ?: 0L) else 0L
          val monthlyLimit = budgetSnap.getLong("monthlyLimit") ?: AuthService.DEFAULT_MONTHLY_LIMIT

          // All writes strictly after all reads
          transaction.update(
            senderRef,
            mapOf("balance" to senderBalance - amount, "lastTransactionId" to txRef.id),
          )
          transaction.update(
            receiverRef,
            mapOf("balance" to FieldValue.increment(amount), "lastTransactionId" to txRef.id),
          )

          val dailyData =
            mapOf(
              "uid" to senderId,
              "dayKey" to dayKeyNow,
              "transferredAmount" to dailyTransferred + amount,
              "lastTransactionId" to txRef.id,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          if (dailySnap.exists()) transaction.update(dailyRef, dailyData) else transaction.set(dailyRef, dailyData)

          if (budgetSnap.exists()) {
            transaction.update(
              budgetRef,
              mapOf(
                "monthKey" to monthKeyNow,
                "spentThisMonth" to prevSpent + amount,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          } else {
            transaction.set(
              budgetRef,
              mapOf(
                "uid" to senderId,
                "monthlyLimit" to monthlyLimit,
                "monthKey" to monthKeyNow,
                "spentThisMonth" to amount,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }

          val txPayload =
            mapOf(
              "senderId" to senderId,
              "receiverId" to receiverId,
              "senderName" to senderName,
              "receiverName" to receiverName,
              "type" to "transfer",
              "amount" to amount,
              "note" to cleanNote,
              "outOfBudget" to outOfBudget,
              "dayKey" to dayKeyNow,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          transaction.set(txRef, txPayload)
          null
        }
        .await()

      ServiceResult.Success(
        TransactionRecord(
          id = txRef.id,
          senderId = senderId,
          receiverId = receiverId,
          senderName = senderName,
          receiverName = receiverName,
          type = "transfer",
          amount = amount,
          note = cleanNote,
          outOfBudget = outOfBudget,
          createdAt = Timestamp.now(),
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: IllegalStateException) {
      ServiceResult.Error(e.message ?: "Transfer gagal diproses.")
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Transfer gagal diproses. Coba lagi."))
    }
  }

  suspend fun executeTopUp(
    uid: String,
    userName: String,
    amount: Long,
    selectedBankIds: List<String>,
    outOfBudget: Boolean,
  ): ServiceResult<TransactionRecord> {
    if (amount <= 0L) return ServiceResult.Error("Nominal harus lebih dari Rp 0.")
    if (amount > MAX_TOPUP_AMOUNT) {
      return ServiceResult.Error("Batas maksimal top up adalah Rp 1.000.000.")
    }
    if (selectedBankIds.isEmpty()) {
      return ServiceResult.Error("Pilih minimal satu rekening bank.")
    }

    val userRef = db.collection("users").document(uid)
    val budgetRef = db.collection("budgets").document(uid)
    val txRef = db.collection("transactions").document()
    val bankRefs = selectedBankIds.map { db.collection("bank_accounts").document(it) }
    val monthKeyNow = currentMonthKey()

    return try {
      val computedSplit =
        db.runTransaction { transaction ->
            val userSnap = transaction.get(userRef)
            if (!userSnap.exists()) {
              throw IllegalStateException("Profil pengguna tidak ditemukan.")
            }
            val currentBalance = userSnap.getLong("balance") ?: 0L

            val liveBanks =
              bankRefs.map { ref ->
                val snap = transaction.get(ref)
                if (!snap.exists()) {
                  throw IllegalStateException("Rekening bank tidak ditemukan.")
                }
                BankAccount(
                  id = snap.id,
                  ownerId = snap.getString("ownerId") ?: "",
                  bankName = snap.getString("bankName") ?: "Bank",
                  accountNumber = snap.getString("accountNumber") ?: "",
                  balance = snap.getLong("balance") ?: 0L,
                )
              }

            val splitResult = calculateMultiBankSplit(amount, liveBanks)
            if (splitResult is ServiceResult.Error) {
              throw IllegalStateException(splitResult.message)
            }
            val splitDetails = (splitResult as ServiceResult.Success).data

            val budgetSnap = transaction.get(budgetRef)
            val prevMonth = budgetSnap.getString("monthKey") ?: ""
            val prevSpent = if (prevMonth == monthKeyNow) (budgetSnap.getLong("spentThisMonth") ?: 0L) else 0L
            val monthlyLimit = budgetSnap.getLong("monthlyLimit") ?: AuthService.DEFAULT_MONTHLY_LIMIT

            // All writes after reads
            splitDetails.forEach { detail ->
              val bank = liveBanks.first { it.id == detail.bankId }
              val bankRef = db.collection("bank_accounts").document(detail.bankId)
              transaction.update(
                bankRef,
                mapOf(
                  "balance" to bank.balance - detail.amount,
                  "lastTransactionId" to txRef.id,
                ),
              )
            }

            transaction.update(
              userRef,
              mapOf("balance" to currentBalance + amount, "lastTransactionId" to txRef.id),
            )

            if (budgetSnap.exists()) {
              transaction.update(
                budgetRef,
                mapOf(
                  "monthKey" to monthKeyNow,
                  "spentThisMonth" to prevSpent + amount,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            } else {
              transaction.set(
                budgetRef,
                mapOf(
                  "uid" to uid,
                  "monthlyLimit" to monthlyLimit,
                  "monthKey" to monthKeyNow,
                  "spentThisMonth" to amount,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }

            val splitMaps =
              splitDetails.map { item ->
                mapOf(
                  "bankId" to item.bankId,
                  "bankName" to item.bankName,
                  "amount" to item.amount,
                )
              }
            val txPayload =
              mapOf(
                "senderId" to uid,
                "receiverId" to uid,
                "senderName" to userName,
                "receiverName" to "Dompet ASTERA",
                "type" to "topup",
                "amount" to amount,
                "note" to "Top Up Simulasi Multi-Bank",
                "outOfBudget" to outOfBudget,
                "splitDetail" to splitMaps,
                "createdAt" to FieldValue.serverTimestamp(),
              )
            transaction.set(txRef, txPayload)
            splitDetails
          }
          .await()

      ServiceResult.Success(
        TransactionRecord(
          id = txRef.id,
          senderId = uid,
          receiverId = uid,
          senderName = userName,
          receiverName = "Dompet ASTERA",
          type = "topup",
          amount = amount,
          note = "Top Up Simulasi Multi-Bank",
          outOfBudget = outOfBudget,
          splitDetail = computedSplit,
          createdAt = Timestamp.now(),
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: IllegalStateException) {
      ServiceResult.Error(e.message ?: "Top up gagal diproses.")
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Top up gagal diproses. Coba lagi."))
    }
  }

  companion object {
    const val MAX_TRANSFER_AMOUNT = 5_000_000L
    const val MAX_DAILY_TRANSFER_AMOUNT = 5_000_000L
    const val MAX_TOPUP_AMOUNT = 1_000_000L
  }
}
