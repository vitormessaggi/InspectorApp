package com.example.inspecta;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class CadastroActivity extends AppCompatActivity {
    public static class Estado extends ViewModel { Task<Void> tarefa; boolean concluido; }
    private Estado estado;
    private EditText nome, email, senha;
    private TextView mensagem;
    private Button salvar;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        estado = new ViewModelProvider(this).get(Estado.class);
        Tela tela = new Tela(this, "Criar conta", true);
        nome = tela.campo(1101, "Nome completo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        email = tela.campo(1102, "E-mail", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        senha = tela.campo(1103, "Senha (mínimo 6 caracteres)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tela.texto("O responsável define seu perfil de acesso após o cadastro.");
        mensagem = tela.texto("");
        salvar = tela.botao("Criar conta", android.R.drawable.ic_menu_save, view -> cadastrar());
        tela.botao("Voltar", android.R.drawable.ic_menu_revert, view -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        if (estado.tarefa != null) acompanhar();
        if (estado.concluido) mostrarConcluido();
    }

    private void cadastrar() {
        String nomeTexto = nome.getText().toString().trim();
        String emailTexto = email.getText().toString().trim();
        String senhaTexto = senha.getText().toString();
        if (nomeTexto.isEmpty()) { nome.setError("Informe seu nome"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailTexto).matches()) { email.setError("Informe um e-mail válido"); return; }
        if (senhaTexto.length() < 6) { senha.setError("Use pelo menos 6 caracteres"); return; }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && !emailTexto.equalsIgnoreCase(auth.getCurrentUser().getEmail())) auth.signOut();
        Task<?> autenticacao = auth.getCurrentUser() == null
                ? auth.createUserWithEmailAndPassword(emailTexto, senhaTexto) : Tasks.forResult(null);
        estado.tarefa = autenticacao.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            String uid = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            com.google.firebase.firestore.DocumentReference ref = db.collection("usuarios").document(uid);
            return db.runTransaction(transaction -> {
                if (!transaction.get(ref).exists()) transaction.set(ref, Map.of("nome", nomeTexto, "perfil", "Pendente"));
                return null;
            });
        });
        acompanhar();
    }

    private void mostrarConcluido() {
        mensagem.setText("Conta criada. Aguarde a definição do perfil pelo responsável e entre pela tela de login.");
        salvar.setEnabled(false);
    }

    private void acompanhar() {
        salvar.setEnabled(false);
        mensagem.setText("Salvando cadastro…");
        estado.tarefa.addOnCompleteListener(this, task -> {
            estado.tarefa = null;
            salvar.setEnabled(true);
            if (task.isSuccessful()) {
                FirebaseAuth.getInstance().signOut();
                estado.concluido = true;
                mostrarConcluido();
            } else {
                mensagem.setText("Não foi possível concluir. Confira a conexão e o e-mail. Se a conta já existe, use a recuperação de senha.");
            }
        });
    }
}
