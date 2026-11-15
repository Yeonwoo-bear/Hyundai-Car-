package com.way.app.rest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.way.app.ui.components.PillChip
import com.way.app.ui.components.SampleBadge
import com.way.app.ui.components.ScreenHeader
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing

enum class RestType(val label: String) { REST_AREA("휴게소"), SHELTER("졸음쉼터"), CAFE("카페") }

/** 휴식 장소. 실제 데이터 소스가 없으므로 현재는 모두 샘플이다 (isSample = true). */
data class RestPlace(
    val id: String,
    val name: String,
    val type: RestType,
    val distanceKm: Double,
    val etaMinutes: Int,
    val isSample: Boolean = true
)

/** 화면 틀을 보여주기 위한 샘플. 실제 위치/경로와 무관하다. */
val SampleRestPlaces = listOf(
    RestPlace("sample-rest-1", "샘플 휴게소 A", RestType.REST_AREA, 7.8, 8),
    RestPlace("sample-rest-2", "샘플 휴게소 B", RestType.REST_AREA, 23.4, 19),
    RestPlace("sample-shelter-1", "샘플 졸음쉼터", RestType.SHELTER, 12.1, 11),
    RestPlace("sample-cafe-1", "샘플 드라이브스루 카페", RestType.CAFE, 4.2, 6)
)

/** (3) 휴식 장소 추천 — 화면 틀 + 샘플 데이터. */
@Composable
fun RestPlacesScreen(
    onSelect: (RestPlace) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf<RestType?>(RestType.REST_AREA) }
    Column(
        modifier
            .fillMaxSize()
            .background(WayColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(WaySpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
    ) {
        ScreenHeader(title = "휴식 장소 추천", label = "STOP SENSE", onBack = onBack) { SampleBadge() }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RestType.entries.forEach { t -> PillChip(t.label, filter == t, { filter = t }) }
            PillChip("전체", filter == null, { filter = null })
        }
        val items = SampleRestPlaces.filter { filter == null || it.type == filter }
        if (items.isEmpty()) {
            Text("조건에 맞는 장소가 없어요", style = MaterialTheme.typography.titleLarge, color = WayColors.SubText)
        }
        items.forEach { place ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .background(WayColors.Surface, WayShapes.Card)
                    .clickable(role = Role.Button) { onSelect(place) }
                    .padding(18.dp)
            ) {
                PhotoPlaceholder(width = 150.dp, height = 96.dp)
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(place.name, style = MaterialTheme.typography.headlineSmall, color = WayColors.Text)
                        if (place.isSample) { Spacer(Modifier.width(12.dp)); SampleBadge() }
                    }
                    Text(
                        "${place.type.label} · ${place.distanceKm} km · 약 ${place.etaMinutes}분",
                        style = MaterialTheme.typography.bodyLarge, color = WayColors.SubText
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineMedium, color = WayColors.SubText)
            }
        }
        Text(
            "※ 휴게소·졸음쉼터 데이터 소스가 아직 연결되지 않아 샘플을 보여주고 있어요.",
            style = MaterialTheme.typography.bodySmall, color = WayColors.Muted
        )
    }
}

/** 사진 자리 표시 (실제 사진 없음). */
@Composable
fun PhotoPlaceholder(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(width, height).background(WayColors.SurfaceHigh, WayShapes.Row),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Image, "사진 없음", tint = WayColors.Muted, modifier = Modifier.size(40.dp))
    }
}
