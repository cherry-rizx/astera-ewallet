package com.astera.wallet.model

import com.astera.wallet.util.formatAccountNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferDestinationTest {
  private val me = "me"

  @Test
  fun direction_isCorrectForEachTransactionType() {
    val external = TransactionRecord(senderId = me, receiverId = TransactionTypes.EXTERNAL_RECEIVER_ID, type = TransactionTypes.EXTERNAL_TRANSFER)
    val incoming = TransactionRecord(senderId = "b", receiverId = me, type = TransactionTypes.TRANSFER)
    val outgoing = TransactionRecord(senderId = me, receiverId = "b", type = TransactionTypes.TRANSFER)
    val topUp = TransactionRecord(senderId = me, receiverId = me, type = TransactionTypes.TOPUP)

    assertTrue(external.isOutgoingFor(me))
    assertFalse(external.isIncomingFor(me))
    assertTrue(incoming.isIncomingFor(me))
    assertFalse(incoming.isOutgoingFor(me))
    assertTrue(outgoing.isOutgoingFor(me))
    assertFalse(outgoing.isIncomingFor(me))
    assertTrue(topUp.isIncomingFor(me))
    assertFalse(topUp.isOutgoingFor(me))
  }

  @Test
  fun providers_andAccountFormatting() {
    assertTrue("BCA" in TransferProviders.forType(DestinationType.BANK))
    assertTrue("GoPay" in TransferProviders.forType(DestinationType.EWALLET))
    assertEquals(DestinationType.EWALLET, DestinationType.from("ewallet"))
    assertEquals("3067 2519 4800", formatAccountNumber("306725194800"))
  }
}
