package com.astera.wallet.util

import com.astera.wallet.model.TransactionRecord
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Total satu bulan. Pengeluaran = transfer keluar; top up dicatat terpisah karena saldo pindah dari bank sendiri. */
data class MonthlySpending(
  val monthKey: String,
  val outgoing: Long,
  val incoming: Long,
  val topUp: Long,
)

data class RecipientTotal(val name: String, val amount: Long, val count: Int)

/** Daftar [months] bulan terakhir, dari yang paling lama ke bulan berjalan (zona Asia/Jakarta). */
fun lastMonthKeys(months: Int, now: Date = Date()): List<String> {
  val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
    time = now
    set(Calendar.DAY_OF_MONTH, 1)
  }
  return (months - 1 downTo 0).map { offset ->
    val c = cal.clone() as Calendar
    c.add(Calendar.MONTH, -offset)
    currentMonthKey(c.time)
  }
}

fun buildMonthlySpending(
  currentUserId: String,
  transactions: List<TransactionRecord>,
  months: Int = 6,
  now: Date = Date(),
): List<MonthlySpending> {
  val keys = lastMonthKeys(months, now)
  val byMonth = transactions.groupBy { tx -> tx.createdAt?.toDate()?.let { currentMonthKey(it) } }
  return keys.map { key ->
    val txs = byMonth[key].orEmpty()
    MonthlySpending(
      monthKey = key,
      outgoing = txs.filter { it.isOutgoingTransfer(currentUserId) }.sumOf { it.amount },
      incoming = txs.filter { it.type == "transfer" && it.receiverId == currentUserId && it.senderId != currentUserId }.sumOf { it.amount },
      topUp = txs.filter { it.type == "topup" && it.senderId == currentUserId }.sumOf { it.amount },
    )
  }
}

/** Penerima dengan total transfer terbesar pada [monthKey]. */
fun topRecipients(
  currentUserId: String,
  transactions: List<TransactionRecord>,
  monthKey: String,
  limit: Int = 3,
): List<RecipientTotal> =
  transactions
    .filter { it.isOutgoingTransfer(currentUserId) && it.createdAt?.toDate()?.let(::currentMonthKey) == monthKey }
    .groupBy { it.receiverId }
    .map { (_, txs) -> RecipientTotal(txs.first().receiverName, txs.sumOf { it.amount }, txs.size) }
    .sortedByDescending { it.amount }
    .take(limit)

private fun TransactionRecord.isOutgoingTransfer(uid: String): Boolean =
  type == "transfer" && senderId == uid && receiverId != uid
