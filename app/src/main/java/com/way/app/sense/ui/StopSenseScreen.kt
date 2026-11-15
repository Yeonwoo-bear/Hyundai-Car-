package com.way.app.sense.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.way.app.sense.SenseResult
import com.way.app.sense.TripTimeUnit
import com.way.app.ui.components.CollapsibleCard
import com.way.app.ui.components.DemoBadge
import com.way.app.ui.components.InfoRow
import com.way.app.ui.components.ScreenHeader
import com.way.app.ui.components.WayCard
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing

/**
 * (2) STOP SENSE — "지금, 휴식이 필요할까요?"
 * 판단 결과는 StopSenseEngine 결과(title/message)를 그대로 보여준다.
 */
@Composable
fun StopSenseScreen(
    sense: SenseController,
    onOpenRest: () -> Unit,
    onOpenParking: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val decision = sense.decision
    Column(
        modifier
            .fillMaxSize()
            .background(WayColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(WaySpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(WaySpacing.Gap)
    ) {
        ScreenHeader(title = "지금, 휴식이 필요할까요?", label = "STOP SENSE", onBack = onBack) {
            Box(
                Modifier.size(56.dp).clickable(role = Role.Button) { sense.reader.refresh() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Refresh, "차량 데이터 새로고침", tint = WayColors.SubText, modifier = Modifier.size(30.dp)) }
        }
        Text(sense.friendlyStatus, style = MaterialTheme.typography.bodyMedium, color = WayColors.SubText)

        // 상태 원형 아이콘 + 오른쪽 정보 행
        WayCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusRing(decision?.result, Modifier.size(220.dp))
                Spacer(Modifier.width(32.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val rows = infoRows(sense)
                    if (rows.isEmpty()) {
                        Text(
                            "표시할 주행 데이터가 없어요.\n아래 '데모 설정'에서 값을 입력하면 판단해 볼 수 있어요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = WayColors.SubText
                        )
                    } else rows.forEach { it() }
                }
            }
        }

        ResultCard(sense, onOpenRest, onOpenParking)

        Text(
            "※ 차량의 '트립 주행 시간'은 쉬지 않고 운전한 시간과 다를 수 있으며, 졸음이나 건강 상태를 측정하지 않아요.",
            style = MaterialTheme.typography.bodySmall,
            color = WayColors.Muted
        )

        DemoSettingsCard(sense)
    }
}

private fun infoRows(sense: SenseController): List<@Composable () -> Unit> = buildList {
    sense.minutes?.let { m ->
        add { InfoRow(Icons.Outlined.Schedule, "주행 시간", formatMinutes(m), badge = if (sense.minutesAreDemo) ({ DemoBadge() }) else null) }
    }
    sense.etaMinutes?.let { eta ->
        add { InfoRow(Icons.Outlined.Place, "목적지까지", "약 ${formatMinutes(eta)}", badge = { DemoBadge() }) }
    }
    sense.reading.gear?.takeIf { it.isParked != null }?.let { gear ->
        add {
            InfoRow(
                Icons.Outlined.DirectionsCar, "기어",
                if (sense.parkOverridden) "${gear.caption} → 주행 간주" else gear.caption,
                badge = if (sense.parkOverridden) ({ DemoBadge() }) else null
            )
        }
    }
    // '주행 패턴'은 데이터 소스가 없어 표시하지 않는다.
}

@Composable
private fun StatusRing(result: SenseResult?, modifier: Modifier) {
    val ringColor = when (result) {
        SenseResult.REST -> WayColors.Primary
        SenseResult.PARKING -> WayColors.Primary
        SenseResult.NORMAL -> WayColors.Good
        null -> WayColors.Outline
    }
    val icon = when (result) {
        SenseResult.REST -> Icons.Outlined.LocalCafe
        SenseResult.PARKING -> Icons.Outlined.LocalParking
        else -> Icons.Outlined.DirectionsCar
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(ringColor.copy(alpha = 0.22f), Color.Transparent)),
                radius = size.minDimension / 2
            )
            drawArc(
                color = WayColors.SurfaceHigh, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = 10.dp.toPx())
            )
            drawArc(
                color = ringColor, startAngle = -90f, sweepAngle = if (result == null) 0f else 300f, useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Icon(icon, null, tint = WayColors.Text, modifier = Modifier.size(84.dp))
    }
}

