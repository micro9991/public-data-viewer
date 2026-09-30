package com.gyeonggi.transit.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gyeonggi.transit.data.KatecConverter
import com.gyeonggi.transit.data.NaverMapNavigator
import com.gyeonggi.transit.data.NearbyStation
import com.gyeonggi.transit.data.brandName

private const val GASOLINE_CODE = "B027"
private const val DIESEL_CODE = "D047"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowestFuelPriceScreen(onBack: () -> Unit, viewModel: LowestFuelPriceViewModel = viewModel()) {
    val state = viewModel.state
    val context = LocalContext.current
    var pendingProduct by remember { mutableStateOf<String?>(null) }
    var selectedStationId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            pendingProduct?.let { viewModel.selectProduct(it, context) }
        }
        pendingProduct = null
    }

    fun requestProduct(productCode: String) {
        selectedStationId = null
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.selectProduct(productCode, context)
        } else {
            pendingProduct = productCode
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("유가 최저가") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ProductButtonRow(
                selectedProduct = state.selectedProduct,
                onSelectGasoline = { requestProduct(GASOLINE_CODE) },
                onSelectDiesel = { requestProduct(DIESEL_CODE) }
            )

            if (state.isLoading) {
                FuelInlineLoading()
            }
            if (state.error != null) {
                FuelSectionMessage(state.error)
            }

            if (state.selectedProduct != null && !state.isLoading) {
                FuelSectionHeader(
                    if (state.selectedProduct == GASOLINE_CODE) "휘발유 최저가 (반경 3km)" else "경유 최저가 (반경 3km)"
                )

                val userLat = state.userLat
                val userLon = state.userLon
                val points = state.stations.map { station ->
                    val (lat, lon) = KatecConverter.katecToWgs84(station.gisX, station.gisY)
                    StationMapPoint(station, lat, lon)
                }
                if (userLat != null && userLon != null && points.isNotEmpty()) {
                    StationMapView(
                        userLat = userLat,
                        userLon = userLon,
                        points = points,
                        selectedUniId = selectedStationId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.stations) { station ->
                        NearbyStationRow(
                            station = station,
                            selected = station.uniId == selectedStationId,
                            onClick = { selectedStationId = station.uniId }
                        )
                        HorizontalDivider()
                    }
                }

                if (points.isNotEmpty()) {
                    val target = points.firstOrNull { it.station.uniId == selectedStationId } ?: points.first()
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = {
                                NaverMapNavigator.navigateByCar(context, target.lat, target.lon, target.station.name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.width(16.dp).height(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${target.station.name}(으)로 길찾기")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductButtonRow(
    selectedProduct: String?,
    onSelectGasoline: () -> Unit,
    onSelectDiesel: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Button(
            onClick = onSelectGasoline,
            modifier = Modifier.weight(1f),
            colors = if (selectedProduct == GASOLINE_CODE) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        ) {
            Text("휘발유 최저가")
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onSelectDiesel,
            modifier = Modifier.weight(1f),
            colors = if (selectedProduct == DIESEL_CODE) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        ) {
            Text("경유 최저가")
        }
    }
}

@Composable
private fun NearbyStationRow(station: NearbyStation, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(station.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${brandName(station.brandCode)} · ${formatPrice(station.distanceMeters)}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${formatPrice(station.price.toDouble())}원",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
