package com.csj.csjmarket;

import static android.content.ContentValues.TAG;
import static com.android.volley.Response.error;
import static com.android.volley.Response.success;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import com.csj.csjmarket.databinding.ActivityDireccionEntregaBinding;
import com.csj.csjmarket.modelos.CabeceraPedido;
import com.csj.csjmarket.modelos.Direccion;
import com.csj.csjmarket.modelos.ItemPedido;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.PedidoNuevo;
import com.csj.csjmarket.modelos.PedidoNuevoOptimizado;
import com.csj.csjmarket.modelos.PedidoActual;
import com.csj.csjmarket.modelos.ProductoItem;
import com.csj.csjmarket.modelos.RespuestaPedido;

import com.csj.csjmarket.ui.adaptadores.itemDireccionAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DireccionEntrega extends AppCompatActivity implements itemDireccionAdapter.OnItemChangedListener {
    private ActivityDireccionEntregaBinding binding;
    private AlertDialog alertDialog;
    private AlertDialog loaderDialog;
    private ArrayList<Direccion> direcciones = new ArrayList<>();
    private CabeceraPedido cabeceraPedido = new CabeceraPedido();
    private ArrayList<ItemPedido> itemsPedido = new ArrayList<>();
    private PedidoNuevo pedidoNuevo = new PedidoNuevo();
    private PedidoNuevoOptimizado pedidoNuevoOptimizado = new PedidoNuevoOptimizado();
    private SharedPreferences sharedPreferences;
    private ArrayList<MiCarrito> carrito;
    private RespuestaPedido respuestaPedido;
    private FirebaseAuth mAuth;
    private boolean pedidoEnProceso = false;
    private Button btnContinuarDialog;
    private android.os.Handler pedidoTimeoutHandler;
    private Runnable pedidoTimeoutRunnable;
    private String lastRequestId;
    private boolean mostrandoModalDireccion = false;
    private boolean suprimirSiguienteModalDireccion = false;

    // Campos para manejo de factura
    private boolean facturaActiva = false;
    private com.csj.csjmarket.modelos.RucData rucDataActual = null;

    private Double montoTotal;
    private String idPersona;
    private String email;
    private String nombre;
    private String docIdentidad;
    private String diasUltimaCompra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("NiubizVersion", "SDK Version: visanet-lib-lite-release-2.2.1");
        setContentView(R.layout.activity_direccion_entrega);

        binding = ActivityDireccionEntregaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        montoTotal = getIntent().getDoubleExtra("totalVenta", 0.0);
        idPersona = getIntent().getStringExtra("idPersona");
        email = getIntent().getStringExtra("email");
        docIdentidad = getIntent().getStringExtra("docIden");
        diasUltimaCompra = getIntent().getStringExtra("diasUltCompra");
        nombre = getIntent().getStringExtra("nombre");

        binding.vpBtnRegresar.setOnClickListener(view -> finish());

        binding.rvListaDireccion.setLayoutManager(new LinearLayoutManager(this));

        binding.btnAddDireccion.setOnClickListener(view -> {
            Intent intent = new Intent(DireccionEntrega.this, MapsActivity.class);
            intent.putExtra("idPersona", idPersona);
            startActivity(intent);
        });

        binding.btnSolicitudFactura.setOnClickListener(view -> {
            mostrarDialogoFactura();
        });

        // Verificar si hay factura activa al iniciar
        verificarFacturaActiva();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarFacturaActiva();
        recargar();
    }

    private void mostrarLoader() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View progress = getLayoutInflater().inflate(R.layout.loader, null);
        builder.setView(progress);
        builder.setCancelable(false);
        loaderDialog = builder.create();
        loaderDialog.show();
    }

    private void mostrarLoader(String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View progress = getLayoutInflater().inflate(R.layout.loader, null);
        TextView tvMsg = progress.findViewById(R.id.tvLoaderMessage);
        if (tvMsg != null) {
            tvMsg.setText(mensaje);
        }
        builder.setView(progress);
        builder.setCancelable(false);
        loaderDialog = builder.create();
        loaderDialog.show();
    }

    private void mostrarAlerta(String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle("Error");
        builder.setMessage(mensaje);
        builder.setPositiveButton("Aceptar", null);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    // Diálogo de alerta con botón "Reintentar" para relanzar enviarPedido()
    private void mostrarAlertaReintentar(String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle("Pedido no confirmado");
        builder.setMessage(mensaje);
        builder.setPositiveButton("Reintentar", (dialog, which) -> {
            try {
                dialog.dismiss();
            } catch (Exception ignore) {
            }
            if (lastRequestId != null && !lastRequestId.trim().isEmpty()) {
                mostrarLoader("Verificando estado del pedido...");
                verificarEstadoPedidoYProcesar(lastRequestId);
            } else {
                mostrarLoader("Reintentando crear pedido, por favor espere...");
                enviarPedido();
            }
        });
        builder.setNegativeButton("Aceptar", null);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarMensaje() {
        FirebaseUser user = mAuth.getCurrentUser();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_baseline_good);
        builder.setTitle("Aviso");
        builder.setMessage("");
        builder.setPositiveButton("Aceptar", (dialogInterface, i) -> {
            finish();
        });
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarRegistro() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        FirebaseUser user = mAuth.getCurrentUser();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_registro, null);
        TextView txtNumPedidoNiubiz = view.findViewById(R.id.dr_txtNumPedidoNiubiz);
        TextView txtNumPedido = view.findViewById(R.id.dr_txtNumPedido);
        TextView txtFechaHora = view.findViewById(R.id.dr_txtFechaHora);
        TextView txtImporte = view.findViewById(R.id.dr_txtImporte);
        TextView txtTarjeta = view.findViewById(R.id.dr_txtTarjeta);
        TextView txtMensaje = view.findViewById(R.id.dr_txtMensaje);
        TextView numOpe = view.findViewById(R.id.dr_NumPedido);
        TextView tarjeta = view.findViewById(R.id.dr_tarjeta);
        TextView importe = view.findViewById(R.id.dr_importe);
        TextView txtMensajeDelivery = view.findViewById(R.id.dr_txtMensajeDelivery);

        // Validación y logs de respuesta
        if (respuestaPedido == null) {
            Log.e("PedidoConfirm", "respuestaPedido es null al mostrar registro");
            mostrarAlerta("No se pudo confirmar el pedido. Por favor, reintente.");
            return;
        }
        Log.i("PedidoConfirm", "NumCp=" + String.valueOf(respuestaPedido.getNumCp()));
        Log.i("PedidoConfirm", "Fecha=" + String.valueOf(respuestaPedido.getFecha()));

        String numCp = respuestaPedido.getNumCp() != null ? respuestaPedido.getNumCp().trim() : "";
        txtNumPedido.setText(numCp.isEmpty() ? "-" : numCp);

        // Parsing de fecha con fallback
        String fechaStr = respuestaPedido.getFecha();
        String formattedDate = "";
        if (fechaStr != null && !fechaStr.trim().isEmpty()) {
            Date date = null;
            // Intento 1: formato con milisegundos
            SimpleDateFormat inputDateFormatMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            try {
                date = inputDateFormatMs.parse(fechaStr);
            } catch (Exception e1) {
                Log.w("PedidoConfirm", "Fallo parse con milisegundos: " + e1.getMessage());
                // Intento 2: formato sin milisegundos
                try {
                    SimpleDateFormat inputDateFormatNoMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                            Locale.getDefault());
                    date = inputDateFormatNoMs.parse(fechaStr);
                } catch (Exception e2) {
                    Log.e("PedidoConfirm", "Fallo parse sin milisegundos: " + e2.getMessage());
                }
            }
            if (date != null) {
                formattedDate = outputDateFormat.format(date);
            }
        }
        if (formattedDate.isEmpty()) {
            // Fallback final: usar fecha/hora actual si no vino formateable
            SimpleDateFormat out = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            formattedDate = out.format(new Date());
            Log.w("PedidoConfirm", "Usando fecha/hora actual como fallback");
        }
        txtFechaHora.setText(formattedDate);

        // Ocultar siempre datos de tarjeta y operación Niubiz
        numOpe.setVisibility(View.GONE);
        tarjeta.setVisibility(View.GONE);
        importe.setVisibility(View.GONE);
        txtNumPedidoNiubiz.setVisibility(View.GONE);
        txtImporte.setVisibility(View.GONE);
        txtTarjeta.setVisibility(View.GONE);

        // Mensaje de confirmación con email
        String emailMsg = (user != null && user.getEmail() != null) ? user.getEmail() : "su correo";
        txtMensaje.setText("Hemos enviado un mensaje de correo electrónico a " + emailMsg
                + " con el detalle de su pedido. Por favor, compruebe su bandeja de entrada.");

        txtMensajeDelivery.setVisibility(View.GONE);

        builder.setView(view).setPositiveButton("Aceptar", (dialogInterface, i) -> {
            finish();
        })
                .setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarMedioPago() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_medio_de_pago, null);
        RadioButton rbEnLinea = view.findViewById(R.id.mp_rbEnLinea);
        Button btnContinuar = view.findViewById(R.id.mp_btnContinuar);
        btnContinuarDialog = btnContinuar;

        // Mostrar opción de pago en línea deshabilitada visualmente
        rbEnLinea.setVisibility(View.VISIBLE);
        rbEnLinea.setEnabled(false);
        rbEnLinea.setChecked(false);
        rbEnLinea.setText(rbEnLinea.getText() + " (Próximamente)");
        rbEnLinea.setTextColor(android.graphics.Color.GRAY);

        btnContinuar.setOnClickListener(view1 -> {
            Log.d("NiubizFlow", "=== BOTÓN CONTINUAR PRESIONADO ===");
            if (pedidoEnProceso) {
                return;
            }
            btnContinuar.setEnabled(false);
            // Forzar contra entrega
            Log.d("NiubizFlow", "=== SELECCIONADO PAGO CONTRA ENTREGA (forzado) ===");
            cabeceraPedido.setTipoCp(4415);
            alertDialog.dismiss();
            mostrarLoader("Creando pedido, por favor espere...");
            enviarPedido();
        });

        builder.setView(view);

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarAdvertenciaHorario() {
        Log.d("NiubizFlow", "=== MOSTRAR ADVERTENCIA HORARIO ===");
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        Log.d("NiubizFlow", "Día de la semana: " + dayOfWeek + ", Hora: " + hour);

        boolean fueraHorario = false;
        String mensaje = null;
        if (dayOfWeek == Calendar.SUNDAY) {
            // Domingo: todo el día fuera de horario, entrega mañana (lunes)
            fueraHorario = true;
            mensaje = "Nuestro reparto no está atendiendo en este momento. Su pedido se le entregará mañana a partir de las 9:00 am. ¿Desea continuar?";
        } else if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) {
            // Lunes a viernes: después de las 5 pm
            if (hour >= 17) {
                fueraHorario = true;
                mensaje = "Nuestro reparto no está atendiendo en este momento. Su pedido se le entregará mañana a partir de las 9:00 am. ¿Desea continuar?";
            }
        } else if (dayOfWeek == Calendar.SATURDAY) {
            // Sábado: después de las 2 pm, entrega el lunes
            if (hour >= 14) {
                fueraHorario = true;
                mensaje = "Nuestro reparto no está atendiendo en este momento. Su pedido se le entregará el lunes a partir de las 9:00 am. ¿Desea continuar?";
            }
        }

        Log.d("NiubizFlow", "¿Fuera de horario? " + fueraHorario);

        if (!fueraHorario) {
            mostrarMedioPago();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_advertencia_horario, null);
        TextView txtTitulo = view.findViewById(R.id.ah_txtTitulo);
        TextView txtMensaje = view.findViewById(R.id.ah_txtMensaje);
        txtTitulo.setText("Aviso de horario");
        // Usar mensaje calculado según día/hora
        String nuevoMensaje = mensaje != null ? mensaje
                : "Nuestro reparto no está atendiendo en este momento. Su pedido se le entregará mañana a partir de las 9:00 am. ¿Desea continuar?";
        txtMensaje.setText(nuevoMensaje);

        Button btnNo = view.findViewById(R.id.ah_btnNo);
        Button btnSi = view.findViewById(R.id.ah_btnSi);
        btnNo.setOnClickListener(v -> {
            alertDialog.dismiss();
        });
        btnSi.setOnClickListener(v -> {
            alertDialog.dismiss();
            mostrarMedioPago();
        });

        builder.setView(view)
                .setCancelable(true);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void recargar() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/cliente/direcciones?idPersona=" + idPersona;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            Type productListType = new TypeToken<List<Direccion>>() {
            }.getType();
            direcciones = gson.fromJson(response.toString(), productListType);
            itemDireccionAdapter direccionAdapter = new itemDireccionAdapter(direcciones);
            direccionAdapter.setOnItemChangedListener(this);
            binding.rvListaDireccion.setAdapter(direccionAdapter);
            direccionAdapter.notifyDataSetChanged();
            if (loaderDialog != null && loaderDialog.isShowing())
                loaderDialog.dismiss();
        }, error -> {
            if (loaderDialog != null && loaderDialog.isShowing())
                loaderDialog.dismiss();
            if (error instanceof TimeoutError) {
                mostrarAlerta("Tiempo de espera agotado. Verifique su conexión y reintente.");
            } else if (error instanceof NoConnectionError) {
                mostrarAlerta("Sin conexión con el servidor. Revise su internet o el servicio.");
            } else {
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null && networkResponse.data != null) {
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                } else {
                    mostrarAlerta(error.toString());
                }
            }
        });
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(6000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        jsonArrayRequest.setShouldCache(false);
        Volley.newRequestQueue(this).add(jsonArrayRequest);
    }

    private void enviarPedido() {
        // Evitar envíos duplicados por taps repetidos o reintentos locales
        if (pedidoEnProceso) {
            mostrarAlerta("Un pedido está en proceso. Espere la respuesta antes de reintentar.");
            return;
        }
        pedidoEnProceso = true;
        if (btnContinuarDialog != null)
            btnContinuarDialog.setEnabled(false);

        try {
            if (lastRequestId == null || lastRequestId.trim().isEmpty()) {
                lastRequestId = java.util.UUID.randomUUID().toString();
            }
            String requestId = lastRequestId;

            // Crear PedidoActual (nueva estructura)
            PedidoActual pedidoActual = new PedidoActual();

            // Determinar si usar datos del RUC o del usuario logueado
            Integer idPersonaFinal;
            Integer idDireccionFinal;

            Log.i("PedidoFacturaDebug", "facturaActiva=" + facturaActiva);
            Log.i("PedidoFacturaDebug", "rucDataActual=" + (rucDataActual != null ? "OK" : "null"));
            if (rucDataActual != null) {
                Log.i("PedidoFacturaDebug",
                        "RUC IdUsuario=" + rucDataActual.getIdUsuario() + ", IdDireccion="
                                + rucDataActual.getIdDireccion() + ", Nombre=" + rucDataActual.getNombreCompleto());
            }
            Log.i("PedidoFacturaDebug", "Usuario loggeado idPersona=" + idPersona);

            if (facturaActiva && rucDataActual != null && rucDataActual.getIdUsuario() != null
                    && rucDataActual.getIdUsuario() > 0) {
                // MODO FACTURA: Usar IdPersona del RUC (empresa), pero dirección seleccionada por el usuario
                idPersonaFinal = rucDataActual.getIdUsuario();
                // Usar la dirección que el usuario seleccionó manualmente, NO la de la empresa automáticamente
                idDireccionFinal = cabeceraPedido.getIdDireccionEntrega();
                Log.i("PedidoFacturaDebug", "USANDO EMPRESA: IdPersona=" + idPersonaFinal 
                        + ", IdDireccion (seleccionada por usuario)=" + idDireccionFinal
                        + ", IdDireccion empresa=" + rucDataActual.getIdDireccion());
            } else {
                // MODO NORMAL: Usar datos del usuario logueado (persona natural)
                idPersonaFinal = Integer.parseInt(idPersona);
                idDireccionFinal = cabeceraPedido.getIdDireccionEntrega();
                Log.i("PedidoFacturaDebug", "USANDO USUARIO: IdPersona=" + idPersonaFinal + ", IdDireccion="
                        + idDireccionFinal);
            }

            pedidoActual.setIdPersona(idPersonaFinal);
            pedidoActual.setTotalVenta(montoTotal);
            pedidoActual.setPeso(getIntent().getDoubleExtra("totalPeso", 0.0));
            pedidoActual.setTipoCp(4415);
            int idDirEnvio = idDireccionFinal != null ? idDireccionFinal : cabeceraPedido.getIdDireccionEntrega();
            pedidoActual.setIdDireccionEntrega(idDirEnvio);

            // Buscar el texto de la dirección para enviarlo explícitamente y evitar error de NULL en BD
            String textoDireccion = "";
            if (direcciones != null) {
                for (Direccion d : direcciones) {
                    if (d.getId() == idDirEnvio) {
                        textoDireccion = d.getDescripcion();
                        break;
                    }
                }
            }
            // Si no se encontró en la lista de usuario, verificar si corresponde a la dirección fiscal del RUC
            if ((textoDireccion == null || textoDireccion.isEmpty()) && rucDataActual != null) {
                if (rucDataActual.getIdDireccion() != null && rucDataActual.getIdDireccion() == idDirEnvio) {
                    textoDireccion = rucDataActual.getDireccion();
                }
            }
            pedidoActual.setDireccion(textoDireccion != null ? textoDireccion : "");

            // Crear lista de ProductoItem (nueva estructura)
            ArrayList<ProductoItem> productosItems = new ArrayList<>();

            cargarCarrito();
            for (MiCarrito item : carrito) {
                try {
                    if (item.isEsBonificacion()) {
                        continue;
                    }
                } catch (Exception ignore) {
                }

                ProductoItem productoItem = new ProductoItem();
                productoItem.setIdCp(0);
                productoItem.setIdCpInventario(0);
                productoItem.setIdProducto(item.getIdProducto());
                productoItem.setIdUnidad(item.getIdUnidad());
                productoItem.setPeso(item.getPesoTotal());
                productoItem.setDescripcion(item.getNombre());
                productoItem.setCantidad(item.getCantidad());
                productoItem.setPrecio(item.getPrecio());
                productoItem.setTotal(item.getTotal());
                productoItem.setTieneBono(item.isTieneBonificacion());
                productoItem.setTieneDescuento(item.isTieneDescuento());
                try {
                    productoItem.setIdDefinicionDescuento(item.getIdDefinicionDescuento());
                } catch (Exception ignore) {
                }

                productosItems.add(productoItem);
            }

            // Configurar el nuevo objeto PedidoNuevoOptimizado
            pedidoNuevoOptimizado.setPedido(pedidoActual);
            pedidoNuevoOptimizado.setItems(productosItems);
            pedidoNuevoOptimizado.setRequestId(requestId);

            String endpointPath = "/api/pedido/CrearPedidoCompletoHibrido"; // NUEVO: Usar API híbrida
            String url = getString(R.string.connection) + endpointPath;
            final long startMs = System.currentTimeMillis();
            Gson gson = new Gson();
            JSONObject jsonBody = new JSONObject(gson.toJson(pedidoNuevoOptimizado));
            Log.i("PedidoPayload", jsonBody.toString());

            mostrarLoader("Enviando pedido...");

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.i("PedidoAPI", "Endpoint " + endpointPath + " ok en "
                                + (System.currentTimeMillis() - startMs) + " ms");
                        try {
                            int idCp = response.optInt("IdCp", 0);
                            String numCp = response.optString("NumCp");
                            String fecha = response.optString("Fecha");
                            if (fecha == null || fecha.isEmpty()) {
                                fecha = response.optString("FechaCreacion", "");
                            }

                            JSONObject rpJson = new JSONObject();
                            rpJson.put("IdCp", idCp);
                            rpJson.put("IdCpInventario", 0);
                            rpJson.put("NumCp", numCp);
                            rpJson.put("Fecha", fecha);
                            rpJson.put("RequestId", requestId);
                            rpJson.put("ImagenProducto", "");
                            this.respuestaPedido = gson.fromJson(rpJson.toString(), RespuestaPedido.class);

                            if (sharedPreferences == null) {
                                sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
                            }
                            try {
                                sharedPreferences.edit().clear().commit();
                            } catch (Exception ignore) {
                            }

                            // Enviar correo de confirmación de manera asíncrona y no bloqueante
                            enviarCorreoNuevo(idCp);

                            // Limpiar RequestId tras confirmación exitosa
                            lastRequestId = null;

                            mostrarRegistro();
                        } catch (Exception e) {
                            mostrarAlerta("Error al procesar respuesta: " + e.getMessage());
                        } finally {
                            pedidoEnProceso = false;
                            if (btnContinuarDialog != null)
                                btnContinuarDialog.setEnabled(true);
                            try {
                                if (loaderDialog != null && loaderDialog.isShowing())
                                    loaderDialog.dismiss();
                            } catch (Exception ignore) {
                            }
                        }
                    }, error -> {
                        Log.e("PedidoAPI", "Endpoint " + endpointPath + " error en "
                                + (System.currentTimeMillis() - startMs) + " ms: " + String.valueOf(error));
                        boolean treatAsTimeout = false;
                        int statusCode = -1;
                        String errorMessage = null;

                        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                            treatAsTimeout = true;
                        } else if (error.networkResponse != null && error.networkResponse.data != null) {
                            statusCode = error.networkResponse.statusCode;
                            errorMessage = new String(error.networkResponse.data);
                            if (statusCode == 504 || statusCode == 502) {
                                treatAsTimeout = true;
                            }
                        }

                        if (treatAsTimeout) {
                            mostrarLoader("Verificando estado del pedido...");
                            verificarEstadoPedidoYProcesar(requestId);
                        } else {
                            if (errorMessage != null && !errorMessage.isEmpty()) {
                                mostrarAlerta(errorMessage);
                            } else {
                                mostrarAlerta(error.toString());
                            }
                            // Reset estado al finalizar manejo de error no-timeout
                            pedidoEnProceso = false;
                            if (btnContinuarDialog != null)
                                btnContinuarDialog.setEnabled(true);
                            try {
                                if (loaderDialog != null && loaderDialog.isShowing())
                                    loaderDialog.dismiss();
                            } catch (Exception ignore) {
                            }
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            jsonObjectRequest
                    .setRetryPolicy(new DefaultRetryPolicy(180000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            jsonObjectRequest.setShouldCache(false);
            Volley.newRequestQueue(this).add(jsonObjectRequest);
        } catch (Exception e) {
            mostrarAlerta("Error al enviar pedido: " + e.getMessage());
            pedidoEnProceso = false;
            if (btnContinuarDialog != null)
                btnContinuarDialog.setEnabled(true);
            try {
                if (loaderDialog != null && loaderDialog.isShowing())
                    loaderDialog.dismiss();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Verifica si hay una factura activa guardada en SharedPreferences
     * y actualiza el estado de la UI
     */
    private void verificarFacturaActiva() {
        SharedPreferences sp = getSharedPreferences("rucData", MODE_PRIVATE);
        String json = sp.getString("rucData", null);
        Log.i("VerificarFactura", "JSON guardado: " + (json != null ? json : "null"));
        if (json != null) {
            try {
                rucDataActual = new Gson().fromJson(json, com.csj.csjmarket.modelos.RucData.class);
                Log.i("VerificarFactura", "RucData parseado - IdUsuario: " + (rucDataActual != null ? rucDataActual.getIdUsuario() : "null")
                        + ", IdDireccion: " + (rucDataActual != null ? rucDataActual.getIdDireccion() : "null")
                        + ", Nombre: " + (rucDataActual != null ? rucDataActual.getNombreCompleto() : "null"));
                if (rucDataActual != null && rucDataActual.getIdUsuario() != null && rucDataActual.getIdUsuario() > 0) {
                    facturaActiva = true;
                    actualizarUIFactura();
                    Log.i("FacturaActiva", "Factura cargada: " + rucDataActual.getNombreCompleto() + ", IdUsuario: "
                            + rucDataActual.getIdUsuario());

                    // Auto-seleccionar dirección del RUC si existe
                    if (rucDataActual.getIdDireccion() != null && rucDataActual.getIdDireccion() > 0) {
                        cabeceraPedido.setIdDireccionEntrega(rucDataActual.getIdDireccion());
                        Log.i("FacturaActiva",
                                "Dirección del RUC disponible: " + rucDataActual.getIdDireccion());
                        // No mostrar modal automáticamente al cargar, solo cuando el usuario seleccione una dirección
                    }
                } else {
                    Log.w("VerificarFactura", "RucData inválido - IdUsuario es 0 o null, desactivando factura");
                    facturaActiva = false;
                    rucDataActual = null;
                }
            } catch (Exception e) {
                Log.e("DireccionEntrega", "Error al cargar RUC: " + e.getMessage(), e);
                facturaActiva = false;
                rucDataActual = null;
            }
        } else {
            Log.i("VerificarFactura", "No hay JSON guardado, factura desactivada");
            facturaActiva = false;
            rucDataActual = null;
        }
    }

    /**
     * Actualiza la UI del botón de factura según el estado
     */
    private void actualizarUIFactura() {
        if (facturaActiva && rucDataActual != null) {
            String nombreCorto = rucDataActual.getNombreCompleto();
            if (nombreCorto != null && nombreCorto.length() > 25) {
                nombreCorto = nombreCorto.substring(0, 22) + "...";
            }
            binding.btnSolicitudFactura.setText("✓ Factura: " + nombreCorto);
            binding.btnSolicitudFactura.setEnabled(true);
        } else {
            binding.btnSolicitudFactura.setText("Solicitar factura");
            binding.btnSolicitudFactura.setEnabled(true);
        }
    }

    /**
     * Muestra un modal preguntando si desea usar la dirección de la empresa o cambiarla
     * Se llama cuando se registra el RUC o se carga la factura activa
     */
    private void mostrarConfirmacionDireccionFactura() {
        if (!facturaActiva || rucDataActual == null) {
            Log.w("ConfirmacionDireccion", "No hay factura activa, no se muestra modal");
            return;
        }
        Integer idDir = rucDataActual.getIdDireccion();
        if (idDir == null || idDir <= 0) {
            Log.w("ConfirmacionDireccion", "No hay IdDireccion válido, no se muestra modal");
            return;
        }

        if (mostrandoModalDireccion) {
            return;
        }
        mostrandoModalDireccion = true;
        Log.i("ConfirmacionDireccion", "Mostrando modal de confirmación de dirección");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_confirmacion_direccion_empresa, null);
        TextView txtTitulo = view.findViewById(R.id.de_txtTitulo);
        TextView txtDireccion = view.findViewById(R.id.de_txtDireccion);
        TextView txtDistrito = view.findViewById(R.id.de_txtDistrito);
        TextView txtPregunta = view.findViewById(R.id.de_txtPregunta);
        Button btnUsarEmpresa = view.findViewById(R.id.de_btnUsarEmpresa);
        Button btnCambiar = view.findViewById(R.id.de_btnUsarSeleccionada);

        txtTitulo.setText("Dirección de empresa");
        String direccionTexto = rucDataActual.getDireccion() != null ? rucDataActual.getDireccion() : "";
        String distrito = rucDataActual.getDistrito() != null ? rucDataActual.getDistrito() : "";
        txtDireccion.setText(direccionTexto);
        txtDistrito.setText(distrito.length() > 0 ? "Distrito: " + distrito : "");
        txtPregunta.setText("¿Desea usar esta dirección para la entrega?");
        btnUsarEmpresa.setText("Usar esta dirección");
        btnCambiar.setText("Cambiar dirección");

        builder.setView(view).setCancelable(true);
        alertDialog = builder.create();
        btnUsarEmpresa.setOnClickListener(v -> {
            Log.i("ConfirmacionDireccion", "Usuario aceptó usar dirección de empresa: " + idDir);
            cabeceraPedido.setIdDireccionEntrega(idDir);
            try { alertDialog.dismiss(); } catch (Exception ignore) {}
            continuarConEntrega();
        });
        btnCambiar.setOnClickListener(v -> {
            Log.i("ConfirmacionDireccion", "Usuario quiere cambiar dirección, se mantiene selección manual");
            suprimirSiguienteModalDireccion = true;
            try { alertDialog.dismiss(); } catch (Exception ignore) {}
        });
        alertDialog.setOnDismissListener(d -> mostrandoModalDireccion = false);
        alertDialog.show();
    }

    /**
     * Muestra un modal cuando el usuario selecciona una dirección diferente a la de la empresa
     * @param idDireccionSeleccionada La dirección que el usuario acaba de seleccionar
     */
    private void mostrarConfirmacionDireccionFacturaConOpcion(int idDireccionSeleccionada) {
        if (!facturaActiva || rucDataActual == null) {
            mostrarAdvertenciaHorario();
            return;
        }
        Integer idDirEmpresa = rucDataActual.getIdDireccion();
        if (idDirEmpresa == null || idDirEmpresa <= 0) {
            mostrarAdvertenciaHorario();
            return;
        }

        if (mostrandoModalDireccion) {
            return;
        }
        mostrandoModalDireccion = true;
        Log.i("ConfirmacionDireccion", "Mostrando modal de confirmación (dirección seleccionada: " + idDireccionSeleccionada + ")");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_confirmacion_direccion_empresa, null);
        TextView txtTitulo = view.findViewById(R.id.de_txtTitulo);
        TextView txtDireccion = view.findViewById(R.id.de_txtDireccion);
        TextView txtDistrito = view.findViewById(R.id.de_txtDistrito);
        TextView txtPregunta = view.findViewById(R.id.de_txtPregunta);
        Button btnUsarEmpresa = view.findViewById(R.id.de_btnUsarEmpresa);
        Button btnUsarSeleccionada = view.findViewById(R.id.de_btnUsarSeleccionada);

        txtTitulo.setText("Dirección de empresa");
        String direccionTexto = rucDataActual.getDireccion() != null ? rucDataActual.getDireccion() : "";
        String distrito = rucDataActual.getDistrito() != null ? rucDataActual.getDistrito() : "";
        txtDireccion.setText(direccionTexto);
        txtDistrito.setText(distrito.length() > 0 ? "Distrito: " + distrito : "");
        txtPregunta.setText("¿Desea usar esta dirección para la entrega?");
        btnUsarEmpresa.setText("Usar dirección de empresa");
        btnUsarSeleccionada.setText("Usar dirección seleccionada");

        builder.setView(view).setCancelable(true);
        alertDialog = builder.create();
        btnUsarEmpresa.setOnClickListener(v -> {
            Log.i("ConfirmacionDireccion", "Usuario aceptó usar dirección de empresa: " + idDirEmpresa);
            cabeceraPedido.setIdDireccionEntrega(idDirEmpresa);
            try { alertDialog.dismiss(); } catch (Exception ignore) {}
            continuarConEntrega();
        });
        btnUsarSeleccionada.setOnClickListener(v -> {
            Log.i("ConfirmacionDireccion", "Usuario quiere usar dirección seleccionada: " + idDireccionSeleccionada);
            cabeceraPedido.setIdDireccionEntrega(idDireccionSeleccionada);
            try { alertDialog.dismiss(); } catch (Exception ignore) {}
            continuarConEntrega();
        });
        alertDialog.setOnDismissListener(d -> mostrandoModalDireccion = false);
        alertDialog.show();
    }

    private String construirMensajeDireccionEmpresa() {
        String direccionTexto = rucDataActual.getDireccion();
        String distrito = rucDataActual.getDistrito();
        StringBuilder sb = new StringBuilder();
        sb.append("La dirección de su empresa está registrada en:\n\n");
        sb.append(direccionTexto != null ? direccionTexto : "");
        if (distrito != null && !distrito.isEmpty()) {
            sb.append("\nDistrito: ").append(distrito);
        }
        sb.append("\n\n¿Desea usar esta dirección para la entrega?");
        return sb.toString();
    }

    private void continuarConEntrega() {
        mostrarAdvertenciaHorario();
    }

    private void mostrarDialogoFactura() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_datos_factura, null);
        android.widget.EditText dfDoc = view.findViewById(R.id.df_doc);
        android.widget.EditText dfNombre = view.findViewById(R.id.df_nombre);
        android.widget.EditText dfDireccion = view.findViewById(R.id.df_direccion);
        android.widget.EditText dfDistrito = view.findViewById(R.id.df_distrito);
        android.widget.Button btnCancel = view.findViewById(R.id.df_btnCancelar);
        android.widget.Button btnConfirm = view.findViewById(R.id.df_btnConfirmar);
        android.widget.Button btnEliminar = view.findViewById(R.id.df_btnEliminar);

        // Pre-cargar datos si ya existe factura activa
        if (facturaActiva && rucDataActual != null) {
            dfDoc.setText(rucDataActual.getNumeroDocumento());
            dfNombre.setText(rucDataActual.getNombreCompleto());
            dfDireccion.setText(rucDataActual.getDireccion());
            dfDistrito.setText(rucDataActual.getUbigeoDistrito());
            btnEliminar.setVisibility(View.VISIBLE);
        }

        dfDoc.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(android.text.Editable s) {
                String ruc = s.toString().trim();
                if (ruc.length() == 11) {
                    consultarDocumento(ruc, dfNombre, dfDireccion, dfDistrito);
                }
            }
        });

        btnCancel.setOnClickListener(v -> {
            try {
                alertDialog.dismiss();
            } catch (Exception ignore) {
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String ruc = dfDoc.getText().toString().trim();
            String nombreRuc = dfNombre.getText().toString().trim();
            String dirRuc = dfDireccion.getText().toString().trim();
            String distRuc = dfDistrito.getText().toString().trim();
            if (ruc.length() != 11 || nombreRuc.isEmpty() || dirRuc.isEmpty()) {
                android.widget.Toast
                        .makeText(this, "Complete RUC, nombre y dirección", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            registrarRuc(nombreRuc, ruc, dirRuc, distRuc);
        });

        // Botón para eliminar/desactivar factura
        btnEliminar.setOnClickListener(v -> {
            SharedPreferences sp = getSharedPreferences("rucData", MODE_PRIVATE);
            sp.edit().remove("rucData").apply();
            facturaActiva = false;
            rucDataActual = null;
            cabeceraPedido.setIdDireccionEntrega(0); // Reset dirección
            actualizarUIFactura();
            android.widget.Toast.makeText(this, "Factura desactivada", android.widget.Toast.LENGTH_SHORT).show();
            Log.i("FacturaDesactivada", "Factura eliminada, volviendo a modo normal");
            try {
                alertDialog.dismiss();
            } catch (Exception ignore) {
            }
        });

        builder.setView(view);
        builder.setCancelable(true);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void consultarDocumento(String ruc, android.widget.EditText nombre, android.widget.EditText direccion,
            android.widget.EditText distrito) {
        mostrarLoader("Consultando RUC...");
        String url = "https://api.comsanjuan.com:8443/api/document/" + ruc;
        com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.GET, url, resp -> {
                    try {
                        if (loaderDialog != null && loaderDialog.isShowing()) {
                            loaderDialog.dismiss();
                        }
                        Object parsed = new org.json.JSONTokener(resp).nextValue();
                        if (!(parsed instanceof org.json.JSONObject))
                            return;
                        org.json.JSONObject jo = (org.json.JSONObject) parsed;
                        boolean success = jo.optBoolean("success", false);
                        if (!success)
                            return;
                        org.json.JSONObject data = jo.optJSONObject("data");
                        if (data == null)
                            return;
                        String nom = data.optString("Nombre", "").trim();
                        String dir = data.optString("Direccion1", "").trim();
                        String dist = data.optString("Distrito1", "").trim();
                        if (nom.length() > 0)
                            nombre.setText(nom);
                        if (dir.length() > 0 && !"-".equals(dir))
                            direccion.setText(dir);
                        
                        // Si es RUC 10 y no hay dirección fiscal, usar la dirección principal del usuario
                        if (ruc.startsWith("10") && (dir.isEmpty() || "-".equals(dir))) {
                            if (direcciones != null && !direcciones.isEmpty()) {
                                com.csj.csjmarket.modelos.Direccion dirPrincipal = direcciones.get(0);
                                if (dirPrincipal.getDescripcion() != null) {
                                    direccion.setText(dirPrincipal.getDescripcion());
                                }
                                if (dirPrincipal.getUbigeo() != null && !dirPrincipal.getUbigeo().isEmpty()) {
                                    distrito.setText(dirPrincipal.getUbigeo());
                                }
                            }
                        }

                        if (dist.length() > 0 && !"-".equals(dist))
                            distrito.setText(dist);
                    } catch (Exception ignore) {
                    }
                }, err -> {
                    if (loaderDialog != null && loaderDialog.isShowing()) {
                        loaderDialog.dismiss();
                    }
                });
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(9000, 2, 1));
        req.setShouldCache(false);
        com.android.volley.toolbox.Volley.newRequestQueue(this).add(req);
    }

    private void registrarRuc(String nombre, String doc, String direccion, String distrito) {
        mostrarLoader("Registrando RUC...");
        String url = "https://api.comsanjuan.com:8443/api/ruc-registration";
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("Nombre", nombre);
            body.put("DocIdentidad", doc);
            body.put("Direccion1", direccion);
            body.put("Distrito1", distrito);
            com.android.volley.toolbox.JsonObjectRequest req = new com.android.volley.toolbox.JsonObjectRequest(
                    com.android.volley.Request.Method.POST, url, body, response -> {
                        try {
                            if (loaderDialog != null && loaderDialog.isShowing()) {
                                loaderDialog.dismiss();
                            }
                            // Log completo de la respuesta para depuración
                            Log.i("RegistrarRuc", "Respuesta RUC: " + response.toString());

                            // Los datos vienen dentro de un objeto "data"
                            org.json.JSONObject data = response.optJSONObject("data");
                            if (data == null) {
                                Log.e("RegistrarRuc", "ERROR: No se encontró el objeto 'data' en la respuesta");
                                android.widget.Toast.makeText(this, "Error: Respuesta del servidor inválida",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                return;
                            }

                            com.csj.csjmarket.modelos.RucData rd = new com.csj.csjmarket.modelos.RucData();

                            // Leer datos del objeto "data" (con fallback a mayúsculas/minúsculas)
                            rd.setIdUsuario(data.optInt("IdUsuario", data.optInt("idUsuario", 0)));
                            rd.setNombreCompleto(data.optString("NombreCompleto", data.optString("nombreCompleto", "")));
                            rd.setNumeroDocumento(data.optString("NumeroDocumento", data.optString("numeroDocumento", "")));
                            rd.setTipoDocumento(data.optString("TipoDocumento", data.optString("tipoDocumento", "RUC")));
                            rd.setDireccion(data.optString("Direccion", data.optString("direccion", "")));
                            rd.setUbigeoDistrito(data.optString("UbigeoDistrito", data.optString("ubigeoDistrito", "")));
                            rd.setIdDireccion(data.optInt("IdDireccion", data.optInt("idDireccion", 0)));
                            rd.setDistrito(data.optString("Distrito", data.optString("distrito", "")));
                            // En SQL la columna viene como "Departamento" (con 'to')
                            rd.setDepartamento(data.optString("Departamento", data.optString("departamento", "")));
                            rd.setProvincia(data.optString("Provincia", data.optString("provincia", "")));

                            Log.i("RucDataDebug",
                                    "IdUsuario=" + rd.getIdUsuario() + ", IdDireccion=" + rd.getIdDireccion()
                                            + ", Nombre=" + rd.getNombreCompleto());

                            // Validar que los datos sean válidos antes de guardar
                            if (rd.getIdUsuario() == null || rd.getIdUsuario() <= 0) {
                                Log.e("RegistrarRuc", "ERROR: IdUsuario inválido (" + rd.getIdUsuario() + "). No se guardará el RUC.");
                                android.widget.Toast.makeText(this, "Error: No se pudo obtener el ID del cliente. Verifique la respuesta del servidor.",
                                        android.widget.Toast.LENGTH_LONG).show();
                                return;
                            }

                            String json = new com.google.gson.Gson().toJson(rd);
                            Log.i("RegistrarRuc", "JSON a guardar: " + json);
                            SharedPreferences sp = getSharedPreferences("rucData", MODE_PRIVATE);
                            sp.edit().putString("rucData", json).apply();
                            Log.i("RegistrarRuc", "JSON guardado en SharedPreferences");

                            // Activar factura y actualizar UI (solo si los datos son válidos)
                            facturaActiva = true;
                            rucDataActual = rd;
                            actualizarUIFactura();

                            Log.i("FacturaRegistrada", "RUC registrado - IdUsuario: " + rd.getIdUsuario()
                                    + ", IdDireccion: " + rd.getIdDireccion());

                            // Cerrar el modal de formulario (alertDialog actual) ANTES de mostrar cualquier otro modal
                            try {
                                if (alertDialog != null && alertDialog.isShowing()) {
                                    alertDialog.dismiss();
                                }
                            } catch (Exception ignore) {
                            }

                            // Auto-seleccionar dirección del RUC si existe
                            if (rd.getIdDireccion() != null && rd.getIdDireccion() > 0) {
                                cabeceraPedido.setIdDireccionEntrega(rd.getIdDireccion());
                                Log.i("FacturaRegistrada",
                                        "Dirección del RUC auto-seleccionada: " + rd.getIdDireccion());
                                // Recargar lista de direcciones para mostrar la seleccionada
                                recargar();
                                // Mostrar modal de confirmación de dirección de empresa
                                mostrarConfirmacionDireccionFactura();
                            }

                            android.widget.Toast
                                    .makeText(this, "RUC registrado correctamente", android.widget.Toast.LENGTH_SHORT)
                                    .show();
                        } catch (Exception e) {
                            Log.e("RegistrarRuc", "Error al procesar respuesta: " + e.getMessage());
                            android.widget.Toast.makeText(this, "Error al procesar datos del RUC",
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }, error -> {
                        if (loaderDialog != null && loaderDialog.isShowing()) {
                            loaderDialog.dismiss();
                        }
                        android.widget.Toast
                                .makeText(this, "No fue posible registrar RUC", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(9000, 1, 1));
            req.setShouldCache(false);
            com.android.volley.toolbox.Volley.newRequestQueue(this).add(req);
        } catch (Exception e) {
            Log.e("RegistrarRuc", "Error al crear request: " + e.getMessage());
        }
    }

    private boolean useApiBeta() {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("csjmarket_prefs", MODE_PRIVATE);
        }
        return sharedPreferences.getBoolean("use_api_beta", BuildConfig.DEBUG);
    }

    private void cargarCarrito() {
        if (sharedPreferences == null) {
            sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        }
        String carritojson = sharedPreferences.getString("carrito", "");
        Gson gson = new Gson();
        java.lang.reflect.Type tipoLista = new com.google.gson.reflect.TypeToken<java.util.ArrayList<com.csj.csjmarket.modelos.MiCarrito>>() {
        }.getType();
        try {
            carrito = gson.fromJson(carritojson, tipoLista);
        } catch (Exception e) {
            carrito = null;
        }
        if (carrito == null) {
            carrito = new ArrayList<>();
        }
    }

    @Override
    public void onItemChanged(int idDireccion) {
        try {
            cabeceraPedido.setIdDireccionEntrega(idDireccion);
            Log.d("DireccionEntrega", "Dirección seleccionada id=" + idDireccion);
            
            if (suprimirSiguienteModalDireccion) {
                suprimirSiguienteModalDireccion = false;
                mostrarAdvertenciaHorario();
                return;
            }
            
            // Si hay factura activa y la dirección seleccionada es diferente a la de la empresa,
            // mostrar modal de confirmación
            if (facturaActiva && rucDataActual != null 
                    && rucDataActual.getIdDireccion() != null 
                    && rucDataActual.getIdDireccion() > 0
                    && !rucDataActual.getIdDireccion().equals(idDireccion)) {
                // El usuario seleccionó una dirección diferente a la de la empresa
                // Mostrar modal preguntando si quiere usar la de la empresa o la seleccionada
                mostrarConfirmacionDireccionFacturaConOpcion(idDireccion);
            } else {
                // Continuar con el flujo original: advertencia de horario y medio de pago
                mostrarAdvertenciaHorario();
            }
        } catch (Exception e) {
            Log.e("DireccionEntrega", "Error al seleccionar dirección: " + e.getMessage());
        }
    }

    private void verificarEstadoPedidoYProcesar(String requestId) {
        try {
            if (loaderDialog != null && loaderDialog.isShowing())
                loaderDialog.dismiss();
        } catch (Exception ignore) {
        }
        mostrarLoader("Verificando estado del pedido...");
        try {
            String url = getString(R.string.connection) + "/api/pedido/EstadoPorRequestId?requestId=" + requestId;
            JsonObjectRequest estadoRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
                try {
                    int idCp = response.optInt("IdCp", 0);
                    String numCp = response.optString("NumCp");
                    String fecha = response.optString("Fecha");
                    if (idCp > 0) {
                        Gson gson = new Gson();
                        JSONObject rpJson = new JSONObject();
                        rpJson.put("IdCp", idCp);
                        rpJson.put("IdCpInventario", 0);
                        rpJson.put("NumCp", numCp);
                        rpJson.put("Fecha", fecha);
                        rpJson.put("RequestId", requestId);
                        rpJson.put("ImagenProducto", "");
                        this.respuestaPedido = gson.fromJson(rpJson.toString(), RespuestaPedido.class);

                        if (sharedPreferences == null) {
                            sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
                        }
                        try {
                            sharedPreferences.edit().clear().commit();
                        } catch (Exception ignore) {
                        }

                        // Enviar correo de confirmación y mostrar registro
                        enviarCorreoNuevo(idCp);
                        lastRequestId = null;
                        mostrarRegistro();
                    } else {
                        mostrarAlertaReintentar(
                                "No pudimos confirmar su pedido por tiempo de espera. Es posible que se haya registrado. Si recibió correo de confirmación, puede ignorar este mensaje. Puede reintentar ahora.");
                    }
                } catch (Exception ex) {
                    mostrarAlertaReintentar("No fue posible verificar el estado del pedido. Puede reintentar ahora.");
                } finally {
                    pedidoEnProceso = false;
                    if (btnContinuarDialog != null)
                        btnContinuarDialog.setEnabled(true);
                    try {
                        if (loaderDialog != null && loaderDialog.isShowing())
                            loaderDialog.dismiss();
                    } catch (Exception ignore) {
                    }
                }
            }, err -> {
                mostrarAlertaReintentar("No fue posible verificar el estado del pedido. Puede reintentar ahora.");
                pedidoEnProceso = false;
                if (btnContinuarDialog != null)
                    btnContinuarDialog.setEnabled(true);
                try {
                    if (loaderDialog != null && loaderDialog.isShowing())
                        loaderDialog.dismiss();
                } catch (Exception ignore) {
                }
            });
            estadoRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            estadoRequest.setShouldCache(false);
            Volley.newRequestQueue(this).add(estadoRequest);
        } catch (Exception ex) {
            mostrarAlertaReintentar("No fue posible verificar el estado del pedido. Puede reintentar ahora.");
            pedidoEnProceso = false;
            if (btnContinuarDialog != null)
                btnContinuarDialog.setEnabled(true);
            try {
                if (loaderDialog != null && loaderDialog.isShowing())
                    loaderDialog.dismiss();
            } catch (Exception ignore) {
            }
        }
    }

    // Envío de correo no bloqueante tras crear pedido
    private void enviarCorreoNuevo(int idCp) {
        try {
            String url = getString(R.string.connection) + "/EnviarCorreo/?idCp=" + idCp;
            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> Log.i("PedidoEmail", "Correo enviado correctamente: " + response),
                    err -> {
                        String msg = null;
                        if (err != null && err.networkResponse != null && err.networkResponse.data != null) {
                            msg = new String(err.networkResponse.data);
                        }
                        Log.e("PedidoEmail", "Error al enviar correo: " + (msg != null ? msg : String.valueOf(err)));
                    });
            request.setRetryPolicy(new DefaultRetryPolicy(15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            request.setShouldCache(false);
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            Log.e("PedidoEmail", "Excepción al enviar correo: " + e.getMessage());
        }
    }
}
