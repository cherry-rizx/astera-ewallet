package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BankAccount
import com.example.model.ServiceResult
import com.example.model.SplitDetailItem
import com.example.service.TransactionService
import com.example.ui.components.AsteraBackground
import com.example.ui.components.AsteraCard
import com.example.ui.components.AsteraPrimaryButton
import com.example.ui.components.AsteraTextField
import com.example.ui.components.AsteraTopBar
import com.example.ui.components.SimulasiBadge
import com.example.ui.theme.AsteraAmber
import com.example.ui.theme.AsteraCard
import com.example.ui.theme.AsteraCardBorder
import com.example.ui.theme.AsteraFieldBg
import com.example.ui.theme.AsteraPrimary
import com.example.ui.theme.AsteraRed
import com.example.ui.theme.AsteraSkyBlue
import com.example.ui.theme.AsteraTextSecondary
import com.example.util.calculateMultiBankSplit
import com.example.util.formatRupiah
import com.example.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopUpScreen(
  bankAccountsState: UiState<List<BankAccount>>,
  onContinueTopUp: (amount: Long, selectedBankIds: List<String>, splitPreview: List<SplitDetailItem>) -> Unit,
  onBack: () -> Unit,
) {
  var amountText by remember { mutableStateOf("100000") }
  var selectedBankIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var amountError by remember { mutableStateOf<String?>(null) }

  val banks = (bankAccountsState as? UiState.Success)?.data ?: emptyList()

  // Preselect all banks once loaded if none selected yet
  LaunchedEffect(banks) {
    if (banks.isNotEmpty() && selectedBankIds.isEmpty()) {
      selectedBankIds = banks.map { it.id }.toSet()
    }
  }

  val amount = amountText.toLongOrNull() ?: 0L
  val selectedBanks = banks.filter { it.id in selectedBankIds }
  val splitPreviewResult =
    remember(amount, selectedBanks) {
      if (amount > 0L && amount <= TransactionService.MAX_TOPUP_AMOUNT && selectedBanks.isNotEmpty()) {
        calculateMultiBankSplit(amount, selectedBanks)
      } else {
        null
      }
    }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter),
    ) {
      AsteraTopBar(
        title = "Top Up Simulasi",
        onBack = onBack,
      )

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        SimulasiBadge(text = "Simulasi, tidak menggunakan uang sungguhan")

        AsteraTextField(
          value = amountText,
          onValueChange = { input ->
            amountText = input.filter { it.isDigit() }.take(7)
            amountError = null
          },
          label = "Nominal Top Up",
          placeholder = "0",
          prefix = {
            Text("Rp ", style = MaterialTheme.typography.titleMedium, color = AsteraAmber)
          },
          helperText = "Maksimal Rp 1.000.000 per top up",
          errorText = amountError,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.testTag("topup_amount_input"),
        )

        // Quick Nominal Chips
        val quickAmounts = listOf(50_000L, 100_000L, 500_000L, 1_000_000L)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          quickAmounts.forEach { preset ->
            val isSelected = amount == preset
            Surface(
              shape = CircleShape,
              color = if (isSelected) AsteraPrimary else AsteraFieldBg,
              border =
                BorderStroke(
                  1.dp,
                  if (isSelected) AsteraAmber else AsteraCardBorder.copy(alpha = 0.6f),
                ),
              modifier =
                Modifier
                  .clip(CircleShape)
                  .clickable {
                    amountText = preset.toString()
                    amountError = null
                  }
                  .testTag("topup_chip_$preset"),
            ) {
              Text(
                text = formatRupiah(preset),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else AsteraTextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              )
            }
          }
        }

        // Bank Selection (Multi-Bank Split)
        Text(
          text = "Pilih Sumber Rekening Bank (Multi-Bank Split)",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
        )

        when (bankAccountsState) {
          UiState.Loading -> {
            AsteraCard {
              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AsteraSkyBlue, modifier = Modifier.size(24.dp))
              }
            }
          }
          is UiState.Error -> {
            AsteraCard {
              Text(
                text = bankAccountsState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
          is UiState.Success -> {
            if (banks.isEmpty()) {
              AsteraCard {
                Text(
                  text = "Belum ada rekening bank terhubung.",
                  style = MaterialTheme.typography.bodySmall,
                  color = AsteraTextSecondary,
                )
              }
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                banks.forEach { bank ->
                  val checked = bank.id in selectedBankIds
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (checked) AsteraPrimary.copy(alpha = 0.45f) else AsteraCard,
                    border =
                      BorderStroke(
                        width = if (checked) 1.5.dp else 1.dp,
                        color = if (checked) AsteraAmber else AsteraCardBorder.copy(alpha = 0.45f),
                      ),
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                          selectedBankIds =
                            if (checked) selectedBankIds - bank.id else selectedBankIds + bank.id
                        }
                        .testTag("bank_select_${bank.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                          selectedBankIds =
                            if (isChecked) selectedBankIds + bank.id else selectedBankIds - bank.id
                        },
                        colors =
                          CheckboxDefaults.colors(
                            checkedColor = AsteraAmber,
                            checkmarkColor = Color(0xFF101E40),
                            uncheckedColor = AsteraTextSecondary,
                          ),
                      )
                      Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = AsteraSkyBlue,
                        modifier = Modifier.size(22.dp),
                      )
                      Spacer(modifier = Modifier.width(12.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = bank.bankName,
                          style = MaterialTheme.typography.titleSmall,
                          color = Color.White,
                        )
                        Text(
                          text = bank.accountNumber,
                          style = MaterialTheme.typography.bodySmall,
                          color = AsteraTextSecondary,
                        )
                      }
                      Text(
                        text = formatRupiah(bank.balance),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Multi-Bank Split Live Preview
        if (selectedBanks.isEmpty() && banks.isNotEmpty()) {
          Text(
            text = "Pilih minimal satu rekening bank untuk melanjutkan.",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraAmber,
          )
        } else if (splitPreviewResult is ServiceResult.Error) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = AsteraRed.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, AsteraRed.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = splitPreviewResult.message,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFCA5A5),
              modifier = Modifier.padding(12.dp),
            )
          }
        } else if (splitPreviewResult is ServiceResult.Success) {
          AsteraCard {
            Text(
              text = "Pratinjau Pembagian Bank",
              style = MaterialTheme.typography.labelLarge,
              color = AsteraAmber,
            )
            Spacer(modifier = Modifier.height(8.dp))
            splitPreviewResult.data.forEach { item ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = item.bankName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = AsteraTextSecondary,
                )
                Text(
                  text = formatRupiah(item.amount),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White,
                )
              }
            }
          }
        }

        AsteraPrimaryButton(
          text = "Top Up",
          enabled =
            amount in 1L..TransactionService.MAX_TOPUP_AMOUNT &&
              splitPreviewResult is ServiceResult.Success,
          onClick = {
            if (amount <= 0L) {
              amountError = "Nominal harus lebih dari Rp 0"
              return@AsteraPrimaryButton
            }
            if (amount > TransactionService.MAX_TOPUP_AMOUNT) {
              amountError = "Maksimal Rp 1.000.000 per top up"
              return@AsteraPrimaryButton
            }
            if (splitPreviewResult is ServiceResult.Success) {
              onContinueTopUp(
                amount,
                selectedBanks.map { it.id },
                splitPreviewResult.data,
              )
            }
          },
          modifier = Modifier.padding(bottom = 24.dp).testTag("topup_submit_button"),
        )
      }
    }
  }
}
