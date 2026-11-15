package com.way.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private val WayColorScheme = darkColorScheme(
    primary = WayColors.Primary,
    onPrimary = WayColors.Text,
    primaryContainer = WayColors.PrimarySoft,
    onPrimaryContainer = WayColors.OnPrimarySoft,
    secondary = WayColors.SubText,
    background = WayColors.Background,
    onBackground = WayColors.Text,
    surface = WayColors.Surface,
    onSurface = WayColors.Text,
    surfaceVariant = WayColors.SurfaceHigh,
    onSurfaceVariant = WayColors.SubText,
    outline = WayColors.Outline,
    error = WayColors.Bad
)

/** 시안의 둥근 카드 모서리. */
object WayShapes {
    val Card = RoundedCornerShape(20.dp)
    val Row = RoundedCornerShape(14.dp)
    val Pill = RoundedCornerShape(50)
}

/** 화면 공통 간격. */
object WaySpacing {
    val Screen = 28.dp
    val Gap = 16.dp
    val CardPadding = 24.dp
}

/**
 * 헤드유닛 배율. Pleos 헤드유닛(2560x1440, 160dpi)은 화면 폭이 2560dp 라서
 * 휴대폰 기준 dp/sp 로는 글자가 너무 작게 보인다. 넓은 화면에서만 UI 전체를 확대한다.
 */
private fun headUnitScale(screenWidthDp: Int, density: Float): Float =
    if (density < 1.5f && screenWidthDp >= 1200) 1.35f else 1f

/** 차량 화면은 항상 다크 테마 (다이나믹 컬러 사용 안 함). */
@Composable
fun WAYTheme(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val scale = headUnitScale(LocalConfiguration.current.screenWidthDp, base.density)
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
        MaterialTheme(
            colorScheme = WayColorScheme,
            typography = Typography,
            shapes = Shapes(
                small = WayShapes.Row,
                medium = WayShapes.Row,
                large = WayShapes.Card
            ),
            content = content
        )
    }
}
