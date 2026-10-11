package com.ldnfc.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldnfc.app.data.model.TagType
import com.ldnfc.app.ui.components.HexDumpView
import com.ldnfc.app.ui.viewmodel.MainViewModel

@Composable
fun ReadBackupScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.let { os ->
                    val ok = viewModel.exportLastDumpToFile(os)
                    if (ok) {
                        Toast.makeText(context, "Backup salvo com sucesso!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Falha ao gravar arquivo de backup.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.read_backup_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.read_backup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Card de status da leitura
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (uiState.isReading) androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.reading_data) else androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.ready_to_read),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (uiState.readStatusMessage.isNotEmpty())
                        uiState.readStatusMessage
                    else
                        androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.touch_antenna_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (uiState.isReading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = uiState.readProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Se houver dump lido com sucesso
        uiState.lastReadDump?.let { dump ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.item_identification),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.lastReadItemName ?: androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.item_not_identified),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            com.ldnfc.app.R.string.tag_info_format,
                            uiState.lastReadTagType?.displayName ?: "NTAG213",
                            dump.size,
                            dump.size / 4
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val defaultName = "${uiState.lastReadItemName?.replace(" ", "_") ?: "LegoTag"}.bin"
                            exportLauncher.launch(defaultName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.btn_export_dump))
                    }
                }
            }

            Text(
                text = androidx.compose.ui.res.stringResource(com.ldnfc.app.R.string.hex_memory_content),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )

            HexDumpView(
                dumpBytes = dump,
                tagType = uiState.lastReadTagType ?: TagType.NTAG213
            )
        }
    }
}
