package com.way.app.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.way.app.learn.PreferenceFeedback
import com.way.app.learn.StopLearnRepository
import com.way.app.sense.*

/**
 * Dedicated diagnostic screen. UI teammate can replace with real WAY screen.
 * It intentionally has no fake driving-time fallback and does not auto-pop up while driving.
 */
@Composable
fun WayPleosVehicleDiagnosticScreen() {
    val context = LocalContext.current.applicationContext
    var telemetry by remember { mutableStateOf(PleosTripReading()) }
    val reader = remember(context) { PleosVehicleReader(context) { telemetry = it } }
    DisposableEffect(reader) {
        onDispose { reader.close() }
    }
    var unit by remember { mutableStateOf(TripTimeUnit.UNCONFIRMED) }
    var eta by remember { mutableStateOf("") } // Optional manual diagnostic input, NOT vehicle ETA
    val repo = remember(context) { StopLearnRepository(context) }
    var preference by remember { mutableStateOf(repo.getPreference()) }
    var latestNote by remember { mutableStateOf("차량 데이터를 조회한 뒤 단위를 확인해 주세요.") }
    val minutes = if (telemetry.status == ReadingStatus.OK) {
        unit.minutesOrNull(telemetry.numericValue)
    } else null
    val destinationMinutes = eta.toIntOrNull()?.takeIf { it >= 0 }
    val decision = minutes?.let { StopSenseEngine().assess(
        DrivingContext(drivingMinutes = it, minutesToDestination = destinationMinutes)
    ).copy(isDemoDecision = false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("WAY · Pleos 실제 SDK 진단", style = MaterialTheme.typography.headlineSmall)
        Text("가상 주행 시간은 사용하지 않습니다. 차량이 제공하는 데이터만 표시합니다.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { reader.connectAndRead() }) { Text("차량 연결 / 조회") }
            OutlinedButton(onClick = { reader.refresh() }) { Text("새로고침") }
        }
        Text("차량 상태: ${telemetry.status}")
        Text("원시 응답: ${telemetry.rawValue ?: "없음"}")
        Text("설명: ${telemetry.detail}")
        HorizontalDivider()
        Text("반환값의 단위 확인 (실제 값이 맞는지 검증한 뒤 선택)")
        TripTimeUnit.entries.forEach { choice ->
            Row { RadioButton(selected = unit == choice, onClick = { unit = choice })
                  Text(choice.caption, Modifier.padding(top = 12.dp)) }
        }
        Text("해석된 트립 주행 시간: ${minutes?.let { "$it 분" } ?: "미확인"}")
        OutlinedTextField(
            value = eta,
            onValueChange = { eta = it.filter(Char::isDigit).take(4) },
            label = { Text("목적지까지 남은 시간(분) — 수동 입력, 진단 전용") }
        )
        Text("STOP SENSE 판단: ${decision?.result ?: "차량 데이터/단위 확인 전"}")
        Text(decision?.message ?: "실제 차량 주행 시간 확인 후에만 판단합니다.")
        Text("주의: 트립 주행 시간은 연속 무휴식 운전 시간이나 졸음 측정값과 동일하지 않습니다.")
        HorizontalDivider()
        Text("STOP LEARN · 실제 저장되는 개인화")
        Text("요금 중요도: ${(preference.priceWeight * 100).toInt()}%")
        Text("거리 중요도: ${(preference.distanceWeight * 100).toInt()}%")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                preference = repo.submitFeedback(PreferenceFeedback.CHEAPER)
                latestNote = "저렴한 곳 선호 저장"
            }) { Text("가격 우선") }
            Button(onClick = {
                preference = repo.submitFeedback(PreferenceFeedback.CLOSER)
                latestNote = "가까운 곳 선호 저장"
            }) { Text("거리 우선") }
        }
        Text(latestNote)
        Text("피드백 기록: ${repo.getHistory(50).size}건 (최근 최대 50건 표시)")
        Text("주차 후보 추천과 실제 내비게이션 연결은 STOP FIT / UI 담당과 통합해야 합니다.")
    }
}
