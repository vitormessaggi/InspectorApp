package com.example.inspecta;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Componentes compartilhados das telas de cadastro e consulta. */
final class Tela {
    final AppCompatActivity activity;
    final LinearLayout raiz;

    Tela(AppCompatActivity activity, String titulo, boolean formulario) {
        this.activity = activity;
        raiz = new LinearLayout(activity);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(dp(24), dp(24), dp(24), dp(16));
        raiz.setBackgroundColor(Color.rgb(248, 249, 250));
        if (formulario) {
            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(true);
            scroll.addView(raiz);
            activity.setContentView(scroll);
            ChecklistUi.aplicarInsets(scroll);
        } else {
            activity.setContentView(raiz);
            ChecklistUi.aplicarInsets(raiz);
        }
        TextView cabecalho = texto(titulo);
        cabecalho.setTextSize(26);
        cabecalho.setTypeface(null, android.graphics.Typeface.BOLD);
        cabecalho.setTextColor(Color.rgb(17, 17, 17));
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    TextView texto(String valor) {
        TextView view = new TextView(activity);
        view.setText(valor);
        view.setTextSize(16);
        view.setTextColor(Color.rgb(35, 45, 43));
        view.setPadding(8, 12, 8, 12);
        raiz.addView(view);
        return view;
    }

    EditText campo(int id, String dica, int inputType) {
        EditText view = new EditText(activity);
        view.setId(id);
        view.setHint(dica);
        view.setInputType(inputType);
        view.setSingleLine(true);
        view.setMinHeight(dp(56));
        view.setBackgroundResource(R.drawable.bg_search);
        view.setPadding(dp(16), dp(8), dp(16), dp(8));
        view.setTextColor(Color.rgb(17, 17, 17));
        view.setHintTextColor(Color.rgb(102, 102, 102));
        raiz.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    Button botao(String texto, int icone, View.OnClickListener acao) {
        Button button = new Button(activity);
        button.setText(texto);
        button.setTextColor(Color.rgb(0, 121, 107));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        button.setMinHeight(dp(56));
        if (icone != 0) button.setCompoundDrawablesWithIntrinsicBounds(icone, 0, 0, 0);
        button.setOnClickListener(acao);
        raiz.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    RecyclerView lista(RecyclerView.Adapter<?> adapter) {
        RecyclerView lista = new RecyclerView(activity);
        lista.setLayoutManager(new LinearLayoutManager(activity));
        lista.setAdapter(adapter);
        raiz.addView(lista, new LinearLayout.LayoutParams(-1, 0, 1));
        return lista;
    }
}
