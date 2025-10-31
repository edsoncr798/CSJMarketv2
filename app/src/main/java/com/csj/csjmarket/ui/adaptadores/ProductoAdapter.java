package com.csj.csjmarket.ui.adaptadores;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.R;
import com.csj.csjmarket.VerProducto;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.ui.Ayudas;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ProductoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public ArrayList<Producto> productos;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_LOADING = 2;
    private boolean mostrarLoaderAlFinal = false;

    public ProductoAdapter(ArrayList<Producto> productos) {
        this.productos = productos;
    }

    public void setMostrarLoaderAlFinal(boolean mostrar) {
        if (this.mostrarLoaderAlFinal == mostrar) return;
        this.mostrarLoaderAlFinal = mostrar;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mostrarLoaderAlFinal && position == productos.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new ViewHolderLoading(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ViewHolderProducto(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderLoading) {
            return;
        }
        ViewHolderProducto vh = (ViewHolderProducto) holder;
        Glide.with(vh.itemView.getContext())
                .load(vh.itemView.getContext().getString(R.string.connection) + "/imagenes/" + productos.get(position).getCodigo() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(vh.ivFotoProducto);
        vh.txtNombreProducto.setText(new Ayudas().capitalize(productos.get(position).getNombre()));
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        Double precioActual = productos.get(position).getPrecioUnidadBase();
        Double precioAnterior = productos.get(position).getPrecioUnidadAntes();
        String observacion = productos.get(position).getObservacion();

        boolean tieneDescuento = precioAnterior != null && precioAnterior > 0;
        if (tieneDescuento && precioActual != null && precioActual > 0) {
            // Mostrar contenedor de oferta y ocultar precio clásico
            vh.contenedorPreciosOferta.setVisibility(View.VISIBLE);
            vh.txtPrecioProducto.setVisibility(View.GONE);

            vh.txtPrecioAnterior.setText("S/ " + decimalFormat.format(precioAnterior) + " x " + productos.get(position).getUnidadBase());
            vh.txtPrecioAnterior.setTextColor(0xFF9E9E9E);
            vh.txtPrecioAnterior.setPaintFlags(vh.txtPrecioAnterior.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            vh.txtPrecioActual.setText("S/ " + decimalFormat.format(precioActual) + " x " + productos.get(position).getUnidadBase());
            vh.txtPrecioActual.setTextColor(vh.itemView.getResources().getColor(android.R.color.holo_red_dark));

            // Calcular porcentaje de descuento
            int porcentaje = 0;
            if (precioAnterior > 0) {
                porcentaje = (int) Math.round(((precioAnterior - precioActual) * 100.0) / precioAnterior);
                if (porcentaje < 0) porcentaje = 0;
            }
            vh.txtDescuentoBadge.setVisibility(View.GONE);

            // Actualizar ícono de descuento en la imagen
            if (porcentaje > 0) {
                vh.txtDescuentoIcon.setText("-" + porcentaje + "%");
            } else {
                vh.txtDescuentoIcon.setText("OFERTA");
            }
            vh.txtDescuentoIcon.setTextColor(vh.txtDescuentoBadge.getCurrentTextColor());
            vh.txtDescuentoIcon.setVisibility(View.VISIBLE);
        } else {
            // Ocultar contenedor de oferta y mostrar precio clásico u observación
            vh.contenedorPreciosOferta.setVisibility(View.GONE);
            vh.txtPrecioProducto.setVisibility(View.VISIBLE);

            if (observacion != null && !observacion.trim().isEmpty()) {
                vh.txtPrecioProducto.setText(observacion.trim());
            } else {
                String unidad = productos.get(position).getUnidadBase();
                vh.txtPrecioProducto.setText("S/ " + decimalFormat.format(precioActual != null ? precioActual : 0.0) + " x " + unidad);
            }

            // Ocultar el ícono de descuento y limpiar texto
            vh.txtDescuentoIcon.setText("");
            vh.txtDescuentoIcon.setVisibility(View.GONE);
        }

        int disponibleUnidades = productos.get(position).getStockDisponible();
        boolean disponible = disponibleUnidades > 0;
        vh.txtDisponibilidad.setText(disponible ? "Disponible" : "No disponible");
        vh.txtDisponibilidad.setTextColor(vh.itemView.getResources().getColor(
                disponible ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
        ));

        boolean tieneBonificacion = productos.get(position).isTieneBonificacion();
        vh.txtBonusBadge.setVisibility(tieneBonificacion ? View.VISIBLE : View.GONE);
        if (tieneBonificacion) {
            vh.txtBonusBadge.setText("PROMO");
        }

        // Mostrar icono de descuento en la imagen si hay oferta
        vh.txtDescuentoIcon.setVisibility(tieneDescuento ? View.VISIBLE : View.GONE);

        vh.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(vh.itemView.getContext(), VerProducto.class)
                    .putExtra("producto", productos.get(position));
            vh.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productos.size() + (mostrarLoaderAlFinal ? 1 : 0);
    }

    public static class ViewHolderProducto extends RecyclerView.ViewHolder {
        private ImageView ivFotoProducto;
        private TextView txtNombreProducto, txtPrecioProducto, txtDisponibilidad;
        private TextView txtBonusBadge;
        private TextView txtDescuentoIcon;

        private LinearLayout contenedorPreciosOferta;
        private TextView txtPrecioAnterior, txtPrecioActual, txtDescuentoBadge;

        public ViewHolderProducto(@NonNull View itemView) {
            super(itemView);
            ivFotoProducto = itemView.findViewById(R.id.ip_fotoProducto);
            txtNombreProducto = itemView.findViewById(R.id.ip_nombreProducto);
            txtPrecioProducto = itemView.findViewById(R.id.ip_precioProducto);
            txtDisponibilidad = itemView.findViewById(R.id.ip_disponibilidadProducto);
            txtBonusBadge = itemView.findViewById(R.id.ip_bonusBadge);
            txtDescuentoIcon = itemView.findViewById(R.id.ip_descuentoIcon);
            contenedorPreciosOferta = itemView.findViewById(R.id.ip_contenedorPreciosOferta);
            txtPrecioAnterior = itemView.findViewById(R.id.ip_precioAnterior);
            txtPrecioActual = itemView.findViewById(R.id.ip_precioActual);
            txtDescuentoBadge = itemView.findViewById(R.id.ip_descuentoBadge);
        }
    }

    public static class ViewHolderLoading extends RecyclerView.ViewHolder {
        public ViewHolderLoading(@NonNull View itemView) {
            super(itemView);
        }
    }
}
