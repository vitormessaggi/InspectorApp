package com.example.inspecta;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class VisaoTurnoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visao_turno);

        String nome = getIntent().getStringExtra(MainActivity.EXTRA_NOME_USUARIO);
        if (!TextUtils.isEmpty(nome)) {
            TextView saudacao = findViewById(R.id.tvSaudacao);
            saudacao.setText(getString(R.string.saudacao_com_nome, nome));
        }
    }
}
