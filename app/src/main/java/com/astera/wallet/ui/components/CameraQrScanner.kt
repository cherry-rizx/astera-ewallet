package com.astera.wallet.ui.components

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private fun qrScannerOptions(): BarcodeScannerOptions =
  BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()

/**
 * Pratinjau kamera belakang yang memindai QR dengan ML Kit.
 *
 * @param scanKey ubah nilainya untuk memindai lagi setelah satu QR terbaca.
 * @param torchEnabled menyalakan senter (tombol "Kilat") jika kamera punya lampu.
 * @param onTorchAvailable dipanggil sekali setelah kamera siap.
 */
@Composable
fun CameraQrScanner(
  onQrDetected: (String) -> Unit,
  modifier: Modifier = Modifier,
  scanKey: Int = 0,
  torchEnabled: Boolean = false,
  onTorchAvailable: (Boolean) -> Unit = {},
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  val scanner = remember { BarcodeScanning.getClient(qrScannerOptions()) }
  val handled = remember { AtomicBoolean(false) }
  val currentOnQrDetected by rememberUpdatedState(onQrDetected)
  val currentOnTorchAvailable by rememberUpdatedState(onTorchAvailable)
  var camera by remember { mutableStateOf<Camera?>(null) }

  LaunchedEffect(scanKey) { handled.set(false) }
  LaunchedEffect(torchEnabled, camera) {
    val cam = camera ?: return@LaunchedEffect
    if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(torchEnabled)
  }
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
              Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val resolutionSelector =
              ResolutionSelector.Builder()
                .setResolutionStrategy(
                  ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
            val analysis =
              ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
              val mediaImage = imageProxy.image
              if (mediaImage != null && !handled.get()) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner
                  .process(inputImage)
                  .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (!raw.isNullOrBlank() && handled.compareAndSet(false, true)) {
                      currentOnQrDetected(raw)
                    }
                  }
                  .addOnCompleteListener { imageProxy.close() }
              } else {
                imageProxy.close()
              }
            }
            cameraProvider.unbindAll()
            val boundCamera =
              cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            camera = boundCamera
            currentOnTorchAvailable(boundCamera.cameraInfo.hasFlashUnit())
          } catch (_: Exception) {
            // Kamera tidak tersedia di perangkat/emulator.
            currentOnTorchAvailable(false)
          }
        },
        ContextCompat.getMainExecutor(ctx),
      )
      previewView
    },
    modifier = modifier.fillMaxSize(),
  )
}

/** Memindai QR dari gambar galeri. [onResult] menerima isi QR, atau null jika tidak ditemukan. */
fun scanQrFromImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
  val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
  if (image == null) {
    onResult(null)
    return
  }
  val scanner = BarcodeScanning.getClient(qrScannerOptions())
  scanner
    .process(image)
    .addOnSuccessListener { barcodes -> onResult(barcodes.firstOrNull()?.rawValue) }
    .addOnFailureListener { onResult(null) }
    .addOnCompleteListener { scanner.close() }
}
