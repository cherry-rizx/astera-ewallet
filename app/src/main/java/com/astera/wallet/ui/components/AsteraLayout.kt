package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraBgBottom
import com.astera.wallet.ui.theme.AsteraBgTop
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraTextSecondary

@Composable
fun AsteraBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(colors = listOf(AsteraBgTop, AsteraBgBottom)))
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Soft ambient circles matching reference screens
      drawCircle(
        color = Color(0x143A5BA8),
        radius = size.width * 0.42f,
        center = Offset(size.width * 0.92f, size.height * 0.08f),
      )
      drawCircle(
        color = Color(0x103A5BA8),
        radius = size.width * 0.48f,
        center = Offset(size.width * 0.05f, size.height * 0.85f),
      )
      // Subtle star dots
      val stars =
        listOf(
          Triple(0.10f, 0.14f, Color(0x9960A5FA)),
          Triple(0.78f, 0.12f, Color(0xCCFFFFFF)),
          Triple(0.88f, 0.27f, Color(0xCCF59E0B)),
          Triple(0.16f, 0.36f, Color(0x66FFFFFF)),
          Triple(0.72f, 0.54f, Color(0x88FFFFFF)),
          Triple(0.24f, 0.72f, Color(0x8860A5FA)),
          Triple(0.84f, 0.78f, Color(0x99F59E0B)),
        )
      stars.forEach { (fx, fy, color) ->
        drawCircle(
          color = color,
          radius = 3.dp.toPx(),
          center = Offset(size.width * fx, size.height * fy),
        )
      }
    }
    content()
  }
}

@Composable
fun AsteraCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(16.dp)
  Surface(
    modifier =
      modifier.fillMaxWidth().let { base ->
        if (onClick != null) base.clip(shape).clickable(onClick = onClick) else base
      },
    shape = shape,
    color = AsteraCard,
    border = BorderStroke(1.dp, AsteraCardBorder.copy(alpha = 0.45f)),
    tonalElevation = 2.dp,
  ) {
    Column(modifier = Modifier.padding(contentPadding), content = content)
  }
}

@Composable
fun AsteraTopBar(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  trailingAction: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onBack != null) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Kembali",
          tint = Color.White,
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
        )
      }
    }
    if (trailingAction != null) {
      trailingAction()
    }
  }
}
