package com.astera.wallet.util

import com.astera.wallet.model.BankAccount
import com.astera.wallet.model.ServiceResult
import com.astera.wallet.model.SplitDetailItem

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
