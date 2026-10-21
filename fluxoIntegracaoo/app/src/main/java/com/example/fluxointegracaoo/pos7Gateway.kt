package com.example.fluxointegracaoo

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import printx990.br.apix990.internal.TransactionInfo
import printx990.br.apix990.internal.domain.callbacks.TransactionCallback
import printx990.br.apix990.internal.domain.dto.*

import kotlin.coroutines.resume

class Pos7Gateway(private val activity: Activity) : PagamentoGateWay {
    override val nome = "POS7"

    override suspend fun cancelar(onEtapa: (String) -> Unit): ResultTransacao {
        if (!Pos7.info.getSipag(activity)) {
            return ResultTransacao(aprovada = false, mensagem = "App Sipag (POS7) não encontrado")
        }
        return suspendCancellableCoroutine { cont ->
            onEtapa("Abrindo POS7 para cancelamento...")

            val dados = TransactionData(
                IdTransactionPartinner = (System.currentTimeMillis() / 1000).toInt(),
                product = null,
                amount = null,
                creditType = null,
                typeTransaction = TypeTransaction.CANCELTRANSACTION,
                packageName = activity.packageName,
                receipt = true,
            )

            val callback = object : TransactionCallback {
                override fun onTransactionResult(resultData: String) {
                    Log.d("Pos7", "Cancelamento recebido: $resultData")
                    if (cont.isActive) cont.resume(interpretarResultadoPos7(resultData))
                }
            }
            Pos7.info.initTransaction(activity, dados, callback)
        }
    }

    override suspend fun pagar(
        pedido: PedidoPagamento,
        onEtapa: (String) -> Unit,
    ): ResultTransacao {
        if (!Pos7.info.getSipag(activity)) {
            return ResultTransacao(
                aprovada = false,
                mensagem = "App Sipag (POS7) não encontrado neste dispositivo",
            )
        }

        return suspendCancellableCoroutine { cont ->
            onEtapa("Abrindo POS7...")

            val ehCredito = pedido.forma == FormaPagamentos.CREDITO

            val dados = TransactionData(
                IdTransactionPartinner = (System.currentTimeMillis() / 1000).toInt(),
                product = when (pedido.forma) {
                    FormaPagamentos.CREDITO -> PRODUCT.CREDITO
                    FormaPagamentos.DEBITO -> PRODUCT.DEBITO
                    FormaPagamentos.VOUCHER -> PRODUCT.VOUCHER
                    FormaPagamentos.PIX -> PRODUCT.PIX
                },
                amount = pedido.valor,
                creditType = if (ehCredito) CreditInfo(
                    when (pedido.tipoCredito) {
                        TipoCredito.AVISTA -> CREDITTYPE.AVISTA
                        TipoCredito.PARCELADO_LOJA -> CREDITTYPE.PARCELADOLOJA
                        TipoCredito.PARCELADO_EMISSOR -> CREDITTYPE.PARCELADOEMISSAOR
                        null -> CREDITTYPE.AVISTA
                    }.toString(),
                    pedido.parcelas,
                ) else null,
                typeTransaction = TypeTransaction.TRANSACTION,
                packageName = activity.packageName,
                receipt = true,
            )

            val callback = object : TransactionCallback {
                override fun onTransactionResult(resultData: String) {
                    Log.d("Pos7", "Callback recebido: $resultData")
                    if (cont.isActive) cont.resume(interpretarResultadoPos7(resultData))
                }
            }

            Log.d("Pos7", "Chamando POS7: ${dados.product} ${dados.amount}")
            Pos7.info.initTransaction(activity, dados, callback)
        }
    }

    fun ultimaTransacao(): ResultTransacao? = try {
        val json = Pos7.info.readLastOperationResultJson(activity)
        if (json == null || json.size() == 0) null
        else interpretarResultadoPos7(json.toString())
    } catch (e: Exception) {
        Log.e("Pos7", "Falha ao consultar última transação", e)
        null
    }
}