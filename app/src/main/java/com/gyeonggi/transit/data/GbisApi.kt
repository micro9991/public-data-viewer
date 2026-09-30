package com.gyeonggi.transit.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GbisApiException(message: String) : Exception(message)

/**
 * 경기도 버스정보시스템(GBIS) 공유 API 클라이언트.
 * https://www.gbis.go.kr/gbis2014/publicService.action?cmd=openApiInfo
 */
object GbisApi {

    private const val BASE_URL = "https://apis.data.go.kr/6410000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun buildUrl(path: String, params: Map<String, String>): String {
        val query = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$BASE_URL/$path?$query"
    }

    private fun JSONObject.asArray(key: String): JSONArray {
        val value = this.opt(key) ?: return JSONArray()
        return when (value) {
            is JSONArray -> value
            is JSONObject -> JSONArray().put(value)
            else -> JSONArray()
        }
    }

    private fun execute(url: String): JSONObject {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            val bodyText = resp.body?.string()
                ?: throw GbisApiException("서버로부터 빈 응답을 받았습니다")

            val json = try {
                JSONObject(bodyText)
            } catch (e: Exception) {
                throw GbisApiException("응답 형식을 해석할 수 없습니다")
            }

            // 인증키 오류 등은 다른 최상위 구조(OpenAPI_ServiceResponse)로 내려온다
            json.optJSONObject("OpenAPI_ServiceResponse")?.let { errRoot ->
                val header = errRoot.optJSONObject("cmmMsgHeader")
                val message = header?.optString("returnAuthMsg")?.takeIf { it.isNotBlank() }
                    ?: header?.optString("errMsg")
                    ?: "API 인증/요청 오류"
                throw GbisApiException(message)
            }

            val response = json.optJSONObject("response")
                ?: throw GbisApiException("알 수 없는 응답 형식입니다")

            val header = response.optJSONObject("msgHeader")
            val resultCode = header?.optString("resultCode")
            // 정상 응답은 resultCode가 "0"(문자열) 또는 0(숫자)로 내려온다
            if (resultCode != null && resultCode != "0") {
                throw GbisApiException(header.optString("resultMessage", "알 수 없는 오류가 발생했습니다"))
            }

            return response.optJSONObject("msgBody") ?: JSONObject()
        }
    }

    fun searchRoutes(apiKey: String, keyword: String): List<BusRoute> {
        val url = buildUrl(
            "busrouteservice/v2/getBusRouteListv2",
            mapOf("serviceKey" to apiKey, "format" to "json", "keyword" to keyword)
        )
        val body = execute(url)
        val arr = body.asArray("busRouteList")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            BusRoute(
                routeId = o.optString("routeId"),
                routeName = o.optString("routeName"),
                startStationName = o.optString("startStationName"),
                endStationName = o.optString("endStationName"),
                regionName = o.optString("regionName")
            )
        }
    }

    fun getRouteStations(apiKey: String, routeId: String): List<BusRouteStation> {
        val url = buildUrl(
            "busrouteservice/v2/getBusRouteStationListv2",
            mapOf("serviceKey" to apiKey, "format" to "json", "routeId" to routeId)
        )
        val body = execute(url)
        val arr = body.asArray("busRouteStationList")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            BusRouteStation(
                stationId = o.optString("stationId"),
                stationName = o.optString("stationName"),
                staOrder = o.optInt("stationSeq")
            )
        }.sortedBy { it.staOrder }
    }

    fun getArrival(apiKey: String, stationId: String, routeId: String, staOrder: Int): BusArrivalItem {
        val url = buildUrl(
            "busarrivalservice/v2/getBusArrivalItemv2",
            mapOf(
                "serviceKey" to apiKey,
                "format" to "json",
                "stationId" to stationId,
                "routeId" to routeId,
                "staOrder" to staOrder.toString()
            )
        )
        val body = execute(url)
        val o = body.optJSONObject("busArrivalItem")
            ?: throw GbisApiException("도착 정보가 없습니다")
        return BusArrivalItem(
            routeName = o.optString("routeName"),
            predictTime1 = o.optInt("predictTime1", -1).takeIf { it >= 0 },
            predictTime2 = o.optInt("predictTime2", -1).takeIf { it >= 0 },
            locationNo1 = o.optInt("locationNo1", -1).takeIf { it >= 0 },
            locationNo2 = o.optInt("locationNo2", -1).takeIf { it >= 0 },
            plateNo1 = o.optString("plateNo1").takeIf { it.isNotBlank() },
            plateNo2 = o.optString("plateNo2").takeIf { it.isNotBlank() },
            remainSeatCnt1 = o.optInt("remainSeatCnt1", -1).takeIf { it >= 0 },
            remainSeatCnt2 = o.optInt("remainSeatCnt2", -1).takeIf { it >= 0 }
        )
    }
}
