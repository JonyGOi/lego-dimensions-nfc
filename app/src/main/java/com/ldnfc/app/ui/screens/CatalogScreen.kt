package com.ldnfc.app.ui.screens

import com.ldnfc.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldnfc.app.data.model.DimensionsItem
import com.ldnfc.app.data.model.ItemCategory
import com.ldnfc.app.data.model.VehicleRebuild
import com.ldnfc.app.ui.components.HexDumpView
import com.ldnfc.app.ui.components.ItemIcon
import com.ldnfc.app.ui.viewmodel.MainViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun CatalogScreen(
    viewModel: MainViewModel,
    onNavigateToWrite: (DimensionsItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var previewItem by remember { mutableStateOf<DimensionsItem?>(null) }
    var vehicleLevelFilter by remember { mutableStateOf<VehicleRebuild?>(null) }

    val itemsToDisplay = remember(uiState.catalogItems, uiState.selectedCategory, vehicleLevelFilter) {
        if (uiState.selectedCategory == ItemCategory.VEHICLE && vehicleLevelFilter != null) {
            uiState.catalogItems.filter { it.rebuild == vehicleLevelFilter }
        } else {
            uiState.catalogItems
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de Busca
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(androidx.compose.ui.res.stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Abas: Personagens, Veículos, Gadgets
        val categories = listOf(
            ItemCategory.CHARACTER to androidx.compose.ui.res.stringResource(R.string.category_characters),
            ItemCategory.VEHICLE to androidx.compose.ui.res.stringResource(R.string.category_vehicles),
            ItemCategory.GADGET to androidx.compose.ui.res.stringResource(R.string.category_gadgets)
        )
        val selectedIndex = categories.indexOfFirst { it.first == uiState.selectedCategory }.coerceAtLeast(0)

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, (category, title) ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = {
                        vehicleLevelFilter = null
                        viewModel.selectCategory(category)
                    },
                    text = {
                        val count = if (category == uiState.selectedCategory) " (${itemsToDisplay.size})" else ""
                        Text(text = "$title$count", fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal)
                    }
                )
            }
        }

        // Filtro de nível para veículos (Nível 1, 2, 3)
        if (uiState.selectedCategory == ItemCategory.VEHICLE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = vehicleLevelFilter == null,
                    onClick = { vehicleLevelFilter = null },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = vehicleLevelFilter == VehicleRebuild.DEFAULT,
                    onClick = { vehicleLevelFilter = VehicleRebuild.DEFAULT },
                    label = { Text("Nível 1") }
                )
                FilterChip(
                    selected = vehicleLevelFilter == VehicleRebuild.REBUILD_TWO,
                    onClick = { vehicleLevelFilter = VehicleRebuild.REBUILD_TWO },
                    label = { Text("Nível 2") }
                )
                FilterChip(
                    selected = vehicleLevelFilter == VehicleRebuild.REBUILD_THREE,
                    onClick = { vehicleLevelFilter = VehicleRebuild.REBUILD_THREE },
                    label = { Text("Nível 3") }
                )
            }
        }

        // Lista de Itens do Catálogo
        if (uiState.isLoadingCatalog) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (itemsToDisplay.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.no_items_found, uiState.searchQuery),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(itemsToDisplay, key = { "${it.category.name}_${it.id}_${it.name}" }) { item ->
                    CatalogItemCard(
                        item = item,
                        onViewHex = { previewItem = item },
                        onWrite = {
                            viewModel.setItemToWrite(item)
                            onNavigateToWrite(item)
                        }
                    )
                }
            }
        }
    }

    // Modal de Pré-Visualização do Dump
    previewItem?.let { item ->
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ItemIcon(
                        itemId = item.id,
                        isVehicle = item.category != ItemCategory.CHARACTER,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${item.category.displayName} • ID #${item.id} • ${item.world}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        item.rebuild?.let { rb ->
                            Text(
                                text = rb.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.dump_preview_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    item.rawDump?.let { dump ->
                        HexDumpView(dumpBytes = dump)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toWrite = previewItem
                        previewItem = null
                        if (toWrite != null) {
                            viewModel.setItemToWrite(toWrite)
                            onNavigateToWrite(toWrite)
                        }
                    }
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_write_this_tag))
                }
            },
            dismissButton = {
                TextButton(onClick = { previewItem = null }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_close))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogItemCard(
    item: DimensionsItem,
    onViewHex: () -> Unit,
    onWrite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemIcon(
                    itemId = item.id,
                    isVehicle = item.category != ItemCategory.CHARACTER,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ID #${item.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.world,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    item.rebuild?.let { rb ->
                        Text(
                            text = rb.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Habilidades / Poderes
            if (item.abilities.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.abilities.take(6).forEach { ability ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(ability, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = null
                        )
                    }
                    if (item.abilities.size > 6) {
                        Text(
                            text = "+${item.abilities.size - 6}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically).padding(start = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onViewHex,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_view_dump))
                }

                Button(
                    onClick = onWrite,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_write))
                }
            }
        }
    }
}
