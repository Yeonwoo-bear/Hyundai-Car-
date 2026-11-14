package com.way.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.way.app.fit.ParkingRecommender
import com.way.app.fit.ParkingRepository
import com.way.app.fit.Weights
import com.way.app.fit.ui.StopFitScreen
import com.way.app.fit.ui.StopFitUiState
import com.way.app.ui.theme.WAYTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WAYTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StopFitRoute(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** 실제 위치 연동 전까지 사용하는 기본 좌표 (서울시청). */
private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LON = 126.9780

@androidx.compose.runtime.Composable
private fun StopFitRoute(modifier: Modifier = Modifier) {
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
        onSelect = { Toast.makeText(context, "${it.name} 선택", Toast.LENGTH_SHORT).show() },
        onRefresh = { reloadKey++ },
        modifier = modifier
    )
}
