package com.astera.wallet.ui.screens.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.theme.AsteraDivider
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.MonthlySpending
import com.astera.wallet.util.buildMonthlySpending
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.util.formatRupiahCompact
import com.astera.wallet.util.longMonthLabel
import com.astera.wallet.util.shortMonthLabel
import com.astera.wallet.util.topRecipients
import com.astera.wallet.viewmodel.UiState

/** Ringkasan pengeluaran bulanan (tab "Ringkasan"): grafik 6 bulan, total, dan penerima terbanyak. */
@Composable
fun SpendingSummaryScreen(
  currentUserId: String,
  transactionsState: UiState<List<TransactionRecord>>,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column {
        Text("Ringkasan", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text("Pantau pola pengeluaran simulasimu", style = MaterialTheme.typography.bodyMedium, color = AsteraTextSecondary)
      }

      when (transactionsState) {
        UiState.Loading ->
          Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AsteraSkyBlue)
          }
        is UiState.Error ->
          AsteraCard { Text(transactionsState.message, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodyMedium) }
        is UiState.Success -> SummaryContent(currentUserId, transactionsState.data)
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SummaryContent(currentUserId: String, transactions: List<TransactionRecord>) {
  val months = remember(transactions, currentUserId) { buildMonthlySpending(currentUserId, transactions) }
  var selectedKey by rememberSaveable { mutableStateOf(months.last().monthKey) }
  val selected = months.firstOrNull { it.monthKey == selectedKey } ?: months.last()
  val recipients = remember(transactions, selected.monthKey) { topRecipients(currentUserId, transactions, selected.monthKey) }

  // Kartu utama: pengeluaran bulan terpilih.
  AsteraCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Pengeluaran ${longMonthLabel(selected.monthKey)}",
        style = MaterialTheme.typography.bodyMedium,
        color = AsteraTextSecondary,
        modifier = Modifier.weight(1f),
      )
      SimulasiBadge()
    }
    Spacer(Modifier.height(6.dp))
    Text(
      formatRupiah(selected.outgoing),
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = Color.White,
      modifier = Modifier.testTag("summary_selected_outgoing"),
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      StatTile("Transfer masuk", formatRupiah(selected.incoming), Modifier.weight(1f))
      StatTile("Top up", formatRupiah(selected.topUp), Modifier.weight(1f))
    }
  }

  // Grafik batang pengeluaran 6 bulan. Satu seri -> tanpa legenda; judul menamai serinya.
  AsteraCard {
    Text("Pengeluaran 6 bulan terakhir", style = MaterialTheme.typography.titleMedium, color = Color.White)
    Text("Ketuk batang untuk melihat detail bulan itu", style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
    Spacer(Modifier.height(16.dp))
    if (months.all { it.outgoing == 0L }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(Icons.Outlined.BarChart, contentDescription = null, tint = AsteraTextSecondary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(
          "Belum ada transfer keluar dalam 6 bulan terakhir.",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      MonthlyBarChart(
        months = months,
        selectedKey = selected.monthKey,
        onSelect = { selectedKey = it },
      )
    }
  }

  // Tampilan tabel dari data grafik (aksesibilitas).
  AsteraCard {
    Text("Rincian per bulan", style = MaterialTheme.typography.titleMedium, color = Color.White)
    Spacer(Modifier.height(10.dp))
    TableRow("Bulan", "Keluar", "Masuk", header = true)
    months.reversed().forEach { m ->
      HorizontalDivider(color = AsteraDivider)
      TableRow(longMonthLabel(m.monthKey), formatRupiah(m.outgoing), formatRupiah(m.incoming + m.topUp), bold = m.monthKey == selected.monthKey)
    }
    Spacer(Modifier.height(6.dp))
    Text("Masuk = transfer masuk + top up", style = MaterialTheme.typography.labelSmall, color = AsteraTextSecondary)
  }

  AsteraCard {
    Text("Penerima terbanyak • ${shortMonthLabel(selected.monthKey)}", style = MaterialTheme.typography.titleMedium, color = Color.White)
    Spacer(Modifier.height(10.dp))
    if (recipients.isEmpty()) {
      Text("Tidak ada transfer keluar di bulan ini.", style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
    } else {
      recipients.forEachIndexed { index, r ->
        if (index > 0) HorizontalDivider(color = AsteraDivider, modifier = Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text(r.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text("${r.count} transaksi", style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
          }
          Text(formatRupiah(r.amount), style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
      }
    }
  }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = AsteraTextSecondary)
    Spacer(Modifier.height(2.dp))
    Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White)
  }
}

@Composable
private fun TableRow(month: String, outgoing: String, incoming: String, header: Boolean = false, bold: Boolean = false) {
  val style =
    (if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall)
      .copy(fontWeight = if (bold || header) FontWeight.Bold else FontWeight.Normal)
  val color = if (header) AsteraTextSecondary else Color.White
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(month, style = style, color = color, modifier = Modifier.weight(1.2f))
    Text(outgoing, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    Text(incoming, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
  }
}

/**
 * Grafik batang satu seri: batang tipis dengan ujung atas membulat 4dp yang menempel ke garis dasar,
 * jarak antar-batang, label nilai hanya pada batang terpilih, dan area ketuk selebar kolom.
 */
@Composable
private fun MonthlyBarChart(
  months: List<MonthlySpending>,
  selectedKey: String,
  onSelect: (String) -> Unit,
) {
  val maxValue = (months.maxOf { it.outgoing }).coerceAtLeast(1L)
  val selectedIndex = months.indexOfFirst { it.monthKey == selectedKey }
  val description =
    "Grafik pengeluaran: " + months.joinToString { "${longMonthLabel(it.monthKey)} ${formatRupiah(it.outgoing)}" }

  Column(modifier = Modifier.semantics { contentDescription = description }) {
    // Label nilai bulan terpilih, diletakkan di atas kolomnya.
    Row(modifier = Modifier.fillMaxWidth()) {
      months.forEachIndexed { index, m ->
        Text(
          text = if (index == selectedIndex) formatRupiahCompact(m.outgoing) else "",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          textAlign = TextAlign.Center,
          maxLines = 1,
          modifier = Modifier.weight(1f),
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Canvas(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(160.dp)
          .testTag("summary_bar_chart")
          .pointerInput(months) {
            detectTapGestures { offset ->
              val slot = size.width / months.size.toFloat()
              val index = (offset.x / slot).toInt().coerceIn(0, months.lastIndex)
              onSelect(months[index].monthKey)
            }
          },
    ) {
      val slot = size.width / months.size
      val barWidth = (slot * 0.42f).coerceAtMost(28.dp.toPx())
      val radius = 4.dp.toPx()
      val top = 4.dp.toPx()
      val chartHeight = size.height - top

      // Sorotan kolom terpilih (latar samar, bukan warna batang).
      if (selectedIndex >= 0) {
        drawRoundRect(
          color = AsteraPrimary.copy(alpha = 0.28f),
          topLeft = Offset(slot * selectedIndex + 2.dp.toPx(), 0f),
          size = Size(slot - 4.dp.toPx(), size.height),
          cornerRadius = CornerRadius(8.dp.toPx()),
        )
      }
      // Garis bantu tengah yang samar.
      drawLine(AsteraDivider, Offset(0f, top + chartHeight / 2f), Offset(size.width, top + chartHeight / 2f), strokeWidth = 1.dp.toPx())

      months.forEachIndexed { index, m ->
        if (m.outgoing <= 0L) return@forEachIndexed
        val h = (m.outgoing.toFloat() / maxValue.toFloat()) * chartHeight
        val left = slot * index + (slot - barWidth) / 2f
        val barTop = size.height - h
        // Ujung atas membulat, bawah rata dengan garis dasar.
        drawRoundRect(
          color = AsteraSkyBlue,
          topLeft = Offset(left, barTop),
          size = Size(barWidth, h),
          cornerRadius = CornerRadius(radius),
        )
        if (h > radius) {
          drawRect(AsteraSkyBlue, topLeft = Offset(left, size.height - radius), size = Size(barWidth, radius))
        }
      }
      // Garis dasar.
      drawLine(AsteraTextSecondary.copy(alpha = 0.5f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
    }
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
      months.forEachIndexed { index, m ->
        Text(
          text = shortMonthLabel(m.monthKey),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
          ),
          color = if (index == selectedIndex) Color.White else AsteraTextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Tertinggi: ", style = MaterialTheme.typography.labelSmall, color = AsteraTextSecondary)
      Text(formatRupiah(maxValue), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
  }
}
