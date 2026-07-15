package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object CpfCnpjUtils {

    fun validateCpf(cpf: String): Boolean {
        val cleanCpf = cpf.filter { it.isDigit() }
        if (cleanCpf.length != 11) return false

        // Check for known invalid CPFs (all digits identical)
        if (cleanCpf.all { it == cleanCpf[0] }) return false

        try {
            val d1 = calculateCpfDigit(cleanCpf.substring(0, 9), weightCpf1)
            val d2 = calculateCpfDigit(cleanCpf.substring(0, 9) + d1, weightCpf2)
            return cleanCpf.endsWith("$d1$d2")
        } catch (e: Exception) {
            return false
        }
    }

    private val weightCpf1 = intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2)
    private val weightCpf2 = intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2)

    private fun calculateCpfDigit(str: String, weight: IntArray): Int {
        var sum = 0
        for (i in str.indices) {
            sum += str[i].toString().toInt() * weight[i]
        }
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    fun formatCpf(cpf: String): String {
        val clean = cpf.filter { it.isDigit() }
        if (clean.length <= 3) return clean
        if (clean.length <= 6) return "${clean.substring(0, 3)}.${clean.substring(3)}"
        if (clean.length <= 9) return "${clean.substring(0, 3)}.${clean.substring(3, 6)}.${clean.substring(6)}"
        val end = if (clean.length > 11) 11 else clean.length
        return "${clean.substring(0, 3)}.${clean.substring(3, 6)}.${clean.substring(6, 9)}-${clean.substring(9, end)}"
    }

    fun formatCnpj(cnpj: String): String {
        val clean = cnpj.filter { it.isDigit() }
        if (clean.length <= 2) return clean
        if (clean.length <= 5) return "${clean.substring(0, 2)}.${clean.substring(2)}"
        if (clean.length <= 8) return "${clean.substring(0, 2)}.${clean.substring(2, 5)}.${clean.substring(5)}"
        if (clean.length <= 12) return "${clean.substring(0, 2)}.${clean.substring(2, 5)}.${clean.substring(5, 8)}/${clean.substring(8)}"
        val end = if (clean.length > 14) 14 else clean.length
        return "${clean.substring(0, 2)}.${clean.substring(2, 5)}.${clean.substring(5, 8)}/${clean.substring(8, 12)}-${clean.substring(12, end)}"
    }

    fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }

    fun formatPercentage(value: Double): String {
        return String.format(Locale("pt", "BR"), "%.2f%%", value)
    }
}
