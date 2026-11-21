package com.example.inspecta;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final class ChecklistStore {
    static final String CAMPO_EXCLUIDO_EM = "excluidoEm";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference checklists = firestore.collection("checklists");

    String novoId() {
        return checklists.document().getId();
    }

    Task<DocumentSnapshot> buscar(String id) {
        return checklists.document(id).get(Source.SERVER);
    }

    void observar(Activity activity, boolean somentePublicados,
                  Consumer<List<Checklist>> atualizou, Runnable falhou) {
        Query consulta = somentePublicados ? checklists.whereEqualTo("publicado", true) : checklists;
        consulta.addSnapshotListener(activity, (resultado, erro) -> {
            if (erro != null || resultado == null) {
                falhou.run();
                return;
            }
            List<Checklist> lista = new ArrayList<>();
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                if (!documento.contains(CAMPO_EXCLUIDO_EM)) {
                    lista.add(Checklist.ler(documento));
                }
            }
            lista.sort(Comparator.comparing((Checklist c) -> c.atualizadoEm,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(c -> c.id));
            atualizou.accept(lista);
        });
    }

    Task<Void> salvar(Checklist checklist, Boolean publicadoAntes, String uid) {
        DocumentReference referencia = checklists.document(checklist.id);
        DocumentReference versaoAnterior = Boolean.TRUE.equals(publicadoAntes)
                ? referencia.collection("versoes").document() : null;
        List<Map<String, Object>> itens = new ArrayList<>();
        List<Integer> indicesCriticos = new ArrayList<>();
        for (int indice = 0; indice < checklist.itens.size(); indice++) {
            Checklist.Item item = checklist.itens.get(indice);
            Map<String, Object> dadosItem = new HashMap<>();
            dadosItem.put("pergunta", item.pergunta);
            dadosItem.put("critico", item.critico);
            itens.add(dadosItem);
            if (item.critico) {
                indicesCriticos.add(indice);
            }
        }

        return firestore.runTransaction(transacao -> {
            DocumentSnapshot atual = transacao.get(referencia);
            if (atual.contains(CAMPO_EXCLUIDO_EM) || atual.exists() != (publicadoAntes != null) || (atual.exists()
                    && (!Objects.equals(atual.get("publicado"), publicadoAntes)
                    || !Objects.equals(atual.get("atualizadoEm"), checklist.atualizadoEm)))) {
                throw new FirebaseFirestoreException("Checklist alterado ou já salvo",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Map<String, Object> dados = new HashMap<>();
            dados.put("nome", checklist.nome);
            dados.put("codigoMaquina", checklist.codigo);
            dados.put("nomeMaquina", checklist.maquina);
            dados.put("setor", checklist.setor);
            dados.put("itens", itens);
            dados.put("indicesCriticos", indicesCriticos);
            dados.put("publicado", checklist.publicado);
            dados.put("criadoPor", atual.exists() ? atual.get("criadoPor") : uid);
            dados.put("atualizadoEm", FieldValue.serverTimestamp());
            if (versaoAnterior != null) {
                transacao.set(versaoAnterior, atual.getData());
                dados.put("versaoAnteriorId", versaoAnterior.getId());
            }
            transacao.set(referencia, dados);
            return null;
        });
    }

    Task<Void> excluir(String id, Timestamp versao, String uid) {
        DocumentReference referencia = checklists.document(id);
        return firestore.runTransaction(transacao -> {
            DocumentSnapshot atual = transacao.get(referencia);
            if (!atual.exists() || !Objects.equals(atual.get("atualizadoEm"), versao)) {
                throw new FirebaseFirestoreException("Checklist alterado ou indisponível",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (!atual.contains(CAMPO_EXCLUIDO_EM)) {
                transacao.update(referencia, Map.of("publicado", false,
                        CAMPO_EXCLUIDO_EM, FieldValue.serverTimestamp(), "excluidoPor", uid));
            }
            return null;
        });
    }
}
