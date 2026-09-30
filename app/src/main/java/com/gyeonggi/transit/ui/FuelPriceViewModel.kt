package com.gyeonggi.transit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyeonggi.transit.BuildConfig
import com.gyeonggi.transit.data.OpinetApi
import com.gyeonggi.transit.data.OpinetApiException
import com.gyeonggi.transit.data.SidoAvgPrice
import com.gyeonggi.transit.data.SigunAvgPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class FuelScreenState(
    val isLoadingSido: Boolean = false,
    val sidoList: List<SidoAvgPrice> = emptyList(),
    val sidoError: String? = null,
    val selectedSido: SidoAvgPrice? = null,
    val isLoadingSigun: Boolean = false,
    val sigunList: List<SigunAvgPrice> = emptyList(),
    val sigunError: String? = null,
    val selectedSigun: SigunAvgPrice? = null,
    val isRefreshing: Boolean = false
)

class FuelPriceViewModel : ViewModel() {

    private val apiKey = BuildConfig.OPINET_API_KEY

    var state by mutableStateOf(FuelScreenState())
        private set

    init {
        loadSidoList()
    }

    private fun loadSidoList() {
        state = state.copy(isLoadingSido = true, sidoError = null)
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { OpinetApi.getSidoPrices(apiKey) }
                state = state.copy(
                    isLoadingSido = false,
                    sidoList = list,
                    sidoError = if (list.isEmpty()) "지역별 유가 정보를 불러올 수 없습니다" else null
                )
            } catch (e: OpinetApiException) {
                state = state.copy(isLoadingSido = false, sidoError = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isLoadingSido = false, sidoError = "네트워크 연결을 확인해주세요")
            }
        }
    }

    fun selectSido(sido: SidoAvgPrice) {
        state = state.copy(
            selectedSido = sido,
            isLoadingSigun = true,
            sigunList = emptyList(),
            sigunError = null,
            selectedSigun = null
        )
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { OpinetApi.getSigunPrices(apiKey, sido.sidoCode) }
                state = state.copy(
                    isLoadingSigun = false,
                    sigunList = list,
                    sigunError = if (list.isEmpty()) "시군구별 유가 정보를 불러올 수 없습니다" else null
                )
            } catch (e: OpinetApiException) {
                state = state.copy(isLoadingSigun = false, sigunError = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isLoadingSigun = false, sigunError = "네트워크 연결을 확인해주세요")
            }
        }
    }

    fun clearSidoSelection() {
        state = state.copy(
            selectedSido = null,
            sigunList = emptyList(),
            sigunError = null,
            selectedSigun = null
        )
    }

    fun selectSigun(sigun: SigunAvgPrice) {
        state = state.copy(selectedSigun = sigun)
    }

    fun clearSigunSelection() {
        state = state.copy(selectedSigun = null)
    }

    fun refresh() {
        val sido = state.selectedSido ?: return
        val currentSigunCode = state.selectedSigun?.sigunCode
        state = state.copy(isRefreshing = true)
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { OpinetApi.getSigunPrices(apiKey, sido.sidoCode) }
                val matched = currentSigunCode?.let { code -> list.firstOrNull { it.sigunCode == code } }
                state = state.copy(
                    isRefreshing = false,
                    sigunList = list,
                    sigunError = if (list.isEmpty()) "시군구별 유가 정보를 불러올 수 없습니다" else null,
                    selectedSigun = matched ?: state.selectedSigun
                )
            } catch (e: OpinetApiException) {
                state = state.copy(isRefreshing = false, sigunError = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isRefreshing = false, sigunError = "네트워크 연결을 확인해주세요")
            }
        }
    }
}
