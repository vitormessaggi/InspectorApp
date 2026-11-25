package com.example.inspecta;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CadastrarMaquinaActivity extends AppCompatActivity {
    public static class Gravacao extends androidx.lifecycle.ViewModel {
        com.google.android.gms.tasks.Task<String> tarefa;
    }
    private Gravacao gravacao;
    private List<Checklist> modelos = new ArrayList<>();
    private EditText codigo, nome, setor;
    private Spinner modelo;
    private String selecionado;
    private TextView mensagem;
    private Button salvar;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        gravacao = new androidx.lifecycle.ViewModelProvider(this).get(Gravacao.class);
        selecionado = bundle == null ? null : bundle.getString("modeloId");
        Tela tela = new Tela(this, "Cadastrar máquina", true);
        codigo = tela.campo(1201, "Código único da máquina", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        nome = tela.campo(1202, "Nome da máquina", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        setor = tela.campo(1203, "Setor", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        tela.texto("Modelo por tipo de máquina");
        modelo = new Spinner(this);
        tela.raiz.addView(modelo);
        mensagem = tela.texto("Carregando modelos publicados…");
        salvar = tela.botao("Salvar máquina", android.R.drawable.ic_menu_save, view -> salvar());
        salvar.setEnabled(false);
        tela.botao("Voltar", android.R.drawable.ic_menu_revert, view -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        if (gravacao.tarefa != null) acompanhar();
        new ChecklistStore().observar(this, true, lista -> {
            if (!modelos.isEmpty() && modelo.getSelectedItemPosition() >= 0) selecionado = modelos.get(modelo.getSelectedItemPosition()).id;
            modelos = lista;
            List<String> nomes = new ArrayList<>();
            int posicao = 0;
            for (int i = 0; i < lista.size(); i++) {
                Checklist item = lista.get(i);
                nomes.add((item.tipo.isEmpty() ? item.nome : item.tipo) + " — " + item.nome);
                if (item.id.equals(selecionado)) posicao = i;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nomes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modelo.setAdapter(adapter);
            modelo.setSelection(posicao);
            salvar.setEnabled(!lista.isEmpty() && gravacao.tarefa == null);
            mensagem.setText(lista.isEmpty() ? "Publique um modelo de checklist para cadastrar a máquina." : "");
        }, () -> mensagem.setText("Não foi possível carregar os modelos."));
    }

    @Override protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (!modelos.isEmpty() && modelo.getSelectedItemPosition() >= 0) bundle.putString("modeloId", modelos.get(modelo.getSelectedItemPosition()).id);
    }

    private void salvar() {
        String codigoTexto = codigo.getText().toString().trim().toUpperCase(Locale.ROOT);
        String nomeTexto = nome.getText().toString().trim();
        String setorTexto = setor.getText().toString().trim();
        if (codigoTexto.isEmpty() || codigoTexto.length() > 80) { codigo.setError("Informe um código com até 80 caracteres"); return; }
        if (nomeTexto.isEmpty()) { nome.setError("Informe o nome"); return; }
        if (setorTexto.isEmpty()) { setor.setError("Informe o setor"); return; }
        if (modelos.isEmpty()) return;
        Checklist escolhido = modelos.get(modelo.getSelectedItemPosition());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.DocumentReference ref = db.collection("maquinas").document(Inspecao.idMaquina(codigoTexto));
        salvar.setEnabled(false);
        gravacao.tarefa = db.runTransaction(transaction -> {
            if (transaction.get(ref).exists()) throw new FirebaseFirestoreException("Código já cadastrado", FirebaseFirestoreException.Code.ALREADY_EXISTS);
            transaction.set(ref, Map.of("codigo", codigoTexto, "nome", nomeTexto, "setor", setorTexto,
                    "tipo", escolhido.tipo.isEmpty() ? escolhido.nome : escolhido.tipo, "checklistId", escolhido.id));
            return codigoTexto;
        });
        acompanhar();
    }

    private void acompanhar() {
        salvar.setEnabled(false);
        gravacao.tarefa.addOnCompleteListener(this, task -> {
            gravacao.tarefa = null;
            salvar.setEnabled(true);
            if (task.isSuccessful()) {
                startActivity(QrMaquinaActivity.paraMaquina(this, task.getResult()));
                finish();
            } else {
                mensagem.setText("Não foi possível salvar. Confira a conexão, a permissão e se o código já está cadastrado.");
            }
        });
    }
}
