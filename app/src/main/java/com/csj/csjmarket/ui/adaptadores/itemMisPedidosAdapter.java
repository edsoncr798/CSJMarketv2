package com.csj.csjmarket.ui.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.MisPedidos;

import java.util.ArrayList;

public class itemMisPedidosAdapter extends RecyclerView.Adapter<itemMisPedidosAdapter.ViewHolderMisPedidos> {
    private ArrayList<MisPedidos> misPedidos;

    public itemMisPedidosAdapter(ArrayList<MisPedidos> misPedidos) {
        this.misPedidos = misPedidos;
    }

    @NonNull
    @Override
    public itemMisPedidosAdapter.ViewHolderMisPedidos onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mis_pedidos, parent, false);
        return new itemMisPedidosAdapter.ViewHolderMisPedidos(view);
    }

    @Override
    public void onBindViewHolder(@NonNull itemMisPedidosAdapter.ViewHolderMisPedidos holder, int position) {
        holder.txtNumCp.setText(misPedidos.get(position).getNumCp());
        holder.txtFecha.setText(misPedidos.get(position).getFechaEmision());
        holder.txtEstadoEntrega.setText(misPedidos.get(position).getEstadoEntrega());
    }

    @Override
    public int getItemCount() {
        return misPedidos.size();
    }

    public class ViewHolderMisPedidos extends RecyclerView.ViewHolder {
        private TextView txtNumCp, txtFecha, txtEstadoEntrega;
        public ViewHolderMisPedidos(@NonNull View itemView) {
            super(itemView);
            txtNumCp = itemView.findViewById(R.id.misPedidos_txtNumCp);
            txtFecha = itemView.findViewById(R.id.misPedidos_txtFecha);
            txtEstadoEntrega = itemView.findViewById(R.id.misPedidos_txtEstadoEntrega);
        }
    }
}
