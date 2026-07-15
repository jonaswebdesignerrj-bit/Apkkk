package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FunnelChart(
    stages: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxCount = stages.maxOfOrNull { it.second }?.toFloat() ?: 1f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stages.forEach { (stageName, count) ->
            val fillPercent = if (maxCount > 0) count / maxCount else 0f
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stageName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$count leads",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillPercent)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (stageName) {
                                    "Novos" -> Color(0xFF3B82F6) // Blue
                                    "Em Análise" -> Color(0xFFF59E0B) // Gold
                                    "Proposta Enviada" -> Color(0xFF8B5CF6) // Purple
                                    "Ganho" -> Color(0xFF10B981) // Green
                                    "Perdido" -> Color(0xFFEF4444) // Red
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceBarChart(
    data: List<Pair<String, Float>>,
    currencyFormat: Boolean = false,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.second }?.toFloat() ?: 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            val fillPercent = if (maxValue > 0) value / maxValue else 0f
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // Value text
                Text(
                    text = if (currencyFormat) "R$ ${Math.round(value/1000)}k" else Math.round(value).toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Bar
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(fillPercent * 0.8f) // Scale to fit text on top
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            BrushGradientForLabel(label)
                        )
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Label
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun BrushGradientForLabel(label: String): Color {
    return when (label) {
        "Serasa" -> Color(0xFF10B981) // Emerald
        "Boa Vista" -> Color(0xFF06B6D4) // Cyan
        "Itaú" -> Color(0xFFFF7A00) // Orange
        "Nubank" -> Color(0xFF8A05BE) // Purple
        else -> MaterialTheme.colorScheme.primary
    }
}
