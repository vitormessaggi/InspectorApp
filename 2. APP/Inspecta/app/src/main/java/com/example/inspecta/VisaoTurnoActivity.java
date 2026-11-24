package com.example.inspecta;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.ListenerRegistration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;

public class VisaoTurnoActivity extends AppCompatActivity {
    private final ChecklistStore store = new ChecklistStore();
    private final InspecaoStore inspecoes = new InspecaoStore();
    private final Handler horario = new Handler(Looper.getMainLooper());
    private ListenerRegistration consultaTotal;
    private TextView totalFormularios;
    private TextView totalInspecoes;
    private TextView totalInoperantes;
    private TextView totalAlertas;
    private TextView data;
    private TextView estado;
    private LinearLayout listaAlertas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visao_turno);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));

        findViewById(R.id.btnSituacaoMaquinas).setOnClickListener(v -> startActivity(new Intent(this, SituacaoMaquinasActivity.class)));
        findViewById(R.id.btnCadastrarMaquina).setOnClickListener(v -> startActivity(new Intent(this, CadastrarMaquinaActivity.class)));
        findViewById(R.id.btnIndicadores).setOnClickListener(v -> startActivity(new Intent(this, IndicadoresActivity.class)));
        findViewById(R.id.btnSairSupervisor).setOnClickListener(v -> Perfil.sair(this));
        totalFormularios = findViewById(R.id.tvTotalFormularios);
        totalInspecoes = findViewById(R.id.tvTotalInspecoes);
        totalInoperantes = findViewById(R.id.tvTotalInoperantes);
        totalAlertas = findViewById(R.id.tvTotalAlertas);
        data = findViewById(R.id.tvDataTurno);
        estado = findViewById(R.id.tvEstadoTurno);
        listaAlertas = findViewById(R.id.listaAlertasCriticos);
        findViewById(R.id.btnCriarChecklist).setOnClickListener(view ->
                startActivity(new Intent(this, CriarChecklistActivity.class)));
        findViewById(R.id.btnGerenciarFormularios).setOnClickListener(view ->
                startActivity(new Intent(this, GerenciarChecklistsActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        observarTotalDoDia();
        inspecoes.observarPendencias(this, this::mostrarAlertas, () -> {
            totalInoperantes.setText(R.string.checklist_total_indisponivel);
            totalAlertas.setText("");
            estado.setText(R.string.inspecao_erro_alertas);
            listaAlertas.removeAllViews();
        });
        store.observar(this, false, lista -> totalFormularios.setText(Integer.toString(lista.size())), () -> {
            totalFormularios.setText(R.string.checklist_total_indisponivel);
            Toast.makeText(this, R.string.checklist_erro_carregar, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChecklistUi.atualizarSaudacao(findViewById(R.id.tvSaudacao),
                getIntent().getStringExtra(MainActivity.EXTRA_NOME_USUARIO));
    }

    @Override
    protected void onStop() {
        horario.removeCallbacksAndMessages(null);
        if (consultaTotal != null) {
            consultaTotal.remove();
            consultaTotal = null;
        }
        super.onStop();
    }

    private void observarTotalDoDia() {
        if (consultaTotal != null) {
            consultaTotal.remove();
        }
        ZoneId fuso = ZoneId.systemDefault();
        LocalDate hoje = LocalDate.now(fuso);
        Date inicio = Date.from(hoje.atStartOfDay(fuso).toInstant());
        Date fim = Date.from(hoje.plusDays(1).atStartOfDay(fuso).toInstant());
        data.setText(hoje.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)));
        consultaTotal = inspecoes.observarTotal(this, inicio, fim,
                quantidade -> totalInspecoes.setText(Integer.toString(quantidade)),
                () -> totalInspecoes.setText(R.string.checklist_total_indisponivel));
        horario.postDelayed(this::observarTotalDoDia, Math.max(1L, fim.getTime() - System.currentTimeMillis()));
    }

    private void mostrarAlertas(List<Inspecao> pendentes, boolean confirmado) {
        int quantidade = pendentes.size();
        totalInoperantes.setText(confirmado ? Integer.toString(quantidade) : getString(R.string.checklist_total_indisponivel));
        totalAlertas.setText(confirmado ? getResources().getQuantityString(
                R.plurals.inspecao_total_alertas, quantidade, quantidade) : "");
        estado.setText(!confirmado ? R.string.inspecao_verificando_alertas
                : quantidade == 0 ? R.string.inspecao_sem_alertas : R.string.inspecao_aguarda_supervisor);

        listaAlertas.removeAllViews();
        for (Inspecao inspecao : pendentes) {
            View cartao = getLayoutInflater().inflate(R.layout.item_alerta_critico, listaAlertas, false);
            ((TextView) cartao.findViewById(R.id.tvAlertaMaquina)).setText(
                    getString(R.string.inspecao_codigo_maquina, inspecao.codigo, inspecao.maquina));
            TextView setor = cartao.findViewById(R.id.tvAlertaSetor);
            setor.setText(inspecao.setor);
            setor.setVisibility(inspecao.setor.isEmpty() ? View.GONE : View.VISIBLE);
            TextView falha = cartao.findViewById(R.id.tvAlertaFalha);
            falha.setText(getString(R.string.inspecao_item_critico, inspecao.primeiraFalha()));
            int outrasFalhas = inspecao.perguntasCriticas.size() - 1;
            if (outrasFalhas > 0) {
                falha.append("\n" + getResources().getQuantityString(
                        R.plurals.inspecao_outras_falhas, outrasFalhas, outrasFalhas));
            }
            ((TextView) cartao.findViewById(R.id.tvAlertaData)).setText(
                    getString(R.string.inspecao_registrada, inspecao.checklistNome, ChecklistUi.dataInspecao(inspecao.enviadaEm)));
            cartao.findViewById(R.id.btnRevisarMaquina).setOnClickListener(view -> {
                Intent intent = new Intent(this, RevisarMaquinaActivity.class);
                intent.putExtra(RevisarMaquinaActivity.EXTRA_INSPECAO_ID, inspecao.id);
                startActivity(intent);
            });
            listaAlertas.addView(cartao);
        }
    }
}
