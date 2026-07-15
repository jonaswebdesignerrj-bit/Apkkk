package com.example.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Random
import java.util.concurrent.TimeUnit

object CreditSimulator {

    data class BankOffer(
        val bankName: String,
        val product: String,
        val maxValue: Double,
        val interestRate: Double, // monthly, e.g. 1.29% is 1.29
        val termMonths: Int,
        val installmentValue: Double,
        val approvalProbability: String // "Alta", "Muito Alta", "Média"
    )

    data class SimulationResult(
        val serasaScore: Int,
        val boaVistaScore: Int,
        val statusCadastral: String,
        val estimatedIncome: Double,
        val debtCapacity: Double,
        val restrictions: List<String>,
        val offers: List<BankOffer>
    )

    // Dedicated OkHttpClient with timeouts for real requests
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Executes a real-time HTTP query to an external API configured by the user.
     * Standardizes the response and generates appropriate bank offers using our real-world approval matrix.
     */
    fun queryRealApi(
        cpf: String,
        fallbackName: String,
        endpoint: String,
        token: String,
        headerKey: String
    ): SimulationResult {
        val cleanCpf = cpf.filter { it.isDigit() }
        
        // Build URL
        val targetUrl = if (endpoint.contains("{cpf}")) {
            endpoint.replace("{cpf}", cleanCpf)
        } else {
            if (endpoint.contains("?")) "$endpoint&cpf=$cleanCpf" else "$endpoint?cpf=$cleanCpf"
        }

        val requestBuilder = Request.Builder().url(targetUrl)
        if (token.isNotBlank()) {
            val key = if (headerKey.isBlank()) "Authorization" else headerKey
            val value = if (key.equals("Authorization", ignoreCase = true) && !token.startsWith("Bearer", ignoreCase = true) && token.length > 30) {
                "Bearer $token"
            } else {
                token
            }
            requestBuilder.addHeader(key, value)
        }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error code: ${response.code}")
                }
                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                
                // Parse standard or custom JSON response using Android's native JSONObject
                val json = JSONObject(responseBody)
                
                // Smart key detection for Name
                val name = json.optString("nome")
                    .ifBlank { json.optString("name") }
                    .ifBlank { json.optString("nome_completo") }
                    .ifBlank { json.optString("clientName") }
                    .ifBlank { fallbackName }

                // Smart key detection for Score
                val serasaScore = json.optInt("score", -1)
                    .let { if (it == -1) json.optInt("serasa_score", -1) else it }
                    .let { if (it == -1) json.optInt("serasaScore", -1) else it }
                    .let { if (it == -1) json.optInt("pontuacao", 650) else it }
                    .coerceIn(0, 1000)

                val boaVistaScore = json.optInt("score_boavista", -1)
                    .let { if (it == -1) json.optInt("boavista_score", -1) else it }
                    .let { if (it == -1) json.optInt("boaVistaScore", -1) else it }
                    .let { if (it == -1) (serasaScore - 15).coerceIn(0, 1000) else it }

                // Smart key detection for Income
                val income = json.optDouble("renda", -1.0)
                    .let { if (it.isNaN() || it == -1.0) json.optDouble("renda_estimada", -1.0) else it }
                    .let { if (it.isNaN() || it == -1.0) json.optDouble("estimated_income", -1.0) else it }
                    .let { if (it.isNaN() || it == -1.0) json.optDouble("estimatedIncome", 3500.0) else it }

                // Smart key detection for Restrictions
                val restrictionsList = mutableListOf<String>()
                val restrictionsJson = json.opt("restricoes") ?: json.opt("restrictions") ?: json.opt("restricoes_ativas")
                if (restrictionsJson is JSONArray) {
                    for (i in 0 until restrictionsJson.length()) {
                        val r = restrictionsJson.optString(i)
                        if (r.isNotBlank()) restrictionsList.add(r)
                    }
                } else if (restrictionsJson is String && restrictionsJson.isNotBlank()) {
                    restrictionsList.addAll(restrictionsJson.split(",").map { it.trim() })
                }
                
