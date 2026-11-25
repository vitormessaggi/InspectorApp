package com.example.inspecta;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MenuOpeActivity extends AppCompatActivity {
    private final ActivityResultLauncher<ScanOptions> leitorQr =
            registerForActivityResult(new ScanContract(), this::receberQr);
    private final ActivityResultLauncher<String> permissaoCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), autorizada -> {
                if (autorizada) abrirLeitor();
                else Toast.makeText(this, R.string.camera_permissao_necessaria, Toast.LENGTH_LONG).show();
            });
    private final CatalogoStore store = new CatalogoStore();
    private final InspecaoStore inspecoes = new InspecaoStore();
    private List<Checklist> publicados = Collections.emptyList();
    private ChecklistAdapter adapter;
    private EditText busca;
    private TextView totalDisponiveis;
    private TextView totalMaquinas;
    private TextView estado;
    private Button botaoQr;
    private boolean lendoQr;
    private String codigoLido;
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
        adapter = new ChecklistAdapter(this::abrirInspecao);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        ChecklistUi.observarBusca(busca, this::atualizarLista);
        findViewById(R.id.btnSairOperador).setOnClickListener(v -> Perfil.sair(this));
        if (savedInstanceState != null) {
            lendoQr = savedInstanceState.getBoolean("lendoQr");
            codigoLido = savedInstanceState.getString("codigoLido");
        }
        botaoQr = findViewById(R.id.btnQr);
        botaoQr.setOnClickListener(v -> lerQr());
        atualizarBotaoQr();
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
            atualizarBotaoQr();
            concluirLeituraQr();
        }, () -> {
            publicados = Collections.emptyList();
            falhou = true;
            atualizarLista();
            atualizarBotaoQr();
            concluirLeituraQr();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("lendoQr", lendoQr);
        outState.putString("codigoLido", codigoLido);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChecklistUi.atualizarSaudacao(findViewById(R.id.tvSaudacao),
                getIntent().getStringExtra(MainActivity.EXTRA_NOME_USUARIO));
    }

    private void abrirInspecao(Checklist checklist) {
        if (!checklist.publicado) {
            Toast.makeText(this, R.string.checklist_maquina_sem_modelo, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, ExecutarChecklistActivity.class);
        intent.putExtra(ExecutarChecklistActivity.EXTRA_CHECKLIST_ID, checklist.id);
        intent.putExtra(ExecutarChecklistActivity.EXTRA_CODIGO, checklist.codigo);
        startActivity(intent);
    }

    private void atualizarBotaoQr() {
        botaoQr.setEnabled(carregado && !falhou && !lendoQr && codigoLido == null);
    }

    private void lerQr() {
        if (!botaoQr.isEnabled()) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirLeitor();
        } else {
            permissaoCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirLeitor() {
        if (lendoQr) return;
        ScanOptions opcoes = new ScanOptions();
        opcoes.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opcoes.setPrompt(getString(R.string.checklist_qr_enquadrar));
        opcoes.setBeepEnabled(false);
        opcoes.setOrientationLocked(false);
        lendoQr = true;
        atualizarBotaoQr();
        try {
            leitorQr.launch(opcoes);
        } catch (ActivityNotFoundException | SecurityException erro) {
            lendoQr = false;
            atualizarBotaoQr();
            Toast.makeText(this, R.string.checklist_qr_erro_leitor, Toast.LENGTH_LONG).show();
        }
    }

    private void receberQr(ScanIntentResult resultado) {
        lendoQr = false;
        codigoLido = resultado.getContents();
        atualizarBotaoQr();
        concluirLeituraQr();
    }

    private void concluirLeituraQr() {
        if (codigoLido == null || (!carregado && !falhou)) return;

        String codigo = codigoLido.trim();
        codigoLido = null;
        atualizarBotaoQr();
        if (falhou) {
            Toast.makeText(this, R.string.checklist_erro_carregar, Toast.LENGTH_LONG).show();
            return;
        }

        if (!codigo.isEmpty()) {
            String idMaquina = Inspecao.idMaquina(codigo);
            for (Checklist checklist : publicados) {
                if (Inspecao.idMaquina(checklist.codigo).equals(idMaquina)) {
                    abrirInspecao(checklist);
                    return;
                }
            }
        }
        Toast.makeText(this, R.string.checklist_qr_desconhecido, Toast.LENGTH_LONG).show();
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
