package com.example.ui.components

import com.astera.wallet.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.AsteraAmber
import com.example.ui.theme.AsteraAmberSoft
import com.example.ui.theme.AsteraBgBottom
import com.example.ui.theme.AsteraBgTop
import com.example.ui.theme.AsteraCard
import com.example.ui.theme.AsteraCardBorder
import com.example.ui.theme.AsteraFieldBg
import com.example.ui.theme.AsteraPrimary
import com.example.ui.theme.AsteraSkyBlue
import com.example.ui.theme.AsteraTextPrimary
import com.example.ui.theme.AsteraTextSecondary
import com.example.util.initialsOf

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
fun AsteraPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
) {
  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    shape = CircleShape,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = AsteraPrimary,
        contentColor = Color.White,
        disabledContainerColor = AsteraPrimary.copy(alpha = 0.45f),
        disabledContentColor = Color.White.copy(alpha = 0.6f),
      ),
    modifier = modifier.fillMaxWidth().height(52.dp).minimumInteractiveComponentSize(),
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        color = Color.White,
        strokeWidth = 2.5.dp,
      )
    } else {
      Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      )
    }
  }
}

@Composable
fun AsteraOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  borderColor: Color = AsteraPrimary,
  contentColor: Color = Color.White,
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    border = BorderStroke(1.5.dp, borderColor),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    modifier = modifier.fillMaxWidth().height(52.dp).minimumInteractiveComponentSize(),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
  }
}

@Composable
fun AsteraTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  modifier: Modifier = Modifier,
  errorText: String? = null,
  helperText: String? = null,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  prefix: (@Composable () -> Unit)? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  singleLine: Boolean = true,
  readOnly: Boolean = false,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = AsteraTextPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(text = placeholder, color = AsteraTextSecondary.copy(alpha = 0.75f))
      },
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      prefix = prefix,
      isError = errorText != null,
      singleLine = singleLine,
      readOnly = readOnly,
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      shape = RoundedCornerShape(14.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = AsteraFieldBg,
          unfocusedContainerColor = AsteraFieldBg,
          disabledContainerColor = AsteraFieldBg,
          errorContainerColor = AsteraFieldBg,
          focusedBorderColor = AsteraSkyBlue,
          unfocusedBorderColor = AsteraCardBorder.copy(alpha = 0.6f),
          errorBorderColor = MaterialTheme.colorScheme.error,
          focusedTextColor = AsteraTextPrimary,
          unfocusedTextColor = AsteraTextPrimary,
          cursorColor = AsteraAmber,
        ),
      modifier = Modifier.fillMaxWidth(),
    )
    if (errorText != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    } else if (helperText != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = helperText,
        style = MaterialTheme.typography.bodySmall,
        color = AsteraTextSecondary,
      )
    }
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

enum class MainTab(val route: String, val label: String) {
  BERANDA("beranda", "Beranda"),
  TRANSAKSI("transaksi", "Transaksi"),
  AKUN("akun", "Akun"),
}

@Composable
fun AsteraBottomBar(
  currentRoute: String?,
  onSelectTab: (MainTab) -> Unit,
) {
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
          .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      MainTab.entries.forEach { tab ->
        val selected = currentRoute == tab.route
        val tint = if (selected) AsteraAmber else AsteraTextSecondary
        val icon =
          when (tab) {
            MainTab.BERANDA -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
            MainTab.TRANSAKSI -> if (selected) Icons.Rounded.SwapHoriz else Icons.Outlined.SwapHoriz
            MainTab.AKUN -> if (selected) Icons.Rounded.Person else Icons.Outlined.Person
          }
        Column(
          modifier =
            Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable { onSelectTab(tab) }
              .minimumInteractiveComponentSize()
              .padding(vertical = 6.dp)
              .testTag("nav_${tab.route}"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = tab.label,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
              ),
            color = tint,
          )
        }
      }
    }
  }
}
