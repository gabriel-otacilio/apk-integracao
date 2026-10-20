package com.example.fluxointegracaoo
import org.json.JSONException
import org.json.JSONObject

fun interpretarResultadoPos7(json: String): ResultTransacao {
    return try {
        val o = JSONObject(json)

        // Doc: "APPROVED (1) ou DENIED (0)": aceita número ou texto
        val result = o.opt("result")?.toString().orEmpty()
        val aprovada = result.equals("APPROVED", ignoreCase = true) || result == "1"

        val erros = mutableListOf<String>()
        o.optJSONArray("mError")?.let { arr ->
            for (i in 0 until arr.length()) erros.add(arr.optString(i))
        }

        ResultTransacao(
            aprovada = aprovada,
            transactionId = o.optString("transactionID"),
            authorizationCode = o.optString("authorizationCode"),
            mensagem = o.optString("displayMessage"),
            jsonOriginal = json,
            merchantId = o.optString("merchantID"),
            produto = o.optString("product"),
            valor = o.optString("amount"),
            parcelas = o.optString("installments").toIntOrNull() ?: 0,
            horario = o.optString("time"),
            bandeira = o.optString("brand"),
            cartaoMascarado = o.optString("maskedPan"),
            codigoResposta = o.optString("responseCode"),
            pixId = o.optString("pixId"),
            pixEndToEndId = o.optString("PixEnd2EndId"),   // P maiúsculo, como na doc
            erros = erros,
        )
    } catch (e: JSONException) {
        // Resposta ilegível NUNCA vira aprovada
        ResultTransacao(
            aprovada = false,
            mensagem = "Resposta inválida do POS7",
            jsonOriginal = json,
        )
    }
}