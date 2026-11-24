package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.InsertChartOutlined
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraBrightBlue
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary

/** Tab utama. [QRIS] digambar sebagai tombol bulat melayang di tengah, sesuai desain. */
enum class MainTab(val route: String, val label: String) {
  BERANDA("beranda", "Beranda"),
  TRANSAKSI("transaksi", "Transaksi"),
  QRIS("qris", "QRIS"),
  RINGKASAN("ringkasan", "Ringkasan"),
  AKUN("akun", "Akun"),
}

@Composable
fun AsteraBottomBar(
  currentRoute: String?,
  onSelectTab: (MainTab) -> Unit,
) {
  Box(contentAlignment = Alignment.TopCenter) {
    Surface(
      color = Color(0xFF0E1A38),
      border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.35f)),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
      ) {
        MainTab.entries.forEach { tab ->
          val selected = currentRoute == tab.route
          if (tab == MainTab.QRIS) {
            // Ruang kosong untuk tombol QRIS melayang + labelnya.
            NavLabelSlot(tab = tab, selected = selected, onClick = { onSelectTab(tab) }, modifier = Modifier.weight(1f))
          } else {
            NavItem(tab = tab, selected = selected, onClick = { onSelectTab(tab) }, modifier = Modifier.weight(1f))
          }
        }
      }
    }
    QrisFab(
      selected = currentRoute == MainTab.QRIS.route,
      onClick = { onSelectTab(MainTab.QRIS) },
      modifier = Modifier.offset(y = (-22).dp),
    )
  }
}

@Composable
private fun NavItem(tab: MainTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
  val tint = if (selected) AsteraSkyBlue else AsteraTextSecondary
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .padding(vertical = 6.dp)
        .testTag("nav_${tab.route}"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(imageVector = iconFor(tab, selected), contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.height(4.dp))
    NavLabel(tab.label, selected)
  }
}

@Composable
private fun NavLabelSlot(tab: MainTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(top = 30.dp, bottom = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    NavLabel(tab.label, selected)
  }
}

@Composable
private fun NavLabel(text: String, selected: Boolean) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
    color = if (selected) AsteraSkyBlue else AsteraTextSecondary,
  )
}

@Composable
private fun QrisFab(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .size(60.dp)
        .shadow(10.dp, CircleShape)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(AsteraSkyBlue, AsteraBrightBlue)))
        .clickable(onClick = onClick)
        .testTag("nav_qris"),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.QrCode2,
      contentDescription = "QRIS - scan atau tunjukkan QR",
      tint = Color.White,
      modifier = Modifier.size(if (selected) 30.dp else 28.dp),
    )
  }
}

private fun iconFor(tab: MainTab, selected: Boolean): ImageVector =
  when (tab) {
    MainTab.BERANDA -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    MainTab.TRANSAKSI -> if (selected) Icons.Rounded.SwapHoriz else Icons.Outlined.SwapHoriz
    MainTab.QRIS -> Icons.Rounded.QrCode2
    MainTab.RINGKASAN -> if (selected) Icons.Rounded.InsertChart else Icons.Outlined.InsertChartOutlined
    MainTab.AKUN -> if (selected) Icons.Rounded.Person else Icons.Outlined.Person
  }
