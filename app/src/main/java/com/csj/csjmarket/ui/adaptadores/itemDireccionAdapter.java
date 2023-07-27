package com.csj.csjmarket.ui.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.Direccion;

import java.util.ArrayList;

public class itemDireccionAdapter extends RecyclerView.Adapter<itemDireccionAdapter.ViewHolderDireccion>{
    private ArrayList<Direccion> direcciones;

    public itemDireccionAdapter(ArrayList<Direccion> direcciones) {
        this.direcciones = direcciones;
    }

    @NonNull
    @Override
    public itemDireccionAdapter.ViewHolderDireccion onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_direccion, parent, false);
        return new itemDireccionAdapter.ViewHolderDireccion(view);
    }

    @Override
    public void onBindViewHolder(@NonNull itemDireccionAdapter.ViewHolderDireccion holder, int position) {
        holder.txtDireccion.setText(direcciones.get(position).getDescripcion());
        holder.btnElejir.setOnClickListener(view -> {
            if (onItemChangedListener != null) {
                onItemChangedListener.onItemChanged(direcciones.get(holder.getAdapterPosition()).getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return direcciones.size();
    }

    public class ViewHolderDireccion extends RecyclerView.ViewHolder {
        private TextView txtDireccion;
        private Button btnElejir;
        public ViewHolderDireccion(@NonNull View itemView) {
            super(itemView);
            txtDireccion = itemView.findViewById(R.id.idireccion_txtDescripcion);
            btnElejir = itemView.findViewById(R.id.idireccion_btnElegir);
        }
    }

    public interface OnItemChangedListener {
        void onItemChanged(int idDireccion);
    }

    private OnItemChangedListener onItemChangedListener;

    public void setOnItemChangedListener(itemDireccionAdapter.OnItemChangedListener listener) {
        this.onItemChangedListener = listener;
    }
}
