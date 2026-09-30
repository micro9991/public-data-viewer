package com.gyeonggi.transit.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SubwayApiException(message: String) : Exception(message)

/**
 * 서울 열린데이터광장 지하철 실시간 도착정보 API 클라이언트.
 * http://swopenapi.seoul.go.kr/api/subway/{key}/json/realtimeStationArrival/{start}/{end}/{역명}
 */
object SubwayApi {

    private const val BASE_URL = "http://swopenapi.seoul.go.kr/api/subway"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun JSONObject.asArray(key: String): JSONArray {
        val value = this.opt(key) ?: return JSONArray()
        return when (value) {
            is JSONArray -> value
            is JSONObject -> JSONArray().put(value)
            else -> JSONArray()
        }
    }

    fun searchStation(apiKey: String, stationName: String): List<SubwayArrival> {
        val encodedName = URLEncoder.encode(stationName, "UTF-8")
        val url = "$BASE_URL/$apiKey/json/realtimeStationArrival/0/30/$encodedName"

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            val bodyText = resp.body?.string()
                ?: throw SubwayApiException("서버로부터 빈 응답을 받았습니다")

            val json = try {
                JSONObject(bodyText)
            } catch (e: Exception) {
                throw SubwayApiException("응답 형식을 해석할 수 없습니다")
            }

            val status = json.optInt("status", 200)
            if (status != 200) {
                val message = json.optString("message").takeIf { it.isNotBlank() }
                    ?: "요청한 역 정보를 찾을 수 없습니다"
                throw SubwayApiException(message)
            }

            val header = json.optJSONObject("errorMessage")
            val code = header?.optString("code")
            if (code != null && code != "INFO-000") {
                throw SubwayApiException(header.optString("message", "요청한 역 정보를 찾을 수 없습니다"))
            }

            val arr = json.asArray("realtimeArrivalList")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val arvlMsg2 = o.optString("arvlMsg2").takeIf { it.isNotBlank() } ?: "정보 없음"
                val rawBarvlDt = o.optInt("barvlDt", -1).takeIf { it >= 0 }
                val parsedFromMessage = parseSecondsFromArrivalMessage(arvlMsg2)
                // "[N]번째 전역" 형태는 정거장 수 표현이라 시간으로 환산할 수 없다 -> null 유지
                val effectiveSeconds = when {
                    parsedFromMessage != null -> parsedFromMessage
                    arvlMsg2.contains("번째 전역") -> null
                    else -> rawBarvlDt
                }
                SubwayArrival(
                    lineName = subwayLineName(o.optString("subwayId")),
                    updnLine = o.optString("updnLine"),
                    trainLineNm = o.optString("trainLineNm"),
                    trainNo = o.optString("btrainNo"),
                    arrivalMessage = arvlMsg2,
                    currentOrNextStation = o.optString("arvlMsg3"),
                    destinationStation = o.optString("bstatnNm"),
                    arrivalSeconds = effectiveSeconds,
                    recptnDt = o.optString("recptnDt")
                )
            }
        }
    }
}
