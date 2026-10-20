package com.example.fluxointegracaoo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

sealed interface  Tela {
    object Cardapio: Tela
    object Pagamento: Tela
    object Processando: Tela
    data class Resultado(val resultado: ResultTransacao) : Tela

}



@Composable
fun CaixaApp() {
    val activity = LocalContext.current as Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val usarPos7 = true  // false = simulado | true = POS7 (maquininha)
    val gateway: PagamentoGateWay = remember {
        if (usarPos7) Pos7Gateway(activity) else SimulatedGateway()
    }
    val scope = rememberCoroutineScope()

    var tela by remember { mutableStateOf<Tela>(Tela.Cardapio) }
    val carrinho = remember { mutableStateMapOf<Produto, Int>() }
    val etapas = remember { mutableStateListOf<String>() }
    var forma by remember { mutableStateOf(FormaPagamentos.CREDITO) }
    var tipoCredito by remember { mutableStateOf(TipoCredito.AVISTA) }
    var parcelas by remember { mutableStateOf(2) }

    val total = carrinho.entries.sumOf { it.key.precoReais * it.value }

    fun cobrar() {
        if (tela is Tela.Processando) return

        val ehCredito = forma == FormaPagamentos.CREDITO
        val pedido = PedidoPagamento(
            forma = forma,
            valor = "$total,00",
            tipoCredito = if (ehCredito) tipoCredito else null,
            parcelas = if (ehCredito && tipoCredito != TipoCredito.AVISTA) parcelas else 0,
        )

        etapas.clear()
        tela = Tela.Processando
        scope.launch {
            val resultado = try {
                gateway.pagar(pedido) { etapas.add(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Caixa", "Falha no pagamento", e)
                ResultTransacao(
                    aprovada = false,
                    mensagem = e.message ?: "Erro ao processar o pagamento",
                )
            }


            withTimeoutOrNull(15_000) {
                lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
            }

            tela = Tela.Resultado(resultado)
        }
    }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                if (usarPos7) "MODO POS7 — pagamento REAL"
                else "MODO ${gateway.nome.uppercase()} — nenhum pagamento real é feito",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (usarPos7) Color(0xFFC62828) else Color(0xFFE65100))
                    .padding(8.dp),
            )
            val atual = tela
            when (atual) {
                is Tela.Cardapio -> TelaCardapio(carrinho, total) { tela = Tela.Pagamento }
                is Tela.Pagamento -> TelaPagamento(
                    total = total,
                    forma = forma, onForma = { forma = it },
                    tipoCredito = tipoCredito, onTipo = { tipoCredito = it },
                    parcelas = parcelas, onParcelas = { parcelas = it },
                    onConfirmar = { cobrar() },
                    onVoltar = { tela = Tela.Cardapio },
                )
                is Tela.Processando -> TelaProcessando(etapas)
                is Tela.Resultado -> TelaResultado(atual.resultado) {
                    carrinho.clear()
                    tela = Tela.Cardapio
                }
            }
        }
    }
}

@Composable
fun TelaCardapio(
    carrinho: SnapshotStateMap<Produto, Int>,
    total: Int,
    onCobrar: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Produtos", style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.weight(1f)) {
            items(cardapio) { p ->
                val qtd = carrinho[p] ?: 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.nome)
                        Text("R$ ${formatarValor(p.precoReais)}")
                    }
                    OutlinedButton(
                        onClick = { if (qtd > 1) carrinho[p] = qtd - 1 else carrinho.remove(p) },
                        enabled = qtd > 0,
                    ) { Text("−") }
                    Text("$qtd", Modifier.padding(horizontal = 12.dp))
                    Button(onClick = { carrinho[p] = qtd + 1 }) { Text("+") }
                }
            }
        }
        Text("Total: R$ ${formatarValor(total)}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCobrar, enabled = total > 0, modifier = Modifier.fillMaxWidth()) {
            Text("Cobrar")
        }
    }
}


@Composable
fun OpcaoRadio(rotulo: String, selecionado: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionado, onClick = onClick)
        Text(rotulo)
    }
}

@Composable
fun TelaPagamento(
    total: Int,
    forma: FormaPagamentos, onForma: (FormaPagamentos) -> Unit,
    tipoCredito: TipoCredito, onTipo: (TipoCredito) -> Unit,
    parcelas: Int, onParcelas: (Int) -> Unit,
    onConfirmar: () -> Unit,
    onVoltar: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Total: R$ ${formatarValor(total)}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text("Forma de pagamento", style = MaterialTheme.typography.titleMedium)
        FormaPagamentos.entries.forEach { f ->
            OpcaoRadio(f.rotulo, forma == f) { onForma(f) }
        }

        if (forma == FormaPagamentos.CREDITO) {
            Spacer(Modifier.height(16.dp))
            Text("Tipo de crédito", style = MaterialTheme.typography.titleMedium)
            TipoCredito.entries.forEach { t ->
                OpcaoRadio(t.rotulo, tipoCredito == t) { onTipo(t) }
            }
            if (tipoCredito != TipoCredito.AVISTA) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Parcelas:", Modifier.padding(end = 12.dp))
                    OutlinedButton(onClick = { onParcelas((parcelas - 1).coerceAtLeast(2)) }) { Text("−") }
                    Text("$parcelas", Modifier.padding(horizontal = 12.dp))
                    OutlinedButton(onClick = { onParcelas((parcelas + 1).coerceAtMost(99)) }) { Text("+") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfirmar, modifier = Modifier.fillMaxWidth()) { Text("Confirmar pagamento") }
        TextButton(onClick = onVoltar, modifier = Modifier.fillMaxWidth()) { Text("Voltar ao cardápio") }
    }
}

@Composable
fun TelaProcessando(etapas: List<String>) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        etapas.forEachIndexed { i, etapa ->
            val ultima = i == etapas.lastIndex
            Text(
                (if (ultima) "● " else "✓ ") + etapa,
                fontWeight = if (ultima) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
fun TelaResultado(r: ResultTransacao, onNovaVenda: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            if (r.aprovada) "✔ APROVADA" else "✖ NEGADA",
            style = MaterialTheme.typography.headlineMedium,
            color = if (r.aprovada) Color(0xFF2E7D32) else Color(0xFFC62828),
        )
        Spacer(Modifier.height(12.dp))
        Text("transactionID: ${r.transactionId}")
        Text("Código de autorização: ${r.authorizationCode.ifEmpty { "-" }}")
        Text(r.mensagem)
        if (r.valor.isNotEmpty()) Text("Valor: R$ ${r.valor}")
        if (r.produto.isNotEmpty()) Text("Produto: ${r.produto}")
        if (r.parcelas > 0) Text("Parcelas: ${r.parcelas}")
        if (r.bandeira.isNotEmpty()) Text("Bandeira: ${r.bandeira}")
        if (r.cartaoMascarado.isNotEmpty()) Text("Cartão: ${r.cartaoMascarado}")
        if (r.horario.isNotEmpty()) Text("Data/hora: ${r.horario}")
        if (r.pixId.isNotEmpty()) Text("PIX: ${r.pixId}")
        r.erros.forEach { Text("Erro: $it", color = Color(0xFFC62828)) }
        Spacer(Modifier.height(16.dp))
        Text("JSON recebido (resultData)", style = MaterialTheme.typography.titleMedium)
        Text(
            r.jsonOriginal,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().background(Color(0x11000000)).padding(8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNovaVenda, modifier = Modifier.fillMaxWidth()) { Text("Nova venda") }
    }
}