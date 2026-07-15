package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "PF" or "PJ"
    val cpfCnpj: String,
    val email: String,
    val phone: String,
    val income: Double,
    val householdSize: Int,
    val notes: String = "",
    val status: String = "Novo", // "Novo", "Em Análise", "Proposta Enviada", "Ganho", "Perdido"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cpf_query_history")
data class CpfQueryHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cpf: String,
    val clientName: String,
    val serasaScore: Int,
    val boaVistaScore: Int,
    val estimatedIncome: Double,
    val debtCapacity: Double,
    val restrictions: String, // comma-separated restrictions, e.g. "Nenhuma", "Ação Judicial", "Cheque sem Fundo"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "proposals")
data class Proposal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val clientName: String,
    val bankName: String,
    val product: String,
    val value: Double,
    val interestRate: Double,
    val term: Int,
    val installment: Double,
    val status: String = "Pendente", // "Pendente", "Enviada", "Aprovada", "Recusada"
    val brokerName: String = "Corretor Principal",
    val timestamp: Long = System.currentTimeMillis()
)
