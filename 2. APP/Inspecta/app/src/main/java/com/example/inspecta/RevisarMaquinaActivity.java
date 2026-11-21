package com.example.inspecta;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class RevisarMaquinaActivity extends AppCompatActivity {
    static final String EXTRA_INSPECAO_ID = "com.example.inspecta.extra.INSPECAO_ID";

    private final InspecaoStore inspecoes = new InspecaoStore();
    private String inspecaoId;
    private Inspecao inspecao;
    private GravacaoInspecao gravacao;
    private boolean statusConfirmado;
    private TextView estado;
    private TextView mensagem;
    private View formulario;
    private View liberar;
    private EditText justificativa;
    private CheckBox correcao;
    private CheckBox seguranca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisar_maquina);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));
        inspecaoId = getIntent().getStringExtra(EXTRA_INSPECAO_ID);
        if (inspecaoId == null) {
            finish();
            return;
        }
        gravacao = new ViewModelProvider(this).get(GravacaoInspecao.class);
        estado = findViewById(R.id.tvRevisaoEstado);
        mensagem = findViewById(R.id.tvEstadoLiberacao);
        formulario = findViewById(R.id.formularioLiberacao);
        liberar = findViewById(R.id.btnLiberarMaquina);
        justificativa = findViewById(R.id.etJustificativaLiberacao);
        correcao = findViewById(R.id.cbCorrecaoReteste);
        seguranca = findViewById(R.id.cbMaquinaSegura);
        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        findViewById(R.id.btnManterInoperante).setOnClickListener(view -> finish());
        liberar.setOnClickListener(view -> confirmarLiberacao());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFinishing()) {
            return;
        }
        statusConfirmado = false;
        habilitarLiberacao();
        if (gravacao.tarefa != null) {
            acompanharLiberacao();
        }
        inspecoes.observarInspecao(this, inspecaoId, (registro, servidor) -> {
            statusConfirmado = servidor;
            inspecao = registro;
            mostrarOcorrencia();
        }, () -> {
            statusConfirmado = false;
            estado.setText(R.string.inspecao_erro_alertas);
            habilitarLiberacao();
        });
    }

    private void mostrarOcorrencia() {
        if (inspecao == null) {
            estado.setText(statusConfirmado ? R.string.inspecao_ocorrencia_indisponivel : R.string.inspecao_carregando);
            formulario.setVisibility(View.GONE);
            findViewById(R.id.detalhesRevisao).setVisibility(View.GONE);
            return;
        }

        estado.setText(!statusConfirmado ? R.string.inspecao_verificando_status : inspecao.pendente
                ? R.string.inspecao_inoperante : R.string.inspecao_maquina_liberada);
        findViewById(R.id.detalhesRevisao).setVisibility(View.VISIBLE);
        TextView maquina = findViewById(R.id.tvRevisaoMaquina);
        maquina.setText(getString(R.string.inspecao_codigo_maquina, inspecao.codigo, inspecao.maquina));
        TextView registro = findViewById(R.id.tvRevisaoRegistro);
        registro.setText(getString(R.string.inspecao_registrada, inspecao.checklistNome,
                ChecklistUi.dataInspecao(inspecao.enviadaEm)));
        if (!inspecao.setor.isEmpty()) {
            registro.append("\n" + inspecao.setor);
        }
        if (!inspecao.operador.isEmpty()) {
            registro.append("\n" + getString(R.string.inspecao_operador, inspecao.operador));
        }
        TextView observacao = findViewById(R.id.tvRevisaoObservacao);
        observacao.setVisibility(inspecao.observacao.isEmpty() ? View.GONE : View.VISIBLE);
        observacao.setText(inspecao.observacao);
        TextView falhas = findViewById(R.id.tvRevisaoFalhas);
        StringBuilder detalhes = new StringBuilder();
        for (String pergunta : inspecao.perguntasCriticas) {
            if (detalhes.length() > 0) {
                detalhes.append("\n\n");
            }
            detalhes.append(getString(R.string.inspecao_item_critico, pergunta));
        }
        falhas.setText(detalhes);
        formulario.setVisibility(inspecao.pendente ? View.VISIBLE : View.GONE);
        habilitarLiberacao();
    }

    private void habilitarLiberacao() {
        boolean habilitada = statusConfirmado && inspecao != null && inspecao.pendente && gravacao.tarefa == null;
        liberar.setEnabled(habilitada);
        justificativa.setEnabled(habilitada);
        correcao.setEnabled(habilitada);
        seguranca.setEnabled(habilitada);
    }

    private void confirmarLiberacao() {
        String texto = justificativa.getText().toString().trim();
        if (texto.isEmpty()) {
            justificativa.setError(getString(R.string.inspecao_justificativa_obrigatoria));
            justificativa.requestFocus();
            return;
        }
        if (!correcao.isChecked() || !seguranca.isChecked()) {
            mensagem.setText(R.string.inspecao_confirmacoes_obrigatorias);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.inspecao_liberar)
                .setMessage(getString(R.string.inspecao_confirmar_maquina, inspecao.codigo))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.inspecao_confirmar, (dialogo, botao) -> liberarMaquina(texto))
                .show();
    }

    private void liberarMaquina(String texto) {
        if (!liberar.isEnabled()) {
            return;
        }
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            mensagem.setText(R.string.inspecao_sessao_expirada);
            return;
        }
        gravacao.tarefa = inspecoes.liberar(inspecao, usuario.getUid(), texto);
        acompanharLiberacao();
    }

    private void acompanharLiberacao() {
        mensagem.setText(R.string.inspecao_registrando_liberacao);
        habilitarLiberacao();
        gravacao.tarefa.addOnCompleteListener(this, tarefa -> {
            gravacao.tarefa = null;
            if (tarefa.isSuccessful()) {
                Toast.makeText(this, R.string.inspecao_maquina_liberada, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            habilitarLiberacao();
            Exception erro = tarefa.getException();
            boolean conflito = erro instanceof FirebaseFirestoreException
                    && ((FirebaseFirestoreException) erro).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION;
            mensagem.setText(conflito ? R.string.inspecao_ocorrencia_alterada : R.string.inspecao_erro_liberacao);
        });
    }
}
