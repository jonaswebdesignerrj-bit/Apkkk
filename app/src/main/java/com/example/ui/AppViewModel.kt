package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.CpfCnpjUtils
import com.example.utils.CreditSimulator
import com.example.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db)

    // SharedPreferences for real-time API configuration persistent state
    private val prefs = application.getSharedPreferences("api_settings", Context.MODE_PRIVATE)

    private val _apiMode = MutableStateFlow(prefs.getString("api_mode", "simulation") ?: "simulation")
    val apiMode: StateFlow<String> = _apiMode.asStateFlow()

    private val _apiEndpoint = MutableStateFlow(prefs.getString("api_endpoint", "") ?: "")
    val apiEndpoint: StateFlow<String> = _apiEndpoint.asStateFlow()

    private val _apiToken = MutableStateFlow(prefs.getString("api_token", "") ?: "")
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _customHeader = MutableStateFlow(prefs.getString("custom_header", "Authorization") ?: "Authorization")
    val customHeader: StateFlow<String> = _customHeader.asStateFlow()

    // CRM Core State
    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queries: StateFlow<List<CpfQueryHistory>> = repository.allQueries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proposals: StateFlow<List<Proposal>> = repository.allProposals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authentication State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    // Query Screen State
    private val _cpfQueryInput = MutableStateFlow("")
    val cpfQueryInput: StateFlow<String> = _cpfQueryInput.asStateFlow()

    private val _cpfQueryNameInput = MutableStateFlow("")
    val cpfQueryNameInput: StateFlow<String> = _cpfQueryNameInput.asStateFlow()

    private val _hasLgpdConsent = MutableStateFlow(false)
    val hasLgpdConsent: StateFlow<Boolean> = _hasLgpdConsent.asStateFlow()

    private val _isQueryLoading = MutableStateFlow(false)
    val isQueryLoading: StateFlow<Boolean> = _isQueryLoading.asStateFlow()

    private val _queryResult = MutableStateFlow<CreditSimulator.SimulationResult?>(null)
    val queryResult: StateFlow<CreditSimulator.SimulationResult?> = _queryResult.asStateFlow()

    private val _queryError = MutableStateFlow<String?>(null)
    val queryError: StateFlow<String?> = _queryError.asStateFlow()

    init {
        // Zero fictitious data rule - No prepopulateMockData call on init, starts completely clean!
    }

    // Settings actions
    fun updateApiSettings(mode: String, endpoint: String, token: String, header: String) {
        prefs.edit().apply {
            putString("api_mode", mode)
            putString("api_endpoint", endpoint)
            putString("api_token", token)
            putString("custom_header", header)
            apply()
        }
        _apiMode.value = mode
        _apiEndpoint.value = endpoint
        _apiToken.value = token
        _customHeader.value = header
    }

    // Auth actions
    fun login(email: String) {
        _currentUserEmail.value = email
        _isLoggedIn.value = true
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUserEmail.value = ""
    }

    // CPF actions
    fun onCpfInputChanged(input: String) {
        val clean = input.filter { it.isDigit() }
        if (clean.length <= 11) {
            _cpfQueryInput.value = CpfCnpjUtils.formatCpf(clean)
        }
    }

    fun onCpfNameInputChanged(name: String) {
        _cpfQueryNameInput.value = name
    }

    fun onConsentChanged(consent: Boolean) {
        _hasLgpdConsent.value = consent
    }

    fun executeCpfQuery() {
        val rawCpf = _cpfQueryInput.value.filter { it.isDigit() }
        val name = _cpfQueryNameInput.value.trim()

        if (name.isEmpty()) {
            _queryError.value = "Insira o nome completo do cliente."
            return
        }

        if (!CpfCnpjUtils.validateCpf(rawCpf)) {
            _queryError.value = "CPF inválido. Por favor, verifique o dígito verificador."
            return
        }

        if (!_hasLgpdConsent.value) {
            _queryError.value = "É necessário obter o consentimento LGPD do cliente."
            return
        }

        _queryError.value = null
        _isQueryLoading.value = true

        viewModelScope.launch {
            try {
                val result = if (_apiMode.value == "real") {
                    if (_apiEndpoint.value.isBlank()) {
                        throw Exception("URL da API não configurada. Por favor, acesse o painel de configurações (ícone de engrenagem) e configure o endpoint.")
                    }
                    withContext(Dispatchers.IO) {
                        CreditSimulator.queryRealApi(
                            cpf = rawCpf,
                            fallbackName = name,
                            endpoint = _apiEndpoint.value,
                            token = _apiToken.value,
                            headerKey = _customHeader.value
                        )
                    }
                } else {
                    // Simulado, porém de acordo com regras de negócio rígidas (apenas bancos elegíveis!)
                    kotlinx.coroutines.delay(1000)
                    CreditSimulator.simulateQuery(rawCpf)
                }

                _queryResult.value = result
                _isQueryLoading.value = false

                // Automatically save CPF query to database history
                val historyItem = CpfQueryHistory(
                    cpf = rawCpf,
                    clientName = name,
                    serasaScore = result.serasaScore,
                    boaVistaScore = result.boaVistaScore,
                    estimatedIncome = result.estimatedIncome,
                    debtCapacity = result.debtCapacity,
                    restrictions = result.restrictions.joinToString(", ")
                )
                repository.insertCpfQuery(historyItem)

                // Also check if client already exists in our lead list. If not, suggest or add as a Lead!
                val exists = repository.allClients.first().any { it.cpfCnpj.filter { d -> d.isDigit() } == rawCpf }
                if (!exists) {
                    repository.insertClient(
                        Client(
                            name = name,
                            type = "PF",
                            cpfCnpj = CpfCnpjUtils.formatCpf(rawCpf),
                            email = "${name.lowercase().replace(" ", "")}@exemplo.com.br",
                            phone = "(11) 9" + (80000000..99999999).random(),
                            income = result.estimatedIncome,
                            householdSize = (1..5).random(),
                            status = "Novo",
                            notes = "Lead gerado automaticamente via Consulta de Bureau ${if (_apiMode.value == "real") "Real" else "Serasa"}"
                        )
                    )
                }
            } catch (e: Exception) {
                _queryError.value = e.localizedMessage ?: "Erro desconhecido ao processar consulta."
                _isQueryLoading.value = false
            }
        }
    }

    fun clearQueryResult() {
        _queryResult.value = null
        _cpfQueryInput.value = ""
        _cpfQueryNameInput.value = ""
        _hasLgpdConsent.value = false
        _queryError.value = null
    }

    // Client Management Actions
    fun addClient(
        name: String,
        type: String,
        cpfCnpj: String,
        email: String,
        phone: String,
        income: Double,
        householdSize: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertClient(
                Client(
                    name = name,
                    type = type,
                    cpfCnpj = cpfCnpj,
                    email = email,
                    phone = phone,
                    income = income,
                    householdSize = householdSize,
                    notes = notes,
                    status = "Novo"
                )
            )
        }
    }

    fun updateClientStatus(client: Client, newStatus: String) {
        viewModelScope.launch {
            repository.updateClient(client.copy(status = newStatus))
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    // Proposal Actions
    fun createProposal(client: Client, offer: CreditSimulator.BankOffer) {
        viewModelScope.launch {
            // Save Proposal
            val proposal = Proposal(
                clientId = client.id,
                clientName = client.name,
                bankName = offer.bankName,
                product = offer.product,
                value = offer.maxValue,
                interestRate = offer.interestRate,
                term = offer.termMonths,
                installment = offer.installmentValue,
                status = "Enviada"
            )
            repository.insertProposal(proposal)

            // Auto-advance client pipeline state to "Proposta Enviada"
            repository.updateClient(client.copy(status = "Proposta Enviada"))
        }
    }

    fun createProposalFromCpfResult(clientName: String, cpf: String, offer: CreditSimulator.BankOffer) {
        viewModelScope.launch {
            // Find client ID if exists
            val clientList = repository.allClients.first()
            val existingClient = clientList.find { it.cpfCnpj.filter { d -> d.isDigit() } == cpf.filter { d -> d.isDigit() } }
            
            val clientId = existingClient?.id ?: 0
            val proposal = Proposal(
                clientId = clientId,
                clientName = clientName,
                bankName = offer.bankName,
                product = offer.product,
                value = offer.maxValue,
                interestRate = offer.interestRate,
                term = offer.termMonths,
                installment = offer.installmentValue,
                status = "Enviada"
            )
            repository.insertProposal(proposal)

            if (existingClient != null) {
                repository.updateClient(existingClient.copy(status = "Proposta Enviada"))
            }
        }
    }

    fun updateProposalStatus(proposal: Proposal, newStatus: String) {
        viewModelScope.launch {
            repository.updateProposal(proposal.copy(status = newStatus))
            
            // If proposal is won, update client status
            if (newStatus == "Aprovada") {
                val client = repository.getClientById(proposal.clientId)
                if (client != null) {
                    repository.updateClient(client.copy(status = "Ganho"))
                }
            } else if (newStatus == "Recusada") {
                val client = repository.getClientById(proposal.clientId)
                if (client != null) {
                    repository.updateClient(client.copy(status = "Perdido"))
                }
            }
        }
    }

    fun deleteQueryHistory(id: Int) {
        viewModelScope.launch {
            repository.deleteQueryById(id)
        }
    }

    // Export PDF and share
    fun shareProposalPdf(context: Context, clientName: String, cpf: String, offer: CreditSimulator.BankOffer) {
        val file = PdfGenerator.generateProposalPdf(context, clientName, cpf, offer)
        if (file != null) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Proposta de Crédito - $clientName")
                putExtra(Intent.EXTRA_TEXT, "Prezado $clientName, segue anexa a proposta de crédito pré-aprovada do banco ${offer.bankName}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Proposta PDF"))
        }
    }

    // Prepopulate realistic data for demonstration
    private suspend fun prepopulateMockData() {
        // Zero fictitious data rule - empty to keep the database completely clean
    }
}
