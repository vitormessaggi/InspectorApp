package com.example.inspecta;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SituacaoMaquinasActivity extends AppCompatActivity {
    private final SituacaoAdapter adapter = new SituacaoAdapter();
    private final Handler relogio = new Handler(Looper.getMainLooper());
    private List<Checklist> maquinas;
    private List<DocumentSnapshot> registros;
    private boolean confirmado;
    private TextView estado;
    private String filtro = "Todas";
    private String setorSelecionado;
    private final java.util.SortedSet<String> setores = new java.util.TreeSet<>();
    private android.widget.Button filtroSetor;
    private android.widget.Button filtroSituacao;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) filtro = bundle.getString("filtro", "Todas");
        if (bundle != null) setorSelecionado = bundle.getString("setor");
        setContentView(R.layout.activity_situacao_maquinas);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizSituacao));
        estado = findViewById(R.id.estadoSituacao);
        filtroSetor = findViewById(R.id.filtroSetor);
        filtroSituacao = findViewById(R.id.filtroSituacao);
        androidx.recyclerview.widget.RecyclerView lista = findViewById(R.id.listaSituacao);
        lista.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        lista.setAdapter(adapter);
        filtroSituacao.setOnClickListener(v -> {
            String[] opcoes = {"Todas", "Liberada", "Bloqueada", "Sem inspeção"};
            new AlertDialog.Builder(this).setTitle("Situação de hoje").setItems(opcoes, (d, i) -> {
                filtro = opcoes[i]; atualizar();
            }).show();
        });
        filtroSetor.setOnClickListener(v -> {
            List<String> valores = new ArrayList<>(setores);
            List<String> rotulos = new ArrayList<>();
            rotulos.add("Todos os setores");
            for (String valor : valores) rotulos.add(valor.isEmpty() ? "Sem setor informado" : valor);
            new AlertDialog.Builder(this).setTitle("Filtrar por setor")
                    .setItems(rotulos.toArray(new String[0]), (d, i) -> {
                        setorSelecionado = i == 0 ? null : valores.get(i - 1);
                        atualizar();
                    }).show();
        });
        findViewById(R.id.voltarMenuSituacao).setOnClickListener(v -> finish());
        findViewById(R.id.voltarSituacao).setOnClickListener(v -> finish());
        findViewById(R.id.modelosSituacao).setOnClickListener(v -> abrir(GerenciarChecklistsActivity.class));
    }

    private void abrir(Class<?> activity) { startActivity(new Intent(this, activity)); }

    @Override protected void onStart() {
        super.onStart();
        new CatalogoStore().observar(this, lista -> { maquinas = lista; atualizar(); },
                () -> estado.setText("Não foi possível carregar as máquinas. Confira a conexão e as permissões."));
        FirebaseFirestore.getInstance().collection("inspecoes")
                .addSnapshotListener(this, MetadataChanges.INCLUDE, (resultado, erro) -> {
                    if (erro != null || resultado == null) {
                        confirmado = false;
                        estado.setText("Não foi possível consultar as inspeções.");
                        adapter.mostrar(new ArrayList<>());
                        return;
                    }
                    registros = resultado.getDocuments();
                    confirmado = !resultado.getMetadata().isFromCache();
                    atualizar();
                });
        agendarVirada();
    }

    private void agendarVirada() {
        ZoneId fuso = ZoneId.systemDefault();
        long proximoDia = LocalDate.now(fuso).plusDays(1).atStartOfDay(fuso).toInstant().toEpochMilli();
        relogio.postDelayed(() -> { atualizar(); agendarVirada(); }, Math.max(1, proximoDia - System.currentTimeMillis()));
    }
    @Override protected void onStop() { relogio.removeCallbacksAndMessages(null); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle bundle) { super.onSaveInstanceState(bundle); bundle.putString("filtro", filtro); bundle.putString("setor", setorSelecionado); }

    private void atualizar() {
        if (maquinas == null || registros == null) return;
        Map<String, DocumentSnapshot> bloqueios = new HashMap<>();
        Map<String, DocumentSnapshot> hoje = new HashMap<>();
        LocalDate data = LocalDate.now();
        for (DocumentSnapshot registro : registros) {
            String codigo = Inspecao.idMaquina(Inspecao.texto(registro.get("codigoMaquina")));
            if (Boolean.TRUE.equals(registro.getBoolean("pendente"))) bloqueios.put(codigo, registro);
            com.google.firebase.Timestamp timestamp = registro.getTimestamp("enviadaEm");
            if (timestamp != null && timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(data)) {
                hoje.put(codigo, registro);
            }
        }
        List<SituacaoAdapter.Registro> linhas = new ArrayList<>();
        Map<String, Checklist> catalogo = new java.util.LinkedHashMap<>();
        for (Checklist maquina : maquinas) catalogo.put(Inspecao.idMaquina(maquina.codigo), maquina);
        // Mantém visíveis bloqueios mesmo se o modelo original foi arquivado.
        for (Map.Entry<String, DocumentSnapshot> entry : bloqueios.entrySet()) {
            DocumentSnapshot d = entry.getValue();
            catalogo.putIfAbsent(entry.getKey(), new Checklist("", "Modelo arquivado", d.getString("codigoMaquina"),
                    d.getString("nomeMaquina"), d.getString("setor"), new ArrayList<>(), false, null));
        }
        setores.clear();
        for (Checklist maquina : catalogo.values()) setores.add(maquina.setor.trim());
        filtroSetor.setText(setorSelecionado == null ? "Todos os setores"
                : setorSelecionado.isEmpty() ? "Sem setor informado" : setorSelecionado);
        filtroSituacao.setText(filtro.equals("Todas") ? "Todas as situações" : filtro);
        for (Checklist maquina : catalogo.values()) {
            if (setorSelecionado != null && !setorSelecionado.equals(maquina.setor.trim())) continue;
            String id = Inspecao.idMaquina(maquina.codigo);
            DocumentSnapshot bloqueio = bloqueios.get(id);
            String situacao = SituacaoMaquina.status(bloqueio != null, hoje.containsKey(id));
            if (!filtro.equals("Todas") && !filtro.equals(situacao)) continue;
            linhas.add(new SituacaoAdapter.Registro(maquina, situacao, confirmado, () -> {
                List<String> opcoes = new ArrayList<>();
                opcoes.add("Histórico da máquina");
                if (bloqueio != null) opcoes.add("Revisar bloqueio");
                opcoes.add(getString(R.string.maquina_qr_salvar));
                new AlertDialog.Builder(this).setTitle(maquina.codigo).setItems(opcoes.toArray(new String[0]), (d, i) -> {
                    if (i == opcoes.size() - 1) {
                        startActivity(QrMaquinaActivity.paraMaquina(this, maquina.codigo));
                        return;
                    }
                    Intent intent = new Intent(this, i == 0 ? HistoricoActivity.class : RevisarMaquinaActivity.class);
                    intent.putExtra("codigoMaquina", maquina.codigo);
                    intent.putExtra("supervisor", true);
                    if (bloqueio != null) intent.putExtra(RevisarMaquinaActivity.EXTRA_INSPECAO_ID, bloqueio.getId());
                    startActivity(intent);
                }).show();
            }));
        }
        adapter.mostrar(linhas);
        estado.setText(data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " · " + filtro
                + "\n" + (catalogo.isEmpty() ? "Cadastre uma máquina para começar." : linhas.isEmpty()
                ? "Nenhuma máquina para os filtros selecionados." : linhas.size() + " máquina(s)")
                + (confirmado ? "" : "\nAguardando confirmação do servidor."));
    }
}
