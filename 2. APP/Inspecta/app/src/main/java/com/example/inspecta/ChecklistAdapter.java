package com.example.inspecta;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

final class ChecklistAdapter extends ListAdapter<Checklist, ChecklistAdapter.Cartao> {
    private final Consumer<Checklist> abrir;
    private Set<String> bloqueios = Collections.emptySet();

    ChecklistAdapter(Consumer<Checklist> abrir) {
        super(new DiffUtil.ItemCallback<Checklist>() {
            @Override
            public boolean areItemsTheSame(@NonNull Checklist antigo, @NonNull Checklist novo) {
                return antigo.id.equals(novo.id) && antigo.codigo.equals(novo.codigo);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Checklist antigo, @NonNull Checklist novo) {
                return antigo == novo;
            }
        });
        this.abrir = abrir;
    }

    void atualizarBloqueios(Set<String> novosBloqueios) {
        Set<String> anteriores = bloqueios;
        bloqueios = novosBloqueios;
        for (int i = 0; i < getCurrentList().size(); i++) {
            String maquinaId = Inspecao.idMaquina(getItem(i).codigo);
            if (anteriores.contains(maquinaId) != bloqueios.contains(maquinaId)) {
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public Cartao onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Cartao(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maquina, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Cartao cartao, int position) {
        Checklist checklist = getItem(position);
        int criticos = 0;
        for (Checklist.Item item : checklist.itens) {
            if (item.critico) {
                criticos++;
            }
        }
        cartao.codigo.setText(checklist.codigo);
        cartao.codigo.setVisibility(checklist.codigo.isEmpty() ? View.GONE : View.VISIBLE);
        cartao.maquina.setText(checklist.maquina);
        cartao.maquina.setVisibility(checklist.maquina.isEmpty() ? View.GONE : View.VISIBLE);
        cartao.setor.setText(checklist.setor);
        cartao.setor.setVisibility(checklist.setor.isEmpty() ? View.GONE : View.VISIBLE);
        cartao.nome.setText(checklist.nome);
        cartao.bloqueio.setVisibility(bloqueios.contains(Inspecao.idMaquina(checklist.codigo))
                ? View.VISIBLE : View.GONE);
        cartao.detalhes.setText(cartao.itemView.getResources().getQuantityString(
                R.plurals.checklist_itens_resumo, checklist.itens.size(), checklist.itens.size(), criticos));
        cartao.itemView.setOnClickListener(view -> abrir.accept(checklist));
    }

    static final class Cartao extends RecyclerView.ViewHolder {
        final TextView codigo;
        final TextView maquina;
        final TextView setor;
        final TextView nome;
        final TextView detalhes;
        final View bloqueio;

        Cartao(View view) {
            super(view);
            codigo = view.findViewById(R.id.tvCodigoMaquina);
            maquina = view.findViewById(R.id.tvNomeMaquina);
            setor = view.findViewById(R.id.tvSetorMaquina);
            nome = view.findViewById(R.id.tvNomeChecklist);
            detalhes = view.findViewById(R.id.tvDetalhesChecklist);
            bloqueio = view.findViewById(R.id.tvMaquinaBloqueada);
        }
    }
}
