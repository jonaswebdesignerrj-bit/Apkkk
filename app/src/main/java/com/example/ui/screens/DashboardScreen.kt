package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.components.FunnelChart
import com.example.ui.components.PerformanceBarChart
import com.example.utils.CpfCnpjUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onNavigateToQuery: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToKanban: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clients by viewModel.clients.collectAsState()
    val queries by viewModel.queries.collectAsState()
    val proposals by viewModel.proposals.collectAsState()
    val email by viewModel.currentUserEmail.collectAsState()

    // Calculate metrics
    val totalLeads = clients.size
    val totalQueries = queries.size
    val totalProposals = proposals.size
    val totalPreApprovedVolume = proposals.sumOf { it.value }
    val wonDeals = clients.filter { it.status == "Ganho" }.size
    val conversionPercent = if (totalLeads > 0) (wonDeals.toFloat() / totalLeads.toFloat() * 100f) else 0f

    // Funnel data
    val funnelStages = listOf(
        Pair("Novos", clients.filter { it.status == "Novo" }.size),
        Pair("Em Análise", clients.filter { it.status == "Em Análise" }.size),
        Pair("Proposta Enviada", clients.filter { it.status == "Proposta Enviada" }.size),
        Pair("Ganho", wonDeals),
        Pair("Perdido", clients.filter { it.status == "Perdido" }.size)
    )

    // Bar chart data (Real-time database counts)
    val barChartData = listOf(
        Pair("Serasa (Alta)", queries.filter { it.serasaScore >= 700 }.size.toFloat()),
        Pair("Boa Vista (Alta)", queries.filter { it.boaVistaScore >= 700 }.size.toFloat()),
        Pair("Itaú (K R$)", proposals.filter { it.bankName == "Itaú" }.sumOf { it.value }.toFloat() / 1000f),
        Pair("Nubank (K R$)", proposals.filter { it.bankName == "Nubank" }.sumOf { it.value }.toFloat() / 1000f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BancoCorretor CRM", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Olá, $email", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Análise de Crédito Inteligente",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Consulte CPFs, gere propostas comerciais em PDF personalizadas instantaneamente e controle seu pipeline de negócios em um só lugar.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToQuery,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Query")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nova Consulta de CPF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // KPIs Grid
            item {
                Text(
                    text = "Indicadores Principais (KPIs)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Meus Leads",
                        value = totalLeads.toString(),
                        subtitle = "Cadastrados no CRM",
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Consultas",
                        value = totalQueries.toString(),
                        subtitle = "Bureaus de Crédito",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Crédito Emitido",
                        value = CpfCnpjUtils.formatCurrency(totalPreApprovedVolume),
                        subtitle = "$totalProposals Propostas",
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1.2f)
                    )
                    KpiCard(
                        title = "Conversão",
                        value = String.format("%.1f%%", conversionPercent),
                        subtitle = "$wonDeals Ganhos",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }

            // Funnel pipeline chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Funil do Pipeline de Leads",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FunnelChart(stages = funnelStages)
                    }
                }
            }

            // Bar chart statistics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Atividade de Volume e Bureaus (R$ / Score)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PerformanceBarChart(data = barChartData, currencyFormat = false)
                    }
                }
            }

            // Quick Shortcut buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToClients,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Lista")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Leads", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onNavigateToKanban,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ViewWeek, contentDescription = "Kanban")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Kanban", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
