package com.astera.wallet.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class NfcAvailability {
  /** Perangkat tidak punya NFC atau tidak mendukung HCE. */
  UNSUPPORTED,

  /** NFC ada tetapi dimatikan di pengaturan. */
  DISABLED,

  READY,
}

fun nfcAvailability(context: Context): NfcAvailability {
  val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcAvailability.UNSUPPORTED
  val hasHce = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
  if (!hasHce) return NfcAvailability.UNSUPPORTED
  return if (adapter.isEnabled) NfcAvailability.READY else NfcAvailability.DISABLED
}

/** Status NFC yang diperbarui setiap layar kembali aktif (misalnya setelah NFC dinyalakan). */
@Composable
fun rememberNfcAvailability(): NfcAvailability {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var availability by remember { mutableStateOf(nfcAvailability(context)) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) availability = nfcAvailability(context)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  return availability
}

/**
 * Selama [enabled] dan layar aktif, HP ini membaca HP penerima ASTERA lewat NFC (reader mode).
 * [onPayload] dipanggil di main thread dengan URI QR ASTERA yang dibaca.
 */
@Composable
fun NfcRecipientReaderEffect(enabled: Boolean, onPayload: (String) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnPayload by rememberUpdatedState(onPayload)

  DisposableEffect(enabled, lifecycleOwner) {
    val activity = context.findActivity()
    val adapter = NfcAdapter.getDefaultAdapter(context)
    if (!enabled || activity == null || adapter == null) return@DisposableEffect onDispose { }

    val mainHandler = Handler(Looper.getMainLooper())
    val callback =
      NfcAdapter.ReaderCallback { tag ->
        val isoDep = IsoDep.get(tag) ?: return@ReaderCallback
        val payload =
          runCatching {
            isoDep.use {
              it.connect()
              it.timeout = 3_000
              AsteraNfcProtocol.parseResponse(it.transceive(AsteraNfcProtocol.buildSelectApdu()))
            }
          }.getOrNull()
        if (payload != null) mainHandler.post { currentOnPayload(payload) }
      }
    val flags =
      NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

    fun start() = runCatching { adapter.enableReaderMode(activity, callback, flags, null) }
    fun stop() = runCatching { adapter.disableReaderMode(activity) }

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> start()
        Lifecycle.Event.ON_PAUSE -> stop()
        else -> Unit
      }
    }
    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      stop()
    }
  }
}

/** Selama [payload] tidak null, HP ini bisa "ditempel" untuk membagikan payload lewat HCE. */
@Composable
fun NfcShareEffect(payload: String?) {
  DisposableEffect(payload) {
    NfcShareState.sharedPayload = payload
    onDispose { if (NfcShareState.sharedPayload == payload) NfcShareState.sharedPayload = null }
  }
}

private fun Context.findActivity(): Activity? {
  var ctx: Context? = this
  while (ctx is ContextWrapper) {
    if (ctx is Activity) return ctx
    ctx = ctx.baseContext
  }
  return null
}
