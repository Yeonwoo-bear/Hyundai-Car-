package com.way.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes
import com.way.app.ui.theme.WaySpacing

/** 기본 카드 (남색 표면 + 둥근 모서리). */
@Composable
fun WayCard(
    modifier: Modifier = Modifier,
    color: Color = WayColors.Surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .background(color, WayShapes.Card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(WaySpacing.CardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** 화면 상단: (뒤로) + 작은 영문 라벨 + 큰 제목. */
@Composable
fun ScreenHeader(
    title: String,
    label: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            Box(
                Modifier.size(56.dp).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "뒤로", tint = WayColors.Text, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            if (label != null) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = WayColors.SubText)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = WayColors.Text)
        }
        trailing()
    }
}

/** '데모' / '샘플' 같은 작은 배지. */
@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .border(1.5.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** '데모' 배지는 어떤 배경(남색/연보라) 위에서도 눈에 띄도록 노란 바탕으로 채운다. */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Text(
        "데모",
        color = Color(0xFF2B2100),
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(WayColors.DemoBadge, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
fun SampleBadge(modifier: Modifier = Modifier) = Badge("샘플", WayColors.SampleBadge, modifier)

/** 아이콘 + 라벨 + 값 한 줄 (STOP SENSE 정보 행). */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, badge: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(WayColors.SurfaceHigh, WayShapes.Row)
            .padding(horizontal = 20.dp)
    ) {
        Icon(icon, null, tint = WayColors.SubText, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = WayColors.SubText, modifier = Modifier.weight(1f))
        if (badge != null) { badge(); Spacer(Modifier.width(10.dp)) }
        Text(value, style = MaterialTheme.typography.titleLarge, color = WayColors.Text)
    }
}

/** 둥근 알약 모양 선택 칩 (추천순/가까운순 …). 선택된 것만 보라로 강조. */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .background(if (selected) WayColors.PrimarySoft else WayColors.SurfaceHigh, WayShapes.Pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 26.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) WayColors.OnPrimarySoft else WayColors.SubText
        )
    }
}

/** 꽉 찬 보라 버튼 (시안의 '기록하고 다음 추천에 반영하기'). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 68.dp)
            .background(if (enabled) WayColors.PrimaryStrong else WayColors.SurfaceHigh, WayShapes.Pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) WayColors.Text else WayColors.Muted, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = if (enabled) WayColors.Text else WayColors.Muted)
    }
}

/** 화면 아래 접이식 '데모 설정' 카드. 기본은 접힘. */
@Composable
fun CollapsibleCard(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    headerBadge: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(modifier.fillMaxWidth().background(WayColors.Surface, WayShapes.Card)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = WaySpacing.CardPadding)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = WayColors.SubText)
            if (headerBadge != null) { Spacer(Modifier.width(12.dp)); headerBadge() }
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                if (expanded) "접기" else "펼치기",
                tint = WayColors.SubText,
                modifier = Modifier.size(32.dp)
            )
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier.padding(start = WaySpacing.CardPadding, end = WaySpacing.CardPadding, bottom = WaySpacing.CardPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}
