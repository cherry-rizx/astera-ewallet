package com.astera.wallet.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.AsteraOutlinedButton
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.AsteraTextField
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraTextSecondary

@Composable
fun AppUnlockScreen(
  errorMessage: String?,
  onBiometric: () -> Unit,
  onVerifyPin: (String) -> Unit,
) {
  var pin by remember { mutableStateOf("") }
  var usePin by remember { mutableStateOf(false) }
  AsteraBackground {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        .widthIn(max = 600.dp).align(Alignment.TopCenter).padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AsteraLogoBadge(size = 80.dp)
      Spacer(Modifier.height(18.dp))
      Text("Verifikasi untuk membuka ASTERA", style = MaterialTheme.typography.headlineSmall, color = Color.White)
      Spacer(Modifier.height(8.dp))
      Text("Gunakan biometrik perangkat atau PIN transaksi.", style = MaterialTheme.typography.bodyMedium, color = AsteraTextSecondary, textAlign = TextAlign.Center)
      Spacer(Modifier.height(24.dp))
      if (usePin) {
        AsteraTextField(
          value = pin,
          onValueChange = { pin = it.filter(Char::isDigit).take(6) },
          label = "PIN Transaksi",
          placeholder = "6 digit PIN",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.testTag("unlock_pin_input"),
        )
      }
      if (!errorMessage.isNullOrBlank()) {
        Text(errorMessage, color = AsteraRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
      }
      if (!usePin) {
        AsteraPrimaryButton(text = "Coba Biometrik", onClick = onBiometric, modifier = Modifier.testTag("unlock_biometric_button"))
        AsteraOutlinedButton(text = "Gunakan PIN", onClick = { usePin = true })
      } else {
        AsteraPrimaryButton(text = "Buka ASTERA", onClick = { onVerifyPin(pin) }, modifier = Modifier.testTag("unlock_pin_button"))
        AsteraOutlinedButton(text = "Kembali ke biometrik", onClick = { usePin = false; pin = "" })
      }
    }
  }
}
