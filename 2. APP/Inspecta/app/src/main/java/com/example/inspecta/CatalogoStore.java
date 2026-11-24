package com.example.inspecta;

import android.app.Activity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class CatalogoStore {
    private List<Checklist> modelos;
    private List<DocumentSnapshot> maquinas;

    void observar(Activity activity, Consumer<List<Checklist>> atualizou, Runnable falhou) {
        new ChecklistStore().observar(activity, true, lista -> {
            modelos = lista;
            combinar(atualizou);
        }, falhou);
        FirebaseFirestore.getInstance().collection("maquinas").addSnapshotListener(activity, (resultado, erro) -> {
            if (erro != null || resultado == null) { falhou.run(); return; }
            maquinas = resultado.getDocuments();
            combinar(atualizou);
        });
    }

    private void combinar(Consumer<List<Checklist>> atualizou) {
        if (modelos == null || maquinas == null) return;
        Map<String, Checklist> porModelo = new LinkedHashMap<>();
        Map<String, Checklist> resultado = new LinkedHashMap<>();
        // Preserva máquinas dos checklists criados antes do catálogo independente.
        for (Checklist modelo : modelos) {
            porModelo.put(modelo.id, modelo);
            if (!modelo.codigo.isEmpty()) resultado.putIfAbsent(Inspecao.idMaquina(modelo.codigo), modelo);
        }
        for (DocumentSnapshot maquina : maquinas) {
            Checklist modelo = porModelo.get(maquina.getString("checklistId"));
            String codigo = Inspecao.texto(maquina.get("codigo"));
            if (modelo == null) {
                modelo = new Checklist("", "Modelo indisponível", codigo,
                        Inspecao.texto(maquina.get("nome")), Inspecao.texto(maquina.get("setor")),
                        new ArrayList<>(), false, null);
            }
            resultado.put(Inspecao.idMaquina(codigo), modelo.paraMaquina(codigo,
                    Inspecao.texto(maquina.get("nome")), Inspecao.texto(maquina.get("setor"))));
        }
        List<Checklist> lista = new ArrayList<>(resultado.values());
        lista.sort(Comparator.comparing(c -> c.codigo));
        atualizou.accept(lista);
    }

    static Task<Checklist> resolver(Checklist modelo, String codigo) {
        if (codigo == null || codigo.isEmpty()) codigo = modelo.codigo;
        if (codigo.isEmpty()) return Tasks.forException(new IllegalArgumentException("Máquina não selecionada"));
        String selecionado = codigo;
        return FirebaseFirestore.getInstance().collection("maquinas").document(Inspecao.idMaquina(codigo))
                .get(Source.SERVER).continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot maquina = task.getResult();
                    if (!maquina.exists() && modelo.codigo.equals(selecionado)) return modelo;
                    if (!maquina.exists() || !modelo.id.equals(maquina.getString("checklistId"))) {
                        throw new IllegalArgumentException("Modelo alterado");
                    }
                    return modelo.paraMaquina(maquina.getString("codigo"), maquina.getString("nome"), maquina.getString("setor"));
                });
    }
}
