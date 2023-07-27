package com.csj.csjmarket.ui.adaptadores;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.LoginActivity;
import com.csj.csjmarket.MainActivity;
import com.csj.csjmarket.R;
import com.csj.csjmarket.VerProducto;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.ui.Ayudas;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ViewHolderProducto> {
    public ArrayList<Producto> productos;

    public ProductoAdapter(ArrayList<Producto> productos) {
        this.productos = productos;
    }

    @NonNull
    @Override
    public ProductoAdapter.ViewHolderProducto onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ViewHolderProducto(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoAdapter.ViewHolderProducto holder, int position) {
        Glide.with(holder.itemView.getContext())
                .load(holder.itemView.getContext().getString(R.string.connection) + "/imagenes/" + productos.get(position).getCodigo() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(holder.ivFotoProducto);
        holder.txtNombreProducto.setText(new Ayudas().capitalize(productos.get(position).getNombre()));
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        holder.txtPrecioProducto.setText("S/ " + decimalFormat.format(productos.get(position).getPrecioUnidadBase()) + " x " + productos.get(position).getUnidadBase());
        holder.itemView.setOnClickListener(view -> {
            holder.itemView.getContext().startActivity(new Intent(holder.itemView.getContext(), VerProducto.class)
                    .putExtra("producto", productos.get(position)));
        });
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    public class ViewHolderProducto extends RecyclerView.ViewHolder {
        private ImageView ivFotoProducto;
        private TextView txtNombreProducto, txtPrecioProducto;

            public ViewHolderProducto(@NonNull View itemView) {
                super(itemView);
                ivFotoProducto =itemView.findViewById(R.id.ip_fotoProducto);
                txtNombreProducto = itemView.findViewById(R.id.ip_nombreProducto);
                txtPrecioProducto = itemView.findViewById(R.id.ip_precioProducto);
        }
    }
}
