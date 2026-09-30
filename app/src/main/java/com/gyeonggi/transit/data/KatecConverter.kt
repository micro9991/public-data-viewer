package com.gyeonggi.transit.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * WGS84 위경도를 오피넷이 사용하는 KATEC(TM128, Bessel 타원체) 좌표로 변환한다.
 * proj4: +proj=tmerc +lat_0=38 +lon_0=128 +k=0.9999 +x_0=400000 +y_0=600000
 *        +ellps=bessel +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43
 */
object KatecConverter {

    private const val A_WGS = 6378137.0
    private const val F_WGS = 1 / 298.257223563
    private val E2_WGS = 2 * F_WGS - F_WGS.pow(2)

    private const val A_BES = 6377397.155
    private const val F_BES = 1 / 299.1528128
    private val E2_BES = 2 * F_BES - F_BES.pow(2)

    private const val DX = -115.80
    private const val DY = 474.99
    private const val DZ = 674.11
    private const val RX_ARCSEC = 1.16
    private const val RY_ARCSEC = -2.31
    private const val RZ_ARCSEC = -1.63
    private const val DS_PPM = 6.43

    private fun geodeticToEcef(latRad: Double, lonRad: Double, a: Double, e2: Double): Triple<Double, Double, Double> {
        val n = a / sqrt(1 - e2 * sin(latRad).pow(2))
        val x = n * cos(latRad) * cos(lonRad)
        val y = n * cos(latRad) * sin(lonRad)
        val z = n * (1 - e2) * sin(latRad)
        return Triple(x, y, z)
    }

    private fun wgsEcefToBesselEcef(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        val rx = RX_ARCSEC * Math.PI / (180 * 3600)
        val ry = RY_ARCSEC * Math.PI / (180 * 3600)
        val rz = RZ_ARCSEC * Math.PI / (180 * 3600)
        val scale = 1 + DS_PPM * 1e-6
        val dx = x - DX
        val dy = y - DY
        val dz = z - DZ
        val xl = (dx + rz * dy - ry * dz) / scale
        val yl = (-rz * dx + dy + rx * dz) / scale
        val zl = (ry * dx - rx * dy + dz) / scale
        return Triple(xl, yl, zl)
    }

    private fun ecefToGeodetic(x: Double, y: Double, z: Double, a: Double, e2: Double): Pair<Double, Double> {
        val p = sqrt(x * x + y * y)
        val lon = atan2(y, x)
        var lat = atan2(z, p * (1 - e2))
        repeat(10) {
            val n = a / sqrt(1 - e2 * sin(lat).pow(2))
            val h = p / cos(lat) - n
            lat = atan2(z, p * (1 - e2 * n / (n + h)))
        }
        return Pair(lat, lon)
    }

    private fun meridionalArc(lat: Double, a: Double, e2: Double): Double {
        return a * (
            (1 - e2 / 4 - 3 * e2.pow(2) / 64 - 5 * e2.pow(3) / 256) * lat
                - (3 * e2 / 8 + 3 * e2.pow(2) / 32 + 45 * e2.pow(3) / 1024) * sin(2 * lat)
                + (15 * e2.pow(2) / 256 + 45 * e2.pow(3) / 1024) * sin(4 * lat)
                - (35 * e2.pow(3) / 3072) * sin(6 * lat)
            )
    }

    private fun tmForward(
        lat: Double,
        lon: Double,
        lat0: Double,
        lon0: Double,
        k0: Double,
        x0: Double,
        y0: Double,
        a: Double,
        e2: Double
    ): Pair<Double, Double> {
        val ep2 = e2 / (1 - e2)
        val n = a / sqrt(1 - e2 * sin(lat).pow(2))
        val t = tan(lat).pow(2)
        val c = ep2 * cos(lat).pow(2)
        val aa = (lon - lon0) * cos(lat)

        val mLat = meridionalArc(lat, a, e2)
        val m0 = meridionalArc(lat0, a, e2)

        val x = x0 + k0 * n * (aa + (1 - t + c) * aa.pow(3) / 6 +
            (5 - 18 * t + t.pow(2) + 72 * c - 58 * ep2) * aa.pow(5) / 120)
        val y = y0 + k0 * (mLat - m0 + n * tan(lat) * (aa.pow(2) / 2 +
            (5 - t + 9 * c + 4 * c.pow(2)) * aa.pow(4) / 24 +
            (61 - 58 * t + t.pow(2) + 600 * c - 330 * ep2) * aa.pow(6) / 720))
        return Pair(x, y)
    }

