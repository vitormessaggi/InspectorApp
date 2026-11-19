package com.example.inspecta;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GerenciarChecklistsActivity extends AppCompatActivity {
    private static final int VERDE = Color.rgb(0, 77, 64);
    private final ChecklistStore store = new ChecklistStore();
    private List<Checklist> checklists = Collections.emptyList();
    private ChecklistAdapter adapter;
    private EditText busca;
    private Button publicados;
    private Button rascunhos;
    private TextView estado;
    private boolean mostrarPublicados = true;
    private boolean carregado;
    private boolean falhou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_checklists);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));
        if (savedInstanceState != null) {
            mostrarPublicados = savedInstanceState.getBoolean("mostrarPublicados");
        }

        busca = findViewById(R.id.etBuscaFormulario);
        publicados = findViewById(R.id.btnPublicados);
        rascunhos = findViewById(R.id.btnRascunhos);
        estado = findViewById(R.id.tvEstadoChecklists);
        RecyclerView lista = findViewById(R.id.rvFormularios);
        adapter = new ChecklistAdapter(this::abrirChecklist);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);

        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        findViewById(R.id.btnNovoChecklist).setOnClickListener(view ->
                startActivity(new Intent(this, CriarChecklistActivity.class)));
        publicados.setOnClickListener(view -> selecionarAba(true));
        rascunhos.setOnClickListener(view -> selecionarAba(false));
        ChecklistUi.observarBusca(busca, this::atualizarLista);
        selecionarAba(mostrarPublicados);
    }

    @Override
    protected void onStart() {
        super.onStart();
        store.observar(this, false, lista -> {
            checklists = lista;
            carregado = true;
            falhou = false;
            atualizarLista();
        }, () -> {
            checklists = Collections.emptyList();
            falhou = true;
            atualizarLista();
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mostrarPublicados", mostrarPublicados);
    }

    private void selecionarAba(boolean abaPublicados) {
        mostrarPublicados = abaPublicados;
        estilizarAba(publicados, abaPublicados);
        estilizarAba(rascunhos, !abaPublicados);
        atualizarLista();
    }

    private void estilizarAba(Button aba, boolean selecionada) {
        aba.setSelected(selecionada);
        aba.setBackgroundTintList(ColorStateList.valueOf(selecionada ? VERDE : Color.TRANSPARENT));
        aba.setTextColor(selecionada ? Color.WHITE : VERDE);
    }

    private void atualizarLista() {
        if (falhou) {
            adapter.submitList(Collections.emptyList());
            publicados.setText(R.string.checklist_publicados);
            rascunhos.setText(R.string.checklist_rascunhos);
            estado.setText(R.string.checklist_erro_carregar);
            estado.setVisibility(View.VISIBLE);
            return;
        }
        if (!carregado) {
            return;
        }

        String filtro = busca.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Checklist> filtrados = new ArrayList<>();
        int totalPublicados = 0;
        for (Checklist checklist : checklists) {
            if (checklist.publicado) {
                totalPublicados++;
            }
            if (checklist.publicado == mostrarPublicados && checklist.corresponde(filtro)) {
                filtrados.add(checklist);
            }
        }
        int totalRascunhos = checklists.size() - totalPublicados;
        publicados.setText(getString(R.string.checklist_total_aba,
                getString(R.string.checklist_publicados), totalPublicados));
        rascunhos.setText(getString(R.string.checklist_total_aba,
                getString(R.string.checklist_rascunhos), totalRascunhos));
        adapter.submitList(filtrados);
        if (filtrados.isEmpty()) {
            int mensagem = R.string.checklist_sem_resultados;
            if (mostrarPublicados && totalPublicados == 0) {
                mensagem = R.string.checklist_sem_publicados;
            } else if (!mostrarPublicados && totalRascunhos == 0) {
                mensagem = R.string.checklist_sem_rascunhos;
            }
            estado.setText(mensagem);
        }
        estado.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirChecklist(Checklist checklist) {
        if (checklist.publicado) {
            ChecklistUi.mostrarItens(this, checklist, () -> editarChecklist(checklist.id));
            return;
        }
        editarChecklist(checklist.id);
    }

    private void editarChecklist(String id) {
        Intent intent = new Intent(this, CriarChecklistActivity.class);
        intent.putExtra(CriarChecklistActivity.EXTRA_EDITAR_CHECKLIST_ID, id);
        startActivity(intent);
    }
}
