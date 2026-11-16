package com.example.service

import com.astera.wallet.R

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AsteraFirebaseMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    CoroutineScope(Dispatchers.IO).launch {
      runCatching { tokenRef(uid).set(mapOf("uid" to uid, "token" to token)).await() }
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    val notification = message.notification ?: return
    createNotificationChannel()
    val builder =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(notification.title ?: "ASTERA")
        .setContentText(notification.body.orEmpty())
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
    runCatching {
      NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }
  }

  private fun tokenRef(uid: String) =
    FirebaseFirestore.getInstance(
      FirebaseApp.getInstance(),
      getString(R.string.firestore_database_id),
    ).collection("fcm_tokens").document(uid)

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(CHANNEL_ID, "Transaksi ASTERA", NotificationManager.IMPORTANCE_DEFAULT)
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_ID = "astera_transactions"
  }
}
