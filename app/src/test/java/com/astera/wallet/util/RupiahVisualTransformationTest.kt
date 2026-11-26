package com.astera.wallet.util

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class RupiahVisualTransformationTest {
  private fun format(raw: String) = RupiahVisualTransformation.filter(AnnotatedString(raw))

  @Test
  fun typing_growsWithThousandSeparators() {
    val steps = listOf("1", "10", "100", "1000", "10000", "100000", "1000000")
    val expected = listOf("1", "10", "100", "1.000", "10.000", "100.000", "1.000.000")
    assertEquals(expected, steps.map { format(it).text.text })
    assertEquals("", format("").text.text)
  }

  @Test
  fun offsetMapping_isConsistent() {
    listOf("", "5", "1234", "5000000", "123456789").forEach { raw ->
      val t = format(raw)
      for (o in 0..raw.length) {
        val tr = t.offsetMapping.originalToTransformed(o)
        assertEquals("raw=$raw o=$o", o, t.offsetMapping.transformedToOriginal(tr))
      }
      assertEquals(t.text.length, t.offsetMapping.originalToTransformed(raw.length))
      for (o in 0..t.text.length) {
        val orig = t.offsetMapping.transformedToOriginal(o)
        assert(orig in 0..raw.length)
      }
    }
  }

  @Test
  fun sanitize_removesLeadingZerosAndNonDigits() {
    assertEquals("5", sanitizeAmountInput("05", 9))
    assertEquals("", sanitizeAmountInput("000", 9))
    assertEquals("10000", sanitizeAmountInput("10.000", 9))
    assertEquals("1234567", sanitizeAmountInput("123456789", 7))
  }
}
