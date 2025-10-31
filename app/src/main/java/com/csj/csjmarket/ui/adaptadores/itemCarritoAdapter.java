package com.csj.csjmarket.ui.adaptadores;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.ui.Ayudas;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

public class itemCarritoAdapter extends RecyclerView.Adapter<itemCarritoAdapter.ViewHolderCarrito> {
    private Context context;
    public ArrayList<MiCarrito> carrito;
    private Integer cantidad = 0;
    private Double total = 0.0;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;

    private Map<Integer, StockInfo> stockMap; // mapa de stock por id

    private boolean isDeleteButtonVisible = false;
    private int deleteButtonPosition = -1;

    public itemCarritoAdapter(ArrayList<MiCarrito> carrito, Context context) {
        this.carrito = carrito;
        this.context = context;
    }

    public itemCarritoAdapter(ArrayList<MiCarrito> carrito, Context context, Map<Integer, StockInfo> stockMap) {
        this.carrito = carrito;
        this.context = context;
        this.stockMap = stockMap;
    }

    public void setStockMap(Map<Integer, StockInfo> stockMap) {
        this.stockMap = stockMap;
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
        final boolean esRegalo = carrito.get(position).isEsBonificacion();
        String baseUrl = holder.itemView.getContext().getString(R.string.connection) + "/imagenes/";
        String codigo = carrito.get(position).getCodigo();
        String imageUrl;
        if (esRegalo) {
            String imagenUrlObsequio = null;
            try { imagenUrlObsequio = carrito.get(position).getImagenUrlObsequio(); } catch (Exception ignore) {}
            if (imagenUrlObsequio != null && imagenUrlObsequio.trim().length() > 0) {
                imageUrl = imagenUrlObsequio;
            } else if (codigo != null && !codigo.trim().isEmpty()) {
                imageUrl = baseUrl + codigo + ".jpg";
            } else {
                imageUrl = baseUrl + "regalo.jpg";
            }
        } else {
            imageUrl = baseUrl + codigo + ".jpg";
        }
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(holder.imgFotoProducto);
        String nombreBase = new Ayudas().capitalize(carrito.get(position).getNombre());
        holder.txtNombreProducto.setText(esRegalo ? ("🎁 " + nombreBase + " (REGALO)") : nombreBase);
        holder.txtUnidad.setText(esRegalo ? "GRATIS" : carrito.get(position).getUnidad());
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        holder.txtTotal.setText(esRegalo ? "S/ 0.00" : ("S/ " + decimalFormat.format(carrito.get(position).getTotal())));
        holder.txtCantidad.setText(cantidad.toString());

        if (esRegalo) {
            holder.btnDisminuir.setEnabled(false);
            holder.btnAumentar.setEnabled(false);
            holder.txtCantidad.setEnabled(false);
            holder.txtCantidad.setFocusable(false);
            holder.txtCantidad.setText(String.valueOf(carrito.get(position).getCantidad()));
            holder.txtNombreProducto.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.btnDisminuir.setEnabled(true);
            holder.btnAumentar.setEnabled(true);
            holder.txtCantidad.setEnabled(true);
            holder.txtCantidad.setFocusableInTouchMode(true);
            holder.txtNombreProducto.setTextColor(holder.itemView.getResources().getColor(android.R.color.black));
        }

        // Control de visibilidad del contenedor de opciones (Eliminar/Cancelar)
        if (isDeleteButtonVisible && deleteButtonPosition == position) {
            holder.contenedorOpciones.setVisibility(View.VISIBLE);
            try { holder.contenedorOpciones.bringToFront(); } catch (Exception ignore) {}
        } else {
            holder.contenedorOpciones.setVisibility(View.GONE);
        }

        // Listener para eliminar item
        holder.btnEliminar.setOnClickListener(view -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                MiCarrito item = carrito.get(pos);
                // eliminar posibles regalos asociados al producto principal
                eliminarRegalosAsociados(item);
                carrito.remove(pos);
                notifyItemRemoved(pos);
                isDeleteButtonVisible = false;
                deleteButtonPosition = -1;
                guardarCambios();
                if (onItemChangedListener != null) {
                    onItemChangedListener.onItemChanged();
                }
            }
        });

        // Listener para cancelar acción de eliminación
        holder.btnCancelar.setOnClickListener(view -> {
            isDeleteButtonVisible = false;
            int prev = deleteButtonPosition;
            deleteButtonPosition = -1;
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos);
            }
            if (prev >= 0 && prev < carrito.size()) {
                notifyItemChanged(prev);
            }
        });

        holder.btnDisminuir.setOnClickListener(view -> {
            cantidad = carrito.get(holder.getAdapterPosition()).getCantidad();
            if (cantidad > 1){
                cantidad--;
                total = cantidad * carrito.get(holder.getAdapterPosition()).getPrecio();
                holder.txtCantidad.setText(cantidad.toString());
                holder.txtTotal.setText("S/ " +decimalFormat.format(total));
                carrito.get(holder.getAdapterPosition()).setCantidad(cantidad);
                carrito.get(holder.getAdapterPosition()).setTotal(total);

                // Sincronizar regalos
                sincronizarRegalosPorProductoPrincipal(carrito.get(holder.getAdapterPosition()));

                guardarCambios();

                if (onItemChangedListener != null) {
                    onItemChangedListener.onItemChanged();
                }
            }
            else {
                showDeleteButton(holder.getAdapterPosition());
            }
            // actualizar estado del botón aumentar
            try {
                int disponibleActual = getDisponible(carrito.get(holder.getAdapterPosition()).getIdProducto());
                holder.btnAumentar.setEnabled(!esRegalo && cantidad < disponibleActual);
            } catch (Exception ignore) {}
        });
        holder.btnAumentar.setOnClickListener(view -> {
            cantidad = carrito.get(holder.getAdapterPosition()).getCantidad();
            int disponible = getDisponible(carrito.get(holder.getAdapterPosition()).getIdProducto());
            if (disponible <= 0) {
                Toast.makeText(context, "Producto sin stock", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cantidad < disponible){
                cantidad++;
                total = cantidad * carrito.get(holder.getAdapterPosition()).getPrecio();
                holder.txtCantidad.setText(cantidad.toString());
                holder.txtTotal.setText("S/ " + decimalFormat.format(total));
                carrito.get(holder.getAdapterPosition()).setCantidad(cantidad);
                carrito.get(holder.getAdapterPosition()).setTotal(total);

                // Sincronizar regalos
                sincronizarRegalosPorProductoPrincipal(carrito.get(holder.getAdapterPosition()));

                guardarCambios();
                if (onItemChangedListener != null) {
                    onItemChangedListener.onItemChanged();
                }
            }
            else {
                String nombre = carrito.get(holder.getAdapterPosition()).getNombre();
                Toast.makeText(context, "Solo hay " + disponible + " unidades disponibles de " + nombre, Toast.LENGTH_SHORT).show();
            }
            // actualizar estado del botón aumentar
            holder.btnAumentar.setEnabled(!esRegalo && (carrito.get(holder.getAdapterPosition()).getCantidad() < disponible));
        });

        // Validación al editar la cantidad manualmente
        if (!esRegalo) {
            final boolean[] isEditing = {false};
            holder.txtCantidad.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isEditing[0]) return;
                    isEditing[0] = true;
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) { isEditing[0] = false; return; }
                    MiCarrito item = carrito.get(pos);
                    String txt = s.toString().trim();
                    int disponible = getDisponible(item.getIdProducto());
                    if (txt.isEmpty()) {
                        isEditing[0] = false;
                        return;
                    }
                    int nuevo;
                    try { nuevo = Integer.parseInt(txt); } catch (Exception e) { nuevo = 1; }
                    if (nuevo < 1) {
                        showDeleteButton(pos);
                        nuevo = 1;
                    }
                    if (disponible <= 0) {
                        Toast.makeText(context, "Producto sin stock", Toast.LENGTH_SHORT).show();
                        nuevo = 1;
                    } else if (nuevo > disponible) {
                        String nombre = item.getNombre();
                        Toast.makeText(context, "Solo hay " + disponible + " unidades disponibles de " + nombre, Toast.LENGTH_SHORT).show();
                        nuevo = disponible;
                    }
                    item.setCantidad(nuevo);
                    item.setTotal(nuevo * item.getPrecio());
                    holder.txtCantidad.setText(String.valueOf(nuevo));
                    holder.txtCantidad.setSelection(holder.txtCantidad.getText().length());
                    holder.txtTotal.setText("S/ " + decimalFormat.format(item.getTotal()));
                    // sincronizar regalos
                    sincronizarRegalosPorProductoPrincipal(item);
                    guardarCambios();
                    if (onItemChangedListener != null) {
                        onItemChangedListener.onItemChanged();
                    }
                    // actualizar botón aumentar
                    holder.btnAumentar.setEnabled(nuevo < disponible);
                    isEditing[0] = false;
                }
            });
        }
    }

    private void sincronizarRegalosPorProductoPrincipal(MiCarrito principal) {
        try {
            if (principal == null) return;
            // Si el item es regalo, no sincronizar
            if (principal.isEsBonificacion()) return;
            Integer step = principal.getBonusStepUnidades();
            Integer qtyPorPaso = principal.getBonusCantidadPorPaso();
            if (step == null || step <= 0 || qtyPorPaso == null || qtyPorPaso <= 0) {
                // no hay bonificación definida para este principal
                return;
            }
            int factor = 1;
            try { factor = Math.max(principal.getFactor(), 1); } catch (Exception ignore) {}
            int cantidadUnidades = (principal.getCantidad() != null ? principal.getCantidad() : 0) * factor;
            int pasos = cantidadUnidades / step;
            int regalosEsperados = pasos * qtyPorPaso;
            // buscar regalo existente asociado a este principal
            MiCarrito regalo = null;
            int regaloIndex = -1;
            for (int i = 0; i < carrito.size(); i++) {
                MiCarrito item = carrito.get(i);
                if (item.isEsBonificacion()) {
                    Integer idPrincipal = item.getIdProductoPrincipal();
                    if (idPrincipal != null && idPrincipal.equals(principal.getIdProducto())) {
                        regalo = item;
                        regaloIndex = i;
                        break;
                    }
                }
            }
            if (regalo == null && regalosEsperados > 0) {
                // crear regalo asociado
                MiCarrito nuevoRegalo = new MiCarrito();
                nuevoRegalo.setIdProducto(0);
                nuevoRegalo.setIdUnidad(principal.getIdUnidad());
                nuevoRegalo.setNombre(principal.getBonusNombreObsequio() != null && principal.getBonusNombreObsequio().length() > 0 ? principal.getBonusNombreObsequio() : "Producto de bonificación");
                nuevoRegalo.setUnidad("GRATIS");
                nuevoRegalo.setCantidad(regalosEsperados);
                nuevoRegalo.setPrecio(0.0);
                nuevoRegalo.setTotal(0.0);
                // usar código/URL del obsequio si están disponibles
                String codigoObsequio = principal.getCodigoProductoObsequiado();
                String urlObsequio = principal.getImagenUrlObsequio();
                nuevoRegalo.setCodigoProductoObsequiado(codigoObsequio);
                nuevoRegalo.setImagenUrlObsequio(urlObsequio);
                if (codigoObsequio != null && codigoObsequio.trim().length() > 0) {
                    nuevoRegalo.setCodigo(codigoObsequio);
                } else {
                    nuevoRegalo.setCodigo("regalo");
                }
                nuevoRegalo.setPeso(0.0);
                nuevoRegalo.setPesoTotal(0.0);
                nuevoRegalo.setEsBonificacion(true);
                nuevoRegalo.setIdProductoPrincipal(principal.getIdProducto());
                nuevoRegalo.setBonusStepUnidades(step);
                nuevoRegalo.setBonusCantidadPorPaso(qtyPorPaso);
                nuevoRegalo.setBonusNombreObsequio(principal.getBonusNombreObsequio());
                carrito.add(nuevoRegalo);
                notifyItemInserted(carrito.size()-1);
            } else if (regalo != null) {
                // actualizar cantidad de regalo
                regalo.setCantidad(regalosEsperados);
                regalo.setTotal(0.0);
                // si regalosEsperados es 0, remover regalo
                if (regalosEsperados <= 0) {
                    carrito.remove(regaloIndex);
                    notifyItemRemoved(regaloIndex);
                } else {
                    notifyItemChanged(regaloIndex);
                }
            }
        } catch (Exception ignore) {}
    }

    private void eliminarRegalosAsociados(MiCarrito principal) {
        try {
            if (principal == null) return;
            int i = 0;
            while (i < carrito.size()) {
                MiCarrito item = carrito.get(i);
                if (item.isEsBonificacion()) {
                    Integer idPrincipal = item.getIdProductoPrincipal();
                    if (idPrincipal != null && idPrincipal.equals(principal.getIdProducto())) {
                        carrito.remove(i);
                        notifyItemRemoved(i);
                        continue;
                    }
                }
                i++;
            }
        } catch (Exception ignore) {}
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
        editor.apply();
    }

    public void showDeleteButton(int position) {
        isDeleteButtonVisible = true;
        deleteButtonPosition = position;
        notifyItemChanged(position);
    }

    private int getDisponible(int idProducto) {
        if (stockMap != null) {
            StockInfo info = stockMap.get(idProducto);
            if (info != null) {
                return Math.max(info.getStockDisponible(), 0);
            }
        }
        return Integer.MAX_VALUE; // sin dato de stock, no limitar
    }
}