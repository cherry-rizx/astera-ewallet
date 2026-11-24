package com.astera.wallet.util

import com.astera.wallet.model.TransactionRecord
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingSummaryTest {
  private val me = "me"

  private fun jakarta(year: Int, month: Int, day: Int): Date =
    Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
      clear()
      set(year, month - 1, day, 10, 0)
    }.time

  private fun tx(id: String, type: String, sender: String, receiver: String, amount: Long, date: Date, receiverName: String = "Budi") =
    TransactionRecord(
      id = id, senderId = sender, receiverId = receiver, senderName = "S", receiverName = receiverName,
      type = type, amount = amount, createdAt = Timestamp(date),
    )

  @Test
  fun lastMonthKeys_crossesYearBoundary() {
    assertEquals(
      listOf("2026-08", "2026-09", "2026-10", "2026-11", "2026-12", "2027-01"),
      lastMonthKeys(6, jakarta(2027, 1, 15)),
    )
  }

  @Test
  fun buildMonthlySpending_separatesOutgoingIncomingAndTopUp() {
    val now = jakarta(2026, 11, 24)
    val txs = listOf(
      tx("1", "transfer", me, "budi", 500_000, jakarta(2026, 11, 2)),
      tx("2", "transfer", me, "sari", 250_000, jakarta(2026, 11, 20)),
      tx("3", "transfer", "budi", me, 100_000, jakarta(2026, 11, 3)),
      tx("4", "topup", me, me, 300_000, jakarta(2026, 11, 4)),
      tx("5", "transfer", me, "budi", 75_000, jakarta(2026, 10, 9)),
      tx("6", "transfer", me, "budi", 999_000, jakarta(2026, 3, 1)), // di luar 6 bulan
    )
    val months = buildMonthlySpending(me, txs, months = 6, now = now)

    assertEquals(6, months.size)
    assertEquals("2026-11", months.last().monthKey)
    assertEquals(750_000L, months.last().outgoing)
    assertEquals(100_000L, months.last().incoming)
    assertEquals(300_000L, months.last().topUp)
    assertEquals(75_000L, months[4].outgoing)
    assertEquals(0L, months.first().outgoing)
  }

  @Test
  fun topRecipients_sumsPerReceiverAndSortsDescending() {
    val txs = listOf(
      tx("1", "transfer", me, "budi", 100_000, jakarta(2026, 11, 2), "Budi"),
      tx("2", "transfer", me, "budi", 150_000, jakarta(2026, 11, 5), "Budi"),
      tx("3", "transfer", me, "sari", 400_000, jakarta(2026, 11, 6), "Sari"),
      tx("4", "transfer", me, "andi", 999_000, jakarta(2026, 10, 6), "Andi"),
    )
    val top = topRecipients(me, txs, "2026-11")
    assertEquals(listOf("Sari", "Budi"), top.map { it.name })
    assertEquals(250_000L, top[1].amount)
    assertEquals(2, top[1].count)
  }
}
