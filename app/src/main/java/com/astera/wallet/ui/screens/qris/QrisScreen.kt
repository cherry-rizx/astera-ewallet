package com.astera.wallet.ui.screens.qris

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.astera.wallet.model.UserDirectoryEntry
import com.astera.wallet.model.UserProfile
import com.astera.wallet.nfc.NfcAvailability
import com.astera.wallet.nfc.NfcRecipientReaderEffect
import com.astera.wallet.nfc.NfcShareEffect
import com.astera.wallet.nfc.rememberNfcAvailability
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.CameraQrScanner
import com.astera.wallet.ui.components.UserAvatar
import com.astera.wallet.ui.components.scanQrFromImage
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.QrUtils
import com.astera.wallet.util.maskedWalletNumber

private enum class QrisTab(val label: String) {
  SCAN("Scan QR"),
  SHOW("Tunjukkan QR"),
}

/**
 * Layar "QR Astera" (tombol QRIS di tengah bottom nav): pindai QR / NFC penerima untuk transfer,
 * atau tunjukkan QR & aktifkan "Terima via NFC" agar orang lain bisa mengirim saldo.
 */
@Composable
fun QrisScreen(
  currentUserId: String,
  profile: UserProfile?,
  onQrUidScanned: (String, (UserDirectoryEntry) -> Unit, (String) -> Unit) -> Unit,
  onRecipientReady: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by rememberSaveable { mutableStateOf(QrisTab.SCAN) }
  var feedback by remember { mutableStateOf<String?>(null) }
  var scanKey by remember { mutableIntStateOf(0) }
  val nfcAvailability = rememberNfcAvailability()

  fun handlePayload(raw: String?, source: String) {
    val uid = raw?.let { QrUtils.parseAsteraQrUid(it) }
    if (uid == null) {
      feedback = "$source tidak dikenali. Gunakan QR ASTERA yang valid."
      scanKey++
      return
    }
    onQrUidScanned(
      uid,
      { _ ->
        feedback = null
        onRecipientReady()
      },
      { err ->
        feedback = err
        scanKey++
      },
    )
  }

  NfcRecipientReaderEffect(enabled = selectedTab == QrisTab.SCAN && nfcAvailability == NfcAvailability.READY) {
    handlePayload(it, "Data NFC")
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("QR Astera", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
          "Scan atau tunjukkan kode untuk bertransaksi",
          style = MaterialTheme.typography.bodyMedium,
          color = AsteraTextSecondary,
        )
      }

      QrisTabSwitcher(
        selected = selectedTab,
        onSelect = {
          feedback = null
          selectedTab = it
        },
      )

      when (selectedTab) {
        QrisTab.SCAN ->
          ScanQrContent(
            scanKey = scanKey,
            nfcAvailability = nfcAvailability,
            feedback = feedback,
            onQrDetected = { handlePayload(it, "Kode QR") },
            onGalleryResult = { handlePayload(it, "Gambar QR") },
          )
        QrisTab.SHOW ->
          ShowQrContent(
            currentUserId = currentUserId,
            profile = profile,
            nfcAvailability = nfcAvailability,
          )
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun QrisTabSwitcher(selected: QrisTab, onSelect: (QrisTab) -> Unit) {
  Surface(
    shape = RoundedCornerShape(50),
    color = AsteraFieldBg,
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(modifier = Modifier.padding(4.dp)) {
      QrisTab.entries.forEach { tab ->
        val isSelected = tab == selected
        Box(
          modifier =
            Modifier
              .weight(1f)
              .clip(RoundedCornerShape(50))
              .clickable { onSelect(tab) }
              .minimumInteractiveComponentSize()
              .testTag("qris_tab_${tab.name.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = if (isSelected) AsteraPrimary.copy(alpha = 0.7f) else Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              ),
              color = if (isSelected) AsteraSkyBlue else AsteraTextSecondary,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 12.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ScanQrContent(
  scanKey: Int,
  nfcAvailability: NfcAvailability,
  feedback: String?,
  onQrDetected: (String) -> Unit,
  onGalleryResult: (String?) -> Unit,
) {
  val context = LocalContext.current
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var torchOn by remember { mutableStateOf(false) }
  var torchAvailable by remember { mutableStateOf(false) }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted }
  val galleryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) scanQrFromImage(context, uri, onGalleryResult)
    }
  LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

  Box(
    modifier =
      Modifier
        .fillMaxWidth(0.86f)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(24.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Surface(color = Color(0xFF050B1C), modifier = Modifier.fillMaxSize()) {}
    if (hasPermission) {
      CameraQrScanner(
        onQrDetected = onQrDetected,
        scanKey = scanKey,
        torchEnabled = torchOn,
        onTorchAvailable = { torchAvailable = it },
      )
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = AsteraTextSecondary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(10.dp))
        Text(
          "Izin kamera diperlukan untuk memindai QR. Kamu tetap bisa memilih gambar QR dari galeri.",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        AsteraPrimaryButton(text = "Izinkan Kamera", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
      }
    }
    ScanFrameCorners()
  }

  Text(
    "Arahkan kamera ke kode QR",
    style = MaterialTheme.typography.bodyMedium,
    color = AsteraTextSecondary,
  )

  Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
    RoundActionButton(
      label = "Kilat",
      enabled = hasPermission && torchAvailable,
      onClick = { torchOn = !torchOn },
      testTag = "qris_torch_button",
    ) {
      Icon(
        if (torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
        contentDescription = if (torchOn) "Matikan kilat" else "Nyalakan kilat",
        tint = AsteraAmber,
      )
    }
    RoundActionButton(
      label = "Galeri",
      onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
      testTag = "qris_gallery_button",
    ) {
      Icon(Icons.Outlined.Image, contentDescription = "Pilih QR dari galeri", tint = AsteraSkyBlue)
    }
  }

  if (nfcAvailability != NfcAvailability.UNSUPPORTED) {
    NfcStatusCard(
      availability = nfcAvailability,
      readyTitle = "Atau tap HP penerima",
      readyBody = "Tempelkan HP ke HP penerima yang sedang membuka Tunjukkan QR dengan NFC aktif.",
    )
  }

  if (!feedback.isNullOrBlank()) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = AsteraAmber.copy(alpha = 0.16f),
      border = BorderStroke(1.dp, AsteraAmber.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth().testTag("qris_feedback"),
    ) {
      Text(feedback, style = MaterialTheme.typography.bodySmall, color = AsteraAmber, modifier = Modifier.padding(12.dp))
    }
  }
}

@Composable
private fun ShowQrContent(
  currentUserId: String,
  profile: UserProfile?,
  nfcAvailability: NfcAvailability,
) {
  val qrUri = remember(currentUserId) { QrUtils.buildUserQrUri(currentUserId) }
  val bitmap = remember(qrUri) { QrUtils.generateQrBitmap(qrUri, 512) }
  var shareViaNfc by rememberSaveable { mutableStateOf(true) }
  val name = profile?.name ?: "Nasabah ASTERA"

  NfcShareEffect(payload = if (shareViaNfc && nfcAvailability == NfcAvailability.READY) qrUri else null)

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xFFF4F7FD),
    modifier = Modifier.fillMaxWidth(0.86f).testTag("qris_my_qr_card"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      UserAvatar(name = name, photoUrl = profile?.photoUrl, size = 52.dp)
      Spacer(Modifier.height(10.dp))
      Text(name, style = MaterialTheme.typography.titleMedium, color = Color(0xFF101E40))
      Text(
        "Dompet ASTERA ${maskedWalletNumber(profile?.phone)}",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF5B6B8C),
      )
      Spacer(Modifier.height(14.dp))
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Kode QR ASTERA milik $name",
        modifier = Modifier.size(200.dp),
      )
      Spacer(Modifier.height(14.dp))
      Text(
        "Pindai kode ini untuk kirim saldo ke $name",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF5B6B8C),
        textAlign = TextAlign.Center,
      )
    }
  }

  when (nfcAvailability) {
    NfcAvailability.UNSUPPORTED -> Unit
    NfcAvailability.DISABLED ->
      NfcStatusCard(availability = nfcAvailability, readyTitle = "", readyBody = "")
    NfcAvailability.READY ->
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = AsteraCard,
        border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Nfc, contentDescription = null, tint = AsteraSkyBlue, modifier = Modifier.size(28.dp))
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Terima via NFC", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(
              if (shareViaNfc) "Aktif. Pengirim cukup menempelkan HP-nya ke HP kamu selama layar ini terbuka."
              else "Nyalakan agar pengirim bisa tap HP untuk mengisi penerima.",
              style = MaterialTheme.typography.bodySmall,
              color = AsteraTextSecondary,
            )
          }
          Switch(
            checked = shareViaNfc,
            onCheckedChange = { shareViaNfc = it },
            colors = SwitchDefaults.colors(checkedTrackColor = AsteraSkyBlue),
            modifier = Modifier.testTag("qris_nfc_share_switch"),
          )
        }
      }
  }
}

