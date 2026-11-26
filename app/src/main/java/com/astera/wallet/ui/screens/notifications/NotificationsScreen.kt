package com.astera.wallet.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.AppNotification
import com.astera.wallet.model.NotificationCategory
import com.astera.wallet.model.NotificationKind
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraTopBar
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraGreen
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatNotificationTime

@Composable
fun NotificationsScreen(
  notifications: List<AppNotification>,
  onOpenTransaction: (transactionId: String) -> Unit,
  onBack: () -> Unit,
) {
  // null = "Semua"
  var selectedCategory by rememberSaveable { mutableStateOf<NotificationCategory?>(null) }
  val visible = notifications.filter { selectedCategory == null || it.category == selectedCategory }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter),
    ) {
      AsteraTopBar(title = "Notifikasi", subtitle = "Informasi terbaru dari ASTERA", onBack = onBack)

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CategoryChip("Semua", selectedCategory == null, Modifier.weight(1f)) { selectedCategory = null }
        NotificationCategory.entries.forEach { category ->
          CategoryChip(category.label, selectedCategory == category, Modifier.weight(1f)) {
            selectedCategory = category
          }
        }
      }

      Spacer(Modifier.height(14.dp))

      if (visible.isEmpty()) {
        AsteraCard(modifier = Modifier.padding(horizontal = 20.dp)) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = AsteraTextSecondary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Belum ada notifikasi", style = MaterialTheme.typography.titleSmall, color = Color.White)
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(visible, key = { it.id }) { item ->
            NotificationRow(
              item = item,
              onClick = item.transactionId?.let { id -> { onOpenTransaction(id) } },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) AsteraPrimary.copy(alpha = 0.7f) else AsteraFieldBg,
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = if (selected) 0.9f else 0.4f)),
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("notif_tab_${label.lowercase()}"),
  ) {
    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
        color = if (selected) AsteraSkyBlue else AsteraTextSecondary,
      )
    }
  }
}

@Composable
private fun NotificationRow(item: AppNotification, onClick: (() -> Unit)?) {
  val (icon, tint) = iconFor(item.kind)
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = AsteraCard,
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.4f)),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .testTag("notification_${item.id}"),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, tint.copy(alpha = 0.8f)),
        modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
      }
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
        Spacer(Modifier.height(2.dp))
        Text(item.body, style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
        val time = formatNotificationTime(item.timestamp)
        if (time.isNotEmpty()) {
          Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = AsteraTextSecondary,
            modifier = Modifier.align(Alignment.End),
          )
        }
      }
    }
  }
}

private fun iconFor(kind: NotificationKind): Pair<ImageVector, Color> =
  when (kind) {
    NotificationKind.TRANSFER_OUT -> Icons.AutoMirrored.Rounded.CallMade to AsteraSkyBlue
    NotificationKind.TRANSFER_IN -> Icons.AutoMirrored.Rounded.CallReceived to AsteraGreen
    NotificationKind.TOP_UP -> Icons.Rounded.Add to AsteraSkyBlue
    NotificationKind.BUDGET -> Icons.Rounded.PieChart to AsteraAmber
    NotificationKind.SECURITY -> Icons.Outlined.Shield to AsteraRed
    NotificationKind.PROMO -> Icons.Outlined.Star to AsteraSkyBlue
    NotificationKind.REMINDER -> Icons.Outlined.Schedule to AsteraSkyBlue
    NotificationKind.INFO -> Icons.Outlined.Info to AsteraSkyBlue
  }
