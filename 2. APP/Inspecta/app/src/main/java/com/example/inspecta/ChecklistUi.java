package com.example.inspecta;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.time.LocalTime;

final class ChecklistUi {
    private ChecklistUi() {
    }

    static void aplicarInsets(View raiz) {
        int esquerda = raiz.getPaddingLeft();
        int topo = raiz.getPaddingTop();
        int direita = raiz.getPaddingRight();
        int base = raiz.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(esquerda + insets.left, topo + insets.top, direita + insets.right, base + insets.bottom);
            return windowInsets;
        });
    }

    static String dataInspecao(Timestamp timestamp) {
        return timestamp == null ? "" : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(timestamp.toDate());
    }

    static void atualizarSaudacao(TextView campo, String nome) {
        Context context = campo.getContext();
        int hora = LocalTime.now().getHour();
        int periodo = hora < 5 || hora >= 18 ? R.string.saudacao_noite
                : hora < 12 ? R.string.saudacao_manha : R.string.saudacao_tarde;
        String saudacao = context.getString(periodo);
        campo.setText(TextUtils.isEmpty(nome) ? context.getString(R.string.saudacao_sem_nome, saudacao)
                : context.getString(R.string.saudacao_com_nome, saudacao, nome));
    }

    static void mostrarItens(Context context, Checklist checklist, Runnable editar) {
        StringBuilder texto = new StringBuilder();
        for (int indice = 0; indice < checklist.itens.size(); indice++) {
            if (texto.length() > 0) {
                texto.append("\n\n");
            }
            Checklist.Item item = checklist.itens.get(indice);
            texto.append(context.getString(R.string.checklist_item_detalhe, indice + 1, item.pergunta,
                    context.getString(item.critico ? R.string.checklist_critico : R.string.checklist_nao_critico)));
        }
        new AlertDialog.Builder(context)
                .setTitle(checklist.nome)
                .setMessage(texto.length() == 0 ? context.getString(R.string.checklist_sem_itens) : texto)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.checklist_editar, (dialogo, botao) -> editar.run())
                .show();
    }

    static void observarBusca(EditText campo, Runnable mudou) {
        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence texto, int inicio, int quantidade, int depois) {
            }

            @Override
            public void onTextChanged(CharSequence texto, int inicio, int antes, int quantidade) {
                mudou.run();
            }

            @Override
            public void afterTextChanged(Editable texto) {
            }
        });
    }
}
