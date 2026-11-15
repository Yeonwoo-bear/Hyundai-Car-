package com.way.app.learn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.way.app.learn.PreferenceFeedback
import com.way.app.learn.RecommendationAction
import com.way.app.learn.RecommendationEvent
import com.way.app.learn.StopCategory
import com.way.app.learn.StopLearnRepository
import com.way.app.ui.components.PrimaryButton
import com.way.app.ui.components.ScreenHeader
import com.way.app.ui.components.WayCard
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing
import kotlin.math.roundToInt

/** STOP LEARN 이 기록할 대상 (STOP FIT 에서 고른 장소). */
data class LearnTarget(val placeId: String, val placeName: String, val category: StopCategory)

private enum class Satisfaction(val label: String, val icon: ImageVector) {
    GOOD("좋았어요", Icons.Outlined.SentimentSatisfied),
    OK("보통이에요", Icons.Outlined.SentimentNeutral),
    BAD("아쉬웠어요", Icons.Outlined.SentimentDissatisfied)
}

private enum class Highlight(val label: String) {
    PRICE("가격이 합리적이에요"),
    CLOSE("가까워요"),
    FACILITY("시설이 좋아요"),
    EASY("이동이 편했어요")
}

/**
 * (6) STOP LEARN — 이용 경험 기록.
 * 저장은 기존 StopLearnRepository 동작을 그대로 쓴다.
 *  - 만족도 → recordEvent (좋았어요/보통 = USER_CONFIRMED, 아쉬웠어요 = DISMISSED)
 *  - '가격' / '가까움' 체크 → submitFeedback(CHEAPER / CLOSER, 둘 다면 BALANCED) 로 가중치 반영
 *  - '시설' / '이동' 은 현재 가중치 모델에 없어 가중치에는 반영되지 않는다.
 */
@Composable
fun StopLearnScreen(
    target: LearnTarget?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val repo = remember(context) { StopLearnRepository(context) }
    var preference by remember { mutableStateOf(repo.getPreference()) }
    var historyCount by remember { mutableIntStateOf(repo.getHistory(100).size) }
    var satisfaction by rememberSaveable { mutableStateOf<Satisfaction?>(null) }
    var checked by rememberSaveable { mutableStateOf(setOf<Highlight>()) }
    var savedNote by remember { mutableStateOf<String?>(null) }

    val placeId = target?.placeId ?: "recent-recommendation"
    val placeName = target?.placeName ?: "최근 추천 장소"
    val category = target?.category ?: StopCategory.PARKING

    Column(
        modifier
            .fillMaxSize()
            .background(WayColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(WaySpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
    ) {
        ScreenHeader(title = "이용 경험 기록", label = "STOP LEARN", onBack = onBack)
        Text("대상: $placeName", style = MaterialTheme.typography.bodyLarge, color = WayColors.SubText)

        WayCard {
            Text("이번 추천은 어땠나요?", style = MaterialTheme.typography.titleLarge, color = WayColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Satisfaction.entries.forEach { s ->
                    SatisfactionButton(s, selected = satisfaction == s, Modifier.weight(1f)) {
                        satisfaction = if (satisfaction == s) null else s
                        savedNote = null
                    }
                }
            }
        }

        WayCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("무엇이 가장 만족스러웠나요?", style = MaterialTheme.typography.titleLarge, color = WayColors.Text)
                Spacer(Modifier.width(8.dp))
                Text("(복수 선택)", style = MaterialTheme.typography.bodyMedium, color = WayColors.SubText)
            }
            Highlight.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { h ->
                        CheckRow(h.label, h in checked, Modifier.weight(1f)) {
                            checked = if (h in checked) checked - h else checked + h
                            savedNote = null
                        }
                    }
                }
            }
            Text(
                "가격·거리 항목은 다음 주차장 추천의 가중치에 반영돼요.",
                style = MaterialTheme.typography.bodySmall, color = WayColors.Muted
            )
        }

        PrimaryButton(
            text = "기록하고 다음 추천에 반영하기",
            enabled = satisfaction != null || checked.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val now = System.currentTimeMillis()
                satisfaction?.let { s ->
                    repo.recordEvent(
                        RecommendationEvent(
                            placeId = placeId, placeName = placeName, category = category,
                            action = if (s == Satisfaction.BAD) RecommendationAction.DISMISSED
                                     else RecommendationAction.USER_CONFIRMED,
                            timestampMillis = now
                        )
                    )
                }
                val price = Highlight.PRICE in checked
                val close = Highlight.CLOSE in checked
                val feedback = when {
                    price && close -> PreferenceFeedback.BALANCED
                    price -> PreferenceFeedback.CHEAPER
                    close -> PreferenceFeedback.CLOSER
                    else -> null
                }
                if (feedback != null) {
                    preference = repo.submitFeedback(feedback, placeId = placeId, placeName = placeName, timestampMillis = now)
                }
                historyCount = repo.getHistory(100).size
                savedNote = if (feedback != null) "기록했어요. 다음 추천부터 반영돼요." else "기록했어요."
                satisfaction = null
                checked = emptySet()
            }
        )
        savedNote?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = WayColors.Good) }

        // 현재 저장된 개인화 상태
        WayCard {
            Text("지금 나의 추천 기준", style = MaterialTheme.typography.titleMedium, color = WayColors.SubText)
            WeightBar("요금", preference.priceWeight)
            WeightBar("거리", preference.distanceWeight)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("저장된 기록 ${historyCount}건", style = MaterialTheme.typography.bodyMedium, color = WayColors.SubText,
                    modifier = Modifier.weight(1f))
                Text(
                    "초기화",
                    style = MaterialTheme.typography.labelLarge, color = WayColors.Primary,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            repo.clearAll()
                            preference = repo.getPreference()
                            historyCount = 0
                            savedNote = "기록을 초기화했어요."
                        }
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SatisfactionButton(s: Satisfaction, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 120.dp)
            .background(if (selected) WayColors.SelectedFill else WayColors.SurfaceHigh, WayShapes.Row)
            .then(if (selected) Modifier.border(2.dp, WayColors.Primary, WayShapes.Row) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(s.icon, null, tint = if (selected) WayColors.Text else WayColors.SubText, modifier = Modifier.size(48.dp))
        Text(s.label, style = MaterialTheme.typography.titleMedium, color = if (selected) WayColors.Text else WayColors.SubText)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 68.dp)
            .background(if (checked) WayColors.SelectedFill else WayColors.SurfaceHigh, WayShapes.Row)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 18.dp)
    ) {
        Icon(
            if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null,
            tint = if (checked) WayColors.Primary else WayColors.Muted, modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (checked) WayColors.Text else WayColors.SubText)
    }
}

@Composable
private fun WeightBar(label: String, weight: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = WayColors.Text, modifier = Modifier.width(72.dp))
        LinearProgressIndicator(
            progress = { weight.coerceIn(0f, 1f) },
            color = WayColors.Primary,
            trackColor = WayColors.SurfaceHigh,
            modifier = Modifier.weight(1f).heightIn(min = 12.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text("${(weight * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, color = WayColors.Text,
            modifier = Modifier.width(72.dp))
    }
}
