package com.astera.wallet.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.AsteraTextField
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraGreen
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary

@Composable
fun LoginScreen(
  isBusy: Boolean,
  errorMessage: String?,
  rememberedEmail: String?,
  onEmailSignIn: (email: String, password: String, rememberMe: Boolean) -> Unit,
  onForgotPassword: (String) -> Unit,
  onNavigateRegister: () -> Unit,
  onBack: () -> Unit,
) {
  var email by remember { mutableStateOf(rememberedEmail.orEmpty()) }
  var rememberMe by remember { mutableStateOf(rememberedEmail != null) }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter)
          .verticalScroll(rememberScrollState()),
    ) {
      AsteraTopBar(title = "", onBack = onBack)

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AsteraLogoBadge(size = 80.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = "Masuk ke Akun",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Masuk untuk melanjutkan perjalananmu bersama ASTERA",
          style = MaterialTheme.typography.bodyMedium,
          color = AsteraTextSecondary,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        AsteraCard {
          Text(
            text = "Masuk dengan email",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              "Gunakan email dan kata sandi akun ASTERA Anda.",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
          )
          Spacer(modifier = Modifier.height(20.dp))

          if (!errorMessage.isNullOrBlank()) {
            val isResetConfirmation = errorMessage.startsWith("Tautan reset kata sandi")
            Surface(
              color = (if (isResetConfirmation) AsteraGreen else AsteraRed).copy(alpha = 0.16f),
              border = BorderStroke(1.dp, (if (isResetConfirmation) AsteraGreen else AsteraRed).copy(alpha = 0.5f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            ) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (isResetConfirmation) AsteraGreen else Color(0xFFFCA5A5),
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          AsteraTextField(
            value = email,
            onValueChange = { email = it.trimStart() },
            label = "Email",
            placeholder = "nama@email.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = AsteraSkyBlue) },
            modifier = Modifier.testTag("login_email_input"),
          )
          Spacer(modifier = Modifier.height(12.dp))
          AsteraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Kata Sandi",
            placeholder = "Masukkan kata sandi",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AsteraSkyBlue) },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showPassword) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                  tint = AsteraTextSecondary,
                )
              }
            },
            modifier = Modifier.testTag("login_password_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier =
                Modifier
                  .weight(1f)
                  .clickable { rememberMe = !rememberMe }
                  .testTag("remember_me_toggle"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = AsteraSkyBlue,
                    uncheckedColor = AsteraTextSecondary,
                    checkmarkColor = Color(0xFF101E40),
                  ),
              )
              Text("Ingat saya", style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
            }
            TextButton(
              onClick = { onForgotPassword(email) },
              modifier = Modifier.testTag("forgot_password_button"),
            ) {
              Text("Lupa kata sandi?", color = AsteraSkyBlue, fontWeight = FontWeight.Bold)
            }
          }

          AsteraPrimaryButton(
            text = "Masuk",
            isLoading = isBusy,
            onClick = { onEmailSignIn(email, password, rememberMe) },
            modifier = Modifier.padding(top = 8.dp).testTag("email_sign_in_button"),
          )
          Text(
            text = "Setelah masuk, kamu akan diminta kode OTP (simulasi) untuk verifikasi dua langkah.",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .clickable(onClick = onNavigateRegister)
              .padding(8.dp)
              .testTag("go_to_register_link"),
        ) {
          Text(
            text = "Belum punya akun? ",
            style = MaterialTheme.typography.bodyMedium,
            color = AsteraTextSecondary,
          )
          Text(
            text = "Daftar",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = AsteraAmber,
          )
        }
      }
    }
  }
}
