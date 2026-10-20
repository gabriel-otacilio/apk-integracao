// aqui guarda os dados do app como os produtos da mecânica,
// formas de pagamento e o resultado;


package com.example.fluxointegracaoo

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// declarações de 'objetos'
data class Produto(val  nome: String, val precoReais: Int, val nomeFuncionario: String)

data class PedidoPagamento(
    val forma: FormaPagamentos,
    val valor: String,
    val tipoCredito: TipoCredito?, // que pode ser vazia e começa sendo vazia;
    val parcelas:Int,
)

data class ResultTransacao(
    val aprovada: Boolean,
    val transactionId: String = "",
    val authorizationCode: String = "",
    val mensagem: String = "",
    val jsonOriginal: String = "",
    val merchantId: String = "",
    val produto: String = "",
    val valor: String = "",
    val parcelas: Int = 0,
    val horario: String = "",
    val bandeira: String = "",
    val cartaoMascarado: String = "",
    val codigoResposta: String = "",
    val pixId: String = "",
    val pixEndToEndId: String = "",
    val erros: List<String> = emptyList(),
)

interface PagamentoGateWay{


    val nome: String
    suspend fun pagar(pedido: PedidoPagamento, onEtapa:(String) -> Unit): ResultTransacao
}



val cardapio = listOf(
// crio uma lista de objetos

    Produto("lanternagem traseira", 500, "Duda Velha"),
    Produto("mecânica interna", 700,"Leo" ),
    Produto("Troca de óleo", 150,"Kauan" ),
    Produto("Pintura carro inteiro", 1500, "Duda Nova")
)


enum class FormaPagamentos(val rotulo: String){
    // estrutura pra valores fixos

    CREDITO("credito"),
    DEBITO("debito"),
    VOUCHER("voucher"),
    PIX("pix")
}

enum class TipoCredito(val rotulo: String){
    AVISTA("À vista"),
    PARCELADO_LOJA("Parcelado loja"),
    PARCELADO_EMISSOR("Parcelado emissor")
}



fun formatarValor(reais: Int): String =
    String.format(Locale("pt", "BR"), "%,d", reais)