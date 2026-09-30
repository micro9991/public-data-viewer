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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gyeonggi.transit.data.SubwayArrival
import com.gyeonggi.transit.data.formatExpectedArrivalClock
import com.gyeonggi.transit.data.formatMinutesSeconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubwayScreen(onBack: () -> Unit, viewModel: SubwayViewModel = viewModel()) {
    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("지하철 도착정보") },
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
                SubwaySearchBar(
                    keyword = state.keyword,
                    onKeywordChange = viewModel::onKeywordChange,
                    onSearch = viewModel::searchStation,
                    isSearching = state.isSearching
                )
            }

            if (state.error != null) {
                item { SubwaySectionMessage(state.error) }
            }

            if (state.arrivals.isNotEmpty()) {
                val selected = state.selectedArrival
                if (selected == null) {
                    item { SubwaySectionHeader("'${state.searchedKeyword}' 도착정보 (${state.arrivals.size}건)") }
                    items(state.arrivals) { arrival ->
                        SubwayArrivalRow(
                            arrival = arrival,
                            selected = false,
                            onClick = { viewModel.selectArrival(arrival) }
                        )
                        HorizontalDivider()
                    }
                } else {
                    item { SubwaySectionHeader("선택된 열차") }
                    item {
                        SubwayArrivalRow(
                            arrival = selected,
                            selected = true,
                            onClick = { viewModel.clearSelection() }
                        )
                        HorizontalDivider()
                    }

                    if (state.refreshError != null) {
                        item { SubwaySectionMessage(state.refreshError) }
                    }

                    item {
                        SubwaySectionHeader("도착 정보")
                        SubwayArrivalCard(
                            arrival = selected,
                            isRefreshing = state.isRefreshing,
                            onRefresh = viewModel::refresh
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubwaySearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("지하철역 이름을 입력하세요", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            label = { Text("예: 강남, 판교") },
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
private fun SubwaySectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SubwaySectionMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SubwayArrivalRow(arrival: SubwayArrival, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${arrival.lineName} · ${arrival.updnLine}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                arrival.trainLineNm,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${formatMinutesSeconds(arrival.arrivalSeconds)} · ${arrival.arrivalMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
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
private fun SubwayArrivalCard(
    arrival: SubwayArrival,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("${arrival.lineName} · ${arrival.updnLine}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            SubwayInfoLine(label = "행선지", value = arrival.trainLineNm)
            Spacer(modifier = Modifier.height(4.dp))
            SubwayInfoLine(label = "종착역", value = arrival.destinationStation)
            Spacer(modifier = Modifier.height(4.dp))
            SubwayInfoLine(label = "현재 위치", value = arrival.currentOrNextStation)
            Spacer(modifier = Modifier.height(10.dp))

            SubwayInfoLine(
                label = "열차 번호",
                value = arrival.trainNo.takeIf { it.isNotBlank() } ?: "정보 없음"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("도착 예정 시각", style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatExpectedArrivalClock(arrival.recptnDt, arrival.arrivalSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SubwayInfoLine(
                label = "도착 남은 시간",
                value = arrival.arrivalMessage,
                valueColor = androidx.compose.ui.graphics.Color.Red
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
private fun SubwayInfoLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

