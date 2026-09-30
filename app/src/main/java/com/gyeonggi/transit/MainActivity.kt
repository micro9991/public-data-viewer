package com.gyeonggi.transit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gyeonggi.transit.ui.BusScreen
import com.gyeonggi.transit.ui.FuelPriceScreen
import com.gyeonggi.transit.ui.HomeScreen
import com.gyeonggi.transit.ui.LowestFuelPriceScreen
import com.gyeonggi.transit.ui.SubwayScreen

private enum class AppScreen { HOME, BUS, SUBWAY, FUEL_PRICE, LOWEST_FUEL_PRICE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screen by remember { mutableStateOf(AppScreen.HOME) }

                    BackHandler(enabled = screen != AppScreen.HOME) {
                        screen = AppScreen.HOME
                    }

                    when (screen) {
                        AppScreen.HOME -> HomeScreen(
                            onSelectBus = { screen = AppScreen.BUS },
                            onSelectSubway = { screen = AppScreen.SUBWAY },
                            onSelectFuelPrice = { screen = AppScreen.FUEL_PRICE },
                            onSelectLowestFuelPrice = { screen = AppScreen.LOWEST_FUEL_PRICE }
                        )
                        AppScreen.BUS -> BusScreen(onBack = { screen = AppScreen.HOME })
                        AppScreen.SUBWAY -> SubwayScreen(onBack = { screen = AppScreen.HOME })
                        AppScreen.FUEL_PRICE -> FuelPriceScreen(onBack = { screen = AppScreen.HOME })
                        AppScreen.LOWEST_FUEL_PRICE -> LowestFuelPriceScreen(onBack = { screen = AppScreen.HOME })
                    }
                }
            }
        }
    }
}
