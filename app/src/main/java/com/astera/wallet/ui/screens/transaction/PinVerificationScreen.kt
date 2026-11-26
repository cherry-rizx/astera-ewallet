package com.astera.wallet.ui.screens.transaction

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.viewmodel.PendingOperation
import com.astera.wallet.viewmodel.PinSubmissionState
import kotlin.math.roundToInt

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
                is PendingOperation.ExternalTransfer -> "${pendingOperation.accountName} • ${pendingOperation.provider}"
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
