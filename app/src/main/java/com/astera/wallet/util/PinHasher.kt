package com.astera.wallet.util

import java.security.MessageDigest
import java.security.SecureRandom

object PinHasher {
  private val random = SecureRandom()

  fun hashPin(pin: String): String {
    val saltBytes = ByteArray(16)
    random.nextBytes(saltBytes)
    val saltHex = saltBytes.toHex()
    val digestHex = sha256("$saltHex:$pin")
    return "$saltHex:$digestHex"
  }

  fun verifyPin(pin: String, storedHash: String): Boolean {
    val parts = storedHash.split(":")
    if (parts.size != 2) return false
    val saltHex = parts[0]
    val expectedHex = parts[1]
    val actualHex = sha256("$saltHex:$pin")
    return MessageDigest.isEqual(expectedHex.toByteArray(), actualHex.toByteArray())
  }

  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.toHex()
  }

  private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
