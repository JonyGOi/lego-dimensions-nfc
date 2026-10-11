package com.ldnfc.app.ui.screens

import com.ldnfc.app.R
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldnfc.app.data.model.DimensionsItem
import com.ldnfc.app.data.model.ItemCategory
import com.ldnfc.app.data.model.TagType
import com.ldnfc.app.data.model.WriteResult
import com.ldnfc.app.ui.components.ItemIcon
import com.ldnfc.app.ui.viewmodel.MainViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun WriteTagScreen(
    viewModel: MainViewModel,
    onBackToCatalog: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val item = uiState.activeItemToWrite

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(androidx.compose.ui.res.stringResource(R.string.no_item_selected))
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBackToCatalog) {
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_back_to_catalog))
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card do Item Selecionado
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemIcon(
                    itemId = item.id,
                    isVehicle = item.category != ItemCategory.CHARACTER,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "ID #${item.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.category.displayName} • ${item.world}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
        }

        // Se NFC estiver desativado no aparelho
        if (!uiState.isNfcEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.nfc_disabled_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.nfc_disabled_desc),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.btn_enable), fontSize = 12.sp)
                    }
                }
            }
        }

        // Seletor de Tipo de Tag (NTAG213 / NTAG215)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.target_tag_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = uiState.targetTagType == TagType.NTAG213,
                        onClick = { viewModel.setTargetTagType(TagType.NTAG213) },
                        label = { Text(androidx.compose.ui.res.stringResource(R.string.tag_ntag213_chip)) }
                    )
                    FilterChip(
                        selected = uiState.targetTagType == TagType.NTAG215,
                        onClick = { viewModel.setTargetTagType(TagType.NTAG215) },
                        label = { Text(androidx.compose.ui.res.stringResource(R.string.tag_ntag215_chip)) }
                    )
                }

                Text(
                    text = if (uiState.targetTagType == TagType.NTAG213)
                        androidx.compose.ui.res.stringResource(R.string.ntag213_desc)
                    else
                        androidx.compose.ui.res.stringResource(R.string.ntag215_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                // Opções Avançadas de Proteção e Magic Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.lock_tag_title), fontWeight = FontWeight.Medium)
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.lock_tag_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.enablePasswordProtection,
                        onCheckedChange = { viewModel.setPasswordProtectionEnabled(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.clone_uid_title), fontWeight = FontWeight.Medium)
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.clone_uid_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.cloneUidIfMagicTag,
                        onCheckedChange = { viewModel.setCloneUidIfMagicTag(it) }
                    )
                }
            }
        }

        // Card de Espera / Escrita NFC
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isWriting)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (uiState.isWriting)
                        androidx.compose.ui.res.stringResource(R.string.writing_in_progress)
                    else
                        androidx.compose.ui.res.stringResource(R.string.approach_nfc_tag),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (uiState.writeStatusMessage.isNotEmpty())
                        uiState.writeStatusMessage
                    else
                        androidx.compose.ui.res.stringResource(R.string.approach_nfc_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (uiState.isWriting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = uiState.writeProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Resultado da Gravação (Sucesso, Erro ou Divergência)
        uiState.lastWriteResult?.let { result ->
            when (result) {
                is WriteResult.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.write_success_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(androidx.compose.ui.res.stringResource(R.string.tag_uid_label, result.uidHex), fontSize = 13.sp, color = Color(0xFF2E7D32))
                            Text(androidx.compose.ui.res.stringResource(R.string.tag_type_label, result.tagType.displayName), fontSize = 13.sp, color = Color(0xFF2E7D32))
                            Text(androidx.compose.ui.res.stringResource(R.string.write_time_label, result.durationMs), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            if (result.isMagicTag) {
                                Text(androidx.compose.ui.res.stringResource(R.string.magic_tag_cloned), fontSize = 13.sp, color = Color(0xFF2E7D32))
                            }
                            if (result.executedCommands.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(androidx.compose.ui.res.stringResource(R.string.nfc_commands_sent), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                                result.executedCommands.forEach { cmd ->
                                    Text(
                                        text = cmd,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            Text(
                                androidx.compose.ui.res.stringResource(R.string.tag_ready_to_play),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                is WriteResult.PageMismatch -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.write_mismatch_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.write_mismatch_desc),
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )

                            result.divergentPages.forEach { (page, pair) ->
                                val expHex = pair.first.joinToString(" ") { "%02X".format(it) }
                                val readHex = pair.second.joinToString(" ") { "%02X".format(it) }
                                Text(
                                    text = "Pág $page: Esperado [$expHex] | Lido [$readHex]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }
                    }
                }
                is WriteResult.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.write_failed_title),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botão voltar
        OutlinedButton(
            onClick = onBackToCatalog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(androidx.compose.ui.res.stringResource(R.string.btn_back_to_catalog))
        }
    }
}