@Composable
private fun ResultCard(sense: SenseController, onOpenRest: () -> Unit, onOpenParking: () -> Unit) {
    val decision = sense.decision
    val highlighted = decision != null && decision.result != SenseResult.NORMAL
    val bg = if (highlighted) WayColors.PrimarySoft else WayColors.Surface
    val fg = if (highlighted) WayColors.OnPrimarySoft else WayColors.Text
    val sub = if (highlighted) WayColors.OnPrimarySoft.copy(alpha = 0.8f) else WayColors.SubText
    val onClick: (() -> Unit)? = when (decision?.result) {
        SenseResult.REST -> onOpenRest
        SenseResult.PARKING -> onOpenParking
        else -> null
    }
    val icon = when (decision?.result) {
        SenseResult.REST -> Icons.Outlined.LocalCafe
        SenseResult.PARKING -> Icons.Outlined.LocalParking
        SenseResult.NORMAL -> Icons.Outlined.ThumbUp
        null -> Icons.Outlined.Schedule
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(bg, WayShapes.Card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 32.dp, vertical = 20.dp)
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(decision?.title ?: "판단할 데이터가 없어요", style = MaterialTheme.typography.headlineSmall, color = fg)
                if (decision?.isDemoDecision == true) { Spacer(Modifier.width(12.dp)); DemoBadge() }
            }
            Text(
                // 엔진 문구는 그대로 쓰되, 트립 시간은 '연속' 주행이 아닐 수 있어 표시만 '주행 시간'으로 바꾼다.
                decision?.message?.replace("연속 주행", "주행 시간")
                    ?: "차량이 주행 시간을 제공하거나, 데모 설정에 값을 넣으면 판단해요.",
                style = MaterialTheme.typography.bodyLarge, color = sub
            )
        }
        if (onClick != null) {
            Text(
                if (decision?.result == SenseResult.REST) "휴식 장소 보기 ›" else "주차장 보기 ›",
                style = MaterialTheme.typography.titleMedium, color = fg
            )
        }
    }
}

@Composable
private fun DemoSettingsCard(sense: SenseController) {
    CollapsibleCard(title = "데모 설정", headerBadge = { Icon(Icons.Outlined.Settings, null, tint = WayColors.Muted) }) {
        Text(
            "여기서 입력한 값으로 만든 결과에는 항상 '데모' 배지가 붙어요.",
            style = MaterialTheme.typography.bodyMedium, color = WayColors.SubText
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = sense.demoMinutesText,
                onValue = { sense.demoMinutesText = it },
                label = if (sense.vehicleMinutes != null) "주행 시간(분) — 차량 값 사용 중" else "주행 시간(분)",
                enabled = sense.vehicleMinutes == null,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = sense.etaText,
                onValue = { sense.etaText = it },
                label = "목적지까지 남은 시간(분)",
                modifier = Modifier.weight(1f)
            )
        }
        if (sense.parkedByGear) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(role = Role.Checkbox) { sense.treatParkAsDriving = !sense.treatParkAsDriving }
            ) {
                Checkbox(
                    checked = sense.treatParkAsDriving,
                    onCheckedChange = { sense.treatParkAsDriving = it },
                    colors = CheckboxDefaults.colors(checkedColor = WayColors.Primary)
                )
                Text("P 기어여도 주행 중으로 간주", style = MaterialTheme.typography.bodyLarge, color = WayColors.Text)
            }
        }
        if (sense.vehicleProvidesTripTime) {
            Text(
                "차량 주행 시간 단위 확인 (원시 값: ${sense.reading.rawValue ?: "-"})",
                style = MaterialTheme.typography.titleSmall, color = WayColors.SubText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripTimeUnit.entries.forEach { choice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(role = Role.RadioButton) { sense.unit = choice }.padding(end = 12.dp)
                    ) {
                        RadioButton(
                            selected = sense.unit == choice, onClick = { sense.unit = choice },
                            colors = RadioButtonDefaults.colors(selectedColor = WayColors.Primary)
                        )
                        Text(choice.caption, style = MaterialTheme.typography.bodyLarge, color = WayColors.Text)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter(Char::isDigit).take(4)) },
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.titleLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WayColors.Primary,
            unfocusedBorderColor = WayColors.Outline,
            focusedLabelColor = WayColors.Primary,
            unfocusedLabelColor = WayColors.SubText,
            cursorColor = WayColors.Primary
        ),
        modifier = modifier
    )
}

/** 작은 원형 아이콘 배경 (홈 카드 등에서 재사용). */
@Composable
fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color, size: Int = 64) {
    Box(Modifier.size(size.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.55f).dp))
    }
}
