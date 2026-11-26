package com.astera.wallet.ui.screens.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.components.DetailRow
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.components.UserAvatar
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraAmberSoft
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraDivider
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.viewmodel.PendingOperation
import com.astera.wallet.util.formatAccountNumber

@Composable
fun ConfirmOperationScreen(
  pendingOperation: PendingOperation?,
  onProceedToPin: () -> Unit,
  onBack: () -> Unit,
) {
  var showBudgetWarningDialog by remember { mutableStateOf(false) }

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
        title =
          when (pendingOperation) {
            is PendingOperation.TopUp -> "Konfirmasi Top Up"
            else -> "Konfirmasi Transfer"
          },
        subtitle = "Periksa kembali detail transaksi",
        onBack = onBack,
      )

      if (pendingOperation == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          AsteraCard {
            Text(
              text = "Tidak ada transaksi yang dipilih.",
              style = MaterialTheme.typography.bodyMedium,
              color = AsteraTextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AsteraPrimaryButton(text = "Kembali", onClick = onBack)
          }
        }
      } else {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Recipient or Multi-Bank Split Card
          when (pendingOperation) {
            is PendingOperation.Transfer -> {
              AsteraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  UserAvatar(name = pendingOperation.receiverName, photoUrl = null, size = 48.dp)
                  Spacer(modifier = Modifier.width(14.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Penerima",
                      style = MaterialTheme.typography.bodySmall,
                      color = AsteraTextSecondary,
                    )
                    Text(
                      text = pendingOperation.receiverName,
                      style = MaterialTheme.typography.titleMedium,
                      color = Color.White,
                    )
                    Text(
                      text = pendingOperation.receiverAccount,
                      style = MaterialTheme.typography.bodySmall,
                      color = AsteraTextSecondary,
                    )
                  }
                  SimulasiBadge()
                }
              }
            }
            is PendingOperation.ExternalTransfer -> {
              AsteraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  UserAvatar(name = pendingOperation.accountName, photoUrl = null, size = 48.dp)
                  Spacer(modifier = Modifier.width(14.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Penerima • ${pendingOperation.destinationType.label}",
                      style = MaterialTheme.typography.bodySmall,
                      color = AsteraTextSecondary,
                    )
                    Text(
                      text = "${pendingOperation.accountName} • ${pendingOperation.provider}",
                      style = MaterialTheme.typography.titleMedium,
                      color = Color.White,
                    )
                    Text(
                      text = formatAccountNumber(pendingOperation.accountNumber),
                      style = MaterialTheme.typography.bodySmall,
                      color = AsteraTextSecondary,
                    )
                  }
                  SimulasiBadge()
                }
              }
            }
            is PendingOperation.TopUp -> {
              AsteraCard {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Sumber Dana Multi-Bank",
                    style = MaterialTheme.typography.titleSmall,
                    color = AsteraAmber,
                  )
                  SimulasiBadge()
                }
                Spacer(modifier = Modifier.height(10.dp))
                pendingOperation.splitPreview.forEach { split ->
                  DetailRow(
                    label = split.bankName,
                    value = formatRupiah(split.amount),
                  )
                }
              }
            }
          }

          // Details card
          AsteraCard {
            DetailRow(label = "Nominal", value = formatRupiah(pendingOperation.amount))
            DetailRow(label = "Biaya", value = "Rp 0")
            val note =
              when (pendingOperation) {
                is PendingOperation.Transfer -> pendingOperation.note
                is PendingOperation.ExternalTransfer -> pendingOperation.note
                is PendingOperation.TopUp -> ""
              }
            if (note.isNotBlank()) {
              DetailRow(label = "Pesan", value = note)
            }
            HorizontalDivider(color = AsteraDivider, modifier = Modifier.padding(vertical = 10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Total",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
              )
              Text(
                text = formatRupiah(pendingOperation.amount),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = AsteraAmber,
              )
            }
          }

          if (pendingOperation.outOfBudget) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = AsteraAmberSoft,
              border = BorderStroke(1.dp, AsteraAmber.copy(alpha = 0.6f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.WarningAmber,
                  contentDescription = null,
                  tint = AsteraAmber,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Transaksi ini akan melewati batas anggaran bulan ini.",
                  style = MaterialTheme.typography.bodySmall,
                  color = AsteraAmber,
                )
              }
            }
          }

          Text(
            text = "Transaksi ini simulasi, tidak menggunakan uang sungguhan.",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(modifier = Modifier.weight(1f))

          AsteraPrimaryButton(
            text = "Lanjutkan",
            onClick = {
              if (pendingOperation.outOfBudget) {
                showBudgetWarningDialog = true
              } else {
                onProceedToPin()
              }
            },
            modifier = Modifier.padding(bottom = 24.dp).testTag("confirm_proceed_button"),
          )
        }
      }
    }
  }

  if (showBudgetWarningDialog) {
    AlertDialog(
      onDismissRequest = { showBudgetWarningDialog = false },
      containerColor = AsteraCard,
      titleContentColor = Color.White,
      textContentColor = AsteraTextSecondary,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AsteraAmber)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Peringatan Anggaran", style = MaterialTheme.typography.titleLarge)
        }
      },
      text = {
        Text(
          text = "Melewati anggaran bulan ini, lanjutkan?",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showBudgetWarningDialog = false
            onProceedToPin()
          },
          modifier = Modifier.testTag("confirm_over_budget_yes"),
        ) {
          Text("Lanjutkan", color = AsteraAmber, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showBudgetWarningDialog = false }) {
          Text("Batal", color = AsteraTextSecondary)
        }
      },
    )
  }
}
