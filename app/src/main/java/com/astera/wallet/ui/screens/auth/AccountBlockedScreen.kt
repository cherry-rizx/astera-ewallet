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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.theme.AsteraTextSecondary

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
