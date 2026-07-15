package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateProposalPdf(
        context: Context,
        clientName: String,
        cpf: String,
        offer: CreditSimulator.BankOffer,
        brokerName: String = "BancoCorretor Assessoria"
    ): File? {
        val pdfDocument = PdfDocument()
        
        // A4 page size: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()
        val textPaint = Paint()
        val headerPaint = Paint()

        // 1. Draw Background & Layout Accents
        // Header dark slate block
        headerPaint.color = Color.parseColor("#1E293B") // Slate-800
        canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

        // Accent line under header
        paint.color = Color.parseColor("#3B82F6") // Blue-500
        canvas.drawRect(0f, 110f, 595f, 115f, paint)

        // 2. Draw Header Text
        titlePaint.color = Color.WHITE
        titlePaint.textSize = 22f
        titlePaint.isFakeBoldText = true
        titlePaint.isAntiAlias = true
        canvas.drawText("BANCO CORRETOR CRM", 40f, 50f, titlePaint)

        textPaint.color = Color.parseColor("#94A3B8") // Slate-400
        textPaint.textSize = 11f
        textPaint.isAntiAlias = true
        canvas.drawText("Proposta Comercial de Crédito Pré-Aprovado", 40f, 75f, textPaint)

        // Date
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val dateString = sdf.format(Date())
        textPaint.color = Color.WHITE
        textPaint.textSize = 9f
        canvas.drawText("Data de Emissão: $dateString", 420f, 50f, textPaint)
        canvas.drawText("ID Proposta: PRO-${(100000..999999).random()}", 420f, 70f, textPaint)

        // Body Text setups
        val labelPaint = Paint().apply {
            color = Color.parseColor("#475569") // Slate-600
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#0F172A") // Slate-900
            textSize = 12f
            isAntiAlias = true
        }

        var yPosition = 160f

        // 3. Client Section Box
        paint.color = Color.parseColor("#F1F5F9") // Slate-100
        canvas.drawRect(35f, yPosition - 20f, 560f, yPosition + 70f, paint)
        
        paint.color = Color.parseColor("#CBD5E1") // Slate-300
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(35f, yPosition - 20f, 560f, yPosition + 70f, paint)
        paint.style = Paint.Style.FILL // restore

        // Section Title
        val secTitlePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Dark Blue
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("DADOS DO CLIENTE", 45f, yPosition, secTitlePaint)
        yPosition += 25f

        canvas.drawText("Nome Completo:", 45f, yPosition, labelPaint)
        canvas.drawText(clientName, 170f, yPosition, valuePaint)
        yPosition += 20f

        canvas.drawText("Documento (CPF):", 45f, yPosition, labelPaint)
        canvas.drawText(CpfCnpjUtils.formatCpf(cpf), 170f, yPosition, valuePaint)
        
        yPosition += 60f

        // 4. Offer Section Box
        paint.color = Color.parseColor("#EFF6FF") // Blue-50
        canvas.drawRect(35f, yPosition - 20f, 560f, yPosition + 170f, paint)
        
        paint.color = Color.parseColor("#BFDBFE") // Blue-200
        paint.style = Paint.Style.STROKE
        canvas.drawRect(35f, yPosition - 20f, 560f, yPosition + 170f, paint)
        paint.style = Paint.Style.FILL

        canvas.drawText("DETALHES DA OFERTA DE CRÉDITO", 45f, yPosition, secTitlePaint)
        yPosition += 30f

        canvas.drawText("Instituição Financeira:", 45f, yPosition, labelPaint)
        val bankNamePaint = Paint(valuePaint).apply { isFakeBoldText = true; color = Color.parseColor("#1D4ED8") }
        canvas.drawText(offer.bankName, 210f, yPosition, bankNamePaint)
        yPosition += 22f

        canvas.drawText("Produto/Linha:", 45f, yPosition, labelPaint)
        canvas.drawText(offer.product, 210f, yPosition, valuePaint)
        yPosition += 22f

        canvas.drawText("Valor Máximo Disponível:", 45f, yPosition, labelPaint)
        val currencyPaint = Paint(valuePaint).apply { isFakeBoldText = true; color = Color.parseColor("#15803D"); textSize = 13f }
        canvas.drawText(CpfCnpjUtils.formatCurrency(offer.maxValue), 210f, yPosition, currencyPaint)
        yPosition += 22f

        canvas.drawText("Taxa de Juros Estimada:", 45f, yPosition, labelPaint)
        canvas.drawText("${offer.interestRate}% a.m.", 210f, yPosition, valuePaint)
        yPosition += 22f

        canvas.drawText("Prazo de Pagamento:", 45f, yPosition, labelPaint)
        canvas.drawText("${offer.termMonths} meses", 210f, yPosition, valuePaint)
        yPosition += 22f

        canvas.drawText("Valor Estimado da Parcela:", 45f, yPosition, labelPaint)
        val installmentPaint = Paint(valuePaint).apply { isFakeBoldText = true; textSize = 13f }
        canvas.drawText(CpfCnpjUtils.formatCurrency(offer.installmentValue), 210f, yPosition, installmentPaint)
        
        yPosition += 160f

        // 5. Broker Contact Box
        paint.color = Color.parseColor("#F8FAFC") // Slate-50
        canvas.drawRect(35f, yPosition - 20f, 560f, yPosition + 80f, paint)
        
        paint.color = Color.parseColor("#E2E8F0") // Slate-200
        paint.style = Paint.Style.STROKE
        canvas.drawRect(35f, yPosition - 20f, 560f, yPosition + 80f, paint)
        paint.style = Paint.Style.FILL

        canvas.drawText("CONTATO DO CORRETOR ASSESSOR", 45f, yPosition, secTitlePaint)
        yPosition += 25f

        canvas.drawText("Responsável:", 45f, yPosition, labelPaint)
        canvas.drawText(brokerName, 170f, yPosition, valuePaint)
        yPosition += 20f

        canvas.drawText("Canais de Atendimento:", 45f, yPosition, labelPaint)
        canvas.drawText("(11) 99888-7766  |  suporte@bancocorretor.com.br", 170f, yPosition, valuePaint)
        yPosition += 20f

        canvas.drawText("Endereço:", 45f, yPosition, labelPaint)
        canvas.drawText("Av. Paulista, 1000 - Bela Vista, São Paulo - SP", 170f, yPosition, valuePaint)

        yPosition += 140f

        // 6. Signature Lines and Warning
        val warningPaint = Paint().apply {
            color = Color.parseColor("#64748B") // Slate-500
            textSize = 9f
            isAntiAlias = true
        }
        val warningText = "Aviso legal: Esta simulação de crédito não constitui uma promessa ou contrato definitivo de financiamento. Os valores, taxas de juros e condições apresentadas são pré-aprovados e estão sujeitos à análise final de risco, política de crédito das instituições financeiras e conformidade com as diretrizes da LGPD (Lei Geral de Proteção de Dados)."
        
        // Draw wrapped warning text
        drawTextWrapped(canvas, warningText, 35f, yPosition, 520, warningPaint)

        yPosition += 70f

        // Signature lines
        paint.color = Color.parseColor("#94A3B8") // Line
        paint.strokeWidth = 1f
        canvas.drawLine(50f, yPosition, 250f, yPosition, paint)
        canvas.drawLine(340f, yPosition, 540f, yPosition, paint)

        val sigPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 10f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Assinatura do Corretor", 150f, yPosition + 15f, sigPaint)
        canvas.drawText("Assinatura do Cliente", 440f, yPosition + 15f, sigPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache file
        return try {
            val file = File(context.cacheDir, "Proposta_Credito_${cpf.filter { it.isDigit() }}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawTextWrapped(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Int, paint: Paint) {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y
        val bounds = Rect()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            paint.getTextBounds(testLine, 0, testLine.length, bounds)
            if (bounds.width() > maxWidth) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentLine = word
                currentY += paint.textSize + 4f
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }
}