    /** WGS84 위경도(degree) -> KATEC (x, y) in meters. */
    fun wgs84ToKatec(latDeg: Double, lonDeg: Double): Pair<Double, Double> {
        val latRad = Math.toRadians(latDeg)
        val lonRad = Math.toRadians(lonDeg)
        val (x, y, z) = geodeticToEcef(latRad, lonRad, A_WGS, E2_WGS)
        val (xb, yb, zb) = wgsEcefToBesselEcef(x, y, z)
        val (latB, lonB) = ecefToGeodetic(xb, yb, zb, A_BES, E2_BES)
        val lat0 = Math.toRadians(38.0)
        val lon0 = Math.toRadians(128.0)
        return tmForward(latB, lonB, lat0, lon0, 0.9999, 400000.0, 600000.0, A_BES, E2_BES)
    }

    private fun besselEcefToWgsEcef(xl: Double, yl: Double, zl: Double): Triple<Double, Double, Double> {
        val rx = RX_ARCSEC * Math.PI / (180 * 3600)
        val ry = RY_ARCSEC * Math.PI / (180 * 3600)
        val rz = RZ_ARCSEC * Math.PI / (180 * 3600)
        val scale = 1 + DS_PPM * 1e-6
        val x = DX + scale * (xl - rz * yl + ry * zl)
        val y = DY + scale * (rz * xl + yl - rx * zl)
        val z = DZ + scale * (-ry * xl + rx * yl + zl)
        return Triple(x, y, z)
    }

    private fun tmInverse(
        x: Double,
        y: Double,
        lat0: Double,
        lon0: Double,
        k0: Double,
        x0: Double,
        y0: Double,
        a: Double,
        e2: Double
    ): Pair<Double, Double> {
        val e1 = (1 - sqrt(1 - e2)) / (1 + sqrt(1 - e2))
        val m0 = meridionalArc(lat0, a, e2)
        val m = m0 + (y - y0) / k0
        val mu = m / (a * (1 - e2 / 4 - 3 * e2.pow(2) / 64 - 5 * e2.pow(3) / 256))

        val phi1 = mu + (3 * e1 / 2 - 27 * e1.pow(3) / 32) * sin(2 * mu) +
            (21 * e1.pow(2) / 16 - 55 * e1.pow(4) / 32) * sin(4 * mu) +
            (151 * e1.pow(3) / 96) * sin(6 * mu) +
            (1097 * e1.pow(4) / 512) * sin(8 * mu)

        val ep2 = e2 / (1 - e2)
        val c1 = ep2 * cos(phi1).pow(2)
        val t1 = tan(phi1).pow(2)
        val n1 = a / sqrt(1 - e2 * sin(phi1).pow(2))
        val r1 = a * (1 - e2) / (1 - e2 * sin(phi1).pow(2)).pow(1.5)
        val d = (x - x0) / (n1 * k0)

        val lat = phi1 - (n1 * tan(phi1) / r1) * (
            d.pow(2) / 2 - (5 + 3 * t1 + 10 * c1 - 4 * c1.pow(2) - 9 * ep2) * d.pow(4) / 24 +
                (61 + 90 * t1 + 298 * c1 + 45 * t1.pow(2) - 252 * ep2 - 3 * c1.pow(2)) * d.pow(6) / 720
            )
        val lon = lon0 + (
            d - (1 + 2 * t1 + c1) * d.pow(3) / 6 +
                (5 - 2 * c1 + 28 * t1 - 3 * c1.pow(2) + 8 * ep2 + 24 * t1.pow(2)) * d.pow(5) / 120
            ) / cos(phi1)
        return Pair(lat, lon)
    }

    /** KATEC (x, y) in meters -> WGS84 위경도(degree) as (lat, lon). */
    fun katecToWgs84(x: Double, y: Double): Pair<Double, Double> {
        val lat0 = Math.toRadians(38.0)
        val lon0 = Math.toRadians(128.0)
        val (latB, lonB) = tmInverse(x, y, lat0, lon0, 0.9999, 400000.0, 600000.0, A_BES, E2_BES)
        val (xb, yb, zb) = geodeticToEcef(latB, lonB, A_BES, E2_BES)
        val (xw, yw, zw) = besselEcefToWgsEcef(xb, yb, zb)
        val (lat, lon) = ecefToGeodetic(xw, yw, zw, A_WGS, E2_WGS)
        return Pair(Math.toDegrees(lat), Math.toDegrees(lon))
    }
}
