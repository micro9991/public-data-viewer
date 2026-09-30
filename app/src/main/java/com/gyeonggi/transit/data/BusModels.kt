package com.gyeonggi.transit.data

data class BusRoute(
    val routeId: String,
    val routeName: String,
    val startStationName: String,
    val endStationName: String,
    val regionName: String
)

data class BusRouteStation(
    val stationId: String,
    val stationName: String,
    val staOrder: Int
)

data class BusArrivalItem(
    val routeName: String,
    val predictTime1: Int?,
    val predictTime2: Int?,
    val locationNo1: Int?,
    val locationNo2: Int?,
    val plateNo1: String?,
    val plateNo2: String?,
    val remainSeatCnt1: Int?,
    val remainSeatCnt2: Int?
)
