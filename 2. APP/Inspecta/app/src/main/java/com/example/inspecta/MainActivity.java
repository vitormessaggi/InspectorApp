package com.example.inspecta;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    static final String EXTRA_NOME_USUARIO = "com.example.inspecta.extra.NOME_USUARIO";

    private FirebaseAuth firebaseAuth;
    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailField = findViewById(R.id.etEmail);
        passwordField = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        forgotPassword = findViewById(R.id.tvForgotPassword);

        FirebaseApp firebaseApp = FirebaseApp.initializeApp(this);
        if (firebaseApp == null) {
            loginButton.setEnabled(false);
            forgotPassword.setEnabled(false);
            Toast.makeText(this, "Configuração do Firebase não encontrada. Adicione o google-services.json.", Toast.LENGTH_LONG).show();
            return;
        }

        findViewById(R.id.btnCadastro).setOnClickListener(v -> startActivity(new Intent(this, CadastroActivity.class)));
        firebaseAuth = FirebaseAuth.getInstance();
        loginButton.setOnClickListener(view -> signIn());
        forgotPassword.setOnClickListener(view -> sendPasswordReset());
        if (firebaseAuth.getCurrentUser() != null) {
            openScreenForCurrentUser();
            return;
        }
        loginButton.setOnClickListener(view -> signIn());
        forgotPassword.setOnClickListener(view -> sendPasswordReset());
    }

    private void signIn() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Informe seu e-mail");
            emailField.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Informe sua senha");
            passwordField.requestFocus();
            return;
        }

        setLoading(true);
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                openScreenForCurrentUser();
            } else {
                setLoading(false);
                Toast.makeText(this, getAuthError(task.getException()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openScreenForCurrentUser() {
        if (firebaseAuth.getCurrentUser() == null) {
            setLoading(false);
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        Perfil.buscar(userId)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        Toast.makeText(this,
                                "Não foi possível encontrar o perfil deste funcionário. Verifique o cadastro no Firebase.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot profile = task.getResult();
                    Object tagValue = profile.contains("perfil") ? profile.get("perfil") : profile.get("tag");
                    String tag = tagValue == null ? "" : tagValue.toString().trim();
                    Class<?> destination;
                    if ("Operador".equalsIgnoreCase(tag)) {
                        destination = MenuOpeActivity.class;
                    } else if ("Supervisor".equalsIgnoreCase(tag)) {
                        destination = VisaoTurnoActivity.class;
                    } else {
                        firebaseAuth.signOut();
                        Toast.makeText(this,
                                "Seu cadastro aguarda a definição do perfil pelo responsável.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(this, destination);
                    Object nome = profile.get("nome");
                    if (nome instanceof String) {
                        intent.putExtra(EXTRA_NOME_USUARIO, ((String) nome).trim());
                    }
                    startActivity(intent);
                    finish();
                });
    }

    private void sendPasswordReset() {
        String email = emailField.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Informe seu e-mail para recuperar a senha");
            emailField.requestFocus();
            return;
        }

        setLoading(true);
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
            setLoading(false);
            String message = task.isSuccessful()
                    ? "Se o e-mail estiver cadastrado, você receberá as instruções de recuperação."
                    : getAuthError(task.getException());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        forgotPassword.setEnabled(!loading);
        loginButton.setText(loading ? "Aguarde..." : "→ Entrar");
    }

    private String getAuthError(Exception exception) {
        if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException
                || exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            return "E-mail ou senha inválidos. Confira os dados e tente novamente.";
        }
        if (exception instanceof com.google.firebase.FirebaseNetworkException) {
            return "Sem conexão com a internet. Verifique sua rede e tente novamente.";
        }
        return "Não foi possível concluir a autenticação. Tente novamente.";
    }
}
