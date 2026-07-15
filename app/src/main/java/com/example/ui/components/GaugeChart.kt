package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreGauge(
    score: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val maxScore = 1000f
    val animatedScorePercent by animateFloatAsState(
        targetValue = score / maxScore,
        animationSpec = tween(durationMillis = 1000),
        label = "ScoreAnimation"
    )

    // Determine color based on score value
    val gaugeColor = when {
        score >= 750 -> Color(0xFF10B981) // Emerald Green (Excelente)
        score >= 600 -> Color(0xFFF59E0B) // Amber/Gold (Bom)
        score >= 450 -> Color(0xFFEF4444) // Red (Médio/Baixo)
        else -> Color(0xFFDC2626) // Deep Red
    }

    val scoreStatus = when {
        score >= 750 -> "Excelente"
        score >= 600 -> "Bom"
        score >= 450 -> "Regular"
        else -> "Risco Alto"
    }

    Box(
        modifier = modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background track arc (grey)
                    drawArc(
                        color = Color(0xFFE2E8F0),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active filled arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFEF4444), // Red
                                Color(0xFFF59E0B), // Gold
                                Color(0xFF10B981)  // Emerald
                            )
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedScorePercent,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner content displaying the score value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = score.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = scoreStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = gaugeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
