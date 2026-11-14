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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.way.app.fit.ParkingRecommender
import com.way.app.fit.Recommendation
import kotlin.math.roundToInt

/** 차량용 고대비 팔레트 (야간/주간 모두 어두운 배경 고정). */
private object DriveColors {
    val Background = Color(0xFF0E1116)
    val Card = Color(0xFF1B2028)
    val CardBest = Color(0xFF12324F)
    val Accent = Color(0xFF4FC3F7)
    val Text = Color(0xFFFFFFFF)
    val SubText = Color(0xFFB8C2CC)
    val Good = Color(0xFF66BB6A)
    val Warn = Color(0xFFFFB74D)
    val Bad = Color(0xFFEF5350)
}

sealed interface StopFitUiState {
    data object Loading : StopFitUiState
    data class Ready(val items: List<Recommendation>, val fromSample: Boolean) : StopFitUiState
    data class Empty(val message: String) : StopFitUiState
}

/**
 * 운전 중 보기 쉬운 주차장 추천 화면.
 *  - 후보는 최대 3개, 1위는 강조
 *  - 카드 전체가 하나의 큰 터치 영역 (탭 = 선택)
 *  - 읽어야 할 정보는 이름 / 이유 / 여유율 세 가지만
 */
@Composable
fun StopFitScreen(
    state: StopFitUiState,
    onSelect: (Recommendation) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DriveColors.Background)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "주차장 추천",
                color = DriveColors.Text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            BigButton(text = "새로고침", onClick = onRefresh)
        }
        if (state is StopFitUiState.Ready && state.fromSample) {
            Text("샘플 데이터 표시 중", color = DriveColors.Warn, fontSize = 18.sp)
        }
        Spacer(Modifier.size(16.dp))

        when (state) {
            StopFitUiState.Loading -> CenterBox {
                CircularProgressIndicator(color = DriveColors.Accent, modifier = Modifier.size(64.dp))
            }
            is StopFitUiState.Empty -> CenterBox {
                Text(state.message, color = DriveColors.SubText, fontSize = 26.sp)
            }
            is StopFitUiState.Ready -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.items.take(3).forEachIndexed { index, rec ->
                    RecommendationCard(rank = index + 1, rec = rec, onClick = { onSelect(rec) })
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(rank: Int, rec: Recommendation, onClick: () -> Unit) {
    val best = rank == 1
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (best) 132.dp else 104.dp)
            .background(if (best) DriveColors.CardBest else DriveColors.Card, shape)
            .then(if (best) Modifier.border(3.dp, DriveColors.Accent, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (best) 60.dp else 52.dp)
                .background(if (best) DriveColors.Accent else DriveColors.SubText, CircleShape)
        ) {
            Text("$rank", color = DriveColors.Background, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rec.name,
                color = DriveColors.Text,
                fontSize = if (best) 32.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(rec.reason, color = DriveColors.SubText, fontSize = 22.sp, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        VacancyBadge(rec.vacancyRate)
    }
}

@Composable
private fun VacancyBadge(rate: Double) {
    val color = when {
        rate >= 0.3 -> DriveColors.Good
        rate >= 0.1 -> DriveColors.Warn
        else -> DriveColors.Bad
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${(rate * 100).roundToInt()}%", color = color, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("여유", color = DriveColors.SubText, fontSize = 18.sp)
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 64.dp)
            .background(DriveColors.Card, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp)
    ) {
        Text(text, color = DriveColors.Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun StopFitScreenPreview() {
    val items = listOf(
        Recommendation("남산 공영주차장", 37.55, 126.98, 0.82, "자리 여유 · 70% 비어 있음", 1800.0, 0.7, 600),
        Recommendation("세종로 공영주차장", 37.57, 126.97, 0.71, "가까워요 · ${ParkingRecommender.formatDistance(650.0)}", 650.0, 0.22, 1000),
        Recommendation("서울역 서부 공영주차장", 37.55, 126.96, 0.55, "저렴해요 · 기본 800원", 2100.0, 0.06, 800)
    )
    StopFitScreen(StopFitUiState.Ready(items, fromSample = true), onSelect = {}, onRefresh = {})
}
