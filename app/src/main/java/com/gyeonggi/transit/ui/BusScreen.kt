package com.gyeonggi.transit.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gyeonggi.transit.data.BusArrivalItem
import com.gyeonggi.transit.data.BusRoute
import com.gyeonggi.transit.data.BusRouteStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusScreen(onBack: () -> Unit, viewModel: BusViewModel = viewModel()) {
    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("경기버스 도착정보") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SearchBar(
                    keyword = state.keyword,
                    onKeywordChange = viewModel::onKeywordChange,
                    onSearch = viewModel::searchRoutes,
                    isSearching = state.isSearching
                )
            }

            if (state.routeError != null) {
                item { SectionMessage(state.routeError) }
            }

            if (state.routes.isNotEmpty()) {
                val selectedRoute = state.selectedRoute
                if (selectedRoute == null) {
                    item { SectionHeader("'${state.searchedKeyword}' 검색 결과 (${state.routes.size}건)") }
                    items(state.routes) { route ->
                        RouteRow(
                            route = route,
                            selected = false,
                            onClick = { viewModel.selectRoute(route) }
                        )
                        HorizontalDivider()
                    }
                } else {
                    item { SectionHeader("노선") }
                    item {
                        RouteRow(
                            route = selectedRoute,
                            selected = true,
                            onClick = { viewModel.clearRouteSelection() }
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (state.isLoadingStations) {
                item { InlineLoading() }
            }

            if (state.stationError != null) {
                item { SectionMessage(state.stationError) }
            }

            if (state.stations.isNotEmpty()) {
                val selectedStation = state.selectedStation
                if (selectedStation == null) {
                    item { SectionHeader("${state.selectedRoute?.routeName ?: ""} 정류소 선택") }
                    items(state.stations) { station ->
                        StationRow(
                            station = station,
                            selected = false,
                            onClick = { viewModel.selectStation(station) }
                        )
                        HorizontalDivider()
                    }
                } else {
                    item { SectionHeader("정류소") }
                    item {
                        StationRow(
                            station = selectedStation,
                            selected = true,
                            onClick = { viewModel.clearStationSelection() }
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (state.arrivalError != null) {
                item { SectionMessage(state.arrivalError) }
            }

            if (state.arrival != null && state.selectedStation != null) {
                item {
                    SectionHeader("도착 정보")
                    ArrivalCard(
                        arrival = state.arrival,
                        station = state.selectedStation,
                        isRefreshing = state.isRefreshingArrival,
                        onRefresh = viewModel::refreshArrival
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("버스 번호를 입력하세요", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            label = { Text("예: 500-1, 3300") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onSearch()
            }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onSearch()
            },
            enabled = !isSearching,
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("검색", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SectionMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun InlineLoading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun RouteRow(route: BusRoute, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(route.routeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                "${route.startStationName} → ${route.endStationName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "선택됨",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StationRow(station: BusRouteStation, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(station.stationName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${station.staOrder}번째 정류소",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "선택됨",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ArrivalCard(
    arrival: BusArrivalItem,
    station: BusRouteStation,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(arrival.routeName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            InfoLine(label = "차량번호(첫번째)", value = arrival.plateNo1 ?: "정보 없음")
            Spacer(modifier = Modifier.height(2.dp))
            InfoLine(label = "차량번호(두번째)", value = arrival.plateNo2 ?: "정보 없음")
            Spacer(modifier = Modifier.height(10.dp))

            InfoLine(label = "정류소", value = "${station.stationName} (${station.staOrder}번째 정류소)")
            Spacer(modifier = Modifier.height(10.dp))

            ArrivalRow(
                label = "첫번째 도착 예정",
                minutes = arrival.predictTime1,
                stopsAway = arrival.locationNo1,
                remainSeat = arrival.remainSeatCnt1
            )
            Spacer(modifier = Modifier.height(4.dp))
            ArrivalRow(
                label = "두번째 도착 예정",
                minutes = arrival.predictTime2,
                stopsAway = arrival.locationNo2,
                remainSeat = arrival.remainSeatCnt2
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("새로고침", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "20초마다 자동으로 새로고침됩니다",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ArrivalRow(label: String, minutes: Int?, stopsAway: Int?, remainSeat: Int? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (minutes != null) {
                Text(
                    "${minutes}분 후${stopsAway?.let { " ($it 정류소 전)" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text("정보 없음", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (minutes != null && remainSeat != null) {
            Row {
                Text(
                    "잔여석 ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$remainSeat",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Red
                )
                Text(
                    "석",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
