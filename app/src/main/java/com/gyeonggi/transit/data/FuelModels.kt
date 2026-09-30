package com.gyeonggi.transit.data

data class SidoAvgPrice(
    val sidoCode: String,
    val sidoName: String,
    val prices: List<FuelPrice>
)

data class SigunAvgPrice(
    val sigunCode: String,
    val sigunName: String,
    val prices: List<FuelPrice>
)

data class FuelPrice(
    val productCode: String,
    val price: Double,
    val diff: Double
)

private val PRODUCT_NAMES = mapOf(
    "B027" to "휘발유",
    "B034" to "고급휘발유",
    "C004" to "실내등유",
    "D047" to "경유",
    "K015" to "LPG(부탄)"
)

/** 화면에 보여줄 유종: 휘발유, 경유만 */
val PRODUCT_DISPLAY_ORDER = listOf("B027", "D047")

fun productName(productCode: String): String = PRODUCT_NAMES[productCode] ?: productCode

data class NearbyStation(
    val uniId: String,
    val name: String,
    val brandCode: String,
    val price: Int,
    val distanceMeters: Double,
    val gisX: Double,
    val gisY: Double
)

private val BRAND_NAMES = mapOf(
    "SKE" to "SK에너지",
    "GSC" to "GS칼텍스",
    "HDO" to "HD현대오일뱅크",
    "SOL" to "S-OIL",
    "RTE" to "자가상표",
    "RTX" to "자가상표",
    "RTO" to "자가상표",
    "NHO" to "농협",
    "E1G" to "E1",
    "SKG" to "SK가스",
    "ALT" to "고속도로주유소",
    "ETC" to "기타"
)

fun brandName(brandCode: String): String = BRAND_NAMES[brandCode] ?: brandCode
