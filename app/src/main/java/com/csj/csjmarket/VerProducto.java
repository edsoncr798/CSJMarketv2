package com.csj.csjmarket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.csj.csjmarket.databinding.ActivityVerProductoBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.ui.Ayudas;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.content.res.ColorStateList;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class VerProducto extends AppCompatActivity {
    private ActivityVerProductoBinding binding;
    private Producto producto;
    private Integer cantidad = 1;
    private SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private ArrayList<MiCarrito> miCarrito;
    private int stockDisponibleReal = 0;

// Lógica de bonificación dinámica
private boolean bonusActivo = false;
private int bonusStepUnidades = 1; // cada cuántas unidades se otorga obsequio
private int bonusCantidadPorPaso = 0; // cuántos obsequios por paso
private String bonusNombreObsequio = "";
private boolean bonusHayStock = false;
private String bonusFechaInicio = "";
private String bonusFechaFin = "";
private double bonusPrecioObsequio = 0.0; // precio unitario del obsequio si viene del API
private int ultimoTotalRegalos = 0; // para animación al aumentar obsequios
// nuevos: mantener referencia de código y URL de imagen del obsequio
private String bonusCodigoObsequio = "";
private String bonusImagenUrlObsequio = "";

private boolean descuentoActivo = false;
private double descuentoPorcentaje = 0.0;
private double descuentoPrecioAntes = 0.0;
private double descuentoPrecioActual = 0.0;
private String descuentoMensaje = "";
private String descuentoFechaInicio = "";
private String descuentoFechaFin = "";
private org.json.JSONArray descuentosArrayCompleto = null; // Para almacenar todos los descuentos
private Integer descuentoIdSeleccionado = null; // ID de la regla aplicada

// Venta por caja (wholesale)
private boolean modoCaja = false;
private boolean cajaDisponible = false;
private int cajaUnidadId = 0;
private String cajaUnidadDesc = "";
private int cajaFactor = 1;
private double cajaPrecio = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_producto);

        binding = ActivityVerProductoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        Type typeS = new TypeToken<List<MiCarrito>>(){}.getType();
        miCarrito = gson.fromJson(sharedPreferences.getString("carrito", ""), typeS);

        producto = (Producto) getIntent().getSerializableExtra("producto");
        int stockFisico = producto != null ? producto.getStockDisponible() : 0;
        // Calcular stock disponible real restando lo reservado en el carrito
        int reservadoEnCarrito = 0;
        try {
            if (producto != null && miCarrito != null) {
                for (MiCarrito c : miCarrito) {
                    if (c != null && !c.isEsBonificacion()
                            && c.getIdProducto() != null && c.getIdProducto().equals(producto.getId())
                            && c.getIdUnidad() != null && c.getIdUnidad().equals(producto.getIdUnidadBase())) {
                        reservadoEnCarrito += (c.getCantidad() != null ? c.getCantidad() : 0);
                    }
                }
            }
        } catch (Exception ignore) {}
        stockDisponibleReal = Math.max(stockFisico - reservadoEnCarrito, 0);
        try {
            if (producto != null) {
                CsjMarketPopularRegistry.add(producto.getId());
            }
        } catch (Exception ignore) {}

        // Ajuste de cantidad visible acorde al stock disponible
        if (stockDisponibleReal <= 0) {
            cantidad = 0;
            binding.vpTxtCantidad.setText(cantidad.toString());
        } else if (binding.vpTxtCantidad.getText().toString().equals("")) {
            cantidad = 1;
            binding.vpTxtCantidad.setText(cantidad.toString());
        }
        if (producto.getFactor() == 1){
            binding.seccionUnidades.setVisibility(View.GONE);
        }
        // Deshabilitar compra si no hay stock disponible real
        binding.vpBtnComprar.setEnabled(stockDisponibleReal > 0);
        binding.vpBtnComprar.setAlpha(stockDisponibleReal > 0 ? 1f : 0.5f);

        binding.vpNombreProducto.setText(Ayudas.capitalize(producto.getNombre()));
        binding.vpTxtUnidad.setText("S/ " + producto.getPrecioUnidadBase() + " x " + producto.getUnidadBase());
        binding.vpTxtPrecio.setText(producto.getUnidadBase());
        try { binding.vpCajaBadge.setVisibility(View.GONE); } catch (Exception ignore) {}
        try { binding.vpCajaSavings.setVisibility(View.GONE); } catch (Exception ignore) {}
        Glide.with(this)
                .load(getString(R.string.connection) + "/imagenes/" + producto.getCodigo() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(binding.vpImagenProducto);
        try { binding.vpBtnUnidad.setText(producto.getUnidadBase()); } catch (Exception ignore) {}

        // Mostrar stock disponible real en la vista de cantidad
        binding.vpTxtStockDisponible.setText("Stock: " + stockDisponibleReal);
        // Mostrar código del producto debajo del stock
        binding.vpTxtCodigoProducto.setText("Código: " + producto.getCodigo());

        // Cargar precio por caja (wholesale)
        cargarPrecioCaja(producto.getId());

        // Solicitar bonificación: solo si el producto indica bonificación
        if (producto != null) {
            boolean tieneBonificacionFlag = false;
            try {
                tieneBonificacionFlag = producto.isTieneBonificacion();
            } catch (Exception ignore) {}
            if (tieneBonificacionFlag) {
                cargarBonificacion(producto.getId());
            } else {
                binding.vpBonusContainer.setVisibility(View.GONE);
                binding.vpBonusLoader.setVisibility(View.GONE);
            }
            boolean mostrarLoaderDesc = false;
            try {
                mostrarLoaderDesc = producto.isTieneDescuento();
            } catch (Exception ignore) {}
            if (mostrarLoaderDesc) {
                cargarDescuento(producto.getId());
            } else {
                binding.vpDiscountContainer.setVisibility(View.GONE);
                binding.vpDiscountLoader.setVisibility(View.GONE);
            }
        } else {
            binding.vpBonusContainer.setVisibility(View.GONE);
            binding.vpDiscountContainer.setVisibility(View.GONE);
        }

        binding.vpBtnAumentar.setOnClickListener(view -> {
            if (binding.vpTxtCantidad.getText().toString().equals("")){
                cantidad = 1;
                binding.vpTxtCantidad.setText(cantidad.toString());
            }else {
                cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                if (cantidad >= getMaxQtyAllowed()) {
                    mostrarMaximoStock();
                } else {
                    cantidad++;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }
            }
            verificarBotonIncremento();
            actualizarBonificacionDinamica();
            actualizarDescuentoDinamico();
        });

        binding.vpBtnDisminuir.setOnClickListener(view -> {
            if (binding.vpTxtCantidad.getText().toString().isEmpty() || binding.vpTxtCantidad.getText().toString().equals("1")){
                cantidad = 0;
                binding.vpTxtCantidad.setText(cantidad.toString());
            }else {
                if (cantidad > 0) {
                    cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                    cantidad--;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }
            }
            verificarBotonIncremento();
            actualizarBonificacionDinamica();
            actualizarDescuentoDinamico();
        });

        binding.vpTxtCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.vpTxtCantidad.getText().toString().equals("")){
                    cantidad = 0;
                }
                else{
                    try {
                        cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                    } catch (NumberFormatException ex) {
                        cantidad = 0;
                    }
                }

                if (cantidad < 0) {
                    cantidad = 0;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }

                int maxQty = getMaxQtyAllowed();
                if (cantidad > maxQty) {
                    cantidad = Math.max(maxQty, 0);
                    binding.vpTxtCantidad.setText(cantidad.toString());
                    mostrarMaximoStock();
                }
                actualizarBonificacionDinamica();
                actualizarDescuentoDinamico();
            }
        });

        binding.vpBtnComprar.setOnClickListener(view -> {
            // Validación de stock antes de agregar
            int qty = cantidad != null ? cantidad : 0;
            if (getMaxQtyAllowed() <= 0) {
                mostrarSinStock();
                return;
            }
            if (qty <= 0) {
                Toast.makeText(this, "Ingrese una cantidad válida", Toast.LENGTH_SHORT).show();
                return;
            }
            if (qty > getMaxQtyAllowed()) {
                mostrarMaximoStock();
                return;
            }

            MiCarrito miCarritoItem = new MiCarrito();
            miCarritoItem.setIdProducto(producto.getId());
            // Unidad/caja
            if (modoCaja && cajaDisponible) {
                miCarritoItem.setIdUnidad(cajaUnidadId);
                miCarritoItem.setNombre(producto.getNombre());
                miCarritoItem.setUnidad(cajaUnidadDesc != null && !cajaUnidadDesc.isEmpty() ? cajaUnidadDesc : "CAJA");
                miCarritoItem.setCantidad(qty);
                miCarritoItem.setPrecio(cajaPrecio);
                miCarritoItem.setTotal(cajaPrecio * qty);
                miCarritoItem.setEsCaja(true);
            } else {
                miCarritoItem.setIdUnidad(producto.getIdUnidadBase());
                miCarritoItem.setNombre(producto.getNombre());
                miCarritoItem.setUnidad(producto.getUnidadBase());
                miCarritoItem.setCantidad(qty);
                miCarritoItem.setPrecio(producto.getPrecioUnidadBase());
                miCarritoItem.setTotal(producto.getPrecioUnidadBase() * qty);
                miCarritoItem.setEsCaja(false);
            }
            miCarritoItem.setCodigo(producto.getCodigo());
                miCarritoItem.setPeso(producto.getPeso());
                miCarritoItem.setPesoTotal(producto.getPeso() * qty);
                // Bonificación solo aplica si NO es venta por caja
                miCarritoItem.setTieneBonificacion(producto.isTieneBonificacion() && !modoCaja);
                try { miCarritoItem.setTieneDescuento(producto.isTieneDescuento()); } catch (Exception ignore) {}
                try { miCarritoItem.setIdDefinicionDescuento(descuentoIdSeleccionado); } catch (Exception ignore) {}
                // Guardar factor del producto para cálculos en carrito
                try { miCarritoItem.setFactor(Math.max(modoCaja && cajaDisponible ? cajaFactor : producto.getFactor(), 1)); } catch (Exception ignore) { miCarritoItem.setFactor(1); }
            // Persistir reglas de bonificación en el producto principal (para futuras sincronizaciones)
            try {
                if (!modoCaja && bonusActivo && bonusStepUnidades > 0 && bonusCantidadPorPaso > 0) {
                    miCarritoItem.setBonusStepUnidades(bonusStepUnidades);
                    miCarritoItem.setBonusCantidadPorPaso(bonusCantidadPorPaso);
                    miCarritoItem.setBonusNombreObsequio(bonusNombreObsequio);
                    miCarritoItem.setCodigoProductoObsequiado(bonusCodigoObsequio);
                    miCarritoItem.setImagenUrlObsequio(bonusImagenUrlObsequio);
                }
            } catch (Exception ignore) {}

            if (miCarrito == null){
                miCarrito = new ArrayList<>();
            }
            // Fusionar cantidades si ya existe el mismo producto y unidad (excluye bonificaciones)
            boolean merged = false;
            try {
                for (int i = 0; i < miCarrito.size(); i++) {
                    MiCarrito item = miCarrito.get(i);
                    if (!item.isEsBonificacion()
                            && item.getIdProducto() != null && item.getIdProducto().equals(producto.getId())
                            && item.getIdUnidad() != null && item.getIdUnidad().equals(producto.getIdUnidadBase())) {
                        int nuevaCantidad = (item.getCantidad() != null ? item.getCantidad() : 0) + qty;
                        item.setCantidad(nuevaCantidad);
                        Double precioUnit = (item.getPrecio() != null ? item.getPrecio() : (modoCaja && cajaDisponible ? cajaPrecio : producto.getPrecioUnidadBase()));
                        Double pesoUnit = (item.getPeso() != null ? item.getPeso() : producto.getPeso());
                        item.setTotal(precioUnit * nuevaCantidad);
                        item.setPesoTotal(pesoUnit * nuevaCantidad);
                        // Asegurar factor definido para sincronización de regalos en carrito
                        try { if (item.getFactor() == null || item.getFactor() <= 0) item.setFactor(Math.max(modoCaja && cajaDisponible ? cajaFactor : producto.getFactor(), 1)); } catch (Exception ignore) {}
                        merged = true;
                        break;
                    }
                }
            } catch (Exception ignore) {}

            if (!merged) {
                // Asegurar flag no-bonificación para el producto principal
                miCarritoItem.setEsBonificacion(false);
                miCarrito.add(miCarritoItem);
            }

            // Agregar ítems de bonificación (regalo) si aplica
            try {
                if (!modoCaja && bonusActivo && bonusHayStock && bonusStepUnidades > 0 && bonusCantidadPorPaso > 0) {
                    int factor = Math.max(producto != null ? producto.getFactor() : 1, 1);
                    int qtyUnidades = qty * factor;
                    int pasos = qtyUnidades / bonusStepUnidades;
                    int totalRegalos = pasos * bonusCantidadPorPaso;
                    if (totalRegalos > 0) {
                        MiCarrito regaloItem = getMiCarrito(totalRegalos);
                        miCarrito.add(regaloItem);
                    }
                }
            } catch (Exception ex) {
                // Continuar con el producto principal si hay error en bonificación
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("carrito", new Gson().toJson(miCarrito));
            editor.apply();

            Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show();
            finish();
        });
        // Listener del botón de retroceso
        binding.vpBtnRegresar.setOnClickListener(view -> {
            finish();
        });
    }

    @NonNull
    private MiCarrito getMiCarrito(int totalRegalos) {
        MiCarrito regaloItem = new MiCarrito();
        regaloItem.setIdProducto(0);
        regaloItem.setIdUnidad(producto.getIdUnidadBase());
        regaloItem.setNombre(bonusNombreObsequio != null && !bonusNombreObsequio.isEmpty() ? bonusNombreObsequio : "Producto de bonificación");
        regaloItem.setUnidad("GRATIS");
        regaloItem.setCantidad(totalRegalos);
        regaloItem.setPrecio(0.0);
        regaloItem.setTotal(0.0);
        // Usar código/URL real del obsequio si viene del API
        regaloItem.setCodigoProductoObsequiado(bonusCodigoObsequio);
        regaloItem.setImagenUrlObsequio(bonusImagenUrlObsequio);
        if (bonusCodigoObsequio != null && !bonusCodigoObsequio.trim().isEmpty()) {
            regaloItem.setCodigo(bonusCodigoObsequio);
        } else {
            regaloItem.setCodigo("regalo");
        }
        regaloItem.setPeso(0.0);
        regaloItem.setPesoTotal(0.0);
        regaloItem.setEsBonificacion(true);
        // vincular con producto principal y reglas
        regaloItem.setIdProductoPrincipal(producto.getId());
        regaloItem.setBonusStepUnidades(bonusStepUnidades);
        regaloItem.setBonusCantidadPorPaso(bonusCantidadPorPaso);
        regaloItem.setBonusNombreObsequio(bonusNombreObsequio);
        return regaloItem;
    }

    private void cargarBonificacion(int idProducto) {
        // Mostrar u ocultar loader según flag
        binding.vpBonusContainer.setVisibility(View.VISIBLE);
        binding.vpBonusLoader.setVisibility(View.VISIBLE);
        // Reset visibilidades
        binding.vpBonusProgress.setVisibility(View.GONE);
        binding.vpBonusTitle.setVisibility(View.GONE);
        binding.vpBonusMessage.setVisibility(View.GONE);
        binding.vpBonusRequirement.setVisibility(View.GONE);
        binding.vpBonusGift.setVisibility(View.GONE);
        binding.vpBonusImage.setVisibility(View.GONE);
        binding.vpBonusDynamicCount.setVisibility(View.GONE);
        binding.vpBonusStock.setVisibility(View.GONE);
        binding.vpBonusValidity.setVisibility(View.GONE);
        binding.vpBonusNextHint.setVisibility(View.GONE);
        binding.vpBonusSavings.setVisibility(View.GONE);

        String url = getString(R.string.connection) + "/api/productos/bonificacion/v2/" + idProducto;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                // Leer solo campos raíz (JSON directo del API v2)
                String mensajePromocional = response.optString("mensajePromocional", "").trim();
                String tipoCondicion = response.optString("tipoCondicion", "").trim();
                String imagenProducto = response.optString("imagenProducto", "").trim();
                double porCada = response.optDouble("porCada", 0.0);
                String nombreProductoObsequiado = response.optString("nombreProductoObsequiado", "").trim();
                int cantidadObsequiado = response.optInt("cantidadObsequiado", 0);
                int stockDisponibleBonif = response.optInt("stockDisponible", 0);
                String fechaInicio = response.optString("fechaInicio", "").trim();
                String fechaFin = response.optString("fechaFin", "").trim();

                boolean tieneBonificacion = (cantidadObsequiado > 0) && (porCada > 0.0);
                boolean hayStockBonificacion = stockDisponibleBonif >= cantidadObsequiado && stockDisponibleBonif > 0;

                if (!tieneBonificacion) {
                    bonusActivo = false;
                    binding.vpBonusLoader.setVisibility(View.GONE);
                    binding.vpBonusContainer.setVisibility(View.GONE);
                    return;
                }

                // Mostrar contenido de bonificación
                binding.vpBonusLoader.setVisibility(View.GONE);
                binding.vpBonusTitle.setVisibility(View.VISIBLE);
                binding.vpBonusMessage.setVisibility(View.VISIBLE);
                binding.vpBonusRequirement.setVisibility(View.VISIBLE);
                binding.vpBonusGift.setVisibility(View.VISIBLE);
                binding.vpBonusDynamicCount.setVisibility(View.VISIBLE);
                binding.vpBonusStock.setVisibility(View.VISIBLE);
                binding.vpBonusValidity.setVisibility(View.VISIBLE);
                binding.vpBonusNextHint.setVisibility(View.VISIBLE);
                binding.vpBonusSavings.setVisibility(View.VISIBLE);
                binding.vpBonusProgress.setVisibility(View.VISIBLE);

                // Mensaje combinado
                String mensajeFull = mensajePromocional.replace("`", "").trim();
                if (!nombreProductoObsequiado.trim().isEmpty()) {
                    mensajeFull = (mensajeFull.isEmpty() ? "" : (mensajeFull + " ")) + "llevate " + cantidadObsequiado + " " + nombreProductoObsequiado.trim();
                }
                binding.vpBonusMessage.setText(mensajeFull);

                // Requisito porCada + unidad del producto
                String unidadLabel = (producto != null && producto.getUnidadBase() != null && !producto.getUnidadBase().trim().isEmpty()) ? producto.getUnidadBase().trim() : "unidades";
                int factor = (producto != null) ? Math.max(producto.getFactor(), 1) : 1;
                // Convertir porCada a unidades base (ej. tiras) si factor > 1
                double porCadaBase = porCada / factor;
                String porCadaBaseFmt;
                if (Math.abs(porCadaBase - Math.round(porCadaBase)) < 0.0001) {
                    porCadaBaseFmt = String.valueOf((int) Math.round(porCadaBase));
                } else {
                    porCadaBaseFmt = new java.text.DecimalFormat("#0.##").format(porCadaBase);
                }
                // Mostrar requisito y nota de bonificación por cada unidad base
                binding.vpBonusRequirement.setText("Debes comprar al menos " + porCadaBaseFmt + " " + unidadLabel + "\nBonificación por cada " + porCadaBaseFmt + " " + unidadLabel);

                // Obsequio y stock
                binding.vpBonusGift.setText("Obsequio: " + cantidadObsequiado + " x " + nombreProductoObsequiado);
                binding.vpBonusStock.setText(hayStockBonificacion ? "Stock disponible para bonificación" : "Sin stock para obsequio");

                // Imagen del producto obsequiado
                if (!imagenProducto.trim().isEmpty()) {
                    String urlImg = imagenProducto.replace("`", "").trim();
                    binding.vpBonusImage.setVisibility(View.VISIBLE);
                    Glide.with(this)
                            .load(urlImg)
                            .placeholder(R.drawable.default_image)
                            .error(R.drawable.default_image)
                            .into(binding.vpBonusImage);
                } else {
                    binding.vpBonusImage.setVisibility(View.GONE);
                }

                // Fechas amigables
                String vigenciaText = getVigenciaText(fechaInicio, fechaFin);
                binding.vpBonusValidity.setText(vigenciaText);

                // Variables para cálculo dinámico
                bonusActivo = true;
                bonusStepUnidades = Math.max((int) Math.floor(porCada), 1);
                bonusCantidadPorPaso = Math.max(cantidadObsequiado, 0);
                bonusNombreObsequio = nombreProductoObsequiado;
                bonusHayStock = hayStockBonificacion;
                bonusFechaInicio = fechaInicio;
                bonusFechaFin = fechaFin;
                bonusPrecioObsequio = 0.0; // no viene en v2 simplificada
                bonusCodigoObsequio = ""; // no viene en v2 simplificada
                bonusImagenUrlObsequio = imagenProducto;
                ultimoTotalRegalos = 0;

                if (!modoCaja) {
                    binding.vpBonusContainer.setVisibility(View.VISIBLE);
                } else {
                    binding.vpBonusContainer.setVisibility(View.GONE);
                }
                binding.vpBonusProgress.setMax(bonusStepUnidades);
                binding.vpBonusProgress.setProgress(0);
                if (!modoCaja) {
                    binding.vpBonusProgress.setVisibility(View.VISIBLE);
                }

                actualizarBonificacionDinamica();
            } catch (Exception e) {
                binding.vpBonusContainer.setVisibility(View.GONE);
            }
        }, error -> {
            binding.vpBonusLoader.setVisibility(View.GONE);
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse == null && (error instanceof TimeoutError || error instanceof NoConnectionError)) {
                binding.vpBonusContainer.setVisibility(View.GONE);
                return;
            }
            binding.vpBonusContainer.setVisibility(View.GONE);
        });
        req.setRetryPolicy(new DefaultRetryPolicy(9000, 2, 1));
        req.setShouldCache(false);
        Volley.newRequestQueue(this).add(req);
    }

    @NonNull
    private static String getVigenciaText(String fechaInicio, String fechaFin) {
        java.util.Locale localeEs = new java.util.Locale("es", "ES");
        String inicioFmt = "";
        String finFmt = "";
        String[] patrones = new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        if (!fechaInicio.isEmpty()) {
            for (String p : patrones) {
                try {
                    java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                    in.setLenient(true);
                    java.util.Date d = in.parse(fechaInicio);
                    java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("d 'de' MMMM 'de' yyyy", localeEs);
                    inicioFmt = out.format(d);
                    break;
                } catch (Exception ignore) {}
            }
        }
        if (!fechaFin.isEmpty()) {
            for (String p : patrones) {
                try {
                    java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                    in.setLenient(true);
                    java.util.Date d = in.parse(fechaFin);
                    java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("d 'de' MMMM 'de' yyyy", localeEs);
                    finFmt = out.format(d);
                    break;
                } catch (Exception ignore) {}
            }
        }
        String vigenciaText = "";
        if (!inicioFmt.isEmpty() && !finFmt.isEmpty()) {
            vigenciaText = "Válido del " + inicioFmt + " al " + finFmt;
        } else if (!inicioFmt.isEmpty()) {
            vigenciaText = "Válido desde " + inicioFmt;
        } else if (!finFmt.isEmpty()) {
            vigenciaText = "Válido hasta " + finFmt;
        }
        return vigenciaText;
    }

    private void cargarDescuento(int idProducto) {
        binding.vpDiscountContainer.setVisibility(View.VISIBLE);
        binding.vpDiscountLoader.setVisibility(View.VISIBLE);
        binding.vpDiscountTitle.setVisibility(View.VISIBLE); // Siempre mostrar título cuando hay descuento
        binding.vpDiscountMessage.setVisibility(View.GONE);
        binding.vpDiscountDetail.setVisibility(View.GONE);
        binding.vpDiscountSavings.setVisibility(View.GONE);
        binding.vpDiscountValidity.setVisibility(View.GONE);

        String url = getString(R.string.connection) + "/api/productos/descuento/" + idProducto;
        StringRequest req = new StringRequest(Request.Method.GET, url, respStr -> {
            try {
                Object parsed = new org.json.JSONTokener(respStr).nextValue();
                org.json.JSONArray descuentosArray;
                
                if (parsed instanceof org.json.JSONArray arr) {
                    if (arr.length() == 0) {
                        binding.vpDiscountLoader.setVisibility(View.GONE);
                        // No hay descuento activo pero mostrar mensaje genérico
                        descuentoActivo = false;
                        // El título ya está visible desde el inicio
                        binding.vpDiscountMessage.setVisibility(View.VISIBLE);
                        binding.vpDiscountMessage.setText("Descuento disponible");
                        binding.vpDiscountDetail.setVisibility(View.GONE);
                        binding.vpDiscountSavings.setVisibility(View.GONE);
                        binding.vpDiscountValidity.setVisibility(View.GONE);
                        return;
                    }
                    descuentosArray = arr;
                } else {
                    // Si es un solo objeto, convertirlo a array con un solo elemento
                    descuentosArray = new org.json.JSONArray();
                    descuentosArray.put(parsed);
                }

                // Procesar múltiples descuentos
                StringBuilder mensajesBuilder = new StringBuilder();
                StringBuilder detallesBuilder = new StringBuilder();
                String fechaInicio = "";
                String fechaFin = "";
                double maxPorcentaje = 0.0;
                boolean hayDescuentosValidos = false;
                java.text.DecimalFormat dfPorcentaje = new java.text.DecimalFormat("#0.#"); // Para formatear porcentajes con decimales
                
                // Guardar array completo para cálculos dinámicos
                descuentosArrayCompleto = descuentosArray;

                for (int i = 0; i < descuentosArray.length(); i++) {
                    org.json.JSONObject descuento = descuentosArray.getJSONObject(i);
                    
                    String mensaje = descuento.optString("MensajePromocional", descuento.optString("mensajePromocional", descuento.optString("mensaje", ""))).trim();
                    double porcentaje = descuento.optDouble("PorcentajeDescuento", descuento.optDouble("porcentajeDescuento", descuento.optDouble("porcentaje", 0.0)));
                    String tipoCondicion = descuento.optString("tipoCondicion", "").trim();
                    String valorDesde = descuento.optString("valorDesde", "").trim();
                    String valorHasta = descuento.optString("valorHasta", "").trim();
                    
                    // Actualizar fechas (tomar la más amplia)
                    String fi = descuento.optString("FechaInicio", "").trim();
                    String ff = descuento.optString("FechaFin", "").trim();
                    if (!fi.isEmpty() && fechaInicio.isEmpty()) fechaInicio = fi;
                    if (!ff.isEmpty() && fechaFin.isEmpty()) fechaFin = ff;
                    
                    if (porcentaje > 0) {
                        hayDescuentosValidos = true;
                        if (maxPorcentaje < porcentaje) maxPorcentaje = porcentaje;
                        
                        // Agregar mensaje
                        if (mensaje.isEmpty()) {
                            mensaje = "Descuento del " + porcentaje + "%";
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            if (!mensajesBuilder.isEmpty()) mensajesBuilder.append("\n");
                        }
                        mensajesBuilder.append("• ").append(mensaje);
                        
                        // Agregar detalles del rango según el tipo de condición
                        if (tipoCondicion.equals("ValorVenta")) {
                            if (!valorDesde.isEmpty() && !valorHasta.isEmpty() && !valorHasta.equals("null")) {
                                detallesBuilder.append("• Compra S/ ").append(valorDesde.trim()).append(" - S/ ").append(valorHasta.trim()).append(": ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                            } else if (!valorDesde.isEmpty() && (valorHasta.isEmpty() || valorHasta.equals("null"))) {
                                detallesBuilder.append("• Compra desde S/ ").append(valorDesde.trim()).append(": ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                            } else {
                                detallesBuilder.append("• ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                            }
                        } else if (tipoCondicion.equals("CantidadBase")) {
                            if (!valorDesde.isEmpty() && !valorHasta.isEmpty() && !valorHasta.equals("null")) {
                                detallesBuilder.append("• Compra ").append(valorDesde.trim()).append(" - ").append(valorHasta.trim()).append(" unidades: ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                            } else if (!valorDesde.isEmpty() && (valorHasta.isEmpty() || valorHasta.equals("null"))) {
                                detallesBuilder.append("• Compra desde ").append(valorDesde.trim()).append(" unidades: ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                            } else {
                                detallesBuilder.append("• ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                            }
                        } else {
                            detallesBuilder.append("• ").append(dfPorcentaje.format(porcentaje)).append("% descuento");
                        }
                        if (i < descuentosArray.length() - 1) detallesBuilder.append("\n");
                    }
                }

                if (!hayDescuentosValidos) {
                    binding.vpDiscountLoader.setVisibility(View.GONE);
                    descuentoActivo = false;
                    binding.vpDiscountMessage.setVisibility(View.VISIBLE);
                    binding.vpDiscountMessage.setText("Descuento disponible");
                    binding.vpDiscountDetail.setVisibility(View.GONE);
                    binding.vpDiscountSavings.setVisibility(View.GONE);
                    binding.vpDiscountValidity.setVisibility(View.GONE);
                    return;
                }
                double precioAntes = producto != null && producto.getPrecioUnidadAntes() != null ? producto.getPrecioUnidadAntes() : 0.0;
                double precioActual = producto != null && producto.getPrecioUnidadBase() != null ? producto.getPrecioUnidadBase() : 0.0;

                descuentoActivo = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    descuentoMensaje = !mensajesBuilder.isEmpty() ? mensajesBuilder.toString() : "Descuento disponible";
                }
                descuentoPrecioAntes = precioAntes > 0.0 ? precioAntes : (producto != null ? (producto.getPrecioUnidadAntes() != null ? producto.getPrecioUnidadAntes() : 0.0) : 0.0);
                descuentoPrecioActual = precioActual > 0.0 ? precioActual : (producto != null ? (producto.getPrecioUnidadBase() != null ? producto.getPrecioUnidadBase() : 0.0) : 0.0);
                descuentoPorcentaje = maxPorcentaje;
                descuentoFechaInicio = fechaInicio;
                descuentoFechaFin = fechaFin;

                binding.vpDiscountLoader.setVisibility(View.GONE);
                binding.vpDiscountContainer.setVisibility(View.VISIBLE);
                binding.vpDiscountTitle.setVisibility(View.VISIBLE);
                binding.vpDiscountMessage.setVisibility(View.VISIBLE);
                binding.vpDiscountDetail.setVisibility(View.VISIBLE);
                binding.vpDiscountSavings.setVisibility(View.VISIBLE);
                binding.vpDiscountValidity.setVisibility(View.VISIBLE);

                // Mostrar mensajes de descuento (reglas completas)
                binding.vpDiscountMessage.setText(descuentoMensaje);
                
                // Mostrar detalles de rangos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (!detallesBuilder.isEmpty()) {
                        binding.vpDiscountDetail.setVisibility(View.VISIBLE);
                        binding.vpDiscountDetail.setText(detallesBuilder.toString());
                    } else {
                        binding.vpDiscountDetail.setVisibility(View.GONE);
                    }
                }

                // Actualizar la interfaz visual con los valores actuales
                actualizarInterfazVisualDescuento();

                String vigenciaText = getVigenciaText(descuentoFechaInicio, descuentoFechaFin);
                if (vigenciaText.isEmpty()) {
                    binding.vpDiscountValidity.setVisibility(View.GONE);
                } else {
                    binding.vpDiscountValidity.setVisibility(View.VISIBLE);
                    binding.vpDiscountValidity.setText(vigenciaText);
                }

                actualizarDescuentoDinamico();
            } catch (Exception e) {
                // Error en el procesamiento, no hay descuento activo
                descuentoActivo = false;
                binding.vpDiscountLoader.setVisibility(View.GONE);
                // El título ya está visible desde el inicio
                binding.vpDiscountMessage.setVisibility(View.VISIBLE);
                binding.vpDiscountMessage.setText("Descuento disponible");
                binding.vpDiscountDetail.setVisibility(View.GONE);
                binding.vpDiscountSavings.setVisibility(View.GONE);
                binding.vpDiscountValidity.setVisibility(View.GONE);
            }
        }, error -> {
            binding.vpDiscountLoader.setVisibility(View.GONE);
            // Error de red, no hay descuento activo
            descuentoActivo = false;
            // Mostrar contenedor con mensaje genérico si hubo error pero el producto tiene descuento
            try {
                if (producto != null && producto.isTieneDescuento()) {
                    binding.vpDiscountContainer.setVisibility(View.VISIBLE);
                    // El título ya está visible desde el inicio
                    binding.vpDiscountMessage.setVisibility(View.VISIBLE);
                    binding.vpDiscountDetail.setVisibility(View.GONE);
                    binding.vpDiscountSavings.setVisibility(View.GONE);
                    binding.vpDiscountValidity.setVisibility(View.GONE);
                    binding.vpDiscountMessage.setText("Descuento disponible");
                } else {
                    binding.vpDiscountContainer.setVisibility(View.GONE);
                }
            } catch (Exception ignore) {
                binding.vpDiscountContainer.setVisibility(View.GONE);
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(9000, 2, 1));
        req.setShouldCache(false);
        Volley.newRequestQueue(this).add(req);
    }

    private void cargarPrecioCaja(int idProducto) {
        try {
            String url = "https://api.comsanjuan.com:8443/api/products/wholesale/" + idProducto;
            com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(com.android.volley.Request.Method.GET, url, respStr -> {
                try {
                    Object parsed = new org.json.JSONTokener(respStr).nextValue();
                    if (!(parsed instanceof org.json.JSONObject jo)) return;
                    org.json.JSONArray data = jo.optJSONArray("data");
                    if (data == null || data.length() == 0) return;
                    org.json.JSONArray inner = data.optJSONArray(0);
                    if (inner == null || inner.length() == 0) return;
                    org.json.JSONObject p = inner.getJSONObject(0);

                    cajaUnidadId = p.optInt("IDUnidadBase", 0);
                    cajaUnidadDesc = p.optString("UnidadBase", "").trim();
                    cajaFactor = p.optInt("Factor", 1);
                    cajaPrecio = p.optDouble("PrecioUnidadBase", 0.0);
                    cajaDisponible = (cajaUnidadId > 0) && (cajaPrecio > 0.0);

                    if (cajaDisponible) {
                        try { binding.seccionUnidades.setVisibility(android.view.View.VISIBLE); } catch (Exception ignore) {}
                        try {
                            int maxBoxes = Math.max(stockDisponibleReal / Math.max(cajaFactor, 1), 0);
                            binding.vpBtnPaquete.setEnabled(maxBoxes > 0);
                            binding.vpBtnPaquete.setAlpha(maxBoxes > 0 ? 1f : 0.5f);
                            String label = (cajaUnidadDesc != null && !cajaUnidadDesc.isEmpty()) ? cajaUnidadDesc : "PACK";
                            // Validar redundancia de texto "xFactor"
                            if (label.toLowerCase().contains("x" + cajaFactor)) {
                                binding.vpBtnPaquete.setText(label);
                            } else {
                                binding.vpBtnPaquete.setText(label + " x" + cajaFactor);
                            }
                            // Inicializar estilos de botones
                            binding.vpBtnUnidad.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.secondary)));
                            binding.vpBtnPaquete.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                        } catch (Exception ignore) {}
                        double normalUnit = producto.getPrecioUnidadBase() != null ? producto.getPrecioUnidadBase() : 0.0;
                        double cajaUnit = cajaFactor > 0 ? (cajaPrecio / cajaFactor) : cajaPrecio;
                        double ahorroUnit = Math.max(normalUnit - cajaUnit, 0.0);
                        try {
                            binding.vpCajaBadge.setVisibility(android.view.View.VISIBLE);
                            if (ahorroUnit > 0) {
                                String ahorroTxt = "Ahorro por unidad S/ " + new java.text.DecimalFormat("#0.00").format(ahorroUnit) + " (x" + cajaFactor + ")";
                                binding.vpCajaSavings.setText(ahorroTxt);
                                // Inicialmente oculto en modo unidad
                                binding.vpCajaSavings.setVisibility(android.view.View.GONE);
                            } else {
                                binding.vpCajaSavings.setVisibility(android.view.View.GONE);
                            }
                        } catch (Exception ignore) {}
                        int maxBoxes = Math.max(stockDisponibleReal / Math.max(cajaFactor, 1), 0);
                        try {
                            binding.vpBtnPaquete.setEnabled(maxBoxes > 0);
                            binding.vpBtnPaquete.setAlpha(maxBoxes > 0 ? 1f : 0.5f);
                        } catch (Exception ignore) {}
                        try {
                            binding.vpBtnUnidad.setOnClickListener(v -> {
                                modoCaja = false;
                                binding.vpTxtUnidad.setText("S/ " + producto.getPrecioUnidadBase() + " x " + producto.getUnidadBase());
                                
                                // Estilos de botones
                                binding.vpBtnUnidad.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.secondary)));
                                binding.vpBtnPaquete.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                                
                                // Ocultar ahorro en modo unidad
                                binding.vpCajaSavings.setVisibility(android.view.View.GONE);
                                
                                // Mostrar bonificación si está activa
                                if (bonusActivo) {
                                    binding.vpBonusContainer.setVisibility(android.view.View.VISIBLE);
                                }
                                
                                // Validar que la cantidad actual no exceda el stock máximo del nuevo modo
                                int maxQty = getMaxQtyAllowed();
                                if (cantidad > maxQty) {
                                    cantidad = maxQty;
                                    binding.vpTxtCantidad.setText(String.valueOf(cantidad));
                                    mostrarMaximoStock();
                                }
                                
                                verificarBotonIncremento();
                            });
                            binding.vpBtnPaquete.setOnClickListener(v -> {
                                modoCaja = true;
                                String label = (cajaUnidadDesc != null && !cajaUnidadDesc.isEmpty()) ? cajaUnidadDesc : "CAJA";
                                binding.vpTxtUnidad.setText("S/ " + new java.text.DecimalFormat("#0.00").format(cajaPrecio) + " x " + label);
                                
                                // Estilos de botones
                                binding.vpBtnUnidad.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                                binding.vpBtnPaquete.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.secondary)));
                                
                                double nu = producto.getPrecioUnidadBase() != null ? producto.getPrecioUnidadBase() : 0.0;
                                double cu = cajaFactor > 0 ? (cajaPrecio / cajaFactor) : cajaPrecio;
                                double au = Math.max(nu - cu, 0.0);
                                if (au > 0) {
                                    String t = "Ahorro por unidad S/ " + new java.text.DecimalFormat("#0.00").format(au) + " (x" + cajaFactor + ")";
                                    binding.vpCajaSavings.setText(t);
                                    binding.vpCajaSavings.setVisibility(android.view.View.VISIBLE);
                                } else {
                                    binding.vpCajaSavings.setVisibility(android.view.View.GONE);
                                }
                                
                                // Ocultar bonificación en modo caja
                                binding.vpBonusContainer.setVisibility(android.view.View.GONE);
                                
                                // Validar que la cantidad actual no exceda el stock máximo del nuevo modo
                                int maxQty = getMaxQtyAllowed();
                                if (cantidad > maxQty) {
                                    cantidad = maxQty;
                                    binding.vpTxtCantidad.setText(String.valueOf(cantidad));
                                    mostrarMaximoStock();
                                }
                                
                                verificarBotonIncremento();
                            });
                        } catch (Exception ignore) {}
                    }
                } catch (Exception ignore) {}
            }, err -> {
                // ignorar errores silenciosamente; no bloquear la vista
            });
            req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(9000, 2, 1));
            req.setShouldCache(false);
            com.android.volley.toolbox.Volley.newRequestQueue(this).add(req);
        } catch (Exception ignore) {}
    }

    private void actualizarDescuentoDinamico() {
        actualizarInterfazVisualDescuento();
    }
    
    private void actualizarInterfazVisualDescuento() {
        if (!descuentoActivo || descuentosArrayCompleto == null) {
            // Ocultar elementos visuales
            binding.vpDiscountPercentLabel.setVisibility(View.GONE);
            binding.vpDiscountSubtotal.setVisibility(View.GONE);
            binding.vpDiscountTotal.setVisibility(View.GONE);
            binding.vpDiscountProgress.setVisibility(View.GONE);
            binding.vpDiscountSavings.setVisibility(View.GONE);
            binding.vpDiscountConditionType.setVisibility(View.GONE);
            return;
        }
        
        int qty = cantidad != null ? cantidad : 0;
        double valorVenta = descuentoPrecioActual * qty; // Valor total de la venta
        double porcentajeAplicado = 0;
        String rangoAplicado = "";
        String tipoCondicionAplicado = "";
        double ahorroValorEscalonado = 0.0;
        boolean usoEscalonadoValor = false;
        
        try {
            java.util.List<org.json.JSONObject> reglasValor = new java.util.ArrayList<>();
            for (int i = 0; i < descuentosArrayCompleto.length(); i++) {
                org.json.JSONObject d = descuentosArrayCompleto.getJSONObject(i);
                String t = d.optString("tipoCondicion", "").trim();
                if (t.equals("ValorVenta")) reglasValor.add(d);
            }
            java.util.Collections.sort(reglasValor, (a, b) -> {
                double ad = 0, bd = 0;
                try { ad = Double.parseDouble(a.optString("valorDesde", "0").trim()); } catch (Exception ignore) {}
                try { bd = Double.parseDouble(b.optString("valorDesde", "0").trim()); } catch (Exception ignore) {}
                return Double.compare(ad, bd);
            });
            double porcentajeSeleccionado = 0.0;
            double desdeSel = 0.0;
            double hastaSel = Double.MAX_VALUE;
            for (org.json.JSONObject descuento : reglasValor) {
                double porcentaje = descuento.optDouble("PorcentajeDescuento", descuento.optDouble("porcentajeDescuento", 0.0));
                String valorDesdeStr = descuento.optString("valorDesde", "").trim();
                String valorHastaStr = descuento.optString("valorHasta", "").trim();
                Integer idRegla = null;
                try { idRegla = descuento.has("IDDescuento") ? descuento.optInt("IDDescuento") : (descuento.has("idDescuento") ? descuento.optInt("idDescuento") : null); } catch(Exception ignore) {}
                double valorDesde = 0;
                double valorHasta = Double.MAX_VALUE;
                if (!valorDesdeStr.isEmpty()) { try { valorDesde = Double.parseDouble(valorDesdeStr.trim()); } catch (Exception ignore) { valorDesde = 0; } }
                if (!valorHastaStr.isEmpty() && !valorHastaStr.equals("null")) { try { valorHasta = Double.parseDouble(valorHastaStr.trim()); } catch (Exception ignore) { valorHasta = Double.MAX_VALUE; } }
                boolean contiene = (valorVenta >= valorDesde && valorVenta <= valorHasta);
                boolean geSinTope = (valorHasta == Double.MAX_VALUE && valorVenta >= valorDesde);
                if (contiene || geSinTope) {
                    porcentajeSeleccionado = porcentaje;
                    desdeSel = valorDesde;
                    hastaSel = valorHasta;
                    descuentoIdSeleccionado = idRegla;
                    break;
                }
            }
            if (porcentajeSeleccionado > 0.0) {
                porcentajeAplicado = porcentajeSeleccionado;
                tipoCondicionAplicado = "Por valor";
                if (hastaSel == Double.MAX_VALUE) {
                    rangoAplicado = "Desde S/ " + new java.text.DecimalFormat("#0").format(desdeSel);
                } else {
                    rangoAplicado = "S/ " + new java.text.DecimalFormat("#0").format(desdeSel) + " - S/ " + new java.text.DecimalFormat("#0").format(hastaSel);
                }
            }
            for (int i = 0; i < descuentosArrayCompleto.length(); i++) {
                org.json.JSONObject descuento = descuentosArrayCompleto.getJSONObject(i);
                double porcentaje = descuento.optDouble("PorcentajeDescuento", descuento.optDouble("porcentajeDescuento", 0.0));
                String tipoCondicion = descuento.optString("tipoCondicion", "").trim();
                if (porcentaje > 0 && tipoCondicion.equals("CantidadBase")) {
                    String valorDesdeStr = descuento.optString("valorDesde", "").trim();
                    String valorHastaStr = descuento.optString("valorHasta", "").trim();
                    int cantidadDesde = 0;
                    int cantidadHasta = Integer.MAX_VALUE;
                    if (!valorDesdeStr.isEmpty()) {
                        try { cantidadDesde = Integer.parseInt(valorDesdeStr.trim()); } catch (Exception ignore) { cantidadDesde = 0; }
                    }
                    if (!valorHastaStr.isEmpty() && !valorHastaStr.equals("null")) {
                        try { cantidadHasta = Integer.parseInt(valorHastaStr.trim()); } catch (Exception ignore) { cantidadHasta = Integer.MAX_VALUE; }
                    }
                    if (qty >= cantidadDesde && qty <= cantidadHasta) {
                        porcentajeAplicado = porcentaje;
                        if (cantidadHasta == Integer.MAX_VALUE) {
                            rangoAplicado = "Desde " + cantidadDesde + " unidades";
                        } else {
                            rangoAplicado = cantidadDesde + " - " + cantidadHasta + " unidades";
                        }
                        tipoCondicionAplicado = "Por cantidad";
                        break;
                    }
                }
            }
        } catch (Exception e) {
            porcentajeAplicado = descuentoPorcentaje;
        }
        
        // Calcular valores
        double ahorroTotal = 0;
        double totalConDescuento = valorVenta;
        if (porcentajeAplicado > 0) {
            ahorroTotal = valorVenta * (porcentajeAplicado / 100.0);
            totalConDescuento = valorVenta - ahorroTotal;
        }

        double porcentajeLabel = porcentajeAplicado;
        
        java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
        java.text.DecimalFormat dfPorcentaje = new java.text.DecimalFormat("#0.#"); // Muestra decimales solo cuando existen
        
        // Mostrar elementos visuales
        binding.vpDiscountPercentLabel.setVisibility(View.VISIBLE);
        binding.vpDiscountSubtotal.setVisibility(View.VISIBLE);
        binding.vpDiscountTotal.setVisibility(View.VISIBLE);
        binding.vpDiscountConditionType.setVisibility(View.VISIBLE);
        
        // Actualizar tipo de condición
        binding.vpDiscountConditionType.setText(tipoCondicionAplicado.isEmpty() ? "Por valor" : tipoCondicionAplicado);
        
        // Actualizar porcentaje con diseño destacado - mostrar el porcentaje de la regla aplicable (no el promedio efectivo)
        binding.vpDiscountPercentLabel.setText(dfPorcentaje.format(porcentajeLabel) + "%");
        
        // Actualizar montos
        binding.vpDiscountSubtotal.setText("S/ " + df.format(valorVenta));
        binding.vpDiscountTotal.setText("S/ " + df.format(totalConDescuento));
        
        // Mostrar ahorro con diseño mejorado
        if (ahorroTotal > 0.0) {
            binding.vpDiscountSavings.setVisibility(View.VISIBLE);
            String mensajeAhorro = "💰 ¡AHORRAS S/ " + df.format(ahorroTotal) + "!";
            if (!rangoAplicado.isEmpty()) {
                mensajeAhorro += "\n" + rangoAplicado + " → " + dfPorcentaje.format(porcentajeLabel) + "% descuento";
            }
            if (descuentoIdSeleccionado != null) {
                mensajeAhorro += "\nIDRegla: " + descuentoIdSeleccionado;
            }
            binding.vpDiscountSavings.setText(mensajeAhorro);
            
            // Mostrar barra de progreso si hay ahorro
            binding.vpDiscountProgress.setVisibility(View.VISIBLE);
            binding.vpDiscountProgress.setProgress(Math.min((int)Math.round(porcentajeLabel), 100));
        } else {
            binding.vpDiscountSavings.setVisibility(View.GONE);
            binding.vpDiscountProgress.setVisibility(View.GONE);
            // Si no hay descuento aplicable, ocultar también el tipo de condición
            if (porcentajeAplicado == 0) {
                binding.vpDiscountConditionType.setVisibility(View.GONE);
            }
        }
    }

    // Método de aplicarBonificacion desde cache eliminado al revertir el sistema de cache

    private void actualizarBonificacionDinamica() {
        if (!bonusActivo) {
            binding.vpBonusDynamicCount.setText("");
            binding.vpBonusNextHint.setText("");
            binding.vpBonusSavings.setText("");
            binding.vpBonusProgress.setProgress(0);
            binding.vpBonusProgress.setEnabled(false);
            return;
        }

        // Si no hay stock para bonificación, deshabilitar interacción y mostrar estado gris
        if (!bonusHayStock) {
            binding.vpBonusDynamicCount.setText("Sin stock para bonificación");
            binding.vpBonusSavings.setText("");
            binding.vpBonusNextHint.setText("");
            binding.vpBonusProgress.setMax(bonusStepUnidades);
            binding.vpBonusProgress.setProgress(0);
            binding.vpBonusProgress.setEnabled(false);
            int grey = getResources().getColor(android.R.color.darker_gray);
            binding.vpBonusProgress.setProgressTintList(ColorStateList.valueOf(grey));
            binding.vpBonusNextHint.setTextColor(grey);
            binding.vpBonusDynamicCount.setTextColor(grey);
            ultimoTotalRegalos = 0;
            return;
        }

        binding.vpBonusProgress.setEnabled(true);

        // Considerar factor del producto para convertir a unidades reales
        int factor = (producto != null) ? Math.max(producto.getFactor(), 1) : 1;
        int qtyBase = (cantidad != null ? cantidad : 0);
        int qtyUnidades = qtyBase * factor;
        int stepUnidades = Math.max(bonusStepUnidades, 1);
    
        int pasos = stepUnidades > 0 ? (qtyUnidades / stepUnidades) : 0;
        int totalRegalos = pasos * bonusCantidadPorPaso;
    
        // Texto principal de conteo
        if (totalRegalos > 0) {
            binding.vpBonusDynamicCount.setText("Recibirás: " + totalRegalos + " x " + bonusNombreObsequio);
        } else {
            binding.vpBonusDynamicCount.setText("Aún sin obsequios. ¡Sigue sumando!");
        }
    
        // Ahorro si hay precio del obsequio, si no, mostrar progreso en unidades
        if (bonusPrecioObsequio > 0 && totalRegalos > 0) {
            double ahorro = totalRegalos * bonusPrecioObsequio;
            java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
            binding.vpBonusSavings.setText("Ahorro: S/ " + df.format(ahorro));
        } else {
            // Mostrar progreso en unidades hacia el próximo obsequio
            binding.vpBonusSavings.setText("Progreso: " + Math.min(qtyUnidades, stepUnidades) + " unidades de " + stepUnidades + " necesarias");
        }
    
        // Progreso hacia el siguiente obsequio (en unidades reales)
        int restoUnidades = qtyUnidades % stepUnidades;
        int faltanUnidades = restoUnidades == 0 ? (stepUnidades) : (stepUnidades - restoUnidades);
        // Valor de progreso: cercanía en unidades al siguiente obsequio
        int progresoActualUnidades = (restoUnidades == 0 && qtyUnidades > 0) ? stepUnidades : restoUnidades;
        binding.vpBonusProgress.setMax(stepUnidades);
        binding.vpBonusProgress.setProgress(progresoActualUnidades);

        if (faltanUnidades > 0) {
            binding.vpBonusNextHint.setText("Faltan " + faltanUnidades + " unidades para otro obsequio.");
        } else {
            binding.vpBonusNextHint.setText("");
        }

        // Colores dinámicos según cercanía
        float ratio = (float) progresoActualUnidades / (float) stepUnidades;
        int color;
        if (ratio >= 0.8f) {
            color = getResources().getColor(android.R.color.holo_orange_dark);
        } else if (ratio >= 0.4f) {
            color = getResources().getColor(android.R.color.holo_green_dark);
        } else {
            color = getResources().getColor(android.R.color.darker_gray);
        }
        binding.vpBonusProgress.setProgressTintList(ColorStateList.valueOf(color));
        binding.vpBonusNextHint.setTextColor(color);
        binding.vpBonusDynamicCount.setTextColor(color);

        // Animación suave si aumentan los obsequios
        if (totalRegalos > ultimoTotalRegalos) {
            try {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_out);
                binding.vpBonusDynamicCount.startAnimation(animation);
                binding.vpBonusSavings.startAnimation(animation);
            } catch (Exception ex) {
                // Silenciar si el recurso de animación no existe
            }
        }
        ultimoTotalRegalos = totalRegalos;
    }

    private void mostrarSinStock() {
        Toast.makeText(this, "Producto sin stock", Toast.LENGTH_SHORT).show();
    }

    private void mostrarMaximoStock() {
        int maxQty = getMaxQtyAllowed();
        if (maxQty > 0) {
            Toast.makeText(this, "Stock máximo: " + maxQty, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Stock no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarBotonIncremento() {
        boolean habilitar = cantidad < getMaxQtyAllowed();
        binding.vpBtnAumentar.setEnabled(habilitar);
    }

    private int getMaxQtyAllowed() {
        if (modoCaja && cajaDisponible) {
            int f = Math.max(cajaFactor, 1);
            return Math.max(stockDisponibleReal / f, 0);
        }
        return Math.max(stockDisponibleReal, 0);
    }
}
