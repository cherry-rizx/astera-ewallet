package com.astera.wallet.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Menyisipkan titik ribuan ke deretan angka: "1000000" → "1.000.000". */
fun groupThousands(digits: String): String {
  val sb = StringBuilder()
  digits.forEachIndexed { i, c ->
    if (i > 0 && (digits.length - i) % 3 == 0) sb.append('.')
    sb.append(c)
  }
  return sb.toString()
}

/** Membersihkan input nominal: hanya angka, tanpa nol di depan, dan dibatasi panjangnya. */
fun sanitizeAmountInput(input: String, maxDigits: Int): String =
  input.filter { it.isDigit() }.trimStart('0').take(maxDigits)

/**
 * Menampilkan nominal dengan titik ribuan saat diketik (1 → 10 → 100 → 1.000 → 10.000), sementara
 * state tetap berisi angka mentah sehingga validasi `toLongOrNull()` tidak berubah.
 */
object RupiahVisualTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val raw = text.text
    val formatted = groupThousands(raw)
    val mapping =
      object : OffsetMapping {
        // Jumlah titik sebelum posisi `offset` pada teks mentah.
        private fun dotsBefore(offset: Int): Int {
          if (offset <= 0) return 0
          val len = raw.length
          return (1 until offset).count { (len - it) % 3 == 0 }
        }

        override fun originalToTransformed(offset: Int): Int {
          val o = offset.coerceIn(0, raw.length)
          return o + dotsBefore(o)
        }

        override fun transformedToOriginal(offset: Int): Int {
          val o = offset.coerceIn(0, formatted.length)
          return formatted.take(o).count { it != '.' }
        }
      }
    return TransformedText(AnnotatedString(formatted), mapping)
  }
}
