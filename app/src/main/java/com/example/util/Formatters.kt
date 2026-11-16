package com.example.util

import com.example.model.BankAccount
import com.example.model.ServiceResult
import com.example.model.SplitDetailItem
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

/**
 * Multi-Bank Split algorithm:
 * Divide the amount evenly across selected banks. If a bank has less than its share,
 * take its full balance and redistribute the shortfall among the remaining selected banks.
 * Repeats until the amount is covered or all balances are exhausted.
 */
fun calculateMultiBankSplit(
  amount: Long,
  selectedBanks: List<BankAccount>,
): ServiceResult<List<SplitDetailItem>> {
  if (amount <= 0L) {
    return ServiceResult.Error("Nominal harus lebih dari Rp 0.")
  }
  if (selectedBanks.isEmpty()) {
    return ServiceResult.Error("Pilih minimal satu rekening bank.")
  }
  val totalAvailable = selectedBanks.sumOf { it.balance.coerceAtLeast(0L) }
  if (totalAvailable < amount) {
    return ServiceResult.Error("Saldo bank tidak cukup")
  }

  val allocated = selectedBanks.associate { it.id to 0L }.toMutableMap()
  val remainingCap = selectedBanks.associate { it.id to it.balance.coerceAtLeast(0L) }.toMutableMap()
  var remainingAmount = amount

  while (remainingAmount > 0L) {
    val activeBanks = selectedBanks.filter { (remainingCap[it.id] ?: 0L) > 0L }
    if (activeBanks.isEmpty()) {
      return ServiceResult.Error("Saldo bank tidak cukup")
    }
    val count = activeBanks.size.toLong()
    val baseShare = remainingAmount / count
    val extra = (remainingAmount % count).toInt()

    var roundTaken = 0L
    activeBanks.forEachIndexed { index, bank ->
      if (remainingAmount - roundTaken <= 0L) return@forEachIndexed
      val targetShare =
        if (baseShare == 0L) {
          if (index < extra) 1L else 0L
        } else {
          baseShare + if (index < extra) 1L else 0L
        }
      if (targetShare > 0L) {
        val cap = remainingCap[bank.id] ?: 0L
        val take = minOf(targetShare, cap, remainingAmount - roundTaken)
        if (take > 0L) {
          allocated[bank.id] = (allocated[bank.id] ?: 0L) + take
          remainingCap[bank.id] = cap - take
          roundTaken += take
        }
      }
    }
    if (roundTaken == 0L) {
      return ServiceResult.Error("Saldo bank tidak cukup")
    }
    remainingAmount -= roundTaken
  }

  val details =
    selectedBanks.mapNotNull { bank ->
      val amt = allocated[bank.id] ?: 0L
      if (amt > 0L) SplitDetailItem(bankId = bank.id, bankName = bank.bankName, amount = amt) else null
    }
  return ServiceResult.Success(details)
}
