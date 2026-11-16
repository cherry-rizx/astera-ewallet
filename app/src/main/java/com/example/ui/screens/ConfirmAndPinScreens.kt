package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.TransactionRecord
import com.example.ui.components.AsteraBackground
import com.example.ui.components.AsteraCard
import com.example.ui.components.AsteraOutlinedButton
import com.example.ui.components.AsteraPrimaryButton
import com.example.ui.components.AsteraTopBar
import com.example.ui.components.SimulasiBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AsteraAmber
import com.example.ui.theme.AsteraAmberSoft
import com.example.ui.theme.AsteraBrightBlue
import com.example.ui.theme.AsteraCard
import com.example.ui.theme.AsteraCardBorder
import com.example.ui.theme.AsteraDivider
import com.example.ui.theme.AsteraPrimary
import com.example.ui.theme.AsteraRed
import com.example.ui.theme.AsteraSkyBlue
import com.example.ui.theme.AsteraTextSecondary
import com.example.util.formatFullTimestamp
import com.example.util.formatRupiah
import com.example.viewmodel.PendingOperation
import com.example.viewmodel.PinSubmissionState
import kotlin.math.roundToInt

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
                      text = pendingOperation.receiverPhone,
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
            if (pendingOperation is PendingOperation.Transfer && pendingOperation.note.isNotBlank()) {
              DetailRow(label = "Pesan", value = pendingOperation.note)
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

@Composable
fun PinVerificationScreen(
  pendingOperation: PendingOperation?,
  pinState: PinSubmissionState,
  onResetPinState: () -> Unit,
  onSubmitPin: (String) -> Unit,
  onBack: () -> Unit,
) {
  var enteredPin by remember { mutableStateOf("") }
  val shakeOffset = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    onResetPinState()
  }

  LaunchedEffect(pinState) {
    if (pinState is PinSubmissionState.WrongPin) {
      enteredPin = ""
      val amplitudes = listOf(-18f, 18f, -14f, 14f, -8f, 8f, 0f)
      amplitudes.forEach { target ->
        shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 45))
      }
    } else if (pinState is PinSubmissionState.Error) {
      enteredPin = ""
    }
  }

  val isLocked = pinState is PinSubmissionState.Blocked
  val isProcessing = pinState is PinSubmissionState.Processing

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
        title = "Masukkan PIN",
        subtitle = "Masukkan 6 digit PIN ASTERA Anda",
        onBack = onBack,
      )

      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Summary Card at top (matching screenshots 08_08_konfirmasi_pin_*)
        if (pendingOperation != null) {
          AsteraCard {
            val subtitle =
              when (pendingOperation) {
                is PendingOperation.Transfer -> "${pendingOperation.receiverName} • ASTERA"
                is PendingOperation.TopUp -> "Top Up Simulasi Multi-Bank"
              }
            Text(
              text = subtitle,
              style = MaterialTheme.typography.titleSmall,
              color = AsteraTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = formatRupiah(pendingOperation.amount),
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
        }

        // 6 PIN Dots + Feedback
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
            modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            repeat(6) { idx ->
              val filled = idx < enteredPin.length
              Box(
                modifier =
                  Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (filled) AsteraAmber else Color.Transparent)
                    .border(
                      width = 2.dp,
                      color =
                        when {
                          pinState is PinSubmissionState.WrongPin -> AsteraRed
                          filled -> AsteraAmber
                          else -> AsteraSkyBlue
                        },
                      shape = CircleShape,
                    )
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          if (isProcessing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AsteraAmber)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Memverifikasi transaksi...", style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
            }
          } else {
            val message =
              when (pinState) {
                is PinSubmissionState.WrongPin ->
                  "PIN salah. Sisa percobaan: ${pinState.remainingAttempts}"
                PinSubmissionState.Blocked -> "Akun diblokir setelah 3 kali salah PIN. Hubungi petugas ASTERA untuk membuka blokir."
                is PinSubmissionState.Error -> pinState.message
                else -> "Masukkan 6 digit PIN untuk melanjutkan"
              }
            val msgColor =
              when (pinState) {
                is PinSubmissionState.WrongPin,
                PinSubmissionState.Blocked,
                is PinSubmissionState.Error -> Color(0xFFFCA5A5)
                else -> AsteraTextSecondary
              }
            Text(
              text = message,
              style = MaterialTheme.typography.bodySmall,
              color = msgColor,
              textAlign = TextAlign.Center,
            )
          }
        }

        // Numeric Keypad
        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
          rows.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              row.forEach { digit ->
                PinDigitButton(
                  digit = digit,
                  enabled = !isLocked && !isProcessing,
                  onClick = {
                    if (enteredPin.length < 6) {
                      val next = enteredPin + digit
                      enteredPin = next
                      if (next.length == 6) {
                        onSubmitPin(next)
                      }
                    }
                  },
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Spacer(modifier = Modifier.size(68.dp))
            PinDigitButton(
              digit = "0",
              enabled = !isLocked && !isProcessing,
              onClick = {
                if (enteredPin.length < 6) {
                  val next = enteredPin + "0"
                  enteredPin = next
                  if (next.length == 6) {
                    onSubmitPin(next)
                  }
                }
              },
            )
            Box(
              modifier =
                Modifier
                  .size(68.dp)
                  .clip(CircleShape)
                  .clickable(enabled = enteredPin.isNotEmpty() && !isProcessing && !isLocked) {
                    enteredPin = enteredPin.dropLast(1)
                  }
                  .testTag("pin_backspace"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Hapus",
                tint = AsteraSkyBlue,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PinDigitButton(
  digit: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = CircleShape,
    color = AsteraCard.copy(alpha = if (enabled) 0.9f else 0.4f),
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.6f)),
    modifier =
      Modifier
        .size(68.dp)
        .clip(CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("pin_key_$digit"),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = digit,
        style = MaterialTheme.typography.headlineSmall,
        color = if (enabled) Color.White else AsteraTextSecondary,
      )
    }
  }
}

