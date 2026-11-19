package com.example.inspecta;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Checklist {
    final String id;
    final String nome;
    final String codigo;
    final String maquina;
    final String setor;
    final List<Item> itens;
    final boolean publicado;
    final Timestamp atualizadoEm;

    Checklist(String id, String nome, String codigo, String maquina, String setor,
              List<Item> itens, boolean publicado, Timestamp atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.codigo = codigo;
        this.maquina = maquina;
        this.setor = setor;
        this.itens = itens;
        this.publicado = publicado;
        this.atualizadoEm = atualizadoEm;
    }

    static Checklist ler(DocumentSnapshot documento) {
        List<Item> itens = new ArrayList<>();
        Object dadosItens = documento.get("itens");
        if (dadosItens instanceof List<?>) {
            for (Object dado : (List<?>) dadosItens) {
                if (dado instanceof Map<?, ?>) {
                    Map<?, ?> item = (Map<?, ?>) dado;
                    if (item.get("critico") instanceof Boolean) {
                        itens.add(new Item(texto(item.get("pergunta")), Boolean.TRUE.equals(item.get("critico"))));
                    }
                }
            }
        }
        Object data = documento.get("atualizadoEm");
        return new Checklist(documento.getId(), texto(documento.get("nome")),
                texto(documento.get("codigoMaquina")), texto(documento.get("nomeMaquina")),
                texto(documento.get("setor")), itens, Boolean.TRUE.equals(documento.get("publicado")),
                data instanceof Timestamp ? (Timestamp) data : null);
    }

    static Checklist paraInspecao(DocumentSnapshot documento) {
        Checklist checklist = ler(documento);
        Object dadosItens = documento.get("itens");
        if (!checklist.publicado || checklist.codigo.trim().isEmpty() || checklist.itens.isEmpty()
                || !(dadosItens instanceof List<?>) || ((List<?>) dadosItens).size() != checklist.itens.size()) {
            return null;
        }
        for (Item item : checklist.itens) {
            if (item.pergunta.trim().isEmpty()) {
                return null;
            }
        }
        return checklist;
    }

    boolean corresponde(String busca) {
        return nome.toLowerCase(Locale.ROOT).contains(busca)
                || codigo.toLowerCase(Locale.ROOT).contains(busca)
                || maquina.toLowerCase(Locale.ROOT).contains(busca);
    }

    private static String texto(Object valor) {
        return valor instanceof String ? (String) valor : "";
    }

    static final class Item {
        final String pergunta;
        final boolean critico;

        Item(String pergunta, boolean critico) {
            this.pergunta = pergunta;
            this.critico = critico;
        }
    }
}
