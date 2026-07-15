package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.components.ScoreGauge
import com.example.utils.CpfCnpjUtils
import com.example.utils.CreditSimulator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditQueryScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val cpfInput by viewModel.cpfQueryInput.collectAsState()
    val nameInput by viewModel.cpfQueryNameInput.collectAsState()
    val hasConsent by viewModel.hasLgpdConsent.collectAsState()
    val isLoading by viewModel.isQueryLoading.collectAsState()
    val queryResult by viewModel.queryResult.collectAsState()
    val queryError by viewModel.queryError.collectAsState()

    // API settings states
    val apiMode by viewModel.apiMode.collectAsState()
    val apiEndpoint by viewModel.apiEndpoint.collectAsState()
    val apiToken by viewModel.apiToken.collectAsState()
    val customHeader by viewModel.customHeader.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consulta de Crédito Serasa + Boa Vista", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações de API",
                            tint = if (apiMode == "real") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (queryResult != null) {
                        IconButton(onClick = { viewModel.clearQueryResult() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Nova Consulta")
                        }
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
            if (queryResult == null) {
                // Consultation Input Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Dados da Consulta Bureau",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Client Name
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { viewModel.onCpfNameInputChanged(it) },
                                label = { Text("Nome Completo do Cliente") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Cliente") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_name_input")
                            )

                            // CPF Input
                            OutlinedTextField(
                                value = cpfInput,
                                onValueChange = { viewModel.onCpfInputChanged(it) },
                                label = { Text("CPF do Cliente") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "CPF") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cpf_input")
                            )

                            // LGPD consent Checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = hasConsent,
                                    onCheckedChange = { viewModel.onConsentChanged(it) },
                                    modifier = Modifier.testTag("lgpd_consent_checkbox")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "O cliente autoriza expressamente a consulta de seus dados cadastrais e score nos bureaus Serasa e Boa Vista, em conformidade com as diretrizes da LGPD (Lei nº 13.709/18).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    lineHeight = 15.sp
                                )
                            }

                            if (queryError != null) {
                                Text(
                                    text = queryError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = { viewModel.executeCpfQuery() },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("query_serasa_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Consultar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (apiMode == "real") "Consultar API Real CPF" else "Consultar Serasa + Boa Vista",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Bureau technical info card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (apiMode == "real") {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (apiMode == "real") Icons.Default.Sync else Icons.Default.Info,
                                    contentDescription = "Informação",
                                    tint = if (apiMode == "real") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (apiMode == "real") "Modo de Integração Real Ativo" else "Modo Simulado Ativo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (apiMode == "real") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (apiMode == "real") {
                                    "O aplicativo está configurado para fazer chamadas diretas de rede para o endpoint:\n$apiEndpoint\n\nTodos os dados recebidos (Nome, Score, Renda e Restrições) serão reais. O CRM aplicará automaticamente nossa matriz de crédito bancário para exibir unicamente as ofertas que aquele perfil específico tem chance de obter."
                                } else {
                                    "Este aplicativo simula de forma inteligente os bureaus Serasa e Boa Vista SCPC de acordo com as regras oficiais de crédito. CPFs terminados em números pares resultam em scores excelentes e melhores taxas, enquanto CPFs ímpares simulam scores médios e restrições secundárias comuns.\n\nPara alternar para uma consulta real via API externa (Serasa, Receita Federal ou Gateway próprio), clique no ícone de engrenagem no canto superior direito."
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Results displays
                val result = queryResult!!
                
                // Gauges Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Resultados dos Scores do Cliente",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ScoreGauge(
                                    score = result.serasaScore,
                                    label = "Serasa Experian",
                                    modifier = Modifier.weight(1f)
                                )
                                ScoreGauge(
                                    score = result.boaVistaScore,
                                    label = "Boa Vista SCPC",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Financial Capacities Section
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
                                text = "Perfil Cadastral e Capacidades",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Renda Estimada", fontSize = 11.sp, color = Color.Gray)
                                    Text(CpfCnpjUtils.formatCurrency(result.estimatedIncome), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Capacidade Endividamento", fontSize = 11.sp, color = Color.Gray)
                                    Text(CpfCnpjUtils.formatCurrency(result.debtCapacity), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Status cadastral
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Situação Cadastral Receita:", fontSize = 12.sp, color = Color.Gray)
                                Text(result.statusCadastral, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (result.statusCadastral == "Regular") Color(0xFF10B981) else Color(0xFFF59E0B))
                            }

                            // Restrictions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (result.restrictions.contains("Nenhuma")) Color(0xFFEFF6FF) else Color(0xFFFEF2F2)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (result.restrictions.contains("Nenhuma")) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "Alerta",
                                        tint = if (result.restrictions.contains("Nenhuma")) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Restrições Encontradas:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.restrictions.contains("Nenhuma")) Color(0xFF1E40AF) else Color(0xFF991B1B)
                                    )
                                }
                                Text(
                                    text = result.restrictions.joinToString(", "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (result.restrictions.contains("Nenhuma")) Color(0xFF1E3A8A) else Color(0xFF7F1D1D),
                                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Offers Header
                item {
                    Text(
                        text = "Ofertas Pré-Aprovadas de Bancos",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Pre-approved Offers List
                if (result.offers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma oferta pré-aprovada gerada para este score.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(result.offers) { offer ->
                        OfferCard(
                            clientName = nameInput,
                            cpf = cpfInput,
                            offer = offer,
                            onGeneratePdf = {
                                viewModel.createProposalFromCpfResult(nameInput, cpfInput, offer)
                                viewModel.shareProposalPdf(context, nameInput, cpfInput, offer)
                                Toast.makeText(context, "Proposta comercial gerada com sucesso!", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        var tempMode by remember { mutableStateOf(apiMode) }
        var tempEndpoint by remember { mutableStateOf(apiEndpoint) }
        var tempToken by remember { mutableStateOf(apiToken) }
        var tempHeader by remember { mutableStateOf(customHeader) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configurações da API de Crédito", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Configure como as consultas de CPF serão realizadas no aplicativo.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Selection Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { tempMode = "simulation" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tempMode == "simulation") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tempMode == "simulation") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Simulação Sec.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { tempMode = "real" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tempMode == "real") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tempMode == "real") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("API Real Externa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (tempMode == "real") {
                        // Endpoint Field
                        OutlinedTextField(
                            value = tempEndpoint,
                            onValueChange = { tempEndpoint = it },
                            label = { Text("URL / Endpoint da API") },
                            placeholder = { Text("https://api.gateway.com/cpf/{cpf}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        Text(
                            "Dica: Utilize {cpf} na URL para injetar dinamicamente os números limpos do CPF consultado.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )

                        // Token Field
                        OutlinedTextField(
                            value = tempToken,
                            onValueChange = { tempToken = it },
                            label = { Text("Chave de Acesso / Token") },
                            placeholder = { Text("Ex: Bearer abc123xyz") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )

                        // Custom Header Field
                        OutlinedTextField(
                            value = tempHeader,
                            onValueChange = { tempHeader = it },
                            label = { Text("Header de Autorização") },
                            placeholder = { Text("Authorization") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "No modo simulação, o CRM analisa o dígito verificador e gera scores realistas variando entre 450 e 950 com base em regras estritas de concessão de crédito, sem depender de rede.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApiSettings(tempMode, tempEndpoint, tempToken, tempHeader)
                        showSettingsDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar Configurações", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancelar", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun OfferCard(
    clientName: String,
    cpf: String,
    offer: CreditSimulator.BankOffer,
    onGeneratePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Bank and Probability Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Simulated color circle for bank style
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (offer.bankName) {
                                    "Itaú" -> Color(0xFFFF7A00)
                                    "Nubank" -> Color(0xFF8A05BE)
                                    "Bradesco" -> Color(0xFFDC2626)
                                    "Santander" -> Color(0xFFEC1C24)
                                    "Caixa" -> Color(0xFF005CA5)
                                    "BV" -> Color(0xFF003884)
                                    "C6 Bank" -> Color(0xFF000000)
                                    "Banco Pan" -> Color(0xFF00A2E8)
                                    "Inter" -> Color(0xFFFF7A00)
                                    "Original" -> Color(0xFF008000)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = offer.bankName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = offer.bankName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = offer.product,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Probability Tag
                val (probBg, probText, probLabel) = when (offer.approvalProbability) {
                    "Muito Alta" -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Aprovação Excelente")
                    "Alta" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "Aprovação Alta")
                    else -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Aprovação Média")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(probBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = probLabel,
                        color = probText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Details: Max Value, rate, installments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Crédito Pré-Aprovado", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = CpfCnpjUtils.formatCurrency(offer.maxValue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Juros Estimado", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${offer.interestRate}% a.m.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Parcelas", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${offer.termMonths}x de ${CpfCnpjUtils.formatCurrency(offer.installmentValue)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onGeneratePdf,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("generate_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gerar Proposta PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
