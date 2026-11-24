package com.example.inspecta;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

final class SituacaoAdapter extends RecyclerView.Adapter<SituacaoAdapter.Cartao> {
    static final class Registro {
        final Checklist maquina;
        final String situacao;
        final boolean confirmado;
        final Runnable abrir;
        Registro(Checklist maquina, String situacao, boolean confirmado, Runnable abrir) {
            this.maquina = maquina;
            this.situacao = situacao;
            this.confirmado = confirmado;
            this.abrir = abrir;
        }
    }
    private final List<Registro> registros = new ArrayList<>();

    void mostrar(List<Registro> novos) {
        registros.clear();
        registros.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Cartao onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Cartao(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maquina, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Cartao holder, int position) {
        Registro registro = registros.get(position);
        holder.codigo.setText(registro.maquina.codigo);
        holder.nome.setText(registro.maquina.maquina);
        holder.setor.setText(registro.maquina.setor.isEmpty() ? "Sem setor informado" : registro.maquina.setor);
        holder.situacao.setText(registro.situacao);
        int cor = Color.rgb(102, 102, 102);
        if (registro.situacao.equals("Bloqueada")) cor = Color.rgb(183, 28, 28);
        if (registro.situacao.equals("Liberada")) cor = Color.rgb(0, 121, 107);
        holder.situacao.setTextColor(cor);
        holder.detalhes.setText(registro.confirmado ? "Ver histórico e detalhes" : "Aguardando confirmação do servidor");
        holder.itemView.setOnClickListener(v -> registro.abrir.run());
    }

    @Override public int getItemCount() { return registros.size(); }

    static final class Cartao extends RecyclerView.ViewHolder {
        final TextView codigo, nome, setor, situacao, detalhes;
        Cartao(View view) {
            super(view);
            codigo = view.findViewById(R.id.tvCodigoMaquina);
            nome = view.findViewById(R.id.tvNomeMaquina);
            setor = view.findViewById(R.id.tvSetorMaquina);
            situacao = view.findViewById(R.id.tvNomeChecklist);
            detalhes = view.findViewById(R.id.tvDetalhesChecklist);
            view.findViewById(R.id.tvMaquinaBloqueada).setVisibility(View.GONE);
        }
    }
}
