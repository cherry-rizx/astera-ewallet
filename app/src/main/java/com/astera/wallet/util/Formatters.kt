package com.astera.wallet.util

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val indoLocale: Locale = Locale.Builder().setLanguage("id").setRegion("ID").build()

fun formatRupiah(amount: Long): String {
  val formatter = NumberFormat.getIntegerInstance(indoLocale)
  return "Rp ${formatter.format(amount)}"
}

fun currentMonthKey(date: Date = Date()): String {
  val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
  fmt.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
  return fmt.format(date)
}

fun currentDayKey(date: Date = Date()): String {
  val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  fmt.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
  return fmt.format(date)
}

fun formatTimeShort(timestamp: Timestamp?): String {
  val date = timestamp?.toDate() ?: return "--:--"
  return SimpleDateFormat("HH:mm", indoLocale).format(date)
}

fun formatFullTimestamp(timestamp: Timestamp?): String {
  val date = timestamp?.toDate() ?: Date()
  return SimpleDateFormat("d MMMM yyyy • HH:mm", indoLocale).format(date)
}

fun formatDateGroupLabel(timestamp: Timestamp?): String {
  val date = timestamp?.toDate() ?: return "Hari ini"
  val targetCal = Calendar.getInstance().apply { time = date }
  val todayCal = Calendar.getInstance()
  val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

  return when {
    sameDay(targetCal, todayCal) -> "Hari ini"
    sameDay(targetCal, yesterdayCal) -> "Kemarin"
    else -> SimpleDateFormat("d MMMM yyyy", indoLocale).format(date)
  }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
  a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

fun formatLockRemaining(remainingMs: Long): String {
  val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return if (minutes > 0) {
    "${minutes} menit ${seconds} detik"
  } else {
    "${seconds} detik"
  }
}

fun initialsOf(name: String): String {
  val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
  return when {
    parts.isEmpty() -> "A"
    parts.size == 1 -> parts[0].take(1).uppercase()
    else -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
  }
}

/** Label waktu untuk daftar notifikasi: jam untuk hari ini, "Kemarin", lalu tanggal singkat. */
fun formatNotificationTime(timestamp: Timestamp?): String {
  val date = timestamp?.toDate() ?: return ""
  val targetCal = Calendar.getInstance().apply { time = date }
  val todayCal = Calendar.getInstance()
  val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
  return when {
    sameDay(targetCal, todayCal) -> SimpleDateFormat("HH:mm", indoLocale).format(date)
    sameDay(targetCal, yesterdayCal) -> "Kemarin"
    else -> SimpleDateFormat("d MMM", indoLocale).format(date)
  }
}

/** Nomor Dompet ASTERA tersamar, diambil dari 4 digit terakhir nomor HP (contoh: "•••• 4821"). */
fun maskedWalletNumber(phone: String?): String {
  val digits = phone.orEmpty().filter { it.isDigit() }
  return if (digits.length >= 4) "•••• ${digits.takeLast(4)}" else "•••• ----"
}

/** Nama bulan singkat dari monthKey "yyyy-MM" (contoh: "2026-11" -> "Nov"). */
fun shortMonthLabel(monthKey: String): String {
  val parser = SimpleDateFormat("yyyy-MM", Locale.US)
  val date = runCatching { parser.parse(monthKey) }.getOrNull() ?: return monthKey
  return SimpleDateFormat("MMM", indoLocale).format(date)
}

/** Nama bulan lengkap dari monthKey "yyyy-MM" (contoh: "2026-11" -> "November 2026"). */
fun longMonthLabel(monthKey: String): String {
  val parser = SimpleDateFormat("yyyy-MM", Locale.US)
  val date = runCatching { parser.parse(monthKey) }.getOrNull() ?: return monthKey
  return SimpleDateFormat("MMMM yyyy", indoLocale).format(date)
}

/** Rupiah ringkas untuk label grafik (contoh: "Rp 1,2 jt", "Rp 500 rb"). */
fun formatRupiahCompact(amount: Long): String {
  val abs = kotlin.math.abs(amount)
  return when {
    abs >= 1_000_000L -> {
      val value = amount / 1_000_000.0
      val text = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(indoLocale, "%.1f", value)
      "Rp $text jt"
    }
    abs >= 1_000L -> "Rp ${amount / 1_000L} rb"
    else -> "Rp $amount"
  }
}
