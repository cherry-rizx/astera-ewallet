package com.astera.wallet.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrUtils {
  private const val PREFIX = "astera://user/"

  fun buildUserQrUri(uid: String): String = "$PREFIX$uid"

  fun parseAsteraQrUid(rawValue: String): String? {
    val trimmed = rawValue.trim()
    if (!trimmed.startsWith(PREFIX)) return null
    val uid = trimmed.removePrefix(PREFIX).trim()
    return uid.takeIf { it.isNotEmpty() && it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
  }

  fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    val darkColor = Color.rgb(16, 30, 64)
    val lightColor = Color.WHITE
    for (y in 0 until height) {
      val offset = y * width
      for (x in 0 until width) {
        pixels[offset + x] = if (matrix.get(x, y)) darkColor else lightColor
      }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, width, 0, 0, width, height)
    }
  }
}