@Composable
fun SuccessScreen(
  transaction: TransactionRecord?,
  onBackHome: () -> Unit,
) {
  var showDetailSheet by remember { mutableStateOf(false) }
  val scale = remember { Animatable(0.3f) }

  LaunchedEffect(Unit) {
    scale.animateTo(
      targetValue = 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
  }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.Center)
          .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        // Animated checkmark in amber & royal blue
        Box(
          modifier =
            Modifier
              .size(96.dp)
              .scale(scale.value)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(AsteraAmber, AsteraPrimary)))
              .border(3.dp, AsteraAmber.copy(alpha = 0.75f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Berhasil",
            tint = Color.White,
            modifier = Modifier.size(52.dp),
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
          text = if (transaction?.type == "topup") "Top Up Berhasil!" else "Transfer Berhasil!",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = formatRupiah(transaction?.amount ?: 0L),
          style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
          color = AsteraAmber,
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (transaction != null) {
          AsteraCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (transaction.type == "topup") "Tujuan" else "Penerima",
                style = MaterialTheme.typography.bodySmall,
                color = AsteraTextSecondary,
              )
              SimulasiBadge()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = transaction.receiverName,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "ID Transaksi: ${transaction.id}",
              style = MaterialTheme.typography.bodySmall,
              color = AsteraTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = formatFullTimestamp(transaction.createdAt),
              style = MaterialTheme.typography.bodySmall,
              color = AsteraTextSecondary,
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AsteraOutlinedButton(
          text = "Lihat Detail",
          onClick = { showDetailSheet = true },
          borderColor = AsteraBrightBlue,
          modifier = Modifier.testTag("success_detail_button"),
        )
        AsteraPrimaryButton(
          text = "Kembali ke Beranda",
          onClick = onBackHome,
          modifier = Modifier.testTag("success_home_button"),
        )
      }
    }
  }

  if (showDetailSheet && transaction != null) {
    TransactionDetailBottomSheet(
      transaction = transaction,
      currentUserId = transaction.senderId,
      onDismiss = { showDetailSheet = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
  transaction: TransactionRecord,
  currentUserId: String,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = AsteraCard,
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Detail Transaksi",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        SimulasiBadge()
      }

      HorizontalDivider(color = AsteraDivider)

      DetailRow(label = "ID Transaksi", value = transaction.id)
      DetailRow(
        label = "Jenis",
        value = if (transaction.type == "topup") "Top Up Simulasi" else "Transfer Saldo",
      )
      DetailRow(label = "Pengirim", value = transaction.senderName)
      DetailRow(label = "Penerima", value = transaction.receiverName)
      DetailRow(label = "Nominal", value = formatRupiah(transaction.amount))
      DetailRow(label = "Biaya", value = "Rp 0")
      DetailRow(label = "Waktu", value = formatFullTimestamp(transaction.createdAt))
      if (transaction.note.isNotBlank()) {
        DetailRow(label = "Catatan", value = transaction.note)
      }
      if (transaction.outOfBudget) {
        DetailRow(label = "Status Anggaran", value = "Di luar anggaran")
      }
      if (transaction.splitDetail.isNotEmpty()) {
        HorizontalDivider(color = AsteraDivider)
        Text(
          text = "Rincian Sumber Bank (Multi-Bank Split)",
          style = MaterialTheme.typography.labelLarge,
          color = AsteraAmber,
        )
        transaction.splitDetail.forEach { split ->
          DetailRow(label = split.bankName, value = formatRupiah(split.amount))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      AsteraPrimaryButton(text = "Tutup", onClick = onDismiss)
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = AsteraTextSecondary,
      modifier = Modifier.weight(0.42f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      color = Color.White,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(0.58f),
    )
  }
}
