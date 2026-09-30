package com.gyeonggi.transit.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyeonggi.transit.BuildConfig
import com.gyeonggi.transit.data.KatecConverter
import com.gyeonggi.transit.data.LocationHelper
import com.gyeonggi.transit.data.LocationUnavailableException
import com.gyeonggi.transit.data.NearbyStation
import com.gyeonggi.transit.data.OpinetApi
import com.gyeonggi.transit.data.OpinetApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val NEARBY_RADIUS_METERS = 3000

data class LowestPriceScreenState(
    val selectedProduct: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val stations: List<NearbyStation> = emptyList(),
    val userLat: Double? = null,
    val userLon: Double? = null
)

class LowestFuelPriceViewModel : ViewModel() {

    private val apiKey = BuildConfig.OPINET_API_KEY

    var state by mutableStateOf(LowestPriceScreenState())
        private set

    fun selectProduct(productCode: String, context: Context) {
        state = state.copy(selectedProduct = productCode, isLoading = true, error = null, stations = emptyList())
        fetch(productCode, context)
    }

    fun refresh(context: Context) {
        val product = state.selectedProduct ?: return
        state = state.copy(isLoading = true, error = null)
        fetch(product, context)
    }

    private fun fetch(productCode: String, context: Context) {
        viewModelScope.launch {
            try {
                val location = withContext(Dispatchers.IO) { LocationHelper.getCurrentLocation(context) }
                val (x, y) = KatecConverter.wgs84ToKatec(location.latitude, location.longitude)
                val stations = withContext(Dispatchers.IO) {
                    OpinetApi.getNearbyStations(apiKey, x, y, NEARBY_RADIUS_METERS, productCode)
                }
                state = state.copy(
                    isLoading = false,
                    stations = stations.take(10),
                    userLat = location.latitude,
                    userLon = location.longitude,
                    error = if (stations.isEmpty()) "주변 ${NEARBY_RADIUS_METERS / 1000}km 내 주유소 정보를 찾을 수 없습니다" else null
                )
            } catch (e: LocationUnavailableException) {
                state = state.copy(isLoading = false, error = e.message ?: "위치 정보를 가져올 수 없습니다")
            } catch (e: OpinetApiException) {
                state = state.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다")
            } catch (e: IOException) {
                state = state.copy(isLoading = false, error = "네트워크 연결을 확인해주세요")
            }
        }
    }
}
