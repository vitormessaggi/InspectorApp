package com.example.inspecta;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Inspecao {
    static final int FUNCIONANDO = 1;
    static final int NAO_FUNCIONANDO = 2;
    final String id;
    final String checklistNome;
    final String codigo;
    final String maquina;
    final String setor;
    final String operador;
    final String observacao;
    final String fotoUrl;
    final List<String> perguntasCriticas;
    final boolean pendente;
    final Timestamp enviadaEm;

    Inspecao(DocumentSnapshot documento) {
        id = documento.getId();
        checklistNome = texto(documento.get("checklistNome"));
        codigo = texto(documento.get("codigoMaquina"));
        maquina = texto(documento.get("nomeMaquina"));
        setor = texto(documento.get("setor"));
        operador = texto(documento.get("operadorNome"));
        observacao = texto(documento.get("observacao"));
        fotoUrl = texto(documento.get("fotoUrl"));
        pendente = Boolean.TRUE.equals(documento.get("pendente"));
        enviadaEm = documento.getTimestamp("enviadaEm");
        perguntasCriticas = new ArrayList<>();
        Object perguntas = documento.get("perguntasCriticas");
        if (perguntas instanceof List<?>) {
            for (Object pergunta : (List<?>) perguntas) {
                if (pergunta instanceof String) {
                    perguntasCriticas.add((String) pergunta);
                }
            }
        }
    }

    String primeiraFalha() {
        return perguntasCriticas.isEmpty() ? "" : perguntasCriticas.get(0);
    }

    static String idMaquina(String codigo) {
        return "maq-" + codigo.trim().toUpperCase(Locale.ROOT).replace("%", "%25").replace("/", "%2F");
    }

    static String texto(Object valor) {
        return valor instanceof String ? (String) valor : "";
    }
}
