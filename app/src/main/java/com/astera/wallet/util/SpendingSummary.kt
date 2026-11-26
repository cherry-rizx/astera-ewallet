package com.astera.wallet.util

import com.astera.wallet.model.TransactionRecord
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import com.astera.wallet.model.isExternalTransfer
import com.astera.wallet.model.isIncomingFor
import com.astera.wallet.model.isOutgoingFor
import com.astera.wallet.model.isTopUp

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
      outgoing = txs.filter { it.isOutgoingFor(currentUserId) }.sumOf { it.amount },
      incoming = txs.filter { it.isIncomingFor(currentUserId) && !it.isTopUp() }.sumOf { it.amount },
      topUp = txs.filter { it.isTopUp() && it.senderId == currentUserId }.sumOf { it.amount },
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
    .filter { it.isOutgoingFor(currentUserId) && it.createdAt?.toDate()?.let(::currentMonthKey) == monthKey }
    // Penerima ASTERA dikelompokkan per akun; tujuan luar per bank/e-wallet + nomor.
    .groupBy { if (it.isExternalTransfer()) "${it.destinationProvider}:${it.destinationAccount}" else it.receiverId }
    .map { (_, txs) ->
      val first = txs.first()
      val name = if (first.isExternalTransfer()) "${first.receiverName} (${first.destinationProvider})" else first.receiverName
      RecipientTotal(name, txs.sumOf { it.amount }, txs.size)
    }
    .sortedByDescending { it.amount }
    .take(limit)
