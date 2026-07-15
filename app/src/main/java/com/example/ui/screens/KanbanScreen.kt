package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.ui.AppViewModel
import com.example.utils.CpfCnpjUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    
    val stages = listOf("Novo", "Em Análise", "Proposta Enviada", "Ganho", "Perdido")
    var selectedStageIndex by remember { mutableStateOf(0) }
    val currentStage = stages[selectedStageIndex]

    val stageClients = remember(clients, currentStage) {
        clients.filter { it.status == currentStage }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pipeline Kanban de Vendas", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stage Switcher Tab-Row (makes it extremely mobile-friendly to navigate lanes!)
            ScrollableTabRow(
                selectedTabIndex = selectedStageIndex,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                stages.forEachIndexed { index, stage ->
                    val count = clients.count { it.status == stage }
                    Tab(
                        selected = selectedStageIndex == index,
                        onClick = { selectedStageIndex = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selectedStageIndex == index) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        count.toString(),
                                        color = if (selectedStageIndex == index) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // List of Lead Cards in current selected Kanban stage
            if (stageClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum lead nesta etapa: $currentStage",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stageClients) { client ->
                        KanbanCard(
                            client = client,
                            onMoveForward = {
                                val nextStage = when (client.status) {
                                    "Novo" -> "Em Análise"
                                    "Em Análise" -> "Proposta Enviada"
                                    "Proposta Enviada" -> "Ganho"
                                    else -> "Ganho"
                                }
                                viewModel.updateClientStatus(client, nextStage)
                                Toast.makeText(context, "Lead avançou para $nextStage", Toast.LENGTH_SHORT).show()
                            },
                            onMoveBackward = {
                                val prevStage = when (client.status) {
                                    "Em Análise" -> "Novo"
                                    "Proposta Enviada" -> "Em Análise"
                                    "Ganho" -> "Proposta Enviada"
                                    else -> "Novo"
                                }
                                viewModel.updateClientStatus(client, prevStage)
                                Toast.makeText(context, "Lead recuou para $prevStage", Toast.LENGTH_SHORT).show()
                            },
                            onMarkLost = {
                                viewModel.updateClientStatus(client, "Perdido")
                                Toast.makeText(context, "Lead marcado como Perdido", Toast.LENGTH_SHORT).show()
                            },
                            onMarkWon = {
                                viewModel.updateClientStatus(client, "Ganho")
                                Toast.makeText(context, "Lead fechado com Sucesso!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanCard(
    client: Client,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    onMarkLost: () -> Unit,
    onMarkWon: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (client.type == "PF") Color(0xFFEFF6FF) else Color(0xFFFEF2F2))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(client.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (client.type == "PF") Color(0xFF1E40AF) else Color(0xFF991B1B))
                }
            }

            Text(
                text = "Telefone: ${client.phone}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = "Renda Estimada: ${CpfCnpjUtils.formatCurrency(client.income)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (client.notes.isNotEmpty()) {
                Text(
                    text = "Anotação: ${client.notes}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Action Buttons to drag/move card between columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backward
                IconButton(
                    onClick = onMoveBackward,
                    enabled = client.status != "Novo" && client.status != "Perdido",
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Recuar", tint = if (client.status != "Novo" && client.status != "Perdido") MaterialTheme.colorScheme.primary else Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (client.status != "Perdido" && client.status != "Ganho") {
                        OutlinedButton(
                            onClick = onMarkLost,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Perdido", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Perdido", fontSize = 10.sp)
                        }
                    }
                    if (client.status == "Proposta Enviada") {
                        Button(
                            onClick = onMarkWon,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Ganho", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ganho", fontSize = 10.sp)
                        }
                    }
                }

                // Forward
                IconButton(
                    onClick = onMoveForward,
                    enabled = client.status != "Ganho" && client.status != "Perdido",
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Avançar", tint = if (client.status != "Ganho" && client.status != "Perdido") MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }
    }
}
