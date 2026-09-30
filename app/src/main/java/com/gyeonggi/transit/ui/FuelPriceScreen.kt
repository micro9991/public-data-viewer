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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gyeonggi.transit.data.FuelPrice
import com.gyeonggi.transit.data.PRODUCT_DISPLAY_ORDER
import com.gyeonggi.transit.data.SidoAvgPrice
import com.gyeonggi.transit.data.SigunAvgPrice
import com.gyeonggi.transit.data.productName
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelPriceScreen(onBack: () -> Unit, viewModel: FuelPriceViewModel = viewModel()) {
    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("유가정보") },
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
            if (state.isLoadingSido) {
                item { FuelInlineLoading() }
            }
            if (state.sidoError != null) {
                item { FuelSectionMessage(state.sidoError) }
            }

            if (state.sidoList.isNotEmpty()) {
                val selectedSido = state.selectedSido
                if (selectedSido == null) {
                    item { FuelSectionHeader("시/도를 선택하세요") }
                    items(state.sidoList) { sido ->
                        SidoRow(sido = sido, selected = false, onClick = { viewModel.selectSido(sido) })
                        HorizontalDivider()
                    }
                } else {
                    item { FuelSectionHeader("시/도") }
                    item {
                        SidoRow(sido = selectedSido, selected = true, onClick = { viewModel.clearSidoSelection() })
                        HorizontalDivider()
                    }
                }
            }

            if (state.isLoadingSigun) {
                item { FuelInlineLoading() }
            }
            if (state.sigunError != null) {
                item { FuelSectionMessage(state.sigunError) }
            }

            if (state.sigunList.isNotEmpty()) {
                val selectedSigun = state.selectedSigun
                if (selectedSigun == null) {
                    item { FuelSectionHeader("시/군/구를 선택하세요") }
                    items(state.sigunList) { sigun ->
                        SigunRow(sigun = sigun, selected = false, onClick = { viewModel.selectSigun(sigun) })
                        HorizontalDivider()
                    }
                } else {
                    item { FuelSectionHeader("시/군/구") }
                    item {
                        SigunRow(sigun = selectedSigun, selected = true, onClick = { viewModel.clearSigunSelection() })
                        HorizontalDivider()
                    }

                    item {
                        FuelSectionHeader("유가 정보")
                        FuelPriceCard(
                            title = "${state.selectedSido?.sidoName ?: ""} ${selectedSigun.sigunName}",
                            prices = selectedSigun.prices,
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
internal fun FuelInlineLoading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
internal fun FuelSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
internal fun FuelSectionMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

private fun gasolinePrice(prices: List<FuelPrice>): String {
    val gasoline = prices.firstOrNull { it.productCode == "B027" } ?: return ""
    return "휘발유 ${formatPrice(gasoline.price)}원"
}

internal fun formatPrice(price: Double): String = String.format(Locale.KOREA, "%,.0f", price)

@Composable
private fun SidoRow(sido: SidoAvgPrice, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sido.sidoName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                gasolinePrice(sido.prices),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SigunRow(sigun: SigunAvgPrice, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sigun.sigunName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                gasolinePrice(sigun.prices),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FuelPriceCard(
    title: String,
    prices: List<FuelPrice>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            val ordered = PRODUCT_DISPLAY_ORDER.mapNotNull { code ->
                prices.firstOrNull { it.productCode == code }
            }
            ordered.forEachIndexed { index, price ->
                FuelPriceRow(price)
                if (index != ordered.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("새로고침", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun FuelPriceRow(price: FuelPrice) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(productName(price.productCode), style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${formatPrice(price.price)}원",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            val diffColor = when {
                price.diff > 0 -> Color.Red
                price.diff < 0 -> Color(0xFF1E88E5)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val diffText = when {
                price.diff > 0 -> "▲${formatPrice(price.diff)}"
                price.diff < 0 -> "▼${formatPrice(-price.diff)}"
                else -> "-"
            }
            Text(diffText, style = MaterialTheme.typography.bodySmall, color = diffColor)
        }
    }
}
