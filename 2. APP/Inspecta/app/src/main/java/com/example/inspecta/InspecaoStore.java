package com.example.inspecta;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class InspecaoStore {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference inspecoes = firestore.collection("inspecoes");
    private final CollectionReference bloqueios = firestore.collection("bloqueiosMaquinas");

    String novoId() {
        return inspecoes.document().getId();
    }

    void observarBloqueios(Activity activity, Consumer<Set<String>> atualizou, Runnable falhou) {
        bloqueios.addSnapshotListener(activity, (resultado, erro) -> {
            if (erro != null || resultado == null) {
                falhou.run();
                return;
            }
            Set<String> ids = new HashSet<>();
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                ids.add(documento.getId());
            }
            atualizou.accept(ids);
        });
    }

    void observarBloqueio(Activity activity, String codigo, BiConsumer<Boolean, Boolean> atualizou, Runnable falhou) {
        bloqueios.document(Inspecao.idMaquina(codigo))
                .addSnapshotListener(activity, MetadataChanges.INCLUDE, (documento, erro) -> {
                    if (erro != null || documento == null) {
                        falhou.run();
                        return;
                    }
                    atualizou.accept(documento.exists(), !documento.getMetadata().isFromCache());
                });
    }

    void observarPendencias(Activity activity, BiConsumer<List<Inspecao>, Boolean> atualizou, Runnable falhou) {
        inspecoes.whereEqualTo("pendente", true).addSnapshotListener(activity, MetadataChanges.INCLUDE, (resultado, erro) -> {
            if (erro != null || resultado == null) {
                falhou.run();
                return;
            }
            List<Inspecao> pendentes = new ArrayList<>();
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                pendentes.add(new Inspecao(documento));
            }
            pendentes.sort(Comparator.comparing((Inspecao i) -> i.enviadaEm,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(i -> i.id));
            atualizou.accept(pendentes, !resultado.getMetadata().isFromCache());
        });
    }

    ListenerRegistration observarTotal(Activity activity, Date inicio, Date fim,
                                        Consumer<Integer> atualizou, Runnable falhou) {
        return inspecoes.whereGreaterThanOrEqualTo("enviadaEm", inicio).whereLessThan("enviadaEm", fim)
                .addSnapshotListener(activity, MetadataChanges.INCLUDE, (resultado, erro) -> {
                    if (erro != null || resultado == null || resultado.getMetadata().isFromCache()) {
                        falhou.run();
                    } else {
                        atualizou.accept(resultado.size());
                    }
                });
    }

    void observarInspecao(Activity activity, String id, BiConsumer<Inspecao, Boolean> atualizou, Runnable falhou) {
        inspecoes.document(id).addSnapshotListener(activity, MetadataChanges.INCLUDE, (documento, erro) -> {
            if (erro != null || documento == null) {
                falhou.run();
                return;
            }
            atualizou.accept(documento.exists() ? new Inspecao(documento) : null,
                    !documento.getMetadata().isFromCache());
        });
    }

    Task<Void> enviar(String id, String uid, Checklist exibido, int[] respostas, String observacao) {
        DocumentReference registro = inspecoes.document(id);
        DocumentReference formulario = firestore.collection("checklists").document(exibido.id);
        DocumentReference perfil = firestore.collection("users").document(uid);

        return firestore.runTransaction(transacao -> {
            DocumentSnapshot existente = transacao.get(registro);
            if (existente.exists()) {
                if (!uid.equals(existente.get("operadorUid"))) {
                    throw precondicao("Inspeção pertence a outro operador");
                }
                return null;
            }

            Checklist checklist = Checklist.paraInspecao(transacao.get(formulario));
            if (checklist == null || respostas.length != checklist.itens.size()
                    || !Objects.equals(checklist.atualizadoEm, exibido.atualizadoEm)) {
                throw precondicao("Checklist alterado");
            }
            DocumentReference bloqueio = bloqueios.document(Inspecao.idMaquina(checklist.codigo));
            if (transacao.get(bloqueio).exists()) {
                throw new FirebaseFirestoreException("Máquina inoperante",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }
            String operador = Inspecao.texto(transacao.get(perfil).get("nome"));
            List<Integer> falhas = new ArrayList<>();
            List<Integer> criticas = new ArrayList<>();
            List<String> perguntasCriticas = new ArrayList<>();
            for (int indice = 0; indice < respostas.length; indice++) {
                Checklist.Item item = checklist.itens.get(indice);
                Checklist.Item itemExibido = exibido.itens.get(indice);
                if (!item.pergunta.equals(itemExibido.pergunta) || item.critico != itemExibido.critico) {
                    throw precondicao("Item do checklist alterado");
                }
                if (respostas[indice] != Inspecao.FUNCIONANDO && respostas[indice] != Inspecao.NAO_FUNCIONANDO) {
                    throw precondicao("Inspeção incompleta");
                }
                if (respostas[indice] == Inspecao.NAO_FUNCIONANDO) {
                    falhas.add(indice);
                    if (item.critico) {
                        criticas.add(indice);
                        perguntasCriticas.add(item.pergunta);
                    }
                }
            }

            boolean pendente = !criticas.isEmpty();
            Map<String, Object> dados = new HashMap<>();
            dados.put("checklistId", checklist.id);
            if (checklist.atualizadoEm != null) {
                dados.put("checklistAtualizadoEm", checklist.atualizadoEm);
            }
            dados.put("checklistNome", checklist.nome);
            dados.put("codigoMaquina", checklist.codigo);
            dados.put("nomeMaquina", checklist.maquina);
            dados.put("setor", checklist.setor);
            dados.put("operadorUid", uid);
            dados.put("operadorNome", operador);
            dados.put("observacao", observacao);
            dados.put("falhas", falhas);
            dados.put("falhasCriticas", criticas);
            dados.put("perguntasCriticas", perguntasCriticas);
            dados.put("pendente", pendente);
            dados.put("enviadaEm", FieldValue.serverTimestamp());
            transacao.set(registro, dados);
            if (pendente) {
                transacao.set(bloqueio, Map.of("inspecaoId", id));
            }
            return null;
        });
    }

    Task<Void> liberar(Inspecao inspecao, String uid, String justificativa) {
        DocumentReference registro = inspecoes.document(inspecao.id);
        DocumentReference bloqueio = bloqueios.document(Inspecao.idMaquina(inspecao.codigo));

        return firestore.runTransaction(transacao -> {
            DocumentSnapshot atual = transacao.get(registro);
            if (!atual.exists() || !Boolean.TRUE.equals(atual.get("pendente"))
                    || !inspecao.id.equals(transacao.get(bloqueio).get("inspecaoId"))) {
                throw precondicao("Ocorrência já revisada");
            }
            transacao.update(registro, Map.of("pendente", false, "liberadoPor", uid,
                    "liberadaEm", FieldValue.serverTimestamp(), "justificativaLiberacao", justificativa,
                    "correcaoVerificada", true, "segurancaVerificada", true));
            transacao.delete(bloqueio);
            return null;
        });
    }

    private FirebaseFirestoreException precondicao(String mensagem) {
        return new FirebaseFirestoreException(mensagem, FirebaseFirestoreException.Code.FAILED_PRECONDITION);
    }
}
