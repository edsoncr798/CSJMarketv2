package com.csj.csjmarket.ui.adaptadores;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.ui.Ayudas;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class itemCarritoAdapter extends RecyclerView.Adapter<itemCarritoAdapter.ViewHolderCarrito> {
    private Context context;
    public ArrayList<MiCarrito> carrito;
    private Integer cantidad = 0;
    private Double total = 0.0;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;

    private boolean isDeleteButtonVisible = false;
    private int deleteButtonPosition = -1;

    public itemCarritoAdapter(ArrayList<MiCarrito> carrito, Context context) {
        this.carrito = carrito;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolderCarrito onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carrito, parent, false);
        return new itemCarritoAdapter.ViewHolderCarrito(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderCarrito holder, int position) {
        cantidad = carrito.get(position).getCantidad();
        Glide.with(holder.itemView.getContext())
                .load(holder.itemView.getContext().getString(R.string.connection) + "/imagenes/" + carrito.get(position).getCodigo() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(holder.imgFotoProducto);
        holder.txtNombreProducto.setText(new Ayudas().capitalize(carrito.get(position).getNombre()));
        holder.txtUnidad.setText(carrito.get(position).getUnidad());
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        holder.txtTotal.setText("S/ " + decimalFormat.format(carrito.get(position).getTotal()));
        holder.txtCantidad.setText(cantidad.toString());

        holder.btnDisminuir.setOnClickListener(view -> {
            cantidad = carrito.get(holder.getAdapterPosition()).getCantidad();
            if (cantidad > 1){
                cantidad--;
                total = cantidad * carrito.get(holder.getAdapterPosition()).getPrecio();
                holder.txtCantidad.setText(cantidad.toString());
                holder.txtTotal.setText("S/ " +decimalFormat.format(total));
                carrito.get(holder.getAdapterPosition()).setCantidad(cantidad);
                carrito.get(holder.getAdapterPosition()).setTotal(total);

                guardarCambios();

                if (onItemChangedListener != null) {
                    onItemChangedListener.onItemChanged();
                }
            }
            else {
                showDeleteButton(holder.getAdapterPosition());
            }
        });
        holder.btnAumentar.setOnClickListener(view -> {
            cantidad = carrito.get(holder.getAdapterPosition()).getCantidad();
            cantidad++;
            total = cantidad * carrito.get(holder.getAdapterPosition()).getPrecio();
            holder.txtCantidad.setText(cantidad.toString());
            holder.txtTotal.setText("S/ " + decimalFormat.format(total));
            carrito.get(holder.getAdapterPosition()).setCantidad(cantidad);
            carrito.get(holder.getAdapterPosition()).setTotal(total);

            guardarCambios();

            if (onItemChangedListener != null) {
                onItemChangedListener.onItemChanged();
            }
        });

        if (isDeleteButtonVisible && deleteButtonPosition == position) {
            holder.contenedorOpciones.setVisibility(View.VISIBLE);
        } else {
            holder.contenedorOpciones.setVisibility(View.GONE);
        }

        holder.btnCancelar.setOnClickListener(view -> {
            holder.contenedorOpciones.setVisibility(View.GONE);
        });

        holder.btnEliminar.setOnClickListener(view -> {
            carrito.remove(holder.getAdapterPosition());
            notifyItemRemoved(holder.getAdapterPosition());

            guardarCambios();
            if (onItemChangedListener != null) {
                onItemChangedListener.onItemChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return carrito.size();
    }

    public class ViewHolderCarrito extends RecyclerView.ViewHolder {
        private ImageView imgFotoProducto;
        private TextView txtNombreProducto, txtUnidad, txtTotal;
        private EditText txtCantidad;
        private Button btnDisminuir, btnAumentar;
        private LinearLayout contenedorOpciones, btnEliminar, btnCancelar;

        public ViewHolderCarrito(@NonNull View itemView) {
            super(itemView);
            imgFotoProducto = itemView.findViewById(R.id.icarrito_imgProducto);
            txtNombreProducto = itemView.findViewById(R.id.icarrito_txtNombreProducto);
            txtUnidad = itemView.findViewById(R.id.icarrito_txtUnidad);
            txtTotal = itemView.findViewById(R.id.icarrito_txtTotal);
            txtCantidad = itemView.findViewById(R.id.icarrito_txtCantidad);
            btnDisminuir = itemView.findViewById(R.id.icarrito_btnDisminuir);
            btnAumentar = itemView.findViewById(R.id.icarrito_btnAumentar);
            btnEliminar = itemView.findViewById(R.id.icarrito_btnEliminar);
            btnCancelar = itemView.findViewById(R.id.icarrito_btnCancelar);
            contenedorOpciones = itemView.findViewById(R.id.contenedorOpciones);
        }
    }

    public interface OnItemChangedListener {
        void onItemChanged();
    }

    private OnItemChangedListener onItemChangedListener;

    public void setOnItemChangedListener(OnItemChangedListener listener) {
        this.onItemChangedListener = listener;
    }

    private void guardarCambios() {
        sharedPreferences = context.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("carrito", gson.toJson(carrito));
        editor.commit();
    }

    public void showDeleteButton(int position) {
        isDeleteButtonVisible = true;
        deleteButtonPosition = position;
        notifyDataSetChanged();
    }
}