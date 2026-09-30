package com.gyeonggi.transit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyeonggi.transit.BuildConfig
import com.gyeonggi.transit.data.BusArrivalItem
import com.gyeonggi.transit.data.BusRoute
import com.gyeonggi.transit.data.BusRouteStation
import com.gyeonggi.transit.data.GbisApi
import com.gyeonggi.transit.data.GbisApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class BusScreenState(
    val keyword: String = "",
    val isSearching: Boolean = false,
    val searchedKeyword: String? = null,
    val routes: List<BusRoute> = emptyList(),
    val routeError: String? = null,
    val selectedRoute: BusRoute? = null,
    val isLoadingStations: Boolean = false,
    val stations: List<BusRouteStation> = emptyList(),
    val stationError: String? = null,
    val selectedStation: BusRouteStation? = null,
    val arrival: BusArrivalItem? = null,
    val isRefreshingArrival: Boolean = false,
    val arrivalError: String? = null
)

private const val POLL_INTERVAL_MS = 20_000L

class BusViewModel : ViewModel() {

    private val apiKey = BuildConfig.GBIS_API_KEY

    var state by mutableStateOf(BusScreenState())
        private set

    private var stationJob: Job? = null
    private var pollingJob: Job? = null

    fun onKeywordChange(value: String) {
        state = state.copy(keyword = value)
    }

    fun searchRoutes() {
        val trimmed = state.keyword.trim()
        if (trimmed.isEmpty()) return
        stationJob?.cancel()
        pollingJob?.cancel()
        state = state.copy(
            isSearching = true,
            searchedKeyword = trimmed,
            routes = emptyList(),
            routeError = null,
            selectedRoute = null,
            stations = emptyList(),
            stationError = null,
            selectedStation = null,
            arrival = null,
            arrivalError = null
        )
        viewModelScope.launch {
            try {
                val routes = withContext(Dispatchers.IO) { GbisApi.searchRoutes(apiKey, trimmed) }
                state = state.copy(
                    isSearching = false,
                    routes = routes,
                    routeError = if (routes.isEmpty()) "'$trimmed' 노선을 찾을 수 없습니다" else null
                )
            } catch (e: GbisApiException) {
                state = state.copy(isSearching = false, routeError = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isSearching = false, routeError = "네트워크 연결을 확인해주세요")
            }
        }
    }

    fun selectRoute(route: BusRoute) {
        pollingJob?.cancel()
        stationJob?.cancel()
        state = state.copy(
            selectedRoute = route,
            isLoadingStations = true,
            stations = emptyList(),
            stationError = null,
            selectedStation = null,
            arrival = null,
            arrivalError = null
        )
        stationJob = viewModelScope.launch {
            try {
                val stations = withContext(Dispatchers.IO) { GbisApi.getRouteStations(apiKey, route.routeId) }
                state = state.copy(
                    isLoadingStations = false,
                    stations = stations,
                    stationError = if (stations.isEmpty()) "경유 정류소 정보를 찾을 수 없습니다" else null
                )
            } catch (e: GbisApiException) {
                state = state.copy(isLoadingStations = false, stationError = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isLoadingStations = false, stationError = "네트워크 연결을 확인해주세요")
            }
        }
    }

    fun selectStation(station: BusRouteStation) {
        val route = state.selectedRoute ?: return
        state = state.copy(selectedStation = station, arrival = null, arrivalError = null)
        startPolling(route, station)
    }

    fun clearStationSelection() {
        pollingJob?.cancel()
        state = state.copy(
            selectedStation = null,
            arrival = null,
            arrivalError = null,
            isRefreshingArrival = false
        )
    }

    fun clearRouteSelection() {
        stationJob?.cancel()
        pollingJob?.cancel()
        state = state.copy(
            selectedRoute = null,
            isLoadingStations = false,
            stations = emptyList(),
            stationError = null,
            selectedStation = null,
            arrival = null,
            arrivalError = null,
            isRefreshingArrival = false
        )
    }

    private fun startPolling(route: BusRoute, station: BusRouteStation) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var first = true
            while (isActive) {
                if (!first) state = state.copy(isRefreshingArrival = true)
                first = false
                try {
                    val arrival = withContext(Dispatchers.IO) {
                        GbisApi.getArrival(apiKey, station.stationId, route.routeId, station.staOrder)
                    }
                    state = state.copy(arrival = arrival, isRefreshingArrival = false, arrivalError = null)
                } catch (e: GbisApiException) {
                    state = state.copy(isRefreshingArrival = false, arrivalError = e.message ?: "오류가 발생했습니다")
                    return@launch
                } catch (e: IOException) {
                    state = state.copy(isRefreshingArrival = false, arrivalError = "네트워크 연결을 확인해주세요")
                    return@launch
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshArrival() {
        val route = state.selectedRoute ?: return
        val station = state.selectedStation ?: return
        startPolling(route, station)
    }

    override fun onCleared() {
        pollingJob?.cancel()
        stationJob?.cancel()
        super.onCleared()
    }
}
