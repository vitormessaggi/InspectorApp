package com.example.inspecta;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HistoricoActivity extends AppCompatActivity {
    private final ListaAdapter adapter = new ListaAdapter();
    private TextView estado;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Tela tela = new Tela(this, "Histórico de inspeções", false);
        estado = tela.texto("Carregando…");
        tela.lista(adapter);
        tela.botao("Voltar", android.R.drawable.ic_menu_revert, v -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { Perfil.sair(this); return; }
        Query query = FirebaseFirestore.getInstance().collection("inspecoes");
        String codigo = getIntent().getStringExtra("codigoMaquina");
        if (codigo != null) query = query.whereEqualTo("codigoMaquina", codigo);
        if (!getIntent().getBooleanExtra("supervisor", false)) {
            query = query.whereEqualTo("operadorUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        query.addSnapshotListener(this, (resultado, erro) -> {
            if (erro != null || resultado == null) { estado.setText("Não foi possível carregar o histórico."); return; }
            List<DocumentSnapshot> documentos = new ArrayList<>(resultado.getDocuments());
            documentos.sort(Comparator.comparing((DocumentSnapshot d) -> d.getTimestamp("enviadaEm"),
                    Comparator.nullsLast(Comparator.reverseOrder())));
            List<ListaAdapter.Registro> linhas = new ArrayList<>();
            for (DocumentSnapshot doc : documentos) {
                String operador = Inspecao.texto(doc.get("operadorNome"));
                if (operador.isEmpty()) operador = Inspecao.texto(doc.get("operadorUid"));
                String resumo = doc.getString("codigoMaquina") + " · " + doc.getString("nomeMaquina")
                        + "\n" + ChecklistUi.dataInspecao(doc.getTimestamp("enviadaEm")) + " · " + operador;
                linhas.add(new ListaAdapter.Registro(resumo, () -> mostrarDetalhes(doc)));
            }
            adapter.mostrar(linhas);
            estado.setText(linhas.isEmpty() ? "Nenhuma inspeção registrada." : linhas.size() + " inspeção(ões)");
        });
    }

    private void mostrarDetalhes(DocumentSnapshot doc) {
        StringBuilder texto = new StringBuilder();
        texto.append("Observação\n").append(Inspecao.texto(doc.get("observacao")));
        Object itens = doc.get("itens");
        Object respostas = doc.get("respostas");
        if (itens instanceof List<?> && respostas instanceof List<?>) {
            List<?> perguntas = (List<?>) itens;
            List<?> valores = (List<?>) respostas;
            for (int i = 0; i < perguntas.size() && i < valores.size(); i++) {
                if (perguntas.get(i) instanceof Map<?, ?> && valores.get(i) instanceof Number) {
                    Map<?, ?> item = (Map<?, ?>) perguntas.get(i);
                    texto.append("\n\n").append(item.get("pergunta")).append("\n")
                            .append(((Number) valores.get(i)).intValue() == 1 ? "Conforme" : "Não conforme")
                            .append(Boolean.TRUE.equals(item.get("critico")) ? " · Crítico" : "");
                }
            }
        } else {
            texto.append("\n\nRegistro anterior à gravação das respostas completas.");
            Object criticas = doc.get("perguntasCriticas");
            if (criticas instanceof List<?>) for (Object pergunta : (List<?>) criticas) texto.append("\n• ").append(pergunta);
            texto.append("\nÍndices de falhas registrados (base zero)\n").append(doc.get("falhas"));
        }
        if (Boolean.TRUE.equals(doc.getBoolean("pendente"))) texto.append("\n\nBloqueio pendente de revisão.");
        if (doc.contains("justificativaLiberacao")) {
            texto.append("\n\nLiberação\n").append(doc.getString("justificativaLiberacao"))
                    .append("\n").append(ChecklistUi.dataInspecao(doc.getTimestamp("liberadaEm")))
                    .append("\nSupervisor ").append(doc.getString("liberadoPor"));
        }
        new AlertDialog.Builder(this).setTitle(doc.getString("checklistNome"))
                .setMessage(texto).setPositiveButton("Fechar", null).show();
    }
}
