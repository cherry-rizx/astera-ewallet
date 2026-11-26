package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.astera.wallet.R
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraAmberSoft
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.util.initialsOf

@Composable
fun AsteraLogoBadge(
  size: Dp = 92.dp,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .border(1.5.dp, Color(0x5560A5FA), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.astera_icon),
      contentDescription = "Logo ASTERA",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
fun SimulasiBadge(
  text: String = "Simulasi",
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = AsteraAmberSoft,
    border = BorderStroke(1.dp, AsteraAmber.copy(alpha = 0.6f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = AsteraAmber,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun UserAvatar(
  name: String,
  photoUrl: String?,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val baseMod =
    modifier
      .size(size)
      .clip(CircleShape)
      .background(AsteraPrimary)
      .border(1.5.dp, AsteraSkyBlue.copy(alpha = 0.7f), CircleShape)
      .let { if (onClick != null) it.clickable(onClick = onClick) else it }

  Box(modifier = baseMod, contentAlignment = Alignment.Center) {
    if (!photoUrl.isNullOrBlank()) {
      AsyncImage(
        model = photoUrl,
        contentDescription = "Foto profil $name",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Text(
        text = initialsOf(name),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    }
  }
}
