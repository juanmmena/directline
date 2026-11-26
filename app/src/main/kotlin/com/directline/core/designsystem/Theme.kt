package com.directline.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.directline.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF0E7C66),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFA6F2DF),
  onPrimaryContainer = Color(0xFF00201A),
  secondary = Color(0xFF4A635D),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFCCE8E0),
  onSecondaryContainer = Color(0xFF06201B),
  tertiary = Color(0xFF3F6374),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFC3E8FC),
  onTertiaryContainer = Color(0xFF001F2A),
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  background = Color(0xFFF6FBF9),
  onBackground = Color(0xFF171D1B),
  surface = Color(0xFFF6FBF9),
  onSurface = Color(0xFF171D1B),
  surfaceVariant = Color(0xFFDBE5E1),
  onSurfaceVariant = Color(0xFF3F4946),
  outline = Color(0xFF6F7976),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF0F5F3),
  surfaceContainer = Color(0xFFEAEFED),
  surfaceContainerHigh = Color(0xFFE4EAE7),
  surfaceContainerHighest = Color(0xFFDFE4E1),
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF8AD6C3),
  onPrimary = Color(0xFF00382E),
  primaryContainer = Color(0xFF005143),
  onPrimaryContainer = Color(0xFFA6F2DF),
  secondary = Color(0xFFB1CCC4),
  onSecondary = Color(0xFF1C3530),
  secondaryContainer = Color(0xFF334B46),
  onSecondaryContainer = Color(0xFFCCE8E0),
  tertiary = Color(0xFFA7CCE0),
  onTertiary = Color(0xFF0B3444),
  tertiaryContainer = Color(0xFF274B5C),
  onTertiaryContainer = Color(0xFFC3E8FC),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005),
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  background = Color(0xFF0F1513),
  onBackground = Color(0xFFDFE4E1),
  surface = Color(0xFF0F1513),
  onSurface = Color(0xFFDFE4E1),
  surfaceVariant = Color(0xFF3F4946),
  onSurfaceVariant = Color(0xFFBFC9C5),
  outline = Color(0xFF89938F),
  surfaceContainerLowest = Color(0xFF0A0F0E),
  surfaceContainerLow = Color(0xFF171D1B),
  surfaceContainer = Color(0xFF1B211F),
  surfaceContainerHigh = Color(0xFF252B29),
  surfaceContainerHighest = Color(0xFF303634),
)

val DirectlineTypography = Typography(
  headlineSmall = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
  ),
  titleLarge = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
  ),
  titleMedium = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
)

object DirectlineSpacing {
  val xs = 4.dp
  val sm = 8.dp
  val md = 16.dp
  val lg = 24.dp
  val maxReadableWidth = 680.dp
}

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
  ThemeMode.SYSTEM -> isSystemInDarkTheme()
  ThemeMode.LIGHT -> false
  ThemeMode.DARK -> true
}

@Composable
fun DirectlineTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme = resolveDarkTheme(themeMode)
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = DirectlineTypography,
    content = content,
  )
}
