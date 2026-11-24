package com.example.inspecta;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MenuOpeActivity extends AppCompatActivity {
    private final CatalogoStore store = new CatalogoStore();
    private final InspecaoStore inspecoes = new InspecaoStore();
    private List<Checklist> publicados = Collections.emptyList();
    private ChecklistAdapter adapter;
    private EditText busca;
    private TextView totalDisponiveis;
    private TextView totalMaquinas;
    private TextView estado;
    private boolean carregado;
    private boolean falhou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menuope);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));

        busca = findViewById(R.id.etBusca);
        totalDisponiveis = findViewById(R.id.tvTotalDisponiveis);
        totalMaquinas = findViewById(R.id.tvTotalMaquinas);
        estado = findViewById(R.id.tvEstadoChecklists);
        RecyclerView lista = findViewById(R.id.rvMaquinas);
        adapter = new ChecklistAdapter(checklist -> {
            Intent intent = new Intent(this, ExecutarChecklistActivity.class);
            if (!checklist.publicado) {
                android.widget.Toast.makeText(this, "O supervisor precisa publicar um modelo para esta máquina.", android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            intent.putExtra(ExecutarChecklistActivity.EXTRA_CHECKLIST_ID, checklist.id);
            intent.putExtra(ExecutarChecklistActivity.EXTRA_CODIGO, checklist.codigo);
            startActivity(intent);
        });
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        ChecklistUi.observarBusca(busca, this::atualizarLista);
        findViewById(R.id.btnSairOperador).setOnClickListener(v -> Perfil.sair(this));
        findViewById(R.id.btnQr).setOnClickListener(v -> lerQr());
        findViewById(R.id.btnHistoricoOperador).setOnClickListener(v -> startActivity(new Intent(this, HistoricoActivity.class)));

    }

    @Override
    protected void onStart() {
        super.onStart();
        inspecoes.observarBloqueios(this, adapter::atualizarBloqueios, () -> {
            // A tela de inspeção confirma o bloqueio antes de habilitar respostas.
        });
        store.observar(this, lista -> {
            publicados = lista;
            carregado = true;
            falhou = false;
            atualizarLista();
        }, () -> {
            publicados = Collections.emptyList();
            falhou = true;
            atualizarLista();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChecklistUi.atualizarSaudacao(findViewById(R.id.tvSaudacao),
                getIntent().getStringExtra(MainActivity.EXTRA_NOME_USUARIO));
    }

    private void lerQr() {
        com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions options =
                new com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                        .enableAutoZoom().build();
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(this, options).startScan()
                .addOnSuccessListener(this, barcode -> {
                    String codigo = barcode.getRawValue();
                    if (codigo == null) return;
                    for (Checklist checklist : publicados) {
                        if (checklist.codigo.equalsIgnoreCase(codigo.trim())) {
                            Intent intent = new Intent(this, ExecutarChecklistActivity.class);
                            intent.putExtra(ExecutarChecklistActivity.EXTRA_CHECKLIST_ID, checklist.id);
                            intent.putExtra(ExecutarChecklistActivity.EXTRA_CODIGO, checklist.codigo);
                            if (checklist.publicado) startActivity(intent);
                            else android.widget.Toast.makeText(this, "Modelo indisponível para esta máquina.", android.widget.Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    android.widget.Toast.makeText(this, "QR sem máquina cadastrada. Use o código exato da máquina.", android.widget.Toast.LENGTH_LONG).show();
                }).addOnFailureListener(this, erro -> android.widget.Toast.makeText(this,
                        "Não foi possível abrir o leitor. Confira a conexão e o Google Play Services.", android.widget.Toast.LENGTH_LONG).show());
    }

    private void atualizarLista() {
        if (falhou) {
            adapter.submitList(Collections.emptyList());
            totalDisponiveis.setText(R.string.checklist_total_indisponivel);
            totalMaquinas.setText("");
            estado.setText(R.string.checklist_erro_carregar);
            estado.setVisibility(View.VISIBLE);
            return;
        }
        if (!carregado) {
            return;
        }

        String filtro = busca.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Checklist> filtrados = new ArrayList<>();
        Set<String> maquinas = new HashSet<>();
        for (Checklist checklist : publicados) {
            if (checklist.corresponde(filtro)) {
                filtrados.add(checklist);
                if (!checklist.codigo.isEmpty()) {
                    maquinas.add(checklist.codigo.toUpperCase(Locale.ROOT));
                }
            }
        }
        adapter.submitList(filtrados);
        totalDisponiveis.setText(getResources().getQuantityString(R.plurals.checklist_total_disponiveis,
                publicados.size(), publicados.size()));
        totalMaquinas.setText(getResources().getQuantityString(R.plurals.checklist_total_maquinas,
                maquinas.size(), maquinas.size()));
        estado.setText(publicados.isEmpty() ? R.string.checklist_sem_publicados : R.string.checklist_sem_resultados);
        estado.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
