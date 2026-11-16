package com.example.service

import com.example.model.BankAccount
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BankService(private val db: FirebaseFirestore) {

  fun observeBankAccounts(uid: String): Flow<List<BankAccount>> =
    db.collection("bank_accounts")
      .whereEqualTo("ownerId", uid)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(BankAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.bankName }
      }
}
