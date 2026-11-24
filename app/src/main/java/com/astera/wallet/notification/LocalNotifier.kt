package com.astera.wallet.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.astera.wallet.R

/** Menampilkan notifikasi sistem lokal (tanpa server), misalnya kode OTP simulasi. */
object LocalNotifier {
  const val CHANNEL_SECURITY = "astera_security"

  /** @return true jika notifikasi berhasil ditampilkan (izin diberikan). */
  fun showSecurityNotification(context: Context, id: Int, title: String, body: String): Boolean {
    ensureChannel(context)
    val granted =
      Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return false
    val notification =
      NotificationCompat.Builder(context, CHANNEL_SECURITY)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    return runCatching { NotificationManagerCompat.from(context).notify(id, notification) }.isSuccess
  }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(CHANNEL_SECURITY, "Keamanan ASTERA", NotificationManager.IMPORTANCE_HIGH)
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }
}
