package com.astera.wallet.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountNumberTest {
  @Test
  fun generated_isTwelveDigitsWithoutLeadingZero() {
    repeat(500) {
      val number = generateWalletAccountNumber()
      assertTrue(number, isValidWalletAccountNumber(number))
      assertNotEquals('0', number.first())
    }
  }

  @Test
  fun generated_isRandom() {
    val numbers = (1..200).map { generateWalletAccountNumber() }.toSet()
    assertEquals(200, numbers.size)
  }

  @Test
  fun masking_usesAccountNumber() {
    assertEquals("•••• 4462", maskedWalletNumber("583920714462"))
    assertEquals("5839 2071 4462", formatAccountNumber("583920714462"))
  }
}
