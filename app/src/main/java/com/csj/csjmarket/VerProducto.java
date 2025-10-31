package com.csj.csjmarket;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
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
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.csj.csjmarket.databinding.ActivityVerProductoBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.ui.Ayudas;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

import android.content.res.ColorStateList;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class VerProducto extends AppCompatActivity {
    private ActivityVerProductoBinding binding;
    private Producto producto;
    private Integer cantidad = 1;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private ArrayList<MiCarrito> miCarrito;
    private int stockFisico = 0;
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
        stockFisico = producto != null ? producto.getStockDisponible() : 0;
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
        Glide.with(this)
                .load(getString(R.string.connection) + "/imagenes/" + producto.getCodigo() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(binding.vpImagenProducto);

        // Mostrar stock disponible real en la vista de cantidad
        binding.vpTxtStockDisponible.setText("Stock: " + stockDisponibleReal);
        // Mostrar código del producto debajo del stock
        binding.vpTxtCodigoProducto.setText("Código: " + producto.getCodigo());

        // Solicitar bonificación: mostrar loader solo si el producto indica bonificación
        if (producto != null) {
            boolean mostrarLoader = false;
            try {
                mostrarLoader = producto.isTieneBonificacion();
            } catch (Exception ignore) {}
            cargarBonificacion(producto.getId(), mostrarLoader);
        } else {
            binding.vpBonusContainer.setVisibility(View.GONE);
        }

        binding.vpBtnAumentar.setOnClickListener(view -> {
            if (binding.vpTxtCantidad.getText().toString().equals("")){
                cantidad = 1;
                binding.vpTxtCantidad.setText(cantidad.toString());
            }else {
                cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                if (cantidad >= stockDisponibleReal) {
                    mostrarMaximoStock();
                } else {
                    cantidad++;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }
            }
            verificarBotonIncremento();
            actualizarBonificacionDinamica();
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

                if (cantidad > stockDisponibleReal) {
                    cantidad = Math.max(stockDisponibleReal, 0);
                    binding.vpTxtCantidad.setText(cantidad.toString());
                    mostrarMaximoStock();
                }
                actualizarBonificacionDinamica();
            }
        });

        binding.vpBtnComprar.setOnClickListener(view -> {
            // Validación de stock antes de agregar
            int qty = cantidad != null ? cantidad : 0;
            if (stockDisponibleReal <= 0) {
                mostrarSinStock();
                return;
            }
            if (qty <= 0) {
                Toast.makeText(this, "Ingrese una cantidad válida", Toast.LENGTH_SHORT).show();
                return;
            }
            if (qty > stockDisponibleReal) {
                mostrarMaximoStock();
                return;
            }

            MiCarrito miCarritoItem = new MiCarrito();
            miCarritoItem.setIdProducto(producto.getId());
            miCarritoItem.setIdUnidad(producto.getIdUnidadBase());
            miCarritoItem.setNombre(producto.getNombre());
            miCarritoItem.setUnidad(producto.getUnidadBase());
            miCarritoItem.setCantidad(qty);
            miCarritoItem.setPrecio(producto.getPrecioUnidadBase());
            miCarritoItem.setTotal(producto.getPrecioUnidadBase() * qty);
            miCarritoItem.setCodigo(producto.getCodigo());
                miCarritoItem.setPeso(producto.getPeso());
                miCarritoItem.setPesoTotal(producto.getPeso() * qty);
                miCarritoItem.setTieneBonificacion(producto.isTieneBonificacion());
                // Guardar factor del producto para cálculos en carrito
                try { miCarritoItem.setFactor(Math.max(producto.getFactor(), 1)); } catch (Exception ignore) { miCarritoItem.setFactor(1); }
            // Persistir reglas de bonificación en el producto principal (para futuras sincronizaciones)
            try {
                if (bonusActivo && bonusStepUnidades > 0 && bonusCantidadPorPaso > 0) {
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
                        Double precioUnit = (item.getPrecio() != null ? item.getPrecio() : producto.getPrecioUnidadBase());
                        Double pesoUnit = (item.getPeso() != null ? item.getPeso() : producto.getPeso());
                        item.setTotal(precioUnit * nuevaCantidad);
                        item.setPesoTotal(pesoUnit * nuevaCantidad);
                        // Asegurar factor definido para sincronización de regalos en carrito
                        try { if (item.getFactor() == null || item.getFactor() <= 0) item.setFactor(Math.max(producto.getFactor(), 1)); } catch (Exception ignore) {}
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
                if (bonusActivo && bonusHayStock && bonusStepUnidades > 0 && bonusCantidadPorPaso > 0) {
                    int factor = Math.max(producto != null ? producto.getFactor() : 1, 1);
                    int qtyUnidades = qty * factor;
                    int pasos = qtyUnidades / bonusStepUnidades;
                    int totalRegalos = pasos * bonusCantidadPorPaso;
                    if (totalRegalos > 0) {
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

    private void cargarBonificacion(int idProducto, boolean mostrarLoaderInicial) {
        // Mostrar u ocultar loader según flag
        if (mostrarLoaderInicial) {
            binding.vpBonusContainer.setVisibility(View.VISIBLE);
            binding.vpBonusLoader.setVisibility(View.VISIBLE);
        } else {
            binding.vpBonusContainer.setVisibility(View.GONE);
            binding.vpBonusLoader.setVisibility(View.GONE);
        }
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
                    binding.vpBonusContainer.setVisibility(View.VISIBLE);
                    binding.vpBonusTitle.setVisibility(View.VISIBLE);
                    binding.vpBonusMessage.setVisibility(View.VISIBLE);
                    binding.vpBonusRequirement.setVisibility(View.VISIBLE);
                    binding.vpBonusGift.setVisibility(View.GONE);
                    binding.vpBonusDynamicCount.setVisibility(View.GONE);
                    binding.vpBonusStock.setVisibility(View.GONE);
                    binding.vpBonusValidity.setVisibility(View.GONE);
                    binding.vpBonusNextHint.setVisibility(View.GONE);
                    binding.vpBonusSavings.setVisibility(View.GONE);
                    binding.vpBonusProgress.setVisibility(View.GONE);

                    binding.vpBonusMessage.setText(mensajePromocional);
                    binding.vpBonusRequirement.setText(!tipoCondicion.isEmpty() ? tipoCondicion : "Este producto no tiene bonificaciones disponibles actualmente");
                    binding.vpBonusValidity.setText("");
                    binding.vpBonusDynamicCount.setText("");
                    binding.vpBonusNextHint.setText("");
                    binding.vpBonusSavings.setText("");
                    binding.vpBonusProgress.setProgress(0);
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
                String mensajeFull = (mensajePromocional != null ? mensajePromocional.replace("`", "").trim() : "");
                if (cantidadObsequiado > 0 && nombreProductoObsequiado != null && !nombreProductoObsequiado.trim().isEmpty()) {
                    mensajeFull = (mensajeFull.isEmpty() ? "" : (mensajeFull + " ")) + "llevate " + cantidadObsequiado + " " + nombreProductoObsequiado.trim();
                }
                binding.vpBonusMessage.setText(mensajeFull);

                // Requisito porCada + unidad del producto
                String unidadLabel = (producto != null && producto.getUnidadBase() != null && !producto.getUnidadBase().trim().isEmpty()) ? producto.getUnidadBase().trim() : "unidades";
                int factor = (producto != null) ? Math.max(producto.getFactor(), 1) : 1;
                // Convertir porCada a unidades base (ej. tiras) si factor > 1
                double porCadaBase = factor > 0 ? (porCada / factor) : porCada;
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
                if (imagenProducto != null && !imagenProducto.trim().isEmpty()) {
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

                binding.vpBonusContainer.setVisibility(View.VISIBLE);
                binding.vpBonusProgress.setMax(bonusStepUnidades);
                binding.vpBonusProgress.setProgress(0);
                binding.vpBonusProgress.setVisibility(View.VISIBLE);

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
        if (stepUnidades > 0) {
            int restoUnidades = qtyUnidades % stepUnidades;
            int faltanUnidades = restoUnidades == 0 ? (qtyUnidades > 0 ? stepUnidades : stepUnidades) : (stepUnidades - restoUnidades);
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
            float ratio = stepUnidades > 0 ? ((float) progresoActualUnidades / (float) stepUnidades) : 0f;
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
        }
    
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
        if (stockDisponibleReal > 0) {
            Toast.makeText(this, "Stock máximo: " + stockDisponibleReal, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Stock no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarBotonIncremento() {
        boolean habilitar = cantidad < stockDisponibleReal;
        binding.vpBtnAumentar.setEnabled(habilitar);
    }
}