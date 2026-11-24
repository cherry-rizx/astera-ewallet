package com.astera.wallet.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/** Layanan HCE: menjawab SELECT AID ASTERA dengan URI QR penerima selama mode berbagi aktif. */
class AsteraHceService : HostApduService() {
  override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
    val apdu = commandApdu ?: return AsteraNfcProtocol.STATUS_NOT_AVAILABLE
    if (!AsteraNfcProtocol.isSelectAidApdu(apdu)) return AsteraNfcProtocol.STATUS_NOT_AVAILABLE
    val payload = NfcShareState.sharedPayload ?: return AsteraNfcProtocol.STATUS_NOT_AVAILABLE
    return payload.toByteArray(Charsets.UTF_8) + AsteraNfcProtocol.STATUS_OK
  }

  override fun onDeactivated(reason: Int) = Unit
}
