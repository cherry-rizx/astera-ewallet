package com.astera.wallet.ui.screens.transfer

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.UserDirectoryEntry
import com.astera.wallet.nfc.NfcAvailability
import com.astera.wallet.nfc.NfcRecipientReaderEffect
import com.astera.wallet.nfc.rememberNfcAvailability
import com.astera.wallet.service.TransactionService
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraOutlinedButton
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.AsteraTextField
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.components.UserAvatar
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.QrUtils
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.viewmodel.UiState
import com.astera.wallet.model.DestinationType
import androidx.compose.runtime.saveable.rememberSaveable
import com.astera.wallet.util.formatAccountNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
  currentUserId: String,
  currentUserName: String,
  currentUserAccountNumber: String,
  currentBalance: Long,
  initialStartWithQr: Boolean,
  searchResults: UiState<List<UserDirectoryEntry>>,
  selectedRecipient: UserDirectoryEntry?,
  onSearchQueryChanged: (String) -> Unit,
  onSelectRecipient: (UserDirectoryEntry?) -> Unit,
  onQrUidScanned: (String, (UserDirectoryEntry) -> Unit, (String) -> Unit) -> Unit,
  onContinueTransfer: (UserDirectoryEntry, Long, String) -> Unit,
  onContinueExternalTransfer: (
    type: DestinationType,
    provider: String,
    accountNumber: String,
    accountName: String,
    amount: Long,
    note: String,
  ) -> Unit,
  onBack: () -> Unit,
) {
  var mode by rememberSaveable { mutableStateOf(TransferMode.ASTERA) }
  // Di dalam mode ASTERA: 0 = input manual, 1 = scan QR.
  var selectedTab by remember { mutableIntStateOf(if (initialStartWithQr) 1 else 0) }
  var searchQuery by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }
  var recipientError by remember { mutableStateOf<String?>(null) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var qrFeedback by remember { mutableStateOf<String?>(null) }
  var showMyQrSheet by remember { mutableStateOf(false) }
  var scanKey by remember { mutableIntStateOf(0) }
  val nfcAvailability = rememberNfcAvailability()

  // Dipakai oleh scan QR dan baca NFC: keduanya menghasilkan URI "astera://user/{uid}".
  fun handleRecipientPayload(raw: String, fromNfc: Boolean) {
    val uid = QrUtils.parseAsteraQrUid(raw)
    if (uid == null) {
      qrFeedback =
        if (fromNfc) "Data NFC tidak dikenali. Pastikan HP penerima membuka QR Astera > Tunjukkan QR."
        else "Kode QR tidak dikenali. Gunakan QR ASTERA yang valid."
      scanKey++
    } else {
      onQrUidScanned(
        uid,
        { _ ->
          qrFeedback = null
          recipientError = null
          selectedTab = 0
        },
        { err ->
          qrFeedback = err
          scanKey++
        },
      )
    }
  }

  NfcRecipientReaderEffect(enabled = mode == TransferMode.ASTERA && nfcAvailability == NfcAvailability.READY) { payload ->
    handleRecipientPayload(payload, fromNfc = true)
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
        title = "Transfer",
        subtitle = "Saldo tersedia: ${formatRupiah(currentBalance)}",
        onBack = onBack,
        trailingAction = {
          IconButton(
            onClick = { showMyQrSheet = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("show_my_qr_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.QrCode2,
              contentDescription = "Tampilkan QR Saya",
              tint = AsteraAmber,
            )
          }
        },
      )

      // Tab tujuan: sesama ASTERA, rekening bank lain, atau e-wallet lain (desain 06 & 07).
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = AsteraFieldBg,
        border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      ) {
        Row(modifier = Modifier.padding(4.dp)) {
          TransferMode.entries.forEach { tab ->
            TransferTabChip(
              label = tab.label,
              selected = mode == tab,
              onClick = {
                qrFeedback = null
                mode = tab
              },
              modifier = Modifier.weight(1f).testTag("tab_${tab.name.lowercase()}"),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (mode != TransferMode.ASTERA) {
        val type = if (mode == TransferMode.BANK) DestinationType.BANK else DestinationType.EWALLET
        ExternalTransferForm(
          type = type,
          currentBalance = currentBalance,
          onContinue = { provider, account, name, amount, note ->
            onContinueExternalTransfer(type, provider, account, name, amount, note)
          },
        )
      } else if (selectedTab == 0) {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          selectedRecipient?.let { recipient ->
            SelectedRecipientCard(
              recipient = recipient,
              onClear = { onSelectRecipient(null) },
            )
          }

          NfcHintCard(availability = nfcAvailability, feedback = qrFeedback)

          AsteraTextField(
            value = searchQuery,
            onValueChange = {
              searchQuery = it
              onSearchQueryChanged(it)
            },
            label = "Penerima",
            placeholder = "Nama atau nomor rekening ASTERA",
            errorText = recipientError,
            leadingIcon = {
              Icon(Icons.Outlined.Search, contentDescription = null, tint = AsteraSkyBlue)
            },
            modifier = Modifier.testTag("transfer_search_input"),
          )

          // Recipient results card
          when (searchResults) {
            UiState.Loading -> {
              AsteraCard {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AsteraSkyBlue)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Mencari pengguna...", color = AsteraTextSecondary)
                }
              }
            }
            is UiState.Error -> {
              AsteraCard {
                Text(
                  text = searchResults.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }
            is UiState.Success -> {
              val list = searchResults.data
              if (list.isEmpty()) {
                AsteraCard {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Rounded.PersonSearch,
                      contentDescription = null,
                      tint = AsteraTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Pengguna tidak ditemukan. Pastikan penerima sudah mendaftar di ASTERA.",
                      style = MaterialTheme.typography.bodySmall,
                      color = AsteraTextSecondary,
                    )
                  }
                }
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  list.take(5).forEach { entry ->
                    val isSelected = selectedRecipient?.uid == entry.uid
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = if (isSelected) AsteraPrimary.copy(alpha = 0.55f) else AsteraCard,
                      border =
                        BorderStroke(
                          width = if (isSelected) 1.5.dp else 1.dp,
                          color = if (isSelected) AsteraAmber else AsteraCardBorder.copy(alpha = 0.45f),
                        ),
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(14.dp))
                          .clickable {
                            onSelectRecipient(entry)
                            recipientError = null
                          }
                          .testTag("recipient_option_${entry.uid}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        UserAvatar(name = entry.name, photoUrl = null, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                          )
                          Text(
                            text = accountLabel(entry),
                            style = MaterialTheme.typography.bodySmall,
                            color = AsteraTextSecondary,
                          )
                        }
                        if (isSelected) {
                          Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Dipilih",
                            tint = AsteraAmber,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          AsteraTextField(
            value = amountText,
            onValueChange = { input ->
              amountText = input.filter { it.isDigit() }.take(9)
              amountError = null
            },
            label = "Nominal",
            placeholder = "0",
            prefix = {
              Text(
                text = "Rp ",
                style = MaterialTheme.typography.titleMedium,
                color = AsteraAmber,
              )
            },
            helperText = "Maksimal Rp 5.000.000 per transaksi dan per hari",
            errorText = amountError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.testTag("transfer_amount_input"),
          )

          AsteraTextField(
            value = noteText,
            onValueChange = { noteText = it.take(150) },
            label = "Pesan (opsional)",
            placeholder = "Tulis pesan di sini...",
            singleLine = false,
            modifier = Modifier.testTag("transfer_note_input"),
          )

          AsteraOutlinedButton(
            text = "Scan QR Penerima",
            onClick = {
              qrFeedback = null
              selectedTab = 1
            },
            borderColor = AsteraAmber.copy(alpha = 0.7f),
            contentColor = AsteraAmber,
            modifier = Modifier.testTag("open_qr_scanner_button"),
          )

          AsteraPrimaryButton(
            text = "Lanjut",
            onClick = {
              var ok = true
              val recipient = selectedRecipient
              if (recipient == null) {
                recipientError = "Pilih penerima terlebih dahulu"
                ok = false
              }
              val amount = amountText.toLongOrNull() ?: 0L
              if (amount <= 0L) {
                amountError = "Nominal harus lebih dari Rp 0"
                ok = false
              } else if (amount > TransactionService.MAX_TRANSFER_AMOUNT) {
                amountError = "Maksimal Rp 5.000.000 per transaksi"
                ok = false
              } else if (amount > currentBalance) {
                amountError = "Saldo tidak mencukupi"
                ok = false
              }
              if (ok && recipient != null) {
                onContinueTransfer(recipient, amount, noteText)
              }
            },
            modifier = Modifier.padding(bottom = 24.dp).testTag("transfer_continue_button"),
          )
        }
      } else {
        // Scan QR Tab
        QrScanSection(
          feedbackMessage = qrFeedback,
          scanKey = scanKey,
          onQrScanned = { raw -> handleRecipientPayload(raw, fromNfc = false) },
          onSwitchManual = { selectedTab = 0 },
          onShowMyQr = { showMyQrSheet = true },
        )
      }
    }
  }

  if (showMyQrSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMyQrSheet = false },
      containerColor = AsteraCard,
      contentColor = Color.White,
    ) {
      val qrUri = remember(currentUserId) { QrUtils.buildUserQrUri(currentUserId) }
      val bitmap = remember(qrUri) { QrUtils.generateQrBitmap(qrUri, 512) }

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "QR ASTERA Saya",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "$currentUserName • ${formatAccountNumber(currentUserAccountNumber)}",
          style = MaterialTheme.typography.bodyMedium,
          color = AsteraTextSecondary,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color.White,
          modifier = Modifier.size(240.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = "Kode QR ASTERA milik $currentUserName",
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Pindai kode ini untuk kirim saldo simulasi ke $currentUserName",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun TransferTabChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) AsteraPrimary else Color.Transparent,
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize(),
  ) {
    Box(
      modifier = Modifier.padding(vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        color = if (selected) Color.White else AsteraTextSecondary,
      )
    }
  }
}

@Composable
private fun SelectedRecipientCard(
  recipient: UserDirectoryEntry,
  onClear: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = AsteraPrimary.copy(alpha = 0.55f),
    border = BorderStroke(1.5.dp, AsteraSkyBlue),
    modifier = Modifier.fillMaxWidth().testTag("selected_recipient_card"),
  ) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      UserAvatar(name = recipient.name, photoUrl = null, size = 40.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text("Penerima terpilih", style = MaterialTheme.typography.labelSmall, color = AsteraTextSecondary)
        Text(recipient.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
        Text(accountLabel(recipient), style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
      }
      TextButton(onClick = onClear, modifier = Modifier.testTag("clear_recipient_button")) {
        Text("Ganti", color = AsteraSkyBlue)
      }
    }
  }
}

/** Petunjuk "tempelkan HP" untuk mengisi penerima lewat NFC, atau status NFC jika belum siap. */
@Composable
private fun NfcHintCard(availability: NfcAvailability, feedback: String?) {
  if (availability == NfcAvailability.UNSUPPORTED) return
  val context = LocalContext.current
  AsteraCard(
    onClick =
      if (availability == NfcAvailability.DISABLED) {
        { runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) } }
      } else null,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Nfc, contentDescription = null, tint = AsteraSkyBlue, modifier = Modifier.size(28.dp))
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (availability == NfcAvailability.READY) "Tap HP untuk isi penerima" else "NFC mati",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
        )
        Text(
          text =
            if (availability == NfcAvailability.READY)
              "Minta penerima membuka QR Astera > Tunjukkan QR, lalu tempelkan bagian belakang kedua HP."
            else "Ketuk untuk menyalakan NFC di pengaturan agar bisa tap HP ke penerima.",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
        )
      }
    }
    if (!feedback.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(feedback, style = MaterialTheme.typography.bodySmall, color = AsteraAmber)
    }
  }
}

/** Tab tujuan transfer. */
private enum class TransferMode(val label: String) {
  ASTERA("Sesama ASTERA"),
  BANK("Ke Bank"),
  EWALLET("Ke E-Wallet"),
}

/** "No. Rek 5839 2071 4462", atau keterangan jika akun penerima belum punya nomor rekening. */
private fun accountLabel(entry: UserDirectoryEntry): String =
  if (entry.accountNumber.isBlank()) "Dompet ASTERA" else "No. Rek ${formatAccountNumber(entry.accountNumber)}"
