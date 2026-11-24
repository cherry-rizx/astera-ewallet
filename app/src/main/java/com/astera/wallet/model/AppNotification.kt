package com.astera.wallet.model

import com.google.firebase.Timestamp

/** Kategori sesuai tab di layar Notifikasi. */
enum class NotificationCategory(val label: String) {
  TRANSAKSI("Transaksi"),
  PROMO("Promo"),
  INFO("Info"),
}

enum class NotificationKind {
  TRANSFER_OUT,
  TRANSFER_IN,
  TOP_UP,
  BUDGET,
  SECURITY,
  PROMO,
  REMINDER,
  INFO,
}

/** Satu baris di layar Notifikasi. Disusun dari data yang sudah ada (transaksi, anggaran, PIN). */
data class AppNotification(
  val id: String,
  val category: NotificationCategory,
  val kind: NotificationKind,
  val title: String,
  val body: String,
  val timestamp: Timestamp?,
  /** Diisi untuk notifikasi transaksi agar bisa membuka detail transaksi. */
  val transactionId: String? = null,
  /** Notifikasi penting (transaksi, anggaran, keamanan) dihitung sebagai "belum dibaca". */
  val countsAsUnread: Boolean = true,
)
