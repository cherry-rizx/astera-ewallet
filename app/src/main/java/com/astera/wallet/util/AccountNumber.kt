package com.astera.wallet.util

import java.security.SecureRandom

private val accountRandom = SecureRandom()

/** Panjang nomor rekening Dompet ASTERA. */
const val WALLET_ACCOUNT_NUMBER_LENGTH = 12

/**
 * Nomor rekening Dompet ASTERA acak 12 digit (digit pertama bukan 0), terpisah dari nomor HP.
 * Peluang dua akun mendapat nomor yang sama sekitar 1 banding 900 miliar per pasangan.
 */
fun generateWalletAccountNumber(): String {
  val first = (accountRandom.nextInt(9) + 1).toString()
  val rest = (1 until WALLET_ACCOUNT_NUMBER_LENGTH).joinToString("") { accountRandom.nextInt(10).toString() }
  return first + rest
}

fun isValidWalletAccountNumber(value: String): Boolean =
  value.length == WALLET_ACCOUNT_NUMBER_LENGTH && value.all { it.isDigit() }
