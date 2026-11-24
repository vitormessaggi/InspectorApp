package com.example.inspecta;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class IndicadoresActivity extends AppCompatActivity {
    private final ListaAdapter adapter = new ListaAdapter();
    private List<DocumentSnapshot> documentos = new ArrayList<>();
    private EditText mes;
    private TextView estado;
    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Tela tela = new Tela(this, "Conformidade por setor", false);
        mes = tela.campo(1301, "Mês (AAAA-MM)", InputType.TYPE_CLASS_DATETIME);
        if (bundle == null) mes.setText(YearMonth.now().toString());
        tela.texto("Percentual de itens conformes entre todos os itens respondidos no mês.");
        tela.botao("Consultar mês", android.R.drawable.ic_menu_search, v -> atualizar());
        estado = tela.texto("Carregando…");
        tela.lista(adapter);
        tela.botao("Voltar", android.R.drawable.ic_menu_revert, v -> finish());
    }
    @Override protected void onStart() {
        super.onStart();
        FirebaseFirestore.getInstance().collection("inspecoes").addSnapshotListener(this, (resultado, erro) -> {
            if (erro != null || resultado == null) { estado.setText("Não foi possível consultar os indicadores."); return; }
            documentos = resultado.getDocuments();
            atualizar();
        });
    }
    private void atualizar() {
        YearMonth periodo;
        try { periodo = YearMonth.parse(mes.getText().toString().trim()); }
        catch (java.time.format.DateTimeParseException e) { mes.setError("Use o formato AAAA-MM, por exemplo 2026-11"); return; }
        Map<String, long[]> totais = new TreeMap<>();
        int antigos = 0;
        for (DocumentSnapshot doc : documentos) {
            com.google.firebase.Timestamp data = doc.getTimestamp("enviadaEm");
            if (data == null || !YearMonth.from(data.toDate().toInstant().atZone(ZoneId.systemDefault())).equals(periodo)) continue;
            Object respostas = doc.get("respostas");
            if (!(respostas instanceof List<?>)) { antigos++; continue; }
            String setor = Inspecao.texto(doc.get("setor"));
            if (setor.isEmpty()) setor = "Sem setor informado";
            long[] total = totais.computeIfAbsent(setor, key -> new long[2]);
            for (Object resposta : (List<?>) respostas) {
                if (resposta instanceof Number) {
                    total[1]++;
                    if (((Number) resposta).intValue() == 1) total[0]++;
                }
            }
        }
        List<ListaAdapter.Registro> linhas = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : totais.entrySet()) {
            long[] total = entry.getValue();
            linhas.add(new ListaAdapter.Registro(entry.getKey() + "\n" + String.format(new Locale("pt", "BR"),
                    "%.1f%% · %d de %d itens conformes", SituacaoMaquina.percentual(total[0], total[1]), total[0], total[1]), null));
        }
        adapter.mostrar(linhas);
        estado.setText((linhas.isEmpty() ? "Nenhuma inspeção com respostas completas neste mês." : "Mês " + periodo)
                + (antigos == 0 ? "" : "\n" + antigos + " registro(s) antigo(s) sem respostas completas ficaram fora do cálculo."));
    }
}
