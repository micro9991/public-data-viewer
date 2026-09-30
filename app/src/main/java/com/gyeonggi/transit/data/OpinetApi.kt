package com.gyeonggi.transit.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpinetApiException(message: String) : Exception(message)

/**
 * 오피넷(한국석유공사) 유가정보 오픈API 클라이언트.
 * https://www.opinet.co.kr/user/custapi/custApiInfo.do
 */
object OpinetApi {

    private const val BASE_URL = "https://www.opinet.co.kr/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun execute(url: String): JSONArray {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            val bodyText = resp.body?.string()
                ?: throw OpinetApiException("서버로부터 빈 응답을 받았습니다")
            val json = try {
                JSONObject(bodyText)
            } catch (e: Exception) {
                throw OpinetApiException("응답 형식을 해석할 수 없습니다 (인증키를 확인해주세요)")
            }
            val result = json.optJSONObject("RESULT")
                ?: throw OpinetApiException("알 수 없는 응답 형식입니다")
            return result.optJSONArray("OIL") ?: JSONArray()
        }
    }

    private fun buildPrices(items: List<JSONObject>): List<FuelPrice> {
        return items.map {
            FuelPrice(
                productCode = it.optString("PRODCD"),
                price = it.optDouble("PRICE", 0.0),
                diff = it.optDouble("DIFF", 0.0)
            )
        }
    }

    fun getSidoPrices(apiKey: String): List<SidoAvgPrice> {
        val url = "$BASE_URL/avgSidoPrice.do?code=$apiKey&out=json"
        val arr = execute(url)
        val grouped = LinkedHashMap<String, MutableList<JSONObject>>()
        val names = HashMap<String, String>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val code = o.optString("SIDOCD")
            if (code == "00") continue // "전국" 합계는 목록에서 제외
            grouped.getOrPut(code) { mutableListOf() }.add(o)
            names[code] = o.optString("SIDONM")
        }
        return grouped.map { (code, items) ->
            SidoAvgPrice(
                sidoCode = code,
                sidoName = names[code] ?: code,
                prices = buildPrices(items)
            )
        }
    }

    fun getSigunPrices(apiKey: String, sidoCode: String): List<SigunAvgPrice> {
        val url = "$BASE_URL/avgSigunPrice.do?code=$apiKey&out=json&sido=$sidoCode"
        val arr = execute(url)
        val grouped = LinkedHashMap<String, MutableList<JSONObject>>()
        val names = HashMap<String, String>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val code = o.optString("SIGUNCD")
            grouped.getOrPut(code) { mutableListOf() }.add(o)
            names[code] = o.optString("SIGUNNM")
        }
        return grouped.map { (code, items) ->
            SigunAvgPrice(
                sigunCode = code,
                sigunName = names[code] ?: code,
                prices = buildPrices(items)
            )
        }
    }

    /**
     * 좌표(KATEC) 기준 반경 내 주유소를 가격 오름차순으로 조회한다.
     * @param x,y KATEC 좌표(m). [KatecConverter]로 WGS84에서 변환.
     * @param radiusMeters 검색 반경(m), 최대 5000
     * @param productCode 유종코드 (예: B027=휘발유, D047=경유)
     */
    fun getNearbyStations(
        apiKey: String,
        x: Double,
        y: Double,
        radiusMeters: Int,
        productCode: String
    ): List<NearbyStation> {
        val url = "$BASE_URL/aroundAll.do?code=$apiKey&out=json&x=$x&y=$y&radius=$radiusMeters&prodcd=$productCode&sort=1"
        val arr = execute(url)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            NearbyStation(
                uniId = o.optString("UNI_ID"),
                name = o.optString("OS_NM"),
                brandCode = o.optString("POLL_DIV_CD"),
                price = o.optInt("PRICE"),
                distanceMeters = o.optDouble("DISTANCE", 0.0),
                gisX = o.optDouble("GIS_X_COOR", 0.0),
                gisY = o.optDouble("GIS_Y_COOR", 0.0)
            )
        }
    }
}
