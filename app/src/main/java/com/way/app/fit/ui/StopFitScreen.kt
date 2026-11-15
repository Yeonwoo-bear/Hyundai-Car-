package com.way.app.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.way.app.fit.ParkingRecommender
import com.way.app.fit.Recommendation
import com.way.app.ui.components.Badge
import com.way.app.ui.components.PillChip
import com.way.app.ui.components.SampleBadge
import com.way.app.ui.components.ScreenHeader
import com.way.app.ui.theme.WAYTheme
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

sealed interface StopFitUiState {
    data object Loading : StopFitUiState
    data class Ready(val items: List<Recommendation>, val fromSample: Boolean) : StopFitUiState
    data class Empty(val message: String) : StopFitUiState
}

/** 화면에서만 바꾸는 정렬 (추천 점수 자체는 ParkingRecommender 결과 그대로). */
private enum class FitSort(val label: String) { RECOMMENDED("추천순"), NEAREST("가까운순"), CHEAPEST("저렴한순") }

/**
 * (4) 주차 장소 추천 — 시안의 카드 스타일.
 * 카드: 순위 / 이름(+추천 배지) / 거리·기본요금 / 혼잡도 색 / 카드 전체 탭 = 선택
 */
@Composable
fun StopFitScreen(
    state: StopFitUiState,
    onSelect: (Recommendation) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    var sort by rememberSaveable { mutableStateOf(FitSort.RECOMMENDED) }
    Column(
        modifier
            .fillMaxSize()
            .background(WayColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(WaySpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
    ) {
        ScreenHeader(title = "주차장 추천", label = "STOP FIT", onBack = onBack) {
            if (state is StopFitUiState.Ready && state.fromSample) { SampleBadge(); Spacer(Modifier.width(12.dp)) }
            Box(
                Modifier.size(56.dp).clickable(role = Role.Button, onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Refresh, "새로고침", tint = WayColors.SubText, modifier = Modifier.size(30.dp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FitSort.entries.forEach { s -> PillChip(s.label, sort == s, { sort = s }) }
        }

        when (state) {
            StopFitUiState.Loading -> Box(Modifier.fillMaxWidth().heightIn(min = 300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WayColors.Primary, modifier = Modifier.size(64.dp))
            }
            is StopFitUiState.Empty -> Box(Modifier.fillMaxWidth().heightIn(min = 300.dp), contentAlignment = Alignment.Center) {
                Text(state.message, style = MaterialTheme.typography.headlineSmall, color = WayColors.SubText)
            }
            is StopFitUiState.Ready -> {
                val top = state.items.take(3)
                val sorted = when (sort) {
                    FitSort.RECOMMENDED -> top
                    FitSort.NEAREST -> top.sortedBy { it.distanceMeters }
                    FitSort.CHEAPEST -> top.sortedBy { it.baseFee }
                }
                sorted.forEachIndexed { index, rec ->
                    ParkingCard(
                        rank = index + 1,
                        rec = rec,
                        recommended = rec == top.first(),
                        onClick = { onSelect(rec) }
                    )
                }
                if (state.fromSample) {
                    Text(
                        "※ 실시간 API에 연결되지 않아 샘플 주차장 데이터를 보여주고 있어요.",
                        style = MaterialTheme.typography.bodySmall, color = WayColors.Muted
                    )
                }
            }
        }
    }
}

private data class Congestion(val label: String, val color: Color)

private fun congestionOf(rate: Double) = when {
    rate >= 0.3 -> Congestion("여유", WayColors.Good)
    rate >= 0.1 -> Congestion("보통", WayColors.Warn)
    else -> Congestion("혼잡", WayColors.Bad)
}

private val won = NumberFormat.getNumberInstance(Locale.KOREA)

@Composable
private fun ParkingCard(rank: Int, rec: Recommendation, recommended: Boolean, onClick: () -> Unit) {
    val c = congestionOf(rec.vacancyRate)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(WayColors.Surface, WayShapes.Card)
            .then(if (rank == 1) Modifier.border(2.dp, WayColors.Primary, WayShapes.Card) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        // 순위
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .background(if (rank == 1) WayColors.Primary else WayColors.SurfaceHigh, CircleShape)
        ) {
            Text("$rank", style = MaterialTheme.typography.headlineSmall, color = WayColors.Text)
        }
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rec.name, style = MaterialTheme.typography.headlineSmall, color = WayColors.Text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (recommended) { Spacer(Modifier.width(12.dp)); Badge("추천", WayColors.Primary) }
            }
            Text(
                buildString {
                    append(ParkingRecommender.formatDistance(rec.distanceMeters))
                    append(" · 기본 ")
                    append(if (rec.baseFee <= 0) "무료" else "${won.format(rec.baseFee)}원")
                },
                style = MaterialTheme.typography.bodyLarge, color = WayColors.SubText
            )
            Text(rec.reason, style = MaterialTheme.typography.bodyMedium, color = WayColors.Muted, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(c.label, style = MaterialTheme.typography.headlineSmall, color = c.color)
            Text("빈 자리 ${(rec.vacancyRate * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = c.color)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun StopFitScreenPreview() {
    val items = listOf(
        Recommendation("남산 공영주차장", 37.55, 126.98, 0.82, "자리 여유 · 70% 비어 있음", 1800.0, 0.7, 600),
        Recommendation("세종로 공영주차장", 37.57, 126.97, 0.71, "가까워요 · ${ParkingRecommender.formatDistance(650.0)}", 650.0, 0.22, 1000),
        Recommendation("서울역 서부 공영주차장", 37.55, 126.96, 0.55, "저렴해요 · 기본 800원", 2100.0, 0.06, 800)
    )
    WAYTheme { StopFitScreen(StopFitUiState.Ready(items, fromSample = true), onSelect = {}, onRefresh = {}) }
}
