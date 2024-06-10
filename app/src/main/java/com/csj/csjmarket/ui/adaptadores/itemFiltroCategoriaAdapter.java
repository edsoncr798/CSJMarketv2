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

import java.util.ArrayList;

public class itemFiltroCategoriaAdapter extends RecyclerView.Adapter<itemFiltroCategoriaAdapter.ViewHolderFiltroCategoria>{
    private ArrayList<filtroCategorias> filtroCategorias;

    public itemFiltroCategoriaAdapter(ArrayList<filtroCategorias> filtroCategorias) {
        this.filtroCategorias = filtroCategorias;
    }

    @NonNull
    @Override
    public itemFiltroCategoriaAdapter.ViewHolderFiltroCategoria onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria, parent, false);
        return new itemFiltroCategoriaAdapter.ViewHolderFiltroCategoria(view);
    }

    @Override
    public void onBindViewHolder(@NonNull itemFiltroCategoriaAdapter.ViewHolderFiltroCategoria holder, int position) {
        holder.txtCat.setText(filtroCategorias.get(position).getCategoria());
    }

    @Override
    public int getItemCount() {
        return filtroCategorias.size();
    }

    public class ViewHolderFiltroCategoria extends RecyclerView.ViewHolder {
        private CheckBox chkCat;
        private TextView txtCat;

        public ViewHolderFiltroCategoria(@NonNull View itemView) {
            super(itemView);
            chkCat = itemView.findViewById(R.id.cat_chkCat);
            txtCat = itemView.findViewById(R.id.cat_txtCat);
        }
    }
}
