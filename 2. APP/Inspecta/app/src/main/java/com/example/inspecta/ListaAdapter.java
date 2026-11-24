package com.example.inspecta;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

final class ListaAdapter extends RecyclerView.Adapter<ListaAdapter.Linha> {
    static final class Registro {
        final String texto;
        final Runnable abrir;
        Registro(String texto, Runnable abrir) { this.texto = texto; this.abrir = abrir; }
    }
    private final List<Registro> registros = new ArrayList<>();

    void mostrar(List<Registro> novos) {
        registros.clear();
        registros.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView texto = new TextView(parent.getContext());
        texto.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        texto.setTextSize(17);
        texto.setTextColor(Color.rgb(20, 55, 50));
        texto.setPadding(16, 24, 16, 24);
        return new Linha(texto);
    }

    @Override public void onBindViewHolder(@NonNull Linha holder, int position) {
        Registro registro = registros.get(position);
        holder.texto.setText(registro.texto);
        holder.texto.setOnClickListener(registro.abrir == null ? null : view -> registro.abrir.run());
        holder.texto.setFocusable(registro.abrir != null);
    }
    @Override public int getItemCount() { return registros.size(); }
    static final class Linha extends RecyclerView.ViewHolder {
        final TextView texto;
        Linha(TextView texto) { super(texto); this.texto = texto; }
    }
}
