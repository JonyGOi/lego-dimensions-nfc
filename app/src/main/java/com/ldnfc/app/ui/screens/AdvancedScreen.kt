package com.ldnfc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldnfc.app.data.model.DimensionsItem
import com.ldnfc.app.data.model.ItemCategory
import com.ldnfc.app.ui.viewmodel.MainViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AdvancedScreen(
    viewModel: MainViewModel,
    onNavigateToWrite: (DimensionsItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val resolvedItem = uiState.advancedResolvedItem

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.advanced_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.advanced_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Seletor de Categoria
        Text(text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.item_type_label), fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = uiState.advancedCategory == ItemCategory.CHARACTER,
                onClick = { viewModel.setAdvancedCategory(ItemCategory.CHARACTER) },
                label = { Text(androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.type_character)) }
            )
            FilterChip(
                selected = uiState.advancedCategory == ItemCategory.VEHICLE,
                onClick = { viewModel.setAdvancedCategory(ItemCategory.VEHICLE) },
                label = { Text(androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.type_vehicle)) }
            )
        }

        // Campo de entrada do ID numérico
        OutlinedTextField(
            value = uiState.advancedCustomId,
            onValueChange = { viewModel.setAdvancedCustomId(it.filter { ch -> ch.isDigit() }) },
            label = { Text(androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.numeric_id_label)) },
            placeholder = {
                Text(
                    if (uiState.advancedCategory == ItemCategory.CHARACTER)
                        androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.placeholder_char_id)
                    else
                        androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.placeholder_vehicle_id)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Preview do item resolvido
        if (resolvedItem != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.item_identified),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resolvedItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.item_id_world_format, resolvedItem.id, resolvedItem.world),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    resolvedItem.rebuild?.let {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.vehicle_form_format, it.displayName),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.setItemToWrite(resolvedItem)
                            onNavigateToWrite(resolvedItem)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.btn_write_custom_id))
                    }
                }
            }
        } else if (uiState.advancedCustomId.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.unlisted_id_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
