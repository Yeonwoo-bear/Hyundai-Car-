package com.way.app.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.way.app.rest.PhotoPlaceholder
import com.way.app.ui.components.PrimaryButton
import com.way.app.ui.components.SampleBadge
import com.way.app.ui.components.ScreenHeader
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing

/** 상세 화면에 보여줄 장소 요약. isSample 이면 '샘플' 배지를 붙인다. */
data class PlaceDetail(
    val id: String,
    val name: String,
    val summary: String,
    val extra: String? = null,
    val isSample: Boolean
)

/**
 * (5) 상세 및 경로 안내 — 화면 틀.
 * 지도/경로 데이터가 없으므로 지도는 자리 표시만 하고, 경로 안내 버튼은 비활성이다.
 */
@Composable
fun PlaceDetailScreen(
    place: PlaceDetail,
    onRecordExperience: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxSize().background(WayColors.Background).padding(WaySpacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
        ) {
            ScreenHeader(title = place.name, onBack = onBack) { if (place.isSample) SampleBadge() }
            PhotoPlaceholder(width = 460.dp, height = 220.dp, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place.name, style = MaterialTheme.typography.headlineSmall, color = WayColors.Text)
                if (place.isSample) { Spacer(Modifier.width(12.dp)); SampleBadge() }
            }
            Text(place.summary, style = MaterialTheme.typography.titleMedium, color = WayColors.SubText)
            place.extra?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = WayColors.Muted) }
            PrimaryButton(
                text = "경로 안내 시작 (내비 연동 예정)",
                onClick = {},
                enabled = false,
                icon = Icons.Outlined.Navigation,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = "이용 후 경험 기록하기",
                onClick = onRecordExperience,
                icon = Icons.Outlined.EditNote,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // 지도 자리 표시
        Box(
            Modifier.weight(1f).fillMaxHeight().heightIn(min = 400.dp).background(WayColors.Surface, WayShapes.Card),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize().padding(40.dp)) {
                drawLine(
                    color = WayColors.Primary.copy(alpha = 0.5f),
                    start = Offset(size.width * 0.2f, size.height * 0.9f),
                    end = Offset(size.width * 0.8f, size.height * 0.1f),
                    strokeWidth = 8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 24f))
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(WayColors.Surface, WayShapes.Row).padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.Map, null, tint = WayColors.Muted, modifier = Modifier.padding(8.dp))
                Text("지도 미리보기 준비 중", style = MaterialTheme.typography.titleMedium, color = WayColors.Muted)
            }
        }
    }
}
