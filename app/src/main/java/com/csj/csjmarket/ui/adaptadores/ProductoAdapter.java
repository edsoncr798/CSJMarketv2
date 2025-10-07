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
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.ui.Ayudas;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ViewHolderProducto> {
    public ArrayList<Producto> productos;
    private Map<Integer, StockInfo> stockMap;

    public ProductoAdapter(ArrayList<Producto> productos) {
        this.productos = productos;
    }

    public ProductoAdapter(ArrayList<Producto> productos, Map<Integer, StockInfo> stockMap) {
        this.productos = productos;
        this.stockMap = stockMap;
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

        StockInfo info = null;
        if (stockMap != null) {
            info = stockMap.get(productos.get(position).getId());
        }
        if (info != null) {
            int disponibleUnidades = Math.max(info.getStockFisico() - info.getStockPorEntregar(), 0);
            boolean disponible = disponibleUnidades > 0;
            // Badge: solo 'Disponible' / 'No disponible'
            holder.txtDisponibilidad.setText(disponible ? "Disponible" : "No disponible");
            holder.txtDisponibilidad.setTextColor(holder.itemView.getResources().getColor(
                    disponible ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
        } else {
            holder.txtDisponibilidad.setText("No disponible");
            holder.txtDisponibilidad.setTextColor(holder.itemView.getResources().getColor(android.R.color.darker_gray));
        }

        final StockInfo finalInfo = info;
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(holder.itemView.getContext(), VerProducto.class)
                    .putExtra("producto", productos.get(position));
            if (finalInfo != null) {
                intent.putExtra("stockInfo", finalInfo);
            }
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    public class ViewHolderProducto extends RecyclerView.ViewHolder {
        private ImageView ivFotoProducto;
        private TextView txtNombreProducto, txtPrecioProducto, txtDisponibilidad;

            public ViewHolderProducto(@NonNull View itemView) {
                super(itemView);
                ivFotoProducto = itemView.findViewById(R.id.ip_fotoProducto);
                txtNombreProducto = itemView.findViewById(R.id.ip_nombreProducto);
                txtPrecioProducto = itemView.findViewById(R.id.ip_precioProducto);
                txtDisponibilidad = itemView.findViewById(R.id.ip_disponibilidadProducto);
        }
    }
}
