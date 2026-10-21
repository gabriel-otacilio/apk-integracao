// é o modulo de pagamento. O resto do app conhece apenas o 'PaymentGateway'.
//Quando eu puder testar com o Sipag mudar essa interface

package com.example.fluxointegracaoo

import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID

interface PaymentGateway{
    val nome: String
    suspend fun pagar(
        pedido : PedidoPagamento,
        aoMudarEtapa: (String) -> Unit
    ): ResultTransacao

}

// função responsavel por pegar o json do PDF e montar o "ResultTransacao"
object ResultParser{
    fun parse( json:String ) : ResultTransacao{
        val obj= JSONObject(json)// transforma o JSON pra um objeto
        return ResultTransacao(
            aprovada = obj.optInt("result") == 1,// retorna um bool
            transactionId = obj.optString("transactionId"),
            authorizationCode = obj.optString("authorizationCode"),
            mensagem = obj.optString("mensagem"),
            jsonOriginal = obj.toString(2)

        )
    }
}

// essa é a função que vai imitar a resposta da maquininha

class SimulatedGateway: PagamentoGateWay{
    override val nome = "Simulação"
    override suspend fun cancelar(onEtapa: (String) -> Unit): ResultTransacao {
        onEtapa("Cancelando venda...")
        delay(1500)
        val json = """
    {
      "result": 1,
      "transactionID": "CANCEL-0001",
      "authorizationCode": "000000",
      "displayMessage": "VENDA CANCELADA",
      "responseCode": "00"
    }
    """.trimIndent()
        return interpretarResultadoPos7(json)
    }
    override suspend fun pagar (
        pedido: PedidoPagamento,
        onEtapa: (String) -> Unit,
    ): ResultTransacao{
        onEtapa("Solicitação enviada ao Pos07")
        delay(1000)
        onEtapa("Aguardando cartão")
        delay(1000)
        onEtapa("Autorizado")
        delay(1500)
        val aprovada = !pedido.valor.endsWith(",99") // R$ x,99 simula negada
        val json = """
{
  "result": 1,
  "transactionID": "BFC9552C-F33",
  "authorizationCode": "818915",
  "merchantID": "10022110001",
  "product": "CREDITO",
  "amount": "${pedido.valor}",
  "installments": ${pedido.parcelas},
  "time": "19/10/26 16:30:00",
  "brand": "MASTERCARD",
  "maskedPan": "**** **** **** 5000",
  "displayMessage": "TRANSACAO AUTORIZADA",
  "responseCode": "00"
}
""".trimIndent()
        return interpretarResultadoPos7(json)

        onEtapa("Resposta recebida (callback)")
        return ResultParser.parse(json)
}
}


