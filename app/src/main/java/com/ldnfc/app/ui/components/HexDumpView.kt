package com.ldnfc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldnfc.app.data.model.TagType
import com.ldnfc.app.ui.theme.HexCcColor
import com.ldnfc.app.ui.theme.HexConfigColor
import com.ldnfc.app.ui.theme.HexDataColor
import com.ldnfc.app.ui.theme.HexUidColor
import com.ldnfc.app.ui.theme.HexVehicleFlagColor

/**
 * Componente que renderiza um dump de memória NTAG página por página (4 bytes por linha)
 * com destaque colorido por função (UID, CC, Dados, Config, Senha).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HexDumpView(
    dumpBytes: ByteArray,
    tagType: TagType = TagType.NTAG213,
    modifier: Modifier = Modifier
) {
    val totalPages = dumpBytes.size / 4
    val pages = (0 until totalPages).map { pageIdx ->
        dumpBytes.copyOfRange(pageIdx * 4, (pageIdx + 1) * 4)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Legenda das cores
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendChip("UID/Fabricante", HexUidColor)
                LegendChip("CC (Capacidade)", HexCcColor)
                LegendChip("Dados / ID", HexDataColor)
                LegendChip("Flag Veículo", HexVehicleFlagColor)
                LegendChip("Config/PWD", HexConfigColor)
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Cabeçalho da tabela
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pág",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(42.dp)
                )
                Text(
                    text = "Hex (4 Bytes)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Função",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(90.dp)
                )
            }

            Divider(modifier = Modifier.padding(bottom = 4.dp))

            // Lista das páginas
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
            ) {
                itemsIndexed(pages) { pageIdx, pageBytes ->
                    val (roleName, roleColor) = getPageRole(pageIdx, tagType)
                    val hexString = pageBytes.joinToString(" ") { "%02X".format(it) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                roleColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Número da página (Hex / Dec)
                        Text(
                            text = "%02d".format(pageIdx),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(42.dp)
                        )

                        // Bytes em Hex
                        Text(
                            text = hexString,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (roleColor != Color.Gray) roleColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        // Papel / Descrição
                        Text(
                            text = roleName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Default,
                            color = roleColor,
                            modifier = Modifier.width(90.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getPageRole(page: Int, tagType: TagType): Pair<String, Color> {
    return when (page) {
        in 0..2 -> Pair("UID / Lock", HexUidColor)
        3 -> Pair("CC (Tamanho)", HexCcColor)
        36, 37 -> Pair("ID do Item", HexDataColor)
        38 -> Pair("Flag Veículo", HexVehicleFlagColor)
        40 -> Pair("Dynamic Lock", Color.Gray)
        41, 42 -> Pair("CFG0 / CFG1", HexConfigColor)
        43 -> Pair("Senha (PWD)", HexConfigColor)
        44 -> Pair("PACK", HexConfigColor)
        130 -> Pair("Dynamic Lock", Color.Gray)
        131, 132 -> Pair("CFG0 / CFG1", HexConfigColor)
        133 -> Pair("Senha (PWD)", HexConfigColor)
        134 -> Pair("PACK", HexConfigColor)
        else -> Pair("Memória Usr", Color.Gray)
    }
}
