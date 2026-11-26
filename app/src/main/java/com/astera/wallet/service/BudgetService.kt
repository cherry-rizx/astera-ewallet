package com.astera.wallet.service

import com.astera.wallet.model.MonthlyBudget
import com.astera.wallet.model.ServiceResult
import com.astera.wallet.util.OperationType
import com.astera.wallet.util.currentMonthKey
import com.astera.wallet.util.friendlyFirestoreMessage
import com.astera.wallet.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BudgetService(private val db: FirebaseFirestore) {

  fun observeBudget(uid: String): Flow<MonthlyBudget?> =
    db.collection("budgets")
      .document(uid)
      .snapshots()
      .map { snap ->
        if (!snap.exists()) null
        else {
          val raw = snap.toObject(MonthlyBudget::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          val nowMonth = currentMonthKey()
          if (raw != null && raw.monthKey != nowMonth) {
            raw.copy(monthKey = nowMonth, spentThisMonth = 0L)
          } else {
            raw
          }
        }
      }

  suspend fun ensureCurrentMonthReset(uid: String): ServiceResult<Unit> {
    val budgetRef = db.collection("budgets").document(uid)
    val nowMonth = currentMonthKey()
    return try {
      val snap = budgetRef.get().await()
      if (!snap.exists()) {
        budgetRef
          .set(
            mapOf(
              "uid" to uid,
              "monthlyLimit" to AuthService.DEFAULT_MONTHLY_LIMIT,
              "monthKey" to nowMonth,
              "spentThisMonth" to 0L,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else if (snap.getString("monthKey") != nowMonth) {
        budgetRef
          .update(
            mapOf(
              "monthKey" to nowMonth,
              "spentThisMonth" to 0L,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, budgetRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal memeriksa anggaran bulanan."))
    }
  }

  suspend fun updateMonthlyLimit(uid: String, newLimit: Long): ServiceResult<Unit> {
    if (newLimit < 0L || newLimit > 1_000_000_000L) {
      return ServiceResult.Error("Batas anggaran tidak valid.")
    }
    val budgetRef = db.collection("budgets").document(uid)
    val nowMonth = currentMonthKey()
    return try {
      val snap = budgetRef.get().await()
      if (!snap.exists()) {
        budgetRef
          .set(
            mapOf(
              "uid" to uid,
              "monthlyLimit" to newLimit,
              "monthKey" to nowMonth,
              "spentThisMonth" to 0L,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val prevMonth = snap.getString("monthKey") ?: ""
        val spent = if (prevMonth == nowMonth) (snap.getLong("spentThisMonth") ?: 0L) else 0L
        budgetRef
          .update(
            mapOf(
              "monthlyLimit" to newLimit,
              "monthKey" to nowMonth,
              "spentThisMonth" to spent,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      ServiceResult.Success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, budgetRef.path)
      ServiceResult.Error(friendlyFirestoreMessage(e, "Gagal menyimpan batas anggaran."))
    }
  }
}
