package com.gyeonggi.transit.data

data class SubwayArrival(
    val lineName: String,
    val updnLine: String,
    val trainLineNm: String,
    val trainNo: String,
    val arrivalMessage: String,
    val currentOrNextStation: String,
    val destinationStation: String,
    val arrivalSeconds: Int?,
    val recptnDt: String
)

private val SUBWAY_LINE_NAMES = mapOf(
    "1001" to "1호선",
    "1002" to "2호선",
    "1003" to "3호선",
    "1004" to "4호선",
    "1005" to "5호선",
    "1006" to "6호선",
    "1007" to "7호선",
    "1008" to "8호선",
    "1009" to "9호선",
    "1032" to "서해선",
    "1063" to "경의중앙선",
    "1065" to "공항철도",
    "1067" to "경춘선",
    "1075" to "수인분당선",
    "1077" to "신분당선",
    "1092" to "우이신설선",
    "1093" to "서해선",
    "1081" to "경강선",
    "1094" to "신림선"
)

fun subwayLineName(subwayId: String): String = SUBWAY_LINE_NAMES[subwayId] ?: subwayId

private val MIN_SEC_PATTERN = Regex("""(\d+)\s*분\s*(\d+)\s*초\s*후""")
private val MIN_PATTERN = Regex("""(\d+)\s*분\s*후""")
private val SEC_PATTERN = Regex("""(\d+)\s*초\s*후""")

/**
 * 도착 안내 문구("5분 30초 후", "7분 후" 등)에서 초 단위 시간을 추출한다.
 * "[4]번째 전역" 같은 정거장 수 표현은 시간으로 환산할 수 없으므로 null을 반환한다.
 */
fun parseSecondsFromArrivalMessage(message: String): Int? {
    MIN_SEC_PATTERN.find(message)?.let {
        val (minutes, seconds) = it.destructured
        return minutes.toInt() * 60 + seconds.toInt()
    }
    MIN_PATTERN.find(message)?.let {
        return it.groupValues[1].toInt() * 60
    }
    SEC_PATTERN.find(message)?.let {
        return it.groupValues[1].toInt()
    }
    return null
}

/** "6분 50초" 형태의 분/초 문자열로 변환한다. null이면 "정보 없음". */
fun formatMinutesSeconds(totalSeconds: Int?): String {
    if (totalSeconds == null) return "정보 없음"
    if (totalSeconds <= 0) return "0분 0초"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}분 ${seconds}초"
}

private val RECPTN_FORMAT = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.KOREA)
private val CLOCK_FORMAT = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)

/**
 * 서버가 데이터를 수신한 시각(recptnDt)에 도착까지 남은 초(secondsFromNow)를 더해
 * 열차가 실제 도착해야 하는 시계 시각을 계산한다.
 */
fun formatExpectedArrivalClock(recptnDt: String, secondsFromNow: Int?): String {
    if (secondsFromNow == null || recptnDt.isBlank()) return "정보 없음"
    return try {
        val base = RECPTN_FORMAT.parse(recptnDt) ?: return "정보 없음"
        val arrival = java.util.Date(base.time + secondsFromNow * 1000L)
        CLOCK_FORMAT.format(arrival)
    } catch (e: Exception) {
        "정보 없음"
    }
}
