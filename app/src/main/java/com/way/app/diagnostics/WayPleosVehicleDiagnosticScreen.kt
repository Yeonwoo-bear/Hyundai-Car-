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
 * STOP SENSE / STOP LEARN 화면.
 * 차량이 트립 주행 시간을 제공하면 그 값만 사용하고,
 * 제공하지 않는 환경(예: Pleos 에뮬레이터)에서는 '데모 입력'임을 명시한 수동 값으로 판단한다.
 * 운전 중 자동 팝업은 하지 않는다.
 */
@Composable
fun WayPleosVehicleDiagnosticScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var telemetry by remember { mutableStateOf(PleosTripReading()) }
    val reader = remember(context) { PleosVehicleReader(context) { telemetry = it } }
    DisposableEffect(reader) {
        reader.connectAndRead() // 화면 진입 시 자동 연결
        onDispose { reader.close() }
    }
    var unit by remember { mutableStateOf(TripTimeUnit.UNCONFIRMED) }
    var eta by remember { mutableStateOf("") }          // 목적지까지 남은 시간 (수동, Navi 연동 전)
    var demoMinutes by remember { mutableStateOf("") }  // 차량 값이 없을 때만 쓰는 데모 입력
    var ignoreGear by remember { mutableStateOf(false) }
    val repo = remember(context) { StopLearnRepository(context) }
    var preference by remember { mutableStateOf(repo.getPreference()) }
    var historyCount by remember { mutableIntStateOf(repo.getHistory(50).size) }
    var latestNote by remember { mutableStateOf("가격/거리 중 무엇이 더 중요한지 눌러 주세요.") }

    val vehicleMinutes = if (telemetry.status == ReadingStatus.OK) {
        unit.minutesOrNull(telemetry.numericValue)
    } else null
    val vehicleHasTripTime = telemetry.status == ReadingStatus.OK
    val manualMinutes = demoMinutes.toIntOrNull()?.takeIf { it >= 0 }
    val usingDemo = vehicleMinutes == null && manualMinutes != null
    val minutes = vehicleMinutes ?: manualMinutes
    val destinationMinutes = eta.toIntOrNull()?.takeIf { it >= 0 }
    val parked = if (ignoreGear) false else telemetry.gear?.isParked == true
    val decision = minutes?.let {
        StopSenseEngine().assess(
            DrivingContext(
                drivingMinutes = it,
                minutesToDestination = destinationMinutes,
                isVehicleMoving = !parked
            )
        ).copy(isDemoDecision = usingDemo)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("STOP SENSE · 정차 필요 감지", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { reader.connectAndRead() }) { Text("차량 연결 / 조회") }
            OutlinedButton(onClick = { reader.refresh() }) { Text("새로고침") }
        }
        Text("차량 상태: ${telemetry.status}")
        Text("기어: ${telemetry.gear?.caption ?: "미확인"}")
        Text("데이터 종류: ${telemetry.source?.caption ?: "없음"} · 원시 응답: ${telemetry.rawValue ?: "없음"}")
        Text("설명: ${telemetry.detail}")

        if (vehicleHasTripTime) {
            Text("반환값의 단위 확인 (실제 값이 맞는지 검증한 뒤 선택)")
            TripTimeUnit.entries.forEach { choice ->
                Row {
                    RadioButton(selected = unit == choice, onClick = { unit = choice })
                    Text(choice.caption, Modifier.padding(top = 12.dp))
                }
            }
        } else {
            Text(
                "차량이 주행 시간을 제공하지 않아요. 아래 '데모 입력'으로 STOP SENSE를 시험할 수 있어요.",
                color = MaterialTheme.colorScheme.error
            )
        }
        OutlinedTextField(
            value = demoMinutes,
            onValueChange = { demoMinutes = it.filter(Char::isDigit).take(4) },
            enabled = vehicleMinutes == null,
            label = { Text("데모 입력: 주행 시간(분) — 차량 값이 없을 때만 사용") }
        )
        OutlinedTextField(
            value = eta,
            onValueChange = { eta = it.filter(Char::isDigit).take(4) },
            label = { Text("목적지까지 남은 시간(분) — 수동 입력") }
        )
        if (telemetry.gear?.isParked == true) {
            Row {
                Checkbox(checked = ignoreGear, onCheckedChange = { ignoreGear = it })
                Text("데모: P 기어여도 주행 중으로 간주", Modifier.padding(top = 12.dp))
            }
        }

        Text("판단에 쓴 주행 시간: ${minutes?.let { "$it 분" + if (usingDemo) " (데모 입력)" else " (차량)" } ?: "없음"}")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "STOP SENSE: ${decision?.title ?: "주행 시간 입력 대기"}" +
                        if (decision?.isDemoDecision == true) " · 데모" else "",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(decision?.message ?: "차량 값 또는 데모 입력이 있어야 판단해요.")
            }
        }
        Text(
            "주의: 트립 주행 시간은 연속 무휴식 운전 시간이나 졸음 측정값과 같지 않아요.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()
        Text("STOP LEARN · 실제 저장되는 개인화", style = MaterialTheme.typography.headlineSmall)
        Text("요금 중요도: ${Math.round(preference.priceWeight * 100)}%")
        Text("거리 중요도: ${Math.round(preference.distanceWeight * 100)}%")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                preference = repo.submitFeedback(PreferenceFeedback.CHEAPER)
                historyCount = repo.getHistory(50).size
                latestNote = "저렴한 곳 선호 저장"
            }) { Text("가격 우선") }
            Button(onClick = {
                preference = repo.submitFeedback(PreferenceFeedback.CLOSER)
                historyCount = repo.getHistory(50).size
                latestNote = "가까운 곳 선호 저장"
            }) { Text("거리 우선") }
            OutlinedButton(onClick = {
                preference = repo.submitFeedback(PreferenceFeedback.BALANCED)
                historyCount = repo.getHistory(50).size
                latestNote = "균형 선호 저장"
            }) { Text("균형") }
            TextButton(onClick = {
                repo.clearAll()
                preference = repo.getPreference()
                historyCount = 0
                latestNote = "초기화됨"
            }) { Text("초기화") }
        }
        Text(latestNote)
        Text("피드백 기록: ${historyCount}건 (최근 최대 50건 표시)")
    }
}