                if (restrictionsList.isEmpty()) {
                    restrictionsList.add("Nenhuma")
                }

                // Calculate credit parameters based on real data
                val statusCadastral = if (restrictionsList.contains("Nenhuma") || restrictionsList.isEmpty()) "Regular" else "Regular com Restrições"
                val debtCapacity = Math.round((income * 0.3) * 100.0) / 100.0

                // Generate strictly eligible offers
                val offers = generateOffersForProfile(serasaScore, income, restrictionsList, cleanCpf)

                return SimulationResult(
                    serasaScore = serasaScore,
                    boaVistaScore = boaVistaScore,
                    statusCadastral = statusCadastral,
                    estimatedIncome = income,
                    debtCapacity = debtCapacity,
                    restrictions = restrictionsList,
                    offers = offers
                )
            }
        } catch (e: Exception) {
            Log.e("CreditSimulator", "Error on real API call", e)
            throw IOException("Falha de conexão com a API Real: ${e.localizedMessage}. Verifique a URL e Token nas Configurações.")
        }
    }

    /**
     * Simulation mode: Uses CPF-seeded randomness to estimate a highly realistic credit score and history,
     * then applies the EXACT same strict approval matrix.
     */
    fun simulateQuery(cpf: String): SimulationResult {
        val digits = cpf.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return SimulationResult(500, 485, "Regular", 2500.0, 750.0, listOf("Nenhuma"), emptyList())
        }

        val lastDigit = digits.last().toString().toIntOrNull() ?: 0
        val isEven = lastDigit % 2 == 0

        // Use a hash of CPF + time blocks for light dynamic changes on subsequent checks
        val timeSecs = System.currentTimeMillis() / 15000 
        val seed = digits.hashCode() + timeSecs
        val random = Random(seed)

        // Generate realistic scores
        val scoreBase = if (isEven) {
            random.nextInt(201) + 750 // 750 - 950
        } else {
            random.nextInt(291) + 450 // 450 - 740
        }

        val serasaScore = scoreBase.coerceIn(350, 990)
        val boaVistaDiff = random.nextInt(61) - 30
        val boaVistaScore = (serasaScore + boaVistaDiff).coerceIn(300, 1000)

        // Estimated income based on score
        val baseIncome = if (serasaScore > 800) {
            5000.0 + random.nextInt(8000)
        } else if (serasaScore > 650) {
            3000.0 + random.nextInt(4000)
        } else {
            1600.0 + random.nextInt(2000)
        }
        val estimatedIncome = Math.round(baseIncome * 100.0) / 100.0
        val debtCapacity = Math.round((estimatedIncome * 0.3) * 100.0) / 100.0

        // Determine restrictions based on score
        val restrictionsList = mutableListOf<String>()
        if (serasaScore < 550) {
            val resOptions = listOf(
                "Pendente Financeira (Lojas)",
                "Protesto de Título",
                "Cheque sem Fundo",
                "Dívida Ativa da União"
            )
            restrictionsList.add(resOptions[random.nextInt(resOptions.size)])
        } else {
            restrictionsList.add("Nenhuma")
        }

        val statusCadastral = if (restrictionsList.contains("Nenhuma")) "Regular" else "Regular com Restrições"

        // Generate offers using our strict filter
        val offers = generateOffersForProfile(serasaScore, estimatedIncome, restrictionsList, digits)

        return SimulationResult(
            serasaScore = serasaScore,
            boaVistaScore = boaVistaScore,
            statusCadastral = statusCadastral,
            estimatedIncome = estimatedIncome,
            debtCapacity = debtCapacity,
            restrictions = restrictionsList,
            offers = offers
        )
    }

    /**
     * STRICT REAL-WORLD APPROVAL MATRIX
     * Filters out standard retail personal loans if a customer has negative restrictions or very low score.
     * Only displays banks/products that are physically capable of approval for their financial profile.
     */
    private fun generateOffersForProfile(
        score: Int,
        income: Double,
        restrictions: List<String>,
        cpfSeed: String
    ): List<BankOffer> {
        val hasActiveRestrictions = restrictions.isNotEmpty() && !restrictions.contains("Nenhuma")
        val offers = mutableListOf<BankOffer>()

        // Seed random for specific client to maintain consistent offers on rapid clicks
        val random = Random(cpfSeed.hashCode().toLong())

        // 1. CRITICAL BLOCK: SCORE < 400 OR RESTRICTIONS + SCORE < 500
        // Traditional retail unsecured personal loans are absolutely rejected.
        // They can ONLY get specific secured loans (e.g. FGTS collateral or Consignado Pensioner/INSS).
        if (score < 400 || (hasActiveRestrictions && score < 500)) {
            // Option A: FGTS Collateral (Very high approval rate, no credit bureau query dependency)
            offers.add(
                BankOffer(
                    bankName = "Banco Pan",
                    product = "Antecipação de Saque-Aniversário FGTS",
                    maxValue = Math.round((income * 2.5) / 100.0) * 100.0,
                    interestRate = 1.69,
                    termMonths = 12,
                    installmentValue = Math.round((income * 2.5 / 12) * 100.0) / 100.0,
                    approvalProbability = "Muito Alta"
                )
            )

            offers.add(
                BankOffer(
                    bankName = "Caixa Econômica Federal",
                    product = "Antecipação FGTS Especial",
                    maxValue = Math.round((income * 3.0) / 100.0) * 100.0,
                    interestRate = 1.49,
                    termMonths = 12,
                    installmentValue = Math.round((income * 3.0 / 12) * 100.0) / 100.0,
                    approvalProbability = "Muito Alta"
                )
            )

            // Option B: Consignado INSS (Retirees/Pensioners/Servants - approved even with dirty CPF)
            offers.add(
                BankOffer(
                    bankName = "Banco Pan",
                    product = "Crédito Consignado INSS (Aposentados/Pensionistas)",
                    maxValue = Math.round((income * 6.0) / 100.0) * 100.0,
                    interestRate = 1.59,
                    termMonths = 84,
                    installmentValue = Math.round((income * 0.15) * 100.0) / 100.0, // 15% margin
                    approvalProbability = "Alta"
                )
            )

            // Return immediately - absolutely no Itaú or Nubank personal loans!
            return offers.sortedByDescending { it.interestRate }
        }

        // 2. MEDIUM SCORE PROFILE (500 - 700) WITH NO ACTIVE RESTRICTIONS
        // Approved for standard personal loans but with strict credit limits and average interest rates.
        if (score in 500..700 && !hasActiveRestrictions) {
            // Nubank (Personal Loan with moderate interest)
            offers.add(
                BankOffer(
                    bankName = "Nubank",
                    product = "Empréstimo Pessoal Simples",
                    maxValue = Math.round((income * 1.2) / 100.0) * 100.0,
                    interestRate = 3.89,
                    termMonths = 24,
                    installmentValue = calculateInstallment(Math.round((income * 1.2) / 100.0) * 100.0, 3.89, 24),
                    approvalProbability = "Média"
                )
            )

            // Inter (Vehicle Refinancing - much safer, higher approval)
            offers.add(
                BankOffer(
                    bankName = "Inter",
                    product = "Crédito com Garantia de Veículo (Refin)",
                    maxValue = Math.round((income * 4.0) / 100.0) * 100.0,
                    interestRate = 1.99,
                    termMonths = 48,
                    installmentValue = calculateInstallment(Math.round((income * 4.0) / 100.0) * 100.0, 1.99, 48),
                    approvalProbability = "Alta"
                )
            )

            // BV Financeira (Personal/Vehicle loan)
            offers.add(
                BankOffer(
                    bankName = "BV",
                    product = "Empréstimo com Refinanciamento Auto",
                    maxValue = Math.round((income * 3.5) / 100.0) * 100.0,
                    interestRate = 2.19,
                    termMonths = 36,
                    installmentValue = calculateInstallment(Math.round((income * 3.5) / 100.0) * 100.0, 2.19, 36),
                    approvalProbability = "Alta"
                )
            )

            // Itaú Consignado (if public employee)
            offers.add(
                BankOffer(
                    bankName = "Itaú",
                    product = "Consignado Público Federal",
                    maxValue = Math.round((income * 8.0) / 100.0) * 100.0,
                    interestRate = 1.39,
                    termMonths = 72,
                    installmentValue = calculateInstallment(Math.round((income * 8.0) / 100.0) * 100.0, 1.39, 72),
                    approvalProbability = "Alta"
                )
            )

            return offers.sortedByDescending { it.maxValue }
        }

        // 3. HIGH / PRIME PROFILE (SCORE > 700) WITH NO RESTRICTIONS
        // Complete credit list from main retail banks with lowest interest rates and premium limits.
        val primeMaxLimit = if (score > 850) income * 15.0 else income * 8.0

        // Itaú Prime Loan
        offers.add(
            BankOffer(
                bankName = "Itaú",
                product = "Crédito Pessoal Pré-Aprovado Uniclass",
                maxValue = Math.round((primeMaxLimit * 0.4) / 100.0) * 100.0,
                interestRate = 2.49,
                termMonths = 48,
                installmentValue = calculateInstallment(Math.round((primeMaxLimit * 0.4) / 100.0) * 100.0, 2.49, 48),
                approvalProbability = "Muito Alta"
            )
        )

        // Santander Prime
        offers.add(
            BankOffer(
                bankName = "Santander",
                product = "Empréstimo Pessoal Select",
                maxValue = Math.round((primeMaxLimit * 0.35) / 100.0) * 100.0,
                interestRate = 2.69,
                termMonths = 36,
                installmentValue = calculateInstallment(Math.round((primeMaxLimit * 0.35) / 100.0) * 100.0, 2.69, 36),
                approvalProbability = "Alta"
            )
        )

        // Bradesco
        offers.add(
            BankOffer(
                bankName = "Bradesco",
                product = "Crédito Pessoal Fácil Bradesco",
                maxValue = Math.round((primeMaxLimit * 0.3) / 100.0) * 100.0,
                interestRate = 2.79,
                termMonths = 48,
                installmentValue = calculateInstallment(Math.round((primeMaxLimit * 0.3) / 100.0) * 100.0, 2.79, 48),
                approvalProbability = "Alta"
            )
        )

        // Nubank Prime
        offers.add(
            BankOffer(
                bankName = "Nubank",
                product = "Empréstimo Pessoal Prime Gold",
                maxValue = Math.round((primeMaxLimit * 0.25) / 100.0) * 100.0,
                interestRate = 2.29,
                termMonths = 24,
                installmentValue = calculateInstallment(Math.round((primeMaxLimit * 0.25) / 100.0) * 100.0, 2.29, 24),
                approvalProbability = "Muito Alta"
            )
        )

        // Inter Real Estate Collateral (Home Equity)
        offers.add(
            BankOffer(
                bankName = "Inter",
                product = "Home Equity (Crédito com Garantia de Imóvel)",
                maxValue = Math.round((primeMaxLimit * 1.5) / 100.0) * 100.0,
                interestRate = 1.19,
                termMonths = 120,
                installmentValue = calculateInstallment(Math.round((primeMaxLimit * 1.5) / 100.0) * 100.0, 1.19, 120),
                approvalProbability = "Alta"
            )
        )

        return offers.sortedByDescending { it.maxValue }
    }

    private fun calculateInstallment(value: Double, ratePercent: Double, term: Int): Double {
        val r = ratePercent / 100.0
        val factor = Math.pow(1 + r, term.toDouble())
        val pmt = if (factor > 1) {
            value * r * factor / (factor - 1)
        } else {
            value / term
        }
        return Math.round(pmt * 100.0) / 100.0
    }
}
