package com.gyeonggi.transit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyeonggi.transit.BuildConfig
import com.gyeonggi.transit.data.SubwayApi
import com.gyeonggi.transit.data.SubwayApiException
import com.gyeonggi.transit.data.SubwayArrival
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class SubwayScreenState(
    val keyword: String = "",
    val isSearching: Boolean = false,
    val searchedKeyword: String? = null,
    val arrivals: List<SubwayArrival> = emptyList(),
    val error: String? = null,
    val selectedArrival: SubwayArrival? = null,
    val isRefreshing: Boolean = false,
    val refreshError: String? = null
)

private const val POLL_INTERVAL_MS = 20_000L

private fun sameTrain(a: SubwayArrival, b: SubwayArrival): Boolean {
    if (a.trainNo.isNotBlank() && b.trainNo.isNotBlank()) {
        return a.trainNo == b.trainNo && a.lineName == b.lineName
    }
    return a.lineName == b.lineName && a.updnLine == b.updnLine && a.trainLineNm == b.trainLineNm
}

class SubwayViewModel : ViewModel() {

    private val apiKey = BuildConfig.SEOUL_SUBWAY_API_KEY

    var state by mutableStateOf(SubwayScreenState())
        private set

    private var pollingJob: Job? = null

    fun onKeywordChange(value: String) {
        state = state.copy(keyword = value)
    }

    fun searchStation() {
        val trimmed = state.keyword.trim()
        if (trimmed.isEmpty()) return
        pollingJob?.cancel()
        state = state.copy(
            isSearching = true,
            searchedKeyword = trimmed,
            arrivals = emptyList(),
            error = null,
            selectedArrival = null,
            refreshError = null
        )
        viewModelScope.launch {
            try {
                val arrivals = withContext(Dispatchers.IO) { SubwayApi.searchStation(apiKey, trimmed) }
                state = state.copy(
                    isSearching = false,
                    arrivals = arrivals,
                    error = if (arrivals.isEmpty()) "'$trimmed' 역 정보를 찾을 수 없습니다" else null
                )
            } catch (e: SubwayApiException) {
                state = state.copy(isSearching = false, error = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isSearching = false, error = "네트워크 연결을 확인해주세요")
            }
        }
    }

    fun selectArrival(arrival: SubwayArrival) {
        state = state.copy(selectedArrival = arrival, refreshError = null)
        startPolling(arrival)
    }

    fun clearSelection() {
        pollingJob?.cancel()
        state = state.copy(selectedArrival = null, refreshError = null, isRefreshing = false)
    }

    private fun startPolling(initial: SubwayArrival) {
        pollingJob?.cancel()
        val stationName = state.searchedKeyword ?: return
        pollingJob = viewModelScope.launch {
            var first = true
            while (isActive) {
                if (!first) state = state.copy(isRefreshing = true)
                first = false
                try {
                    val arrivals = withContext(Dispatchers.IO) { SubwayApi.searchStation(apiKey, stationName) }
                    val matched = arrivals.firstOrNull { sameTrain(it, initial) }
                    if (matched != null) {
                        state = state.copy(
                            selectedArrival = matched,
                            isRefreshing = false,
                            refreshError = null
                        )
                    } else {
                        state = state.copy(isRefreshing = false, refreshError = "해당 열차 정보가 갱신되지 않았습니다")
                    }
                } catch (e: SubwayApiException) {
                    state = state.copy(isRefreshing = false, refreshError = e.message ?: "오류가 발생했습니다")
                    return@launch
                } catch (e: IOException) {
                    state = state.copy(isRefreshing = false, refreshError = "네트워크 연결을 확인해주세요")
                    return@launch
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        val selected = state.selectedArrival ?: return
        startPolling(selected)
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