@Composable
private fun NfcStatusCard(availability: NfcAvailability, readyTitle: String, readyBody: String) {
  val context = LocalContext.current
  val disabled = availability == NfcAvailability.DISABLED
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = AsteraCard,
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.5f)),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(enabled = disabled) {
          runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        },
  ) {
    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Nfc, contentDescription = null, tint = AsteraSkyBlue, modifier = Modifier.size(28.dp))
      Spacer(Modifier.width(12.dp))
      Column {
        Text(if (disabled) "NFC mati" else readyTitle, style = MaterialTheme.typography.titleSmall, color = Color.White)
        Text(
          if (disabled) "Ketuk untuk menyalakan NFC di pengaturan." else readyBody,
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
        )
      }
    }
  }
}

@Composable
private fun RoundActionButton(
  label: String,
  onClick: () -> Unit,
  testTag: String,
  enabled: Boolean = true,
  icon: @Composable () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      shape = CircleShape,
      color = AsteraCard.copy(alpha = if (enabled) 1f else 0.4f),
      border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.6f)),
      modifier =
        Modifier
          .size(48.dp)
          .clip(CircleShape)
          .clickable(enabled = enabled, onClick = onClick)
          .testTag(testTag),
    ) {
      Box(contentAlignment = Alignment.Center) { icon() }
    }
    Spacer(Modifier.height(6.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = AsteraTextSecondary)
  }
}

/** Empat sudut putih di tepi bingkai pemindai, seperti pada desain. */
@Composable
private fun ScanFrameCorners() {
  Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
    val len = size.minDimension * 0.12f
    val stroke = 4.dp.toPx()
    val w = size.width
    val h = size.height
    val c = Color.White
    fun line(a: Offset, b: Offset) = drawLine(c, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
    line(Offset(0f, 0f), Offset(len, 0f)); line(Offset(0f, 0f), Offset(0f, len))
    line(Offset(w, 0f), Offset(w - len, 0f)); line(Offset(w, 0f), Offset(w, len))
    line(Offset(0f, h), Offset(len, h)); line(Offset(0f, h), Offset(0f, h - len))
    line(Offset(w, h), Offset(w - len, h)); line(Offset(w, h), Offset(w, h - len))
  }
}
