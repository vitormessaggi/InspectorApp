package com.example.inspecta;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.Task;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CriarChecklistActivity extends AppCompatActivity {
    static final String EXTRA_EDITAR_CHECKLIST_ID = "com.example.inspecta.extra.EDITAR_CHECKLIST_ID";

    private final ChecklistStore store = new ChecklistStore();
    private EditText nome;
    private EditText maquina;
    private EditText codigo;
    private EditText setor;
    private LinearLayout itens;
    private TextView estado;
    private View adicionar;
    private Button publicar;
    private View salvarRascunho;
    private View excluir;
    private Salvamento salvamento;
    private String checklistId;
    private Timestamp versao;
    private boolean editandoExistente;
    private boolean editandoPublicado;
    private boolean formularioCarregado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_checklist);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));

        nome = findViewById(R.id.etNomeChecklist);
        maquina = findViewById(R.id.etNomeMaquina);
        codigo = findViewById(R.id.etCodigoMaquina);
        setor = findViewById(R.id.etSetorMaquina);
        itens = findViewById(R.id.itensChecklist);
        estado = findViewById(R.id.tvEstadoSalvamento);
        adicionar = findViewById(R.id.btnAdicionarItem);
        publicar = findViewById(R.id.btnPublicarChecklist);
        salvarRascunho = findViewById(R.id.btnSalvarRascunho);
        excluir = findViewById(R.id.btnExcluirChecklist);
        salvamento = new ViewModelProvider(this).get(Salvamento.class);

        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        adicionar.setOnClickListener(view -> adicionarItem("", false).requestFocus());
        publicar.setOnClickListener(view -> salvar(true));
        salvarRascunho.setOnClickListener(view -> salvar(false));
        excluir.setOnClickListener(view -> confirmarExclusao());

        String editarId = getIntent().getStringExtra(EXTRA_EDITAR_CHECKLIST_ID);
        editandoExistente = editarId != null;
        if (savedInstanceState != null) {
            checklistId = savedInstanceState.getString("checklistId");
            editandoPublicado = savedInstanceState.getBoolean("editandoPublicado");
            if (savedInstanceState.getBoolean("formularioCarregado")) {
                versao = savedInstanceState.getParcelable("versao", Timestamp.class);
                ArrayList<String> perguntas = savedInstanceState.getStringArrayList("perguntas");
                boolean[] criticidades = savedInstanceState.getBooleanArray("criticidades");
                for (int i = 0; i < perguntas.size(); i++) {
                    adicionarItem(perguntas.get(i), criticidades[i]);
                }
                formularioCarregado = true;
            }
        } else if (editandoExistente) {
            checklistId = editarId;
        } else {
            checklistId = store.novoId();
            adicionarItem("", false);
            formularioCarregado = true;
        }

        configurarEdicao();
        if (!formularioCarregado) {
            habilitarFormulario(false);
            estado.setText(R.string.checklist_carregando_formulario);
            carregarChecklist();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (salvamento.tarefa != null) {
            acompanharSalvamento();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("checklistId", checklistId);
        outState.putBoolean("formularioCarregado", formularioCarregado);
        outState.putBoolean("editandoPublicado", editandoPublicado);
        outState.putParcelable("versao", versao);
        ArrayList<String> perguntas = new ArrayList<>();
        boolean[] criticidades = new boolean[itens.getChildCount()];
        for (int i = 0; i < criticidades.length; i++) {
            View linha = itens.getChildAt(i);
            perguntas.add(((EditText) linha.findViewById(R.id.etPerguntaChecklist)).getText().toString());
            criticidades[i] = ((MaterialSwitch) linha.findViewById(R.id.swCriticoChecklist)).isChecked();
        }
        outState.putStringArrayList("perguntas", perguntas);
        outState.putBooleanArray("criticidades", criticidades);
    }

    private void configurarEdicao() {
        if (editandoExistente) {
            ((TextView) findViewById(R.id.tvTituloFormulario)).setText(R.string.checklist_editar);
            excluir.setVisibility(View.VISIBLE);
        }
        if (editandoPublicado) {
            ((TextView) findViewById(R.id.tvStatusFormulario)).setText(R.string.checklist_publicado_status);
            publicar.setText(R.string.checklist_salvar_alteracoes);
            salvarRascunho.setVisibility(View.GONE);
            codigo.setEnabled(false);
            estado.setText(R.string.checklist_aviso_edicao);
        }
    }

    private void carregarChecklist() {
        store.buscar(checklistId).addOnCompleteListener(tarefa -> {
            if (isDestroyed() || isFinishing()) {
                return;
            }
            if (!tarefa.isSuccessful() || !tarefa.getResult().exists()
                    || tarefa.getResult().contains(ChecklistStore.CAMPO_EXCLUIDO_EM)
                    || !(tarefa.getResult().get("publicado") instanceof Boolean)) {
                Toast.makeText(this, R.string.checklist_edicao_indisponivel, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            Checklist checklist = Checklist.ler(tarefa.getResult());
            editandoPublicado = checklist.publicado;
            nome.setText(checklist.nome);
            maquina.setText(checklist.maquina);
            codigo.setText(checklist.codigo);
            setor.setText(checklist.setor);
            versao = checklist.atualizadoEm;
            for (Checklist.Item item : checklist.itens) {
                adicionarItem(item.pergunta, item.critico);
            }
            formularioCarregado = true;
            habilitarFormulario(true);
            estado.setText(R.string.checklist_aviso_publicacao);
            configurarEdicao();
        });
    }

    private EditText adicionarItem(String texto, boolean critico) {
        View linha = getLayoutInflater().inflate(R.layout.item_editar_checklist, itens, false);
        linha.setSaveFromParentEnabled(false);
        EditText pergunta = linha.findViewById(R.id.etPerguntaChecklist);
        pergunta.setText(texto);
        MaterialSwitch risco = linha.findViewById(R.id.swCriticoChecklist);
        risco.setChecked(critico);
        linha.findViewById(R.id.btnRemoverItem).setOnClickListener(view -> itens.removeView(linha));
        itens.addView(linha);
        return pergunta;
    }

    private void salvar(boolean publicado) {
        if (salvamento.tarefa != null || !formularioCarregado
                || !preenchido(nome, R.string.checklist_nome_obrigatorio)) {
            return;
        }
        if (publicado && (!preenchido(maquina, R.string.checklist_maquina_obrigatoria)
                || !preenchido(codigo, R.string.checklist_codigo_obrigatorio))) {
            return;
        }
        if (publicado && itens.getChildCount() == 0) {
            estado.setText(R.string.checklist_adicione_item);
            adicionar.requestFocus();
            return;
        }

        List<Checklist.Item> perguntas = new ArrayList<>();
        for (int i = 0; i < itens.getChildCount(); i++) {
            View linha = itens.getChildAt(i);
            EditText pergunta = linha.findViewById(R.id.etPerguntaChecklist);
            MaterialSwitch risco = linha.findViewById(R.id.swCriticoChecklist);
            if (publicado && !preenchido(pergunta, R.string.checklist_pergunta_obrigatoria)) {
                return;
            }
            perguntas.add(new Checklist.Item(texto(pergunta), risco.isChecked()));
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            estado.setText(R.string.checklist_sem_permissao);
            return;
        }
        String codigoMaquina = editandoPublicado ? codigo.getText().toString() : texto(codigo).toUpperCase(Locale.ROOT);
        Checklist checklist = new Checklist(checklistId, texto(nome), codigoMaquina,
                texto(maquina), texto(setor), perguntas, publicado, versao);
        salvamento.mensagemSucesso = editandoPublicado ? R.string.checklist_alteracoes_salvas
                : publicado ? R.string.checklist_publicado : R.string.checklist_rascunho_salvo;
        Boolean publicadoAntes = editandoExistente ? editandoPublicado : null;
        salvamento.tarefa = store.salvar(checklist, publicadoAntes, usuario.getUid());
        acompanharSalvamento();
    }

    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.checklist_excluir)
                .setMessage(R.string.checklist_confirmar_exclusao)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.checklist_excluir_acao, (dialogo, botao) -> excluirChecklist())
                .show();
    }

    private void excluirChecklist() {
        if (!editandoExistente || !formularioCarregado || salvamento.tarefa != null) {
            return;
        }
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            estado.setText(R.string.checklist_sem_permissao);
            return;
        }
        salvamento.mensagemSucesso = R.string.checklist_excluido;
        salvamento.tarefa = store.excluir(checklistId, versao, usuario.getUid());
        acompanharSalvamento();
    }

    private void acompanharSalvamento() {
        habilitarFormulario(false);
        boolean excluindo = salvamento.mensagemSucesso == R.string.checklist_excluido;
        estado.setText(excluindo ? R.string.checklist_excluindo : R.string.checklist_salvando);
        salvamento.tarefa.addOnCompleteListener(this, tarefa -> {
            salvamento.tarefa = null;
            if (tarefa.isSuccessful()) {
                Toast.makeText(this, salvamento.mensagemSucesso, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            habilitarFormulario(true);
            estado.setText(mensagemErro(tarefa.getException(), excluindo));
        });
    }

    private int mensagemErro(Exception erro, boolean excluindo) {
        if (erro instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code codigo = ((FirebaseFirestoreException) erro).getCode();
            if (codigo == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return R.string.checklist_conflito;
            }
            if (codigo == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    || codigo == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                return R.string.checklist_sem_permissao;
            }
        }
        return excluindo ? R.string.checklist_erro_excluir : R.string.checklist_erro_salvar;
    }

    private boolean preenchido(EditText campo, int mensagem) {
        if (!texto(campo).isEmpty()) {
            return true;
        }
        campo.setError(getString(mensagem));
        campo.requestFocus();
        return false;
    }

    private String texto(EditText campo) {
        return campo.getText().toString().trim();
    }

    private void habilitarFormulario(boolean habilitado) {
        nome.setEnabled(habilitado);
        maquina.setEnabled(habilitado);
        codigo.setEnabled(habilitado && !editandoPublicado);
        setor.setEnabled(habilitado);
        adicionar.setEnabled(habilitado);
        publicar.setEnabled(habilitado);
        salvarRascunho.setEnabled(habilitado);
        excluir.setEnabled(habilitado);
        for (int i = 0; i < itens.getChildCount(); i++) {
            View linha = itens.getChildAt(i);
            linha.findViewById(R.id.etPerguntaChecklist).setEnabled(habilitado);
            linha.findViewById(R.id.swCriticoChecklist).setEnabled(habilitado);
            linha.findViewById(R.id.btnRemoverItem).setEnabled(habilitado);
        }
    }

    public static class Salvamento extends ViewModel {
        private Task<Void> tarefa;
        private int mensagemSucesso;
    }
}
