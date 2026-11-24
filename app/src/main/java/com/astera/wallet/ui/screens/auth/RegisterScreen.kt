package com.astera.wallet.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.AsteraTextField
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.viewmodel.AuthGateState

@Composable
fun RegisterScreen(
  needsProfileCompletion: AuthGateState.NeedsRegistration?,
  isBusy: Boolean,
  serverError: String?,
  onSubmitRegistration: (email: String, password: String, name: String, phone: String, pin: String) -> Unit,
  onBack: () -> Unit,
) {
  var email by remember { mutableStateOf(needsProfileCompletion?.email ?: "") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var name by remember(needsProfileCompletion?.uid) {
    mutableStateOf(needsProfileCompletion?.suggestedName ?: "")
  }
  var phone by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var showPin by remember { mutableStateOf(false) }
  var acceptedTerms by remember { mutableStateOf(false) }

  var nameError by remember { mutableStateOf<String?>(null) }
  var emailError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }
  var confirmPasswordError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var pinError by remember { mutableStateOf<String?>(null) }
  var confirmPinError by remember { mutableStateOf<String?>(null) }
  var termsError by remember { mutableStateOf<String?>(null) }

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
      AsteraTopBar(
        title = "Buat Akun Baru",
        subtitle = "Langkah kecil untuk masa depan lebih besar",
        onBack = onBack,
      )

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        AsteraTextField(
          value = email,
          onValueChange = { email = it.trimStart(); emailError = null },
          label = "Email",
          placeholder = "nama@email.com",
          readOnly = needsProfileCompletion != null,
          errorText = emailError,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = AsteraSkyBlue) },
          modifier = Modifier.testTag("register_email_input"),
        )
        if (needsProfileCompletion == null) {
          AsteraTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = "Kata Sandi",
            placeholder = "Minimal 8 karakter",
            errorText = passwordError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AsteraSkyBlue) },
            modifier = Modifier.testTag("register_password_input"),
          )
          AsteraTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmPasswordError = null },
            label = "Konfirmasi Kata Sandi",
            placeholder = "Ulangi kata sandi",
            errorText = confirmPasswordError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AsteraSkyBlue) },
            modifier = Modifier.testTag("register_confirm_password_input"),
          )
        }

          AsteraTextField(
            value = name,
            onValueChange = {
              name = it
              nameError = null
            },
            label = "Nama Lengkap",
            placeholder = "Masukkan nama lengkap",
            errorText = nameError,
            leadingIcon = {
              Icon(Icons.Outlined.Person, contentDescription = null, tint = AsteraSkyBlue)
            },
            modifier = Modifier.testTag("register_name_input"),
          )

          AsteraTextField(
            value = phone,
            onValueChange = { input ->
              phone = input.filter { it.isDigit() }.take(13)
              phoneError = null
            },
            label = "Nomor Handphone",
            placeholder = "08xxxxxxxxxx",
            errorText = phoneError,
            helperText = "Diawali 08, terdiri dari 10-13 digit angka",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
              Icon(Icons.Outlined.Phone, contentDescription = null, tint = AsteraSkyBlue)
            },
            modifier = Modifier.testTag("register_phone_input"),
          )

          AsteraTextField(
            value = pin,
            onValueChange = { input ->
              pin = input.filter { it.isDigit() }.take(6)
              pinError = null
            },
            label = "PIN Transaksi (6 Digit)",
            placeholder = "Masukkan 6 digit PIN",
            errorText = pinError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation =
              if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
              Icon(Icons.Outlined.Lock, contentDescription = null, tint = AsteraSkyBlue)
            },
            trailingIcon = {
              IconButton(onClick = { showPin = !showPin }) {
                Icon(
                  imageVector =
                    if (showPin) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showPin) "Sembunyikan PIN" else "Tampilkan PIN",
                  tint = AsteraTextSecondary,
                )
              }
            },
            modifier = Modifier.testTag("register_pin_input"),
          )

          AsteraTextField(
            value = confirmPin,
            onValueChange = { input ->
              confirmPin = input.filter { it.isDigit() }.take(6)
              confirmPinError = null
            },
            label = "Konfirmasi PIN",
            placeholder = "Ulangi 6 digit PIN",
            errorText = confirmPinError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation =
              if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
              Icon(Icons.Outlined.Lock, contentDescription = null, tint = AsteraSkyBlue)
            },
            modifier = Modifier.testTag("register_confirm_pin_input"),
          )

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable {
                    acceptedTerms = !acceptedTerms
                    termsError = null
                  }
                  .padding(vertical = 4.dp)
                  .testTag("register_terms_checkbox"),
            ) {
              Checkbox(
                checked = acceptedTerms,
                onCheckedChange = {
                  acceptedTerms = it
                  termsError = null
                },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = AsteraPrimary,
                    checkmarkColor = Color.White,
                    uncheckedColor = AsteraTextSecondary,
                  ),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Saya setuju dengan Syarat & Ketentuan dan Kebijakan Privasi ASTERA.",
                style = MaterialTheme.typography.bodySmall,
                color = AsteraTextSecondary,
              )
            }
            if (termsError != null) {
              Text(
                text = termsError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp),
              )
            }
          }

          if (!serverError.isNullOrBlank()) {
            Text(
              text = serverError,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }

        AsteraPrimaryButton(
          text = if (needsProfileCompletion == null) "Buat Akun" else "Selesaikan Pendaftaran",
          isLoading = isBusy,
          onClick = {
            var valid = true
            if (needsProfileCompletion == null) {
              if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                emailError = "Masukkan alamat email yang valid"
                valid = false
              }
              if (password.length < 8) {
                passwordError = "Kata sandi minimal 8 karakter"
                valid = false
              }
              if (confirmPassword != password) {
                confirmPasswordError = "Konfirmasi kata sandi tidak cocok"
                valid = false
              }
            }
              if (name.trim().isEmpty()) {
                nameError = "Nama lengkap wajib diisi"
                valid = false
              }
              if (!phone.matches(Regex("^08[0-9]{8,11}$"))) {
                phoneError = "Nomor HP harus diawali 08 dan terdiri dari 10-13 digit"
                valid = false
              }
              if (!pin.matches(Regex("^[0-9]{6}$"))) {
                pinError = "PIN harus terdiri dari 6 digit angka"
                valid = false
              }
              if (confirmPin != pin) {
                confirmPinError = "Konfirmasi PIN tidak cocok"
                valid = false
              }
              if (!acceptedTerms) {
                termsError = "Anda harus menyetujui Syarat & Ketentuan"
                valid = false
              }
              if (valid) {
                onSubmitRegistration(email.trim(), password, name, phone, pin)
              }
          },
          modifier = Modifier.padding(top = 4.dp, bottom = 24.dp).testTag("register_submit_button"),
        )
      }
    }
  }
}
