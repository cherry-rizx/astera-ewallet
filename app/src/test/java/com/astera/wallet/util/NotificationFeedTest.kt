package com.astera.wallet.util

import com.astera.wallet.model.MonthlyBudget
import com.astera.wallet.model.NotificationCategory
import com.astera.wallet.model.NotificationKind
import com.astera.wallet.model.SplitDetailItem
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.model.UserSecret
import com.astera.wallet.service.NotificationSettings
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFeedTest {
  private val me = "me"
  private val base = 1_800_000_000_000L

  private fun ts(offsetMin: Long) = Timestamp(Date(base + offsetMin * 60_000L))

  private val transactions = listOf(
    TransactionRecord(id = "out", senderId = me, receiverId = "b", senderName = "Saya", receiverName = "Budi", type = "transfer", amount = 500_000, createdAt = ts(30)),
    TransactionRecord(id = "in", senderId = "b", receiverId = me, senderName = "Budi", receiverName = "Saya", type = "transfer", amount = 250_000, createdAt = ts(20)),
    TransactionRecord(
      id = "top", senderId = me, receiverId = me, senderName = "Saya", receiverName = "Saya", type = "topup", amount = 300_000,
      splitDetail = listOf(SplitDetailItem("a", "Bank Alpha", 150_000), SplitDetailItem("b", "Bank Beta", 150_000)), createdAt = ts(10),
    ),
  )

  @Test
  fun transactions_becomeTypedNotificationsNewestFirst() {
    val feed = buildNotificationFeed(me, null, transactions, null, null, NotificationSettings())
    val txItems = feed.filter { it.category == NotificationCategory.TRANSAKSI }
    assertEquals(listOf(NotificationKind.TRANSFER_OUT, NotificationKind.TRANSFER_IN, NotificationKind.TOP_UP), txItems.map { it.kind })
    assertEquals("Rp 500.000 ke Budi", txItems[0].body)
    assertEquals("Budi mengirim Rp 250.000", txItems[1].body)
    assertEquals("Rp 300.000 dari Bank Alpha, Bank Beta", txItems[2].body)
    assertEquals("out", txItems[0].transactionId)
  }

  @Test
  fun settingsOff_hidesTransactionBudgetAndSecurityItems() {
    val budget = MonthlyBudget(monthlyLimit = 1_000_000, spentThisMonth = 900_000, monthKey = "2026-11", updatedAt = ts(5))
    val secret = UserSecret(failedAttempts = 2, updatedAt = ts(6))
    val off = NotificationSettings(transactionAlerts = false, budgetWarnings = false, securityAlerts = false)
    val feed = buildNotificationFeed(me, null, transactions, budget, secret, off)
    assertTrue(feed.none { it.kind in setOf(NotificationKind.TRANSFER_OUT, NotificationKind.BUDGET, NotificationKind.SECURITY) })

    val on = buildNotificationFeed(me, null, transactions, budget, secret, NotificationSettings())
    assertTrue(on.any { it.kind == NotificationKind.BUDGET && it.title == "Peringatan Anggaran" })
    assertTrue(on.any { it.kind == NotificationKind.SECURITY })
  }

  @Test
  fun unreadCount_onlyCountsImportantItemsAfterLastSeen() {
    val feed = buildNotificationFeed(me, null, transactions, null, null, NotificationSettings())
    assertEquals(3, countUnreadNotifications(feed, lastSeenMs = 0L))
    assertEquals(1, countUnreadNotifications(feed, lastSeenMs = base + 25 * 60_000L))
  }
}
