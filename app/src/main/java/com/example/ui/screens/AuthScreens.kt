package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.AsteraBackground
import com.example.ui.components.AsteraCard
import com.example.ui.components.AsteraLogoBadge
import com.example.ui.components.AsteraOutlinedButton
import com.example.ui.components.AsteraPrimaryButton
import com.example.ui.components.AsteraTextField
import com.example.ui.components.AsteraTopBar
import com.example.ui.components.SimulasiBadge
import com.example.ui.theme.AsteraAmber
import com.example.ui.theme.AsteraGreen
import com.example.ui.theme.AsteraPrimary
import com.example.ui.theme.AsteraRed
import com.example.ui.theme.AsteraSkyBlue
import com.example.ui.theme.AsteraTextSecondary
import com.example.viewmodel.AuthGateState

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

@Composable
fun AccountBlockedScreen(onSignOut: () -> Unit) {
  AsteraBackground {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        .widthIn(max = 600.dp).align(Alignment.TopCenter).padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AsteraLogoBadge(size = 80.dp)
      Spacer(Modifier.height(18.dp))
      Text("Akun ASTERA diblokir", style = MaterialTheme.typography.headlineSmall, color = Color.White)
      Spacer(Modifier.height(8.dp))
      Text(
        "Akun diblokir setelah 3 kali salah memasukkan PIN. Hubungi petugas ASTERA untuk membuka blokir.",
        style = MaterialTheme.typography.bodyMedium,
        color = AsteraTextSecondary,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      AsteraPrimaryButton(text = "Keluar", onClick = onSignOut, modifier = Modifier.testTag("blocked_sign_out"))
    }
  }
}

@Composable
fun SplashScreen(
  gateState: AuthGateState,
  onNavigateNext: (AuthGateState) -> Unit,
) {
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    alpha.animateTo(1f, animationSpec = tween(durationMillis = 700))
    delay(1500L)
    onNavigateNext(gateState)
  }

  AsteraBackground {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .alpha(alpha.value),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsteraLogoBadge(size = 112.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = "ASTERA",
          style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "DIGITAL BANKING",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = AsteraAmber,
        )
        Spacer(modifier = Modifier.height(14.dp))
        SimulasiBadge(text = "Mode Simulasi")
      }
    }
  }
}

@Composable
fun WelcomeScreen(
  onNavigateLogin: () -> Unit,
  onNavigateRegister: () -> Unit,
) {
  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.Center)
          .padding(horizontal = 24.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.padding(top = 24.dp)) {
        SimulasiBadge(text = "Simulasi Digital Banking")
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text =
            buildAnnotatedString {
              append("Selamat datang di ")
              withStyle(SpanStyle(color = AsteraSkyBlue, fontWeight = FontWeight.Bold)) {
                append("ASTERA")
              }
            },
          style = MaterialTheme.typography.displaySmall,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Kelola transfer dan riwayatmu dengan mudah dan aman.",
          style = MaterialTheme.typography.bodyLarge,
          color = AsteraTextSecondary,
        )
      }

      Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
      ) {
        AsteraLogoBadge(size = 148.dp)
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AsteraPrimaryButton(
          text = "Masuk",
          onClick = onNavigateLogin,
          modifier = Modifier.testTag("welcome_login_button"),
        )
        AsteraOutlinedButton(
          text = "Daftar",
          onClick = onNavigateRegister,
          borderColor = AsteraSkyBlue,
          modifier = Modifier.testTag("welcome_register_button"),
        )
      }
    }
  }
}

@Composable
fun LoginScreen(
  isBusy: Boolean,
  errorMessage: String?,
  onEmailSignIn: (String, String) -> Unit,
  onForgotPassword: (String) -> Unit,
  onNavigateRegister: () -> Unit,
  onBack: () -> Unit,
) {
  var email by remember { mutableStateOf("") }
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

          AsteraPrimaryButton(
            text = "Masuk",
            isLoading = isBusy,
            onClick = { onEmailSignIn(email, password) },
            modifier = Modifier.padding(top = 16.dp).testTag("email_sign_in_button"),
          )
          androidx.compose.material3.TextButton(
            onClick = { onForgotPassword(email) },
            modifier = Modifier.align(Alignment.End).testTag("forgot_password_button"),
          ) {
            Text("Lupa kata sandi?", color = AsteraSkyBlue)
          }
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
