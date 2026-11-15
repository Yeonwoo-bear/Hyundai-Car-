package com.way.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.way.app.detail.PlaceDetail
import com.way.app.detail.PlaceDetailScreen
import com.way.app.fit.ParkingRecommender
import com.way.app.fit.ParkingRepository
import com.way.app.fit.Recommendation
import com.way.app.fit.Weights
import com.way.app.fit.ui.StopFitScreen
import com.way.app.fit.ui.StopFitUiState
import com.way.app.home.HomeScreen
import com.way.app.learn.StopCategory
import com.way.app.learn.ui.LearnTarget
import com.way.app.learn.ui.StopLearnScreen
import com.way.app.rest.RestPlacesScreen
import com.way.app.sense.ui.StopSenseScreen
import com.way.app.sense.ui.rememberSenseController
import com.way.app.ui.WayDest
import com.way.app.ui.WayNavRail
import com.way.app.ui.theme.WAYTheme
import com.way.app.ui.theme.WayColors
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WAYTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = WayColors.Background) { innerPadding ->
                    WayApp(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** 상세/기록 화면으로 넘길 선택 장소. */
private data class Selected(val detail: PlaceDetail, val learn: LearnTarget)

@Composable
private fun WayApp(modifier: Modifier = Modifier) {
    val sense = rememberSenseController()
    val backStack = remember { mutableStateListOf(WayDest.HOME) }
    val current = backStack.last()
    var selected by remember { mutableStateOf<Selected?>(null) }

    fun go(dest: WayDest) { if (current != dest) backStack.add(dest) }
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    /** 레일에서 고르면 해당 섹션으로 바로 이동 (스택 초기화). */
    fun railSelect(dest: WayDest) {
        backStack.clear(); backStack.add(WayDest.HOME)
        if (dest != WayDest.HOME) backStack.add(dest)
    }

    BackHandler(enabled = backStack.size > 1) { back() }

    Row(modifier.fillMaxSize().background(WayColors.Background)) {
        WayNavRail(current = current, onSelect = ::railSelect)
        Box(Modifier.weight(1f).fillMaxSize()) {
            val onBack: (() -> Unit)? = if (backStack.size > 1) ({ back() }) else null
            when (current) {
                WayDest.HOME -> HomeScreen(sense, onOpenSense = { go(WayDest.SENSE) }, onOpenFit = { go(WayDest.FIT) })
                WayDest.SENSE -> StopSenseScreen(
                    sense,
                    onOpenRest = { go(WayDest.REST) },
                    onOpenParking = { go(WayDest.FIT) },
                    onBack = onBack
                )
                WayDest.REST -> RestPlacesScreen(
                    onSelect = { p ->
                        selected = Selected(
                            PlaceDetail(p.id, p.name, "${p.type.label} · ${p.distanceKm} km · 약 ${p.etaMinutes}분", isSample = p.isSample),
                            LearnTarget(p.id, p.name, StopCategory.REST)
                        )
                        go(WayDest.DETAIL)
                    },
                    onBack = { back() }
                )
                WayDest.FIT -> StopFitRoute(
                    onSelect = { rec, fromSample ->
                        selected = Selected(parkingDetail(rec, fromSample), LearnTarget(rec.name, rec.name, StopCategory.PARKING))
                        go(WayDest.DETAIL)
                    },
                    onBack = onBack
                )
                WayDest.DETAIL -> selected?.let { s ->
                    PlaceDetailScreen(s.detail, onRecordExperience = { go(WayDest.LEARN) }, onBack = { back() })
                } ?: LaunchedEffect(Unit) { back() }
                WayDest.LEARN -> StopLearnScreen(selected?.learn, onBack = onBack)
            }
        }
    }
}

private val won = NumberFormat.getNumberInstance(Locale.KOREA)

private fun parkingDetail(rec: Recommendation, fromSample: Boolean) = PlaceDetail(
    id = rec.name,
    name = rec.name,
    summary = "${ParkingRecommender.formatDistance(rec.distanceMeters)} · 기본 " +
        (if (rec.baseFee <= 0) "무료" else "${won.format(rec.baseFee)}원") +
        " · 빈 자리 ${Math.round(rec.vacancyRate * 100)}%",
    extra = rec.reason,
    isSample = fromSample
)

/** 실제 위치 연동 전까지 사용하는 기본 좌표 (서울시청). */
private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LON = 126.9780

@Composable
private fun StopFitRoute(
    onSelect: (Recommendation, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { ParkingRepository(context) }
    var state by remember { mutableStateOf<StopFitUiState>(StopFitUiState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        state = StopFitUiState.Loading
        val result = repository.load()
        val recs = ParkingRecommender(result.lots).recommend(DEFAULT_LAT, DEFAULT_LON, Weights())
        state = if (recs.isEmpty()) StopFitUiState.Empty("주변에 빈 주차장이 없어요")
        else StopFitUiState.Ready(recs, fromSample = result.source == ParkingRepository.Source.SAMPLE)
    }

    StopFitScreen(
        state = state,
        onSelect = { rec -> onSelect(rec, (state as? StopFitUiState.Ready)?.fromSample == true) },
        onRefresh = { reloadKey++ },
        modifier = modifier,
        onBack = onBack
    )
}
