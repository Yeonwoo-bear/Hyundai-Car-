package com.way.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.way.app.ui.theme.WayColors
import com.way.app.ui.theme.WayShapes

/** 앱 안의 화면. 레일에는 HOME/SENSE/FIT/LEARN 만 나온다. */
enum class WayDest { HOME, SENSE, REST, FIT, DETAIL, LEARN }

private data class RailItem(val dest: WayDest, val icon: ImageVector, val label: String)

private val railItems = listOf(
    RailItem(WayDest.HOME, Icons.Outlined.Home, "홈"),
    RailItem(WayDest.SENSE, Icons.Outlined.DirectionsCar, "SENSE"),
    RailItem(WayDest.FIT, Icons.Outlined.LocalParking, "FIT"),
    RailItem(WayDest.LEARN, Icons.Outlined.BarChart, "LEARN")
)

/** 하위 화면일 때 레일에서 어느 항목을 강조할지. */
private fun WayDest.railOwner(): WayDest = when (this) {
    WayDest.REST -> WayDest.SENSE
    WayDest.DETAIL -> WayDest.FIT
    else -> this
}

@Composable
fun WayNavRail(current: WayDest, onSelect: (WayDest) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxHeight()
            .width(112.dp)
            .background(WayColors.Rail)
            .padding(vertical = 24.dp)
    ) {
        Text("WAY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WayColors.Primary)
        Spacer(Modifier.height(16.dp))
        railItems.forEach { item ->
            val selected = current.railOwner() == item.dest
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(role = Role.Tab) { onSelect(item.dest) }
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .background(if (selected) WayColors.PrimaryStrong else WayColors.Rail, WayShapes.Row)
                ) {
                    Icon(item.icon, item.label, tint = if (selected) WayColors.Text else WayColors.SubText, modifier = Modifier.size(34.dp))
                }
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) WayColors.Text else WayColors.Muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
