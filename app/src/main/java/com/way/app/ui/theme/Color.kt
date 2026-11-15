package com.way.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WAY 차량용 다크 팔레트 (docs/ui-ref/ref.png 기준).
 * 어두운 남색 배경 + 보라 포인트. 주간/야간 모두 이 팔레트로 고정한다.
 */
object WayColors {
    val Background = Color(0xFF121229)      // 화면 배경 (짙은 남색)
    val Rail = Color(0xFF0C0C1F)            // 왼쪽 내비게이션 레일
    val Surface = Color(0xFF1C1C3A)         // 기본 카드
    val SurfaceHigh = Color(0xFF262650)     // 카드 안의 행 / 눌림 가능 요소
    val Outline = Color(0xFF38385F)

    val Primary = Color(0xFF8B7CF6)         // 보라 포인트
    val PrimaryStrong = Color(0xFF6E5CE6)   // 꽉 찬 버튼
    val PrimarySoft = Color(0xFFCFC7FF)     // 결과 카드(연보라)
    val OnPrimarySoft = Color(0xFF2A1F6B)   // 연보라 위 글자
    val SelectedFill = Color(0xFF3A3170)    // 선택된 버튼 배경

    val Text = Color(0xFFF3F2FB)
    val SubText = Color(0xFFA9A8C8)
    val Muted = Color(0xFF6F6E92)

    val Good = Color(0xFF5BD68A)            // 여유
    val Warn = Color(0xFFF5C451)            // 보통
    val Bad = Color(0xFFFF6B6B)             // 혼잡

    val DemoBadge = Color(0xFFF5C451)       // '데모' 배지
    val SampleBadge = Color(0xFF7FD3FF)     // '샘플' 배지
}
