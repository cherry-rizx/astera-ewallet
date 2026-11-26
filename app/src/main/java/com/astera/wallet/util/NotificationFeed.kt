package com.astera.wallet.util

import com.astera.wallet.model.AppNotification
import com.astera.wallet.model.MonthlyBudget
import com.astera.wallet.model.NotificationCategory
import com.astera.wallet.model.NotificationKind
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.model.UserProfile
import com.astera.wallet.model.UserSecret
import com.astera.wallet.service.NotificationSettings
import com.google.firebase.Timestamp
import com.astera.wallet.model.isExternalTransfer
import com.astera.wallet.model.isTopUp

/**
 * Menyusun daftar notifikasi di sisi aplikasi dari data Firestore yang sudah dipantau, sehingga
 * tidak perlu koleksi atau Security Rules baru. Pengaturan notifikasi pengguna ikut dihormati.
 */
fun buildNotificationFeed(
  currentUserId: String,
  profile: UserProfile?,
  transactions: List<TransactionRecord>,
  budget: MonthlyBudget?,
  secret: UserSecret?,
  settings: NotificationSettings,
): List<AppNotification> {
  val items = mutableListOf<AppNotification>()

  if (settings.transactionAlerts) {
    transactions.take(MAX_TRANSACTION_ITEMS).forEach { tx ->
      items += transactionNotification(currentUserId, tx)
    }
  }

  if (settings.budgetWarnings && budget != null && budget.monthlyLimit > 0L) {
    val ratio = budget.spentThisMonth.toDouble() / budget.monthlyLimit.toDouble()
    if (ratio >= 0.8) {
      items +=
        AppNotification(
          id = "budget_${budget.monthKey}",
          category = NotificationCategory.INFO,
          kind = NotificationKind.BUDGET,
          title = if (ratio >= 1.0) "Anggaran Terlampaui" else "Peringatan Anggaran",
          body =
            "Pengeluaran bulan ini ${formatRupiah(budget.spentThisMonth)} dari batas ${formatRupiah(budget.monthlyLimit)}.",
          timestamp = budget.updatedAt,
        )
    }
  }

  if (settings.securityAlerts && secret != null) {
    when {
      secret.blocked ->
        items +=
          AppNotification(
            id = "security_blocked",
            category = NotificationCategory.INFO,
            kind = NotificationKind.SECURITY,
            title = "Akun Diblokir",
            body = "Akun diblokir setelah 3 kali salah PIN. Hubungi petugas ASTERA untuk membuka blokir.",
            timestamp = secret.updatedAt,
          )
      secret.failedAttempts > 0 ->
        items +=
          AppNotification(
            id = "security_failed_${secret.failedAttempts}",
            category = NotificationCategory.INFO,
            kind = NotificationKind.SECURITY,
            title = "Peringatan Keamanan",
            body = "Terdeteksi ${secret.failedAttempts} kali PIN salah. Jika bukan kamu, segera ubah PIN di menu Akun.",
            timestamp = secret.updatedAt,
          )
    }
  }

  // Informasi tetap dari ASTERA (tidak dihitung sebagai belum dibaca).
  val joinedAt = profile?.createdAt
  items +=
    AppNotification(
      id = "promo_multibank",
      category = NotificationCategory.PROMO,
      kind = NotificationKind.PROMO,
      title = "Coba Top Up Multi-Bank",
      body = "Isi saldo dari Bank Alpha dan Bank Beta sekaligus. Semua saldo di ASTERA adalah simulasi.",
      timestamp = joinedAt,
      countsAsUnread = false,
    )
  items +=
    AppNotification(
      id = "reminder_history",
      category = NotificationCategory.INFO,
      kind = NotificationKind.REMINDER,
      title = "Pengingat",
      body = "Jangan lupa cek riwayat dan ringkasan pengeluaranmu secara berkala.",
      timestamp = joinedAt,
      countsAsUnread = false,
    )
  items +=
    AppNotification(
      id = "info_welcome",
      category = NotificationCategory.INFO,
      kind = NotificationKind.INFO,
      title = "Info ASTERA",
      body = "Selamat datang di ASTERA! Yuk, atur saldo dengan bijak.",
      timestamp = joinedAt,
      countsAsUnread = false,
    )

  return items.sortedByDescending { it.timestamp ?: Timestamp(0, 0) }
}

/** Jumlah notifikasi penting yang lebih baru dari waktu terakhir layar Notifikasi dibuka. */
fun countUnreadNotifications(feed: List<AppNotification>, lastSeenMs: Long): Int =
  feed.count { it.countsAsUnread && (it.timestamp?.toDate()?.time ?: 0L) > lastSeenMs }

private fun transactionNotification(currentUserId: String, tx: TransactionRecord): AppNotification {
  val amount = formatRupiah(tx.amount)
  return when {
    tx.isExternalTransfer() ->
      AppNotification(
        id = "tx_${tx.id}",
        category = NotificationCategory.TRANSAKSI,
        kind = NotificationKind.TRANSFER_OUT,
        title = "Transfer Berhasil",
        body = "$amount ke ${tx.receiverName} (${tx.destinationProvider})",
        timestamp = tx.createdAt,
        transactionId = tx.id,
      )
    tx.isTopUp() ->
      AppNotification(
        id = "tx_${tx.id}",
        category = NotificationCategory.TRANSAKSI,
        kind = NotificationKind.TOP_UP,
        title = "Top Up Berhasil",
        body =
          if (tx.splitDetail.isEmpty()) "$amount masuk ke Dompet ASTERA"
          else "$amount dari ${tx.splitDetail.joinToString { it.bankName }}",
        timestamp = tx.createdAt,
        transactionId = tx.id,
      )
    tx.receiverId == currentUserId ->
      AppNotification(
        id = "tx_${tx.id}",
        category = NotificationCategory.TRANSAKSI,
        kind = NotificationKind.TRANSFER_IN,
        title = "Uang Masuk",
        body = "${tx.senderName} mengirim $amount",
        timestamp = tx.createdAt,
        transactionId = tx.id,
      )
    else ->
      AppNotification(
        id = "tx_${tx.id}",
        category = NotificationCategory.TRANSAKSI,
        kind = NotificationKind.TRANSFER_OUT,
        title = "Transfer Berhasil",
        body = "$amount ke ${tx.receiverName}",
        timestamp = tx.createdAt,
        transactionId = tx.id,
      )
  }
}

private const val MAX_TRANSACTION_ITEMS = 30
