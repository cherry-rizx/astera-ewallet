package com.astera.wallet.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.viewmodel.AuthGateState
import kotlinx.coroutines.delay

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
