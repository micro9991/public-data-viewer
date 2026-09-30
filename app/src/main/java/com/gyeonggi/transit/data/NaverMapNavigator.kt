package com.gyeonggi.transit.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private const val NAVER_MAP_PACKAGE = "com.nhn.android.nmap"

object NaverMapNavigator {

    /** 네이버 지도 앱으로 목적지까지 자동차 길찾기를 실행한다. 미설치 시 스토어로 이동한다. */
    fun navigateByCar(context: Context, lat: Double, lon: Double, destinationName: String) {
        val uri = Uri.parse(
            "nmap://route/car?dlat=$lat&dlng=$lon&dname=${Uri.encode(destinationName)}&appname=${context.packageName}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(NAVER_MAP_PACKAGE)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openStore(context)
        }
    }

    private fun openStore(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$NAVER_MAP_PACKAGE"))
            )
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$NAVER_MAP_PACKAGE")
                )
            )
        }
    }
}
