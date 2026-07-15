package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.ui.AppViewModel
import com.example.utils.CpfCnpjUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) {
            clients
        } else {
            clients.filter {
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.cpfCnpj.contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão de Leads e Clientes", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_lead_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Lead", tint = Color.White)
            }
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pesquisar por nome ou CPF/CNPJ") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Vazio",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum cliente ou lead encontrado.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients) { client ->
                        ClientCard(
                            client = client,
                            onDelete = {
                                viewModel.deleteClient(client)
                                Toast.makeText(context, "Lead excluído com sucesso.", Toast.LENGTH_SHORT).show()
                            },
                            onAdvanceStatus = {
                                val nextStatus = when (client.status) {
                                    "Novo" -> "Em Análise"
                                    "Em Análise" -> "Proposta Enviada"
                                    "Proposta Enviada" -> "Ganho"
                                    else -> "Ganho"
                                }
                                viewModel.updateClientStatus(client, nextStatus)
                            },
                            onCancelStatus = {
                                viewModel.updateClientStatus(client, "Perdido")
                            }
                        )
                    }
                }
            }
        }

        // Add Client Dialog
        if (showAddDialog) {
            AddClientDialog(
                onDismiss = { showAddDialog = false },
                onAddClient = { name, type, doc, email, phone, income, household, notes ->
                    viewModel.addClient(name, type, doc, email, phone, income, household, notes)
                    showAddDialog = false
                    Toast.makeText(context, "Lead cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun ClientCard(
    client: Client,
    onDelete: () -> Unit,
    onAdvanceStatus: () -> Unit,
    onCancelStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (client.type == "PF") Color(0xFFEFF6FF) else Color(0xFFFEF2F2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = client.type,
                                color = if (client.type == "PF") Color(0xFF1E40AF) else Color(0xFF991B1B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = client.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Doc: ${client.cpfCnpj}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Status Indicator Badge
                val (bg, text) = when (client.status) {
                    "Novo" -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
                    "Em Análise" -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
                    "Proposta Enviada" -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8))
                    "Ganho" -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
                    else -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = client.status,
                        color = text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = "Tel", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = client.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Renda", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = CpfCnpjUtils.formatCurrency(client.income), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir"
                    )
                }
            }

            // Expanded details block
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row {
                        Text("E-mail: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(client.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row {
                        Text("Membros do Lar (Householding): ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("${client.householdSize} pessoas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (client.notes.isNotEmpty()) {
                        Text("Notas de Assessoria:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(client.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (client.status != "Ganho" && client.status != "Perdido") {
                                OutlinedButton(
                                    onClick = onCancelStatus,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Perdido", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = onAdvanceStatus,
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = when (client.status) {
                                            "Novo" -> "Analisar"
                                            "Em Análise" -> "Enviar Proposta"
                                            else -> "Aprovar"
                                        },
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onAddClient: (String, String, String, String, String, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PF") } // "PF" or "PJ"
    var cpfCnpj by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var incomeStr by remember { mutableStateOf("") }
    var householdStr by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Novo Lead / Cliente", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tipo de Cliente:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = type == "PF", onClick = { type = "PF" })
                            Text("PF (CPF)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = type == "PJ", onClick = { type = "PJ" })
                            Text("PJ (CNPJ)", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo / Razão Social") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = cpfCnpj,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            cpfCnpj = if (type == "PF") CpfCnpjUtils.formatCpf(clean) else CpfCnpjUtils.formatCnpj(clean)
                        },
                        label = { Text(if (type == "PF") "CPF" else "CNPJ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = incomeStr,
                        onValueChange = { incomeStr = it },
                        label = { Text("Renda Estimada / Faturamento (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = householdStr,
                        onValueChange = { householdStr = it },
                        label = { Text("Composição Familiar (Membros do Lar)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas de Assessoria") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMsg != null) {
                    item {
                        Text(
                            text = errorMsg ?: "",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val incomeVal = incomeStr.toDoubleOrNull() ?: 0.0
                    val householdVal = householdStr.toIntOrNull() ?: 1
                    
                    if (name.trim().isEmpty() || cpfCnpj.trim().isEmpty()) {
                        errorMsg = "Nome e Documento são obrigatórios."
                    } else if (type == "PF" && !CpfCnpjUtils.validateCpf(cpfCnpj.filter { it.isDigit() })) {
                        errorMsg = "CPF inválido."
                    } else {
                        onAddClient(name, type, cpfCnpj, email, phone, incomeVal, householdVal, notes)
                    }
                }
            ) {
                Text("Salvar Lead")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
