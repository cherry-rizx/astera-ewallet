package com.astera.wallet.ui.screens.auth

import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.CodeBoxes
import com.astera.wallet.ui.components.NumericKeypad
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary

/**
 * Kunci aplikasi saat sesi lama dibuka ulang. Langsung menampilkan keypad PIN (tanpa pilihan
 * metode); prompt sidik jari dimunculkan otomatis di atasnya bila perangkat mendukung.
 */
@Composable
fun AppUnlockScreen(
  errorMessage: String?,
  biometricAvailable: Boolean,
  onBiometric: () -> Unit,
  onVerifyPin: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var pin by rememberSaveable { mutableStateOf("") }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AsteraLogoBadge(size = 72.dp)
      Spacer(Modifier.height(18.dp))
      Text("Masukkan PIN ASTERA", style = MaterialTheme.typography.headlineSmall, color = Color.White)
      Spacer(Modifier.height(6.dp))
      Text(
        "Masukkan 6 digit PIN transaksi untuk membuka aplikasi",
        style = MaterialTheme.typography.bodyMedium,
        color = AsteraTextSecondary,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))

      CodeBoxes(code = pin, masked = true, modifier = Modifier.testTag("unlock_pin_boxes"))

      if (!errorMessage.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
          errorMessage,
          color = AsteraRed,
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().testTag("unlock_error"),
        )
      }

      Spacer(Modifier.height(24.dp))
      NumericKeypad(
        onDigit = { digit ->
          if (pin.length < 6) {
            pin += digit
            if (pin.length == 6) {
              onVerifyPin(pin)
              pin = ""
            }
          }
        },
        onDelete = { pin = pin.dropLast(1) },
      )

      Spacer(Modifier.height(16.dp))
      if (biometricAvailable) {
        TextButton(onClick = onBiometric, modifier = Modifier.testTag("unlock_biometric_button")) {
          Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = AsteraSkyBlue, modifier = Modifier.size(22.dp))
          Spacer(Modifier.width(8.dp))
          Text("Gunakan sidik jari", color = AsteraSkyBlue, fontWeight = FontWeight.Bold)
        }
      }
      TextButton(onClick = onSignOut, modifier = Modifier.testTag("unlock_sign_out_button")) {
        Text("Bukan kamu? Keluar", color = AsteraTextSecondary)
      }
    }
  }
}
