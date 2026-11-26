package com.astera.wallet.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.AsteraOutlinedButton
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary

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
