package com.astera.wallet.model

/** Nilai field `type` dokumen transaksi di Firestore. */
object TransactionTypes {
  /** Transfer antar-pengguna ASTERA. */
  const val TRANSFER = "transfer"

  /** Top up dari rekening bank simulasi milik sendiri. */
  const val TOPUP = "topup"

  /** Transfer ke rekening bank / e-wallet lain (simulasi: saldo keluar, tidak ada penerima nyata). */
  const val EXTERNAL_TRANSFER = "external_transfer"

  /** receiverId untuk transfer keluar ASTERA (tidak ada akun ASTERA penerima). */
  const val EXTERNAL_RECEIVER_ID = "external"
}

/** Jenis tujuan transfer keluar, sesuai tab "Ke Rekening Bank" / "Ke E-Wallet". */
enum class DestinationType(val value: String, val label: String, val numberLabel: String) {
  BANK("bank", "Bank", "Nomor rekening"),
  EWALLET("ewallet", "E-Wallet", "Nomor HP / ID e-wallet");

  companion object {
    fun from(value: String): DestinationType? = entries.firstOrNull { it.value == value }
  }
}

/** Daftar tujuan simulasi. Tidak terhubung ke bank atau e-wallet sungguhan. */
object TransferProviders {
  val banks = listOf("BCA", "Mandiri", "BNI", "BRI", "BSI", "CIMB Niaga", "Permata", "BTN")
  val ewallets = listOf("GoPay", "OVO", "DANA", "ShopeePay", "LinkAja")

  fun forType(type: DestinationType): List<String> =
    when (type) {
      DestinationType.BANK -> banks
      DestinationType.EWALLET -> ewallets
    }
}

fun TransactionRecord.isTopUp(): Boolean = type == TransactionTypes.TOPUP

fun TransactionRecord.isExternalTransfer(): Boolean = type == TransactionTypes.EXTERNAL_TRANSFER

/** Uang masuk ke dompet pengguna ini (transfer masuk atau top up). */
fun TransactionRecord.isIncomingFor(uid: String): Boolean =
  isTopUp() || (type == TransactionTypes.TRANSFER && receiverId == uid && senderId != uid)

/** Pengeluaran: transfer ke pengguna ASTERA lain atau ke bank / e-wallet lain. */
fun TransactionRecord.isOutgoingFor(uid: String): Boolean =
  senderId == uid && (isExternalTransfer() || (type == TransactionTypes.TRANSFER && receiverId != uid))

/** Contoh: "BCA • 3067 2519 4800" untuk transfer keluar. */
fun TransactionRecord.destinationLabel(): String = "$destinationProvider • $destinationAccount"
