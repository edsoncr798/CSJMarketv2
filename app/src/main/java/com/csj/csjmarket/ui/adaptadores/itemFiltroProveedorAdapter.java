package com.csj.csjmarket.ui.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.filtroCategorias;
import com.csj.csjmarket.modelos.filtroProveedor;

import java.util.ArrayList;

public class itemFiltroProveedorAdapter extends RecyclerView.Adapter<itemFiltroProveedorAdapter.ViewHolderFiltroProveedor>{
    private ArrayList<filtroProveedor> filtroProveedores;
    private ArrayList<filtroProveedor> proveedoresSeleccionados;

    public itemFiltroProveedorAdapter(ArrayList<filtroProveedor> filtroProveedores) {
        this.filtroProveedores = filtroProveedores;
        this.proveedoresSeleccionados = new ArrayList<>();
    }

    @NonNull
    @Override
    public itemFiltroProveedorAdapter.ViewHolderFiltroProveedor onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria, parent, false);
        return new itemFiltroProveedorAdapter.ViewHolderFiltroProveedor(view);
    }

    @Override
    public void onBindViewHolder(@NonNull itemFiltroProveedorAdapter.ViewHolderFiltroProveedor holder, int position) {
        holder.txtCat.setText(filtroProveedores.get(position).getNombre());
        holder.chkCat.setChecked(filtroProveedores.get(position).isEstado());
        if (filtroProveedores.get(position).isEstado()){
            proveedoresSeleccionados.add(filtroProveedores.get(position));
        }

        holder.chkCat.setOnCheckedChangeListener((compoundButton, b) -> {
            if (holder.chkCat.isChecked()){
                filtroProveedores.get(position).setEstado(true);
                proveedoresSeleccionados.add(filtroProveedores.get(position));
            }else{
                filtroProveedores.get(position).setEstado(false);
                proveedoresSeleccionados.remove(filtroProveedores.get(position));
            }

            if (onItemChangedListener != null){
                onItemChangedListener.onItemChanged(proveedoresSeleccionados);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filtroProveedores.size();
    }

    public class ViewHolderFiltroProveedor extends RecyclerView.ViewHolder {
        private CheckBox chkCat;
        private TextView txtCat;
        public ViewHolderFiltroProveedor(@NonNull View itemView) {
            super(itemView);
            chkCat = itemView.findViewById(R.id.cat_chkCat);
            txtCat = itemView.findViewById(R.id.cat_txtCat);
        }
    }

    public interface OnItemChangedListener {
        void onItemChanged(ArrayList<filtroProveedor> nuevoFiltro);
    }

    private OnItemChangedListener onItemChangedListener;

    public void setOnItemChangedListener(OnItemChangedListener listener) {
        this.onItemChangedListener = listener;
    }
}
