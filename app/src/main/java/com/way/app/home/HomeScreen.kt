package com.way.app.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.way.app.sense.ui.SenseController
import com.way.app.sense.ui.formatMinutes
import com.way.app.ui.components.DemoBadge
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing

/**
 * (1) 홈 — 주행 정보 한눈에 + 두 개의 큰 진입 카드.
 * 목적지/예상 소요 시간은 내비 연동 전이라 실제 값이 없으면 '정보 없음'으로 둔다.
 */
@Composable
fun HomeScreen(
    sense: SenseController,
    onOpenSense: () -> Unit,
    onOpenFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(WayColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(WaySpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WaySpacing.Gap), modifier = Modifier.fillMaxWidth()) {
            // 히어로 카드 (사진 대신 그라디언트 + 차량 아이콘)
            Box(
                modifier = Modifier
                    .weight(1.6f)
                    .heightIn(min = 300.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF2B2160), Color(0xFF4A3A8C), WayColors.Surface)),
                        WayShapes.Card
                    )
                    .padding(32.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("안전한 이동을 위해", style = MaterialTheme.typography.headlineSmall, color = WayColors.Text)
                    Text("지금의 주행을 분석하고 있어요", style = MaterialTheme.typography.headlineSmall, color = WayColors.Text)
                    Text(sense.friendlyStatus, style = MaterialTheme.typography.bodyMedium, color = WayColors.SubText)
                }
                Canvas(Modifier.align(Alignment.BottomCenter).size(260.dp, 70.dp)) {
                    drawOval(Brush.radialGradient(listOf(WayColors.Primary.copy(alpha = 0.45f), Color.Transparent)))
                }
                Icon(
                    Icons.Outlined.DirectionsCar, null, tint = WayColors.Text,
                    modifier = Modifier.align(Alignment.BottomCenter).size(120.dp)
                )
            }
            // 주행 정보
            Column(
                Modifier.weight(1f).background(WayColors.Surface, WayShapes.Card).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HomeInfo(Icons.Outlined.Place, "목적지", "정보 없음", note = "내비게이션 연동 예정")
                HomeInfo(
                    Icons.Outlined.Schedule, "목적지까지",
                    sense.etaMinutes?.let { "약 ${formatMinutes(it)}" } ?: "정보 없음",
                    demo = sense.etaMinutes != null
                )
                HomeInfo(
                    Icons.Outlined.DirectionsCar, "주행 시간",
                    sense.minutes?.let { formatMinutes(it) } ?: "정보 없음",
                    demo = sense.minutesAreDemo
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WaySpacing.Gap), modifier = Modifier.fillMaxWidth()) {
            EntryCard(
                Icons.Outlined.LocalCafe, "휴식이 필요할까?", "STOP SENSE",
                highlighted = true, onClick = onOpenSense, modifier = Modifier.weight(1f)
            )
            EntryCard(
                Icons.Outlined.LocalParking, "주차는 어디가 좋을까?", "STOP FIT",
                highlighted = false, onClick = onOpenFit, modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeInfo(icon: ImageVector, label: String, value: String, demo: Boolean = false, note: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WayColors.SubText, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = WayColors.SubText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.titleLarge,
                    color = if (value == "정보 없음") WayColors.Muted else WayColors.Text)
                if (demo) { Spacer(Modifier.width(10.dp)); DemoBadge() }
            }
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = WayColors.Muted)
        }
    }
}

@Composable
private fun EntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 130.dp)
            .background(if (highlighted) WayColors.PrimaryStrong else WayColors.SurfaceHigh, WayShapes.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp)
    ) {
        Box(
            Modifier.size(72.dp).background(WayColors.Text.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = WayColors.Text, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.width(22.dp))
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = WayColors.Text)
            Text(subtitle, style = MaterialTheme.typography.titleMedium, color = WayColors.Text.copy(alpha = 0.75f))
        }
    }
}
