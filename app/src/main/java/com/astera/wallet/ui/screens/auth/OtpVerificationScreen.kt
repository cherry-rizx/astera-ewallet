package com.astera.wallet.ui.screens.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.astera.wallet.service.OtpService
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.components.CodeBoxes
import com.astera.wallet.ui.components.NumericKeypad
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.viewmodel.OtpChallenge
import kotlinx.coroutines.delay

/**
 * Verifikasi dua langkah setelah login email/kata sandi. Kode OTP adalah simulasi: dikirim
 * sebagai notifikasi lokal dan ditampilkan di kartu "SMS Simulasi".
 */
@Composable
fun OtpVerificationScreen(
  email: String,
  challenge: OtpChallenge?,
  errorMessage: String?,
  onSubmit: (String) -> Unit,
  onResend: () -> Unit,
  onCancel: () -> Unit,
) {
  val context = LocalContext.current
  var code by rememberSaveable { mutableStateOf("") }
  var showSimulatedSms by rememberSaveable { mutableStateOf(true) }
  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

  val notificationPermission =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= 33 &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
  LaunchedEffect(Unit) {
    while (true) {
      nowMs = System.currentTimeMillis()
      delay(1_000L)
    }
  }
  // Kosongkan input setiap kali kode baru dikirim.
  LaunchedEffect(challenge?.issuedAtMs) { code = "" }

  val resendRemainingSec =
    challenge?.let { ((it.issuedAtMs + OtpService.RESEND_COOLDOWN_MS - nowMs + 999L) / 1000L).coerceAtLeast(0L) } ?: 0L

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter)
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AsteraTopBar(title = "", onBack = onCancel)
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AsteraLogoBadge(size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(
          text = "Verifikasi Dua Langkah",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = "Masukkan 6 digit kode OTP yang dikirim untuk akun $email",
          style = MaterialTheme.typography.bodyMedium,
          color = AsteraTextSecondary,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))

        if (challenge != null && showSimulatedSms) {
          SimulatedSmsCard(code = challenge.code, onHide = { showSimulatedSms = false })
          Spacer(Modifier.height(18.dp))
        }

        CodeBoxes(code = code, modifier = Modifier.testTag("otp_code_boxes"))

        if (!errorMessage.isNullOrBlank()) {
          Spacer(Modifier.height(12.dp))
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = AsteraRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("otp_error"),
          )
        }

        Spacer(Modifier.height(20.dp))
        NumericKeypad(
          onDigit = { digit ->
            if (code.length < 6) {
              code += digit
              if (code.length == 6) {
                onSubmit(code)
                code = ""
              }
            }
          },
          onDelete = { code = code.dropLast(1) },
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Tidak menerima kode?",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
          )
          TextButton(
            onClick = {
              showSimulatedSms = true
              onResend()
            },
            enabled = resendRemainingSec == 0L,
            modifier = Modifier.testTag("otp_resend_button"),
          ) {
            Text(
              text = if (resendRemainingSec > 0L) "Kirim ulang (${resendRemainingSec}s)" else "Kirim ulang",
              color = if (resendRemainingSec > 0L) AsteraTextSecondary else AsteraSkyBlue,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        TextButton(onClick = onCancel, modifier = Modifier.testTag("otp_cancel_button")) {
          Text("Batal dan keluar", color = AsteraTextSecondary)
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

/** Pengganti SMS sungguhan: kode OTP ditampilkan di sini karena aplikasi ini simulasi. */
@Composable
private fun SimulatedSmsCard(code: String, onHide: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = AsteraCard,
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.6f)),
    modifier = Modifier.fillMaxWidth().testTag("otp_simulated_sms"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Sms, contentDescription = null, tint = AsteraSkyBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = "SMS ASTERA",
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          modifier = Modifier.weight(1f),
        )
        SimulasiBadge()
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = "Kode OTP kamu: $code. Berlaku 5 menit. Jangan bagikan kode ini kepada siapa pun.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onHide) { Text("Sembunyikan", color = AsteraTextSecondary) }
      }
    }
  }
}
