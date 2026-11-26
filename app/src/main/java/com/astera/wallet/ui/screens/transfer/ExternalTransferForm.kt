package com.astera.wallet.ui.screens.transfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.DestinationType
import com.astera.wallet.model.TransferProviders
import com.astera.wallet.service.TransactionService
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.AsteraTextField
import com.astera.wallet.ui.components.UserAvatar
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatAccountNumber

/**
 * Form tab "Ke Rekening Bank" / "Ke E-Wallet" sesuai desain 06 & 07: nomor tujuan, pilihan
 * bank/e-wallet, nama pemilik, nominal, dan pesan. Transfer ini simulasi.
 */
@Composable
internal fun ExternalTransferForm(
  type: DestinationType,
  currentBalance: Long,
  onContinue: (provider: String, accountNumber: String, accountName: String, amount: Long, note: String) -> Unit,
) {
  val providers = TransferProviders.forType(type)
  var accountNumber by rememberSaveable(type) { mutableStateOf("") }
  var provider by rememberSaveable(type) { mutableStateOf(providers.first()) }
  var accountName by rememberSaveable(type) { mutableStateOf("") }
  var amountText by rememberSaveable(type) { mutableStateOf("") }
  var noteText by rememberSaveable(type) { mutableStateOf("") }
  var numberError by rememberSaveable(type) { mutableStateOf<String?>(null) }
  var nameError by rememberSaveable(type) { mutableStateOf<String?>(null) }
  var amountError by rememberSaveable(type) { mutableStateOf<String?>(null) }

  val isBank = type == DestinationType.BANK

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    AsteraTextField(
      value = formatAccountNumber(accountNumber),
      onValueChange = { input ->
        accountNumber = input.filter { it.isDigit() }.take(if (isBank) 20 else 13)
        numberError = null
      },
      label = if (isBank) "Rekening Tujuan" else "Nomor HP / ID E-Wallet",
      placeholder = if (isBank) "Masukkan nomor rekening" else "08xx xxxx xxxx",
      errorText = numberError,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = AsteraSkyBlue) },
      modifier = Modifier.testTag("external_account_input"),
    )

    ProviderSelector(
      label = if (isBank) "Bank Tujuan" else "E-Wallet Tujuan",
      providers = providers,
      selected = provider,
      onSelect = { provider = it },
    )

    AsteraTextField(
      value = accountName,
      onValueChange = {
        accountName = it.take(80)
        nameError = null
      },
      label = if (isBank) "Nama Pemilik Rekening" else "Nama Pemilik Akun",
      placeholder = "Sesuai nama di $provider",
      errorText = nameError,
      leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = AsteraSkyBlue) },
      modifier = Modifier.testTag("external_name_input"),
    )

    AsteraTextField(
      value = amountText,
      onValueChange = { input ->
        amountText = input.filter { it.isDigit() }.take(9)
        amountError = null
      },
      label = "Nominal",
      placeholder = "0",
      prefix = { Text("Rp ", style = MaterialTheme.typography.titleMedium, color = AsteraAmber) },
      helperText = "Maksimal Rp 5.000.000 per transaksi dan per hari",
      errorText = amountError,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.testTag("external_amount_input"),
    )

    AsteraTextField(
      value = noteText,
      onValueChange = { noteText = it.take(150) },
      label = "Pesan (opsional)",
      placeholder = "Tulis pesan di sini...",
      singleLine = false,
      modifier = Modifier.testTag("external_note_input"),
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = AsteraSkyBlue.copy(alpha = 0.10f),
      border = BorderStroke(1.dp, AsteraSkyBlue.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = AsteraSkyBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          "Simulasi: saldo Dompet ASTERA berkurang, tetapi dana tidak benar-benar dikirim ke $provider.",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
        )
      }
    }

    AsteraPrimaryButton(
      text = "Lanjut",
      onClick = {
        var ok = true
        val validNumber =
          if (isBank) accountNumber.length in 6..20
          else accountNumber.startsWith("08") && accountNumber.length in 10..13
        if (!validNumber) {
          numberError =
            if (isBank) "Nomor rekening harus 6-20 digit"
            else "Nomor e-wallet harus diawali 08 dan berisi 10-13 digit"
          ok = false
        }
        if (accountName.isBlank()) {
          nameError = "Nama pemilik wajib diisi"
          ok = false
        }
        val amount = amountText.toLongOrNull() ?: 0L
        amountError =
          when {
            amount <= 0L -> "Nominal harus lebih dari Rp 0"
            amount > TransactionService.MAX_TRANSFER_AMOUNT -> "Maksimal Rp 5.000.000 per transaksi"
            amount > currentBalance -> "Saldo tidak mencukupi"
            else -> null
          }
        if (amountError != null) ok = false
        if (ok) onContinue(provider, accountNumber, accountName.trim(), amount, noteText)
      },
      modifier = Modifier.padding(bottom = 24.dp).testTag("external_continue_button"),
    )
  }
}

@Composable
private fun ProviderSelector(
  label: String,
  providers: List<String>,
  selected: String,
  onSelect: (String) -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  Column {
    Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    Spacer(Modifier.height(8.dp))
    Box {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = AsteraFieldBg,
        border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.6f)),
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { expanded = true }
            .testTag("external_provider_selector"),
      ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          UserAvatar(name = selected, photoUrl = null, size = 32.dp)
          Spacer(Modifier.width(12.dp))
          Text(
            selected,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.weight(1f),
          )
          Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Pilih $label", tint = AsteraTextSecondary)
        }
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        containerColor = AsteraCard,
      ) {
        providers.forEach { name ->
          DropdownMenuItem(
            text = {
              Text(
                name,
                color = if (name == selected) AsteraSkyBlue else Color.White,
                fontWeight = if (name == selected) FontWeight.Bold else FontWeight.Normal,
              )
            },
            leadingIcon = { UserAvatar(name = name, photoUrl = null, size = 28.dp) },
            onClick = {
              onSelect(name)
              expanded = false
            },
            modifier = Modifier.testTag("provider_option_$name"),
          )
        }
      }
    }
  }
}
