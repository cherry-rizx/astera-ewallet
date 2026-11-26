package com.astera.wallet.nfc

import com.astera.wallet.util.QrUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsteraNfcProtocolTest {
  @Test
  fun selectApdu_isRecognizedByService() {
    val apdu = AsteraNfcProtocol.buildSelectApdu()
    assertTrue(AsteraNfcProtocol.isSelectAidApdu(apdu))
    assertEquals("00A4040007F041535445524100", apdu.joinToString("") { "%02X".format(it) })
  }

  @Test
  fun otherApdu_isRejected() {
    val wrongAid = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0xA0.toByte(), 0, 0, 0, 0x03, 0x10, 0x10, 0x00)
    assertFalse(AsteraNfcProtocol.isSelectAidApdu(wrongAid))
    assertFalse(AsteraNfcProtocol.isSelectAidApdu(byteArrayOf(0x00, 0xA4.toByte())))
  }

  @Test
  fun hceResponse_roundTripsToRecipientUid() {
    val uri = QrUtils.buildUserQrUri("abcUID_123")
    val response = uri.toByteArray(Charsets.UTF_8) + AsteraNfcProtocol.STATUS_OK
    val payload = AsteraNfcProtocol.parseResponse(response)
    assertEquals(uri, payload)
    assertEquals("abcUID_123", QrUtils.parseAsteraQrUid(payload!!))
  }

  @Test
  fun errorStatus_returnsNull() {
    assertNull(AsteraNfcProtocol.parseResponse(AsteraNfcProtocol.STATUS_NOT_AVAILABLE))
    assertNull(AsteraNfcProtocol.parseResponse(null))
  }
}
