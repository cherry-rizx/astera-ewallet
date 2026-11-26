package com.astera.wallet.ui.screens.transfer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraOutlinedButton
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.CameraQrScanner
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary

@Composable
internal fun QrScanSection(
  feedbackMessage: String?,
  scanKey: Int,
  onQrScanned: (String) -> Unit,
  onSwitchManual: () -> Unit,
  onShowMyQr: () -> Unit,
) {
  val context = LocalContext.current
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var permissionDenied by remember { mutableStateOf(false) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPermission = granted
      permissionDenied = !granted
    }

  LaunchedEffect(Unit) {
    if (!hasPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (hasPermission) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, AsteraAmber),
        color = Color.Black,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
      ) {
        CameraQrScanner(onQrDetected = onQrScanned, scanKey = scanKey)
      }
      Text(
        text = "Arahkan kamera ke kode QR ASTERA penerima",
        style = MaterialTheme.typography.bodyMedium,
        color = AsteraTextSecondary,
        textAlign = TextAlign.Center,
      )
    } else {
      AsteraCard {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = AsteraAmber,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Kamera tidak tersedia",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              if (permissionDenied)
                "Izin kamera ditolak. Anda tetap dapat melakukan transfer melalui Input Manual."
              else "Memerlukan izin kamera untuk memindai QR.",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))
          AsteraPrimaryButton(
            text = "Pindah ke Input Manual",
            onClick = onSwitchManual,
            modifier = Modifier.testTag("switch_to_manual_button"),
          )
        }
      }
    }

    if (!feedbackMessage.isNullOrBlank()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = AsteraAmber.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, AsteraAmber.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = feedbackMessage,
          style = MaterialTheme.typography.bodySmall,
          color = AsteraAmber,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    AsteraOutlinedButton(
      text = "Tampilkan QR Saya",
      onClick = onShowMyQr,
      borderColor = AsteraSkyBlue,
    )
  }
}
