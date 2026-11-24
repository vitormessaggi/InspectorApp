package com.example.inspecta;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Objects;

public class ExecutarChecklistActivity extends AppCompatActivity {
    static final String EXTRA_CHECKLIST_ID = "com.example.inspecta.extra.CHECKLIST_ID";

    private final ChecklistStore checklists = new ChecklistStore();
    private final InspecaoStore inspecoes = new InspecaoStore();
    private LinearLayout perguntas;
    private TextView progresso;
    private TextView estado;
    private TextView mensagemEnvio;
    private EditText observacao;
    private Button enviar;
    private GravacaoInspecao gravacao;
    private Checklist checklist;
    private String checklistId;
    static final String EXTRA_CODIGO = "codigoMaquina";
    private String inspecaoId;
    private int[] respostas;
    private Timestamp versaoRestaurada;
    private boolean statusConfirmado;
    private boolean bloqueada;
    private boolean erroStatus;
    private boolean concluida;
    private boolean enviouFalhaCritica;
    private AlertDialog confirmacaoEnvio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_executar_checklist);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));
        checklistId = getIntent().getStringExtra(EXTRA_CHECKLIST_ID);
        if (checklistId == null) {
            finish();
            return;
        }
        inspecaoId = savedInstanceState == null ? inspecoes.novoId() : savedInstanceState.getString("inspecaoId");
        if (savedInstanceState != null) {
            respostas = savedInstanceState.getIntArray("respostas");
            versaoRestaurada = savedInstanceState.getParcelable("versaoChecklist", Timestamp.class);
            concluida = savedInstanceState.getBoolean("concluida");
            enviouFalhaCritica = savedInstanceState.getBoolean("enviouFalhaCritica");
        }

        gravacao = new ViewModelProvider(this).get(GravacaoInspecao.class);
        perguntas = findViewById(R.id.listaPerguntas);
        progresso = findViewById(R.id.tvProgresso);
        estado = findViewById(R.id.tvInspecaoEstado);
        mensagemEnvio = findViewById(R.id.tvInspecaoEnvio);
        observacao = findViewById(R.id.etDescricaoFalha);
        enviar = findViewById(R.id.btnConcluir);
        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        enviar.setOnClickListener(view -> enviarInspecao());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFinishing()) {
            return;
        }
        if (gravacao.tarefa != null) {
            acompanharEnvio();
        } else if (concluida) {
            mostrarRegistroEnviado();
            return;
        }
        if (checklist == null) {
            carregarChecklist();
        } else {
            observarBloqueio();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("inspecaoId", inspecaoId);
        outState.putIntArray("respostas", respostas);
        outState.putParcelable("versaoChecklist", checklist == null ? versaoRestaurada : checklist.atualizadoEm);
        outState.putBoolean("concluida", concluida);
        outState.putBoolean("enviouFalhaCritica", enviouFalhaCritica);
    }

    private void carregarChecklist() {
        estado.setText(R.string.inspecao_carregando);
        checklists.buscar(checklistId).addOnCompleteListener(this, tarefa -> {
            if (concluida) {
                return;
            }
            checklist = tarefa.isSuccessful() ? Checklist.paraInspecao(tarefa.getResult()) : null;
            if (checklist == null) {
                estado.setText(R.string.inspecao_formulario_indisponivel);
                return;
            }
            if (respostas == null || respostas.length != checklist.itens.size()
                    || !Objects.equals(versaoRestaurada, checklist.atualizadoEm)) {
                respostas = new int[checklist.itens.size()];
            }
            String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
            CatalogoStore.resolver(checklist, codigo).addOnCompleteListener(this, maquina -> {
                if (!maquina.isSuccessful()) {
                    estado.setText("Máquina indisponível. Volte e atualize a lista.");
                    return;
                }
                checklist = maquina.getResult();
                montarPerguntas();
                observarBloqueio();
            });
        });
    }

    private void montarPerguntas() {
        ((TextView) findViewById(R.id.tvInspecaoNome)).setText(checklist.nome);
        ((TextView) findViewById(R.id.tvInspecaoMaquina)).setText(
                getString(R.string.inspecao_codigo_maquina, checklist.codigo, checklist.maquina));
        for (int i = 0; i < checklist.itens.size(); i++) {
            int indice = i;
            Checklist.Item item = checklist.itens.get(indice);
            View linha = getLayoutInflater().inflate(R.layout.item_pergunta, perguntas, false);
            linha.setSaveFromParentEnabled(false);
            ((TextView) linha.findViewById(R.id.tvTituloPergunta)).setText(item.pergunta);
            linha.findViewById(R.id.tvTagCritico).setVisibility(item.critico ? View.VISIBLE : View.GONE);
            RadioGroup grupo = linha.findViewById(R.id.rgResposta);
            if (respostas[indice] != 0) {
                grupo.check(respostas[indice] == Inspecao.FUNCIONANDO ? R.id.rbConforme : R.id.rbNaoConforme);
            }
            grupo.setOnCheckedChangeListener((view, selecionado) -> {
                respostas[indice] = selecionado == R.id.rbConforme ? Inspecao.FUNCIONANDO
                        : selecionado == R.id.rbNaoConforme ? Inspecao.NAO_FUNCIONANDO : 0;
                mensagemEnvio.setText("");
                atualizarFormulario();
            });
            perguntas.addView(linha);
        }
        observacao.setVisibility(View.VISIBLE);
    }

    private void observarBloqueio() {
        statusConfirmado = false;
        erroStatus = false;
        atualizarFormulario();
        inspecoes.observarBloqueio(this, checklist.codigo, (existe, servidor) -> {
            bloqueada = existe;
            statusConfirmado = servidor;
            atualizarFormulario();
        }, () -> {
            statusConfirmado = false;
            erroStatus = true;
            atualizarFormulario();
        });
    }

    private void atualizarFormulario() {
        if (checklist == null || concluida) {
            return;
        }
        int respondidas = 0;
        boolean critica = false;
        for (int i = 0; i < respostas.length; i++) {
            if (respostas[i] != 0) {
                respondidas++;
            }
            critica |= respostas[i] == Inspecao.NAO_FUNCIONANDO && checklist.itens.get(i).critico;
        }
        progresso.setText(getString(R.string.inspecao_progresso, respondidas, respostas.length));
        estado.setText(bloqueada ? R.string.inspecao_maquina_bloqueada
                : erroStatus ? R.string.inspecao_erro_status
                : !statusConfirmado ? R.string.inspecao_verificando_status
                : critica ? R.string.inspecao_falha_bloqueia : R.string.inspecao_orientacao);
        enviar.setText(critica ? R.string.inspecao_enviar_falha : R.string.inspecao_enviar);

        boolean habilitado = statusConfirmado && !bloqueada && gravacao.tarefa == null;
        enviar.setEnabled(habilitado && respondidas == respostas.length);
        observacao.setEnabled(habilitado);
        for (int i = 0; i < perguntas.getChildCount(); i++) {
            View linha = perguntas.getChildAt(i);
            linha.findViewById(R.id.rbConforme).setEnabled(habilitado);
            linha.findViewById(R.id.rbNaoConforme).setEnabled(habilitado);
        }
    }

    private void enviarInspecao() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            mensagemEnvio.setText(R.string.inspecao_sessao_expirada);
            return;
        }
        if (!ValidacaoInspecao.observacaoValida(respostas, observacao.getText().toString())) {
            observacao.setError("Descreva a não conformidade antes de enviar.");
            observacao.requestFocus();
            return;
        }
        enviouFalhaCritica = false;
        for (int i = 0; i < respostas.length; i++) {
            enviouFalhaCritica |= respostas[i] == Inspecao.NAO_FUNCIONANDO && checklist.itens.get(i).critico;
        }
        gravacao.tarefa = inspecoes.enviar(inspecaoId, usuario.getUid(), checklist, respostas.clone(),
                observacao.getText().toString().trim());
        acompanharEnvio();
    }

    private void acompanharEnvio() {
        mensagemEnvio.setText(R.string.inspecao_enviando);
        atualizarFormulario();
        gravacao.tarefa.addOnCompleteListener(this, tarefa -> {
            gravacao.tarefa = null;
            if (tarefa.isSuccessful()) {
                concluida = true;
                mostrarRegistroEnviado();
                return;
            }
            atualizarFormulario();
            int mensagem = R.string.inspecao_erro_envio;
            if (tarefa.getException() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code codigo = ((FirebaseFirestoreException) tarefa.getException()).getCode();
                if (codigo == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    mensagem = R.string.inspecao_maquina_bloqueada;
                } else if (codigo == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    mensagem = R.string.inspecao_formulario_alterado;
                }
            }
            mensagemEnvio.setText(mensagem);
        });
    }

    private void mostrarRegistroEnviado() {
        if (confirmacaoEnvio != null && confirmacaoEnvio.isShowing()) {
            return;
        }
        enviar.setEnabled(false);
        observacao.setEnabled(false);
        for (int i = 0; i < perguntas.getChildCount(); i++) {
            View linha = perguntas.getChildAt(i);
            linha.findViewById(R.id.rbConforme).setEnabled(false);
            linha.findViewById(R.id.rbNaoConforme).setEnabled(false);
        }
        confirmacaoEnvio = new AlertDialog.Builder(this)
                .setTitle(R.string.inspecao_enviada)
                .setMessage(enviouFalhaCritica ? R.string.inspecao_enviada_bloqueada : R.string.inspecao_enviada_registro)
                .setPositiveButton(android.R.string.ok, (dialogo, botao) -> finish())
                .setOnCancelListener(dialogo -> finish())
                .show();
    }
}
