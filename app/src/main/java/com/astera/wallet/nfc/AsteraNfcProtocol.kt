package com.astera.wallet.nfc

/**
 * Protokol NFC antar-HP ASTERA (Host Card Emulation).
 *
 * HP penerima berperan sebagai "kartu": [AsteraHceService] menjawab perintah SELECT AID dengan
 * URI QR ASTERA miliknya ("astera://user/{uid}"). HP pengirim membaca dengan reader mode
 * ([rememberNfcRecipientReader]). AID memakai prefiks proprietary F0 + "ASTERA" dalam hex.
 */
object AsteraNfcProtocol {
  const val AID_HEX = "F0415354455241"

  val AID: ByteArray = hexToBytes(AID_HEX)

  val STATUS_OK = byteArrayOf(0x90.toByte(), 0x00)
  val STATUS_NOT_AVAILABLE = byteArrayOf(0x6A.toByte(), 0x82.toByte())

  /** APDU: SELECT by AID (00 A4 04 00 Lc AID Le). */
  fun buildSelectApdu(): ByteArray =
    byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte()) + AID + byteArrayOf(0x00)

  fun isSelectAidApdu(apdu: ByteArray): Boolean {
    if (apdu.size < 5 + AID.size) return false
    if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte() || apdu[2] != 0x04.toByte()) return false
    val lc = apdu[4].toInt() and 0xFF
    if (lc != AID.size) return false
    return apdu.copyOfRange(5, 5 + lc).contentEquals(AID)
  }

  /** Mengambil payload teks dari respons, atau null jika status bukan 90 00. */
  fun parseResponse(response: ByteArray?): String? {
    if (response == null || response.size < 2) return null
    val sw1 = response[response.size - 2]
    val sw2 = response[response.size - 1]
    if (sw1 != STATUS_OK[0] || sw2 != STATUS_OK[1]) return null
    return String(response, 0, response.size - 2, Charsets.UTF_8)
  }

  private fun hexToBytes(hex: String): ByteArray =
    ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}

/**
 * Status berbagi NFC di proses aplikasi. Hanya terisi saat pengguna membuka tab "Tunjukkan QR"
 * dan menyalakan "Terima via NFC", sehingga HP tidak membagikan ID dompet di luar momen itu.
 */
object NfcShareState {
  @Volatile var sharedPayload: String? = null
}
