package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.UserDirectoryEntry
import com.example.service.TransactionService
import com.example.ui.components.AsteraBackground
import com.example.ui.components.AsteraCard
import com.example.ui.components.AsteraOutlinedButton
import com.example.ui.components.AsteraPrimaryButton
import com.example.ui.components.AsteraTextField
import com.example.ui.components.AsteraTopBar
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AsteraAmber
import com.example.ui.theme.AsteraCard
import com.example.ui.theme.AsteraCardBorder
import com.example.ui.theme.AsteraFieldBg
import com.example.ui.theme.AsteraGreen
import com.example.ui.theme.AsteraPrimary
import com.example.ui.theme.AsteraSkyBlue
import com.example.ui.theme.AsteraTextSecondary
import com.example.util.QrUtils
import com.example.util.formatRupiah
import com.example.viewmodel.UiState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
  currentUserId: String,
  currentUserName: String,
  currentUserPhone: String,
  currentBalance: Long,
  initialStartWithQr: Boolean,
  searchResults: UiState<List<UserDirectoryEntry>>,
  selectedRecipient: UserDirectoryEntry?,
  onSearchQueryChanged: (String) -> Unit,
  onSelectRecipient: (UserDirectoryEntry?) -> Unit,
  onQrUidScanned: (String, (UserDirectoryEntry) -> Unit, (String) -> Unit) -> Unit,
  onContinueTransfer: (UserDirectoryEntry, Long, String) -> Unit,
  onBack: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(if (initialStartWithQr) 1 else 0) }
  var searchQuery by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }
  var recipientError by remember { mutableStateOf<String?>(null) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var qrFeedback by remember { mutableStateOf<String?>(null) }
  var showMyQrSheet by remember { mutableStateOf(false) }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter),
    ) {
      AsteraTopBar(
        title = "Transfer",
        subtitle = "Saldo tersedia: ${formatRupiah(currentBalance)}",
        onBack = onBack,
        trailingAction = {
          IconButton(
            onClick = { showMyQrSheet = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("show_my_qr_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.QrCode2,
              contentDescription = "Tampilkan QR Saya",
              tint = AsteraAmber,
            )
          }
        },
      )

      // Segmented Tabs ("Input Manual" default, "Scan QR")
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = AsteraFieldBg,
        border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      ) {
        Row(modifier = Modifier.padding(4.dp)) {
          TransferTabChip(
            label = "Input Manual",
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.weight(1f).testTag("tab_input_manual"),
          )
          TransferTabChip(
            label = "Scan QR",
            selected = selectedTab == 1,
            onClick = {
              qrFeedback = null
              selectedTab = 1
            },
            modifier = Modifier.weight(1f).testTag("tab_scan_qr"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (selectedTab == 0) {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          AsteraTextField(
            value = searchQuery,
            onValueChange = {
              searchQuery = it
              onSearchQueryChanged(it)
            },
            label = "Penerima",
            placeholder = "Nomor HP atau nama",
            errorText = recipientError,
            leadingIcon = {
              Icon(Icons.Outlined.Search, contentDescription = null, tint = AsteraSkyBlue)
            },
            modifier = Modifier.testTag("transfer_search_input"),
          )

          // Recipient results card
          when (searchResults) {
            UiState.Loading -> {
              AsteraCard {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AsteraSkyBlue)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Mencari pengguna...", color = AsteraTextSecondary)
                }
              }
            }
            is UiState.Error -> {
              AsteraCard {
                Text(
                  text = searchResults.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }
            is UiState.Success -> {
              val list = searchResults.data
              if (list.isEmpty()) {
                AsteraCard {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Rounded.PersonSearch,
                      contentDescription = null,
                      tint = AsteraTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Pengguna tidak ditemukan. Pastikan penerima sudah mendaftar di ASTERA.",
                      style = MaterialTheme.typography.bodySmall,
                      color = AsteraTextSecondary,
                    )
                  }
                }
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  list.take(5).forEach { entry ->
                    val isSelected = selectedRecipient?.uid == entry.uid
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = if (isSelected) AsteraPrimary.copy(alpha = 0.55f) else AsteraCard,
                      border =
                        BorderStroke(
                          width = if (isSelected) 1.5.dp else 1.dp,
                          color = if (isSelected) AsteraAmber else AsteraCardBorder.copy(alpha = 0.45f),
                        ),
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(14.dp))
                          .clickable {
                            onSelectRecipient(entry)
                            recipientError = null
                          }
                          .testTag("recipient_option_${entry.uid}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        UserAvatar(name = entry.name, photoUrl = null, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                          )
                          Text(
                            text = entry.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = AsteraTextSecondary,
                          )
                        }
                        if (isSelected) {
                          Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Dipilih",
                            tint = AsteraAmber,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          AsteraTextField(
            value = amountText,
            onValueChange = { input ->
              amountText = input.filter { it.isDigit() }.take(9)
              amountError = null
            },
            label = "Nominal",
            placeholder = "0",
            prefix = {
              Text(
                text = "Rp ",
                style = MaterialTheme.typography.titleMedium,
                color = AsteraAmber,
              )
            },
            helperText = "Maksimal Rp 5.000.000 per transaksi dan per hari",
            errorText = amountError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.testTag("transfer_amount_input"),
          )

          AsteraTextField(
            value = noteText,
            onValueChange = { noteText = it.take(150) },
            label = "Pesan (opsional)",
            placeholder = "Tulis pesan di sini...",
            singleLine = false,
            modifier = Modifier.testTag("transfer_note_input"),
          )

          AsteraOutlinedButton(
            text = "Tampilkan QR Saya",
            onClick = { showMyQrSheet = true },
            borderColor = AsteraAmber.copy(alpha = 0.7f),
            contentColor = AsteraAmber,
          )

          AsteraPrimaryButton(
            text = "Lanjut",
            onClick = {
              var ok = true
              val recipient = selectedRecipient
              if (recipient == null) {
                recipientError = "Pilih penerima terlebih dahulu"
                ok = false
              }
              val amount = amountText.toLongOrNull() ?: 0L
              if (amount <= 0L) {
                amountError = "Nominal harus lebih dari Rp 0"
                ok = false
              } else if (amount > TransactionService.MAX_TRANSFER_AMOUNT) {
                amountError = "Maksimal Rp 5.000.000 per transaksi"
                ok = false
              } else if (amount > currentBalance) {
                amountError = "Saldo tidak mencukupi"
                ok = false
              }
              if (ok && recipient != null) {
                onContinueTransfer(recipient, amount, noteText)
              }
            },
            modifier = Modifier.padding(bottom = 24.dp).testTag("transfer_continue_button"),
          )
        }
      } else {
        // Scan QR Tab
        QrScanSection(
          feedbackMessage = qrFeedback,
          onQrScanned = { raw ->
            val uid = QrUtils.parseAsteraQrUid(raw)
            if (uid == null) {
              qrFeedback = "Kode QR tidak dikenali. Gunakan QR ASTERA yang valid."
            } else {
              onQrUidScanned(
                uid,
                { _ ->
                  qrFeedback = null
                  selectedTab = 0
                },
                { err -> qrFeedback = err },
              )
            }
          },
          onSwitchManual = { selectedTab = 0 },
          onShowMyQr = { showMyQrSheet = true },
        )
      }
    }
  }

  if (showMyQrSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMyQrSheet = false },
      containerColor = AsteraCard,
      contentColor = Color.White,
    ) {
      val qrUri = remember(currentUserId) { QrUtils.buildUserQrUri(currentUserId) }
      val bitmap = remember(qrUri) { QrUtils.generateQrBitmap(qrUri, 512) }

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "QR ASTERA Saya",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "$currentUserName • $currentUserPhone",
          style = MaterialTheme.typography.bodyMedium,
          color = AsteraTextSecondary,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color.White,
          modifier = Modifier.size(240.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = "Kode QR ASTERA milik $currentUserName",
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Pindai kode ini untuk kirim saldo simulasi ke $currentUserName",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun TransferTabChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) AsteraPrimary else Color.Transparent,
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize(),
  ) {
    Box(
      modifier = Modifier.padding(vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else AsteraTextSecondary,
      )
    }
  }
}

@Composable
private fun QrScanSection(
  feedbackMessage: String?,
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
        CameraQrPreview(onQrDetected = onQrScanned)
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

@Composable
private fun CameraQrPreview(onQrDetected: (String) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  val scanner =
    remember {
      val options =
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
      BarcodeScanning.getClient(options)
    }
  var alreadyHandled by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
      scanner.close()
    }
  }

  AndroidView(
    factory = { ctx ->
      val previewView = PreviewView(ctx)
      val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
      cameraProviderFuture.addListener(
        {
          try {
            val cameraProvider = cameraProviderFuture.get()
            val preview =
              Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
              }
            val resolutionSelector =
              ResolutionSelector.Builder()
                .setResolutionStrategy(
                  ResolutionStrategy(
                    Size(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                  )
                )
                .build()
            val analysis =
              ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
              val mediaImage = imageProxy.image
              if (mediaImage != null && !alreadyHandled) {
                val inputImage =
                  InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner
                  .process(inputImage)
                  .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (!raw.isNullOrBlank() && !alreadyHandled) {
                      alreadyHandled = true
                      onQrDetected(raw)
                    }
                  }
                  .addOnCompleteListener { imageProxy.close() }
              } else {
                imageProxy.close()
              }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA,
              preview,
              analysis,
            )
          } catch (_: Exception) {
            // Camera unavailable on device/emulator
          }
        },
        ContextCompat.getMainExecutor(ctx),
      )
      previewView
    },
    modifier = Modifier.fillMaxSize(),
  )
}
