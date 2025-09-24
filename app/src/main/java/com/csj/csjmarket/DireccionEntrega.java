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
import com.csj.csjmarket.databinding.ActivityDireccionEntregaBinding;
import com.csj.csjmarket.modelos.CabeceraPedido;
import com.csj.csjmarket.modelos.Direccion;
import com.csj.csjmarket.modelos.ItemPedido;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.PedidoNuevo;
import com.csj.csjmarket.modelos.RespuestaPedido;
import com.csj.csjmarket.modelos.niubiz.error;
import com.csj.csjmarket.modelos.niubiz.keySuccess;
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

import lib.visanet.com.pe.visanetlib.VisaNet;
import lib.visanet.com.pe.visanetlib.data.custom.Channel;
import lib.visanet.com.pe.visanetlib.presentation.custom.VisaNetViewAuthorizationCustom;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DireccionEntrega extends AppCompatActivity implements itemDireccionAdapter.OnItemChangedListener {
    private ActivityDireccionEntregaBinding binding;
    private AlertDialog alertDialog;
    private ArrayList<Direccion> direcciones = new ArrayList<>();
    private CabeceraPedido cabeceraPedido = new CabeceraPedido();
    private ArrayList<ItemPedido> itemsPedido = new ArrayList<>();
    private PedidoNuevo pedidoNuevo = new PedidoNuevo();
    private SharedPreferences sharedPreferences;
    private ArrayList<MiCarrito> carrito;
    private RespuestaPedido respuestaPedido;
    private FirebaseAuth mAuth;

    private String comercio = "650237824";
    //650237824
    private String token;
    private String pin;
    private String visanetPurchase;
    private Double montoTotal;
    private String idPersona;
    private String email;
    private String nombre;
    private String docIdentidad;
    private String diasUltimaCompra;
    private keySuccess fromJsonSuccess;
    private error fromJsonError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void mostrarLoader() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View progress = getLayoutInflater().inflate(R.layout.loader, null);
        builder.setView(progress);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
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

    private void mostrarRegistro(){
        Calendar calendar = Calendar.getInstance();
        // Obtenemos día y hora actual
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1 = Domingo, 2 = Lunes, ..., 7 = Sábado
        int hour = calendar.get(Calendar.HOUR_OF_DAY);      // 0 - 23

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

        txtNumPedido.setText(respuestaPedido.getNumCp());

        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date date = null;
        try {
            date = inputDateFormat.parse(respuestaPedido.getFecha());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String formattedDate = outputDateFormat.format(date);
        txtFechaHora.setText(formattedDate);
        if (fromJsonSuccess != null){
            txtNumPedidoNiubiz.setText(visanetPurchase);
            txtImporte.setText(fromJsonSuccess.getOrder().getAuthorizedAmount().toString() + " " + fromJsonSuccess.getOrder().getCurrency());
            txtTarjeta.setText(fromJsonSuccess.getDataMap().getBRAND() + " " + fromJsonSuccess.getDataMap().getCARD());
        }
        else{
            numOpe.setVisibility(View.GONE);
            tarjeta.setVisibility(View.GONE);
            importe.setVisibility(View.GONE);
            txtNumPedidoNiubiz.setVisibility(View.GONE);
            txtImporte.setVisibility(View.GONE);
            txtTarjeta.setVisibility(View.GONE);
        }

        txtMensaje.setText("Hemos enviado un mensaje de correo electrónico a " + user.getEmail() + " con el detalle de su pedido. Por favor, compruebe su bandeja de entrada.");

        if (dayOfWeek == Calendar.SUNDAY) {
            txtMensajeDelivery.setText("Ahora no estamos atendiendo... Su pedido se enviará el dia de mañana a partir de las 09:00 a.m");
        }
        else if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) {
            if (hour >= 17) {
                txtMensajeDelivery.setText("Ahora no estamos atendiendo... Su pedido se enviará el dia de mañana a partir de las 09:00 a.m");
            }
            else{
                txtMensajeDelivery.setVisibility(View.GONE);
            }
        }
        else if (dayOfWeek == Calendar.SATURDAY) {
            if (hour >= 13) {
                txtMensajeDelivery.setText("Ahora no estamos atendiendo... Su pedido se enviará el dia lunes a partir de las 09:00 a.m");
            }
            else{
                txtMensajeDelivery.setVisibility(View.GONE);
            }
        }
        else{
            txtMensajeDelivery.setVisibility(View.GONE);
        }
        
        builder.setView(view).setPositiveButton("Aceptar", (dialogInterface, i) -> {finish();})
                .setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarError(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_error, null);
        TextView txtMotivoError = view.findViewById(R.id.dr_txtMotivoError);
        TextView txtNumPedido = view.findViewById(R.id.dr_txtNumPedido);
        TextView txtFechaHora = view.findViewById(R.id.dr_txtFechaHora);
        txtMotivoError.setText(fromJsonError.getData().getACTION_DESCRIPTION());
        txtNumPedido.setText(visanetPurchase);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String formattedDateTime = sdf.format(calendar.getTime());
        txtFechaHora.setText(formattedDateTime);
        builder.setTitle("Aviso");
        builder.setView(view).setPositiveButton("Aceptar", null)
                .setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarPreVenta(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_pre_venta, null);
        TextView txtNumOpe = view.findViewById(R.id.pv_txtNumOpe);
        TextView txtCliente = view.findViewById(R.id.pv_txtCliente);
        TextView txtDocIden = view.findViewById(R.id.pv_txtDocIden);
        TextView txtMonto = view.findViewById(R.id.pv_txtMonto);
        CheckBox cbTerCon = view.findViewById(R.id.pv_cbTerCon);
        Button btnPagar = view.findViewById(R.id.pv_btnPagar);

        // Crear un objeto DecimalFormat para formatear el número con dos decimales
        DecimalFormat formato = new DecimalFormat("#.##");
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);

        // Convertir el monto total a formato de dos decimales como una cadena de texto
        String montoFormateado = formato.format(montoTotal);

        txtNumOpe.setText("Nro. Operación N° " + visanetPurchase);
        txtCliente.setText(nombre);
        txtDocIden.setText(docIdentidad);
        txtMonto.setText("S/ " + montoFormateado);
        cbTerCon.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                btnPagar.setEnabled(true);
            }
            else{
                btnPagar.setEnabled(false);
            }
        });

        btnPagar.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            new generarToken().execute();
        });
        builder.setView(view);

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarMedioPago(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_medio_de_pago, null);
        RadioButton rbEnLinea = view.findViewById(R.id.mp_rbEnLinea);
        Button btnContinuar = view.findViewById(R.id.mp_btnContinuar);

        btnContinuar.setOnClickListener(view1 -> {
            alertDialog.dismiss();

            if (rbEnLinea.isChecked()) {
                cabeceraPedido.setTipoCp(4411);
                obtenerVISANET_PURCHASE();
            } else {
                cabeceraPedido.setTipoCp(4415);
                mostrarLoader();
                enviarPedido();
            }
        });

        builder.setView(view);

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void recargar() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/cliente/direcciones?idPersona="+getIntent().getStringExtra("idPersona");
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            Type productListType = new TypeToken<List<Direccion>>() {
            }.getType();
            direcciones = gson.fromJson(response.toString(), productListType);
            itemDireccionAdapter direccionAdapter = new itemDireccionAdapter(direcciones);
            direccionAdapter.setOnItemChangedListener(this);
            binding.rvListaDireccion.setAdapter(direccionAdapter);
            direccionAdapter.notifyDataSetChanged();
            alertDialog.dismiss();
        }, error -> {
            alertDialog.dismiss();
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse!= null){
                String errorMessage = new String(networkResponse.data);
                mostrarAlerta(errorMessage);
            }
            else{
                mostrarAlerta(error.toString());
            }
        });
        Volley.newRequestQueue(this).add(jsonArrayRequest);
    }

    private void enviarPedido(){
        String url = getString(R.string.connection) + "/api/pedido/NuevoActual";
        Gson gson = new Gson();
        JSONObject jsonBody = null;
        try {
            itemsPedido.clear();
            cabeceraPedido.setIdPersona(Integer.parseInt(getIntent().getStringExtra("idPersona")));
            cabeceraPedido.setTotalVenta(montoTotal);
            cabeceraPedido.setTotalPeso(getIntent().getDoubleExtra("totalPeso", 0.0));
            pedidoNuevo.setCabeceraPedido(cabeceraPedido);
            cargarCarrito();
            for (MiCarrito item:carrito) {
                ItemPedido itemPedido = new ItemPedido();
                itemPedido.setIdCp(0);
                itemPedido.setIdCpInventario(0);
                itemPedido.setIdProducto(item.getIdProducto());
                itemPedido.setIdUnidad(item.getIdUnidad());
                itemPedido.setPeso(item.getPesoTotal());
                itemPedido.setDescripcion(item.getNombre());
                itemPedido.setCantidad(item.getCantidad());
                itemPedido.setPrecio(item.getPrecio());
                itemPedido.setTotal(item.getTotal());
                itemsPedido.add(itemPedido);
            }
            pedidoNuevo.setItemPedidos(itemsPedido);
            jsonBody = new JSONObject(gson.toJson(pedidoNuevo));

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody, response -> {
//                Type productListType = new TypeToken<RespuestaPedido>() {
//                }.getType();

                this.respuestaPedido = gson.fromJson(response.toString(), RespuestaPedido.class);
                enviarCorreo(this.respuestaPedido.getIdCp());
                alertDialog.dismiss();
                sharedPreferences.edit().clear().commit();
                mostrarRegistro();

            }, error -> {
                alertDialog.dismiss();
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse!= null){
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                }
                else{
                    mostrarAlerta(error.toString());
                }
            }){

                @Override
                public Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
                    return super.setRetryPolicy(new DefaultRetryPolicy(20000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            Volley.newRequestQueue(this).add(jsonObjectRequest);
        } catch (Exception e) {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + e.getMessage());
        }
    }

    private void cargarCarrito(){
        sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        String carritojson = sharedPreferences.getString("carrito", "");
        Gson gson = new Gson();
        Type tipoLista = new TypeToken<ArrayList<MiCarrito>>(){}.getType();
        carrito = gson.fromJson(carritojson, tipoLista);
    }

    @Override
    public void onItemChanged(int idDireccion) {
        cabeceraPedido.setIdDireccionEntrega(idDireccion);
        //crearPedido(); METODO ANTIGUO PARA GUARDAR PEDIDO
        //enviarPedido();
        mostrarMedioPago();
        //obtenerVISANET_PURCHASE();
    }

    private class generarToken extends AsyncTask<Void, Void, String> {
        String errorWS;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mostrarLoader();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String username = "csjdesarrollo@gmail.com";
            String password = "R_H79wxs";
            String credentials = Credentials.basic(username, password);
            String apiUrl = "https://apiprod.vnforapps.com/api.security/v1/security";

            OkHttpClient client = new OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", credentials)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    errorWS = "Respuesta inesperada del servidor. Inténtelo nuevamente.";
                    return null;
                }
            } catch (SocketTimeoutException exc) {
                errorWS = "Tiempo de espera agotado. Revise su conexión a internet.";
                return null;
            } catch (IOException exc) {
                errorWS = "Ocurrio un error al momento de capturar los datos. Inténtelo nuevamente.";
                return null;
            } catch (Exception ex) {
                errorWS = ex.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                token = result;
                new generarPin().execute();
            } else {
                alertDialog.dismiss();
                mostrarAlerta(errorWS);
            }
        }
    }

    private class generarPin extends AsyncTask<Void, Void, String> {
        String errorWS;

        @Override
        protected String doInBackground(Void... voids) {
            String apiUrl = "https://jobs.vnforapps.com/api.certificate/v1/query/" + comercio;
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = RequestBody.create(new byte[0], null);

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .header("Authorization", token)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String pinHash = jsonResponse.getString("pinHash");
                    return pinHash;
                } else {
                    errorWS = "Acceso no autorizado.";
                    return null;
                }
            } catch (SocketTimeoutException exc) {
                errorWS = "Tiempo de espera agotado. Revise su conexión a internet.";
                return null;
            } catch (IOException exc) {
                errorWS = "Ocurrio un error al momento de capturar los datos. Inténtelo nuevamente.";
                return null;
            } catch (Exception ex) {
                errorWS = ex.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                pin = result;

                Map<String, Object> data = new HashMap<>();
                data.put(VisaNet.VISANET_SECURITY_TOKEN, token);

                data.put(VisaNet.VISANET_CHANNEL, Channel.MOBILE);
                data.put(VisaNet.VISANET_COUNTABLE, true);
                data.put(VisaNet.VISANET_MERCHANT, comercio);
                data.put(VisaNet.VISANET_PURCHASE_NUMBER, visanetPurchase);
                data.put(VisaNet.VISANET_AMOUNT, montoTotal);
                data.put(VisaNet.VISANET_USER_TOKEN, docIdentidad);

                HashMap<String, String> MDDdata = new HashMap<String, String>();
                MDDdata.put("MDD4", email);
                MDDdata.put("MDD21", "0");
                MDDdata.put("MDD32", docIdentidad);
                MDDdata.put("MDD75", "Registrado");
                MDDdata.put("MDD77", diasUltimaCompra);

                data.put(VisaNet.VISANET_MDD, MDDdata);
                data.put(VisaNet.VISANET_ENDPOINT_URL, "https://apiprod.vnforapps.com");
                data.put(VisaNet.VISANET_CERTIFICATE_HOST, "apiprod.vnforapps.com");
                data.put(VisaNet.VISANET_CERTIFICATE_PIN, "sha256/" + pin);

                VisaNetViewAuthorizationCustom custom = new VisaNetViewAuthorizationCustom();
                custom.setLogoImage(R.drawable.logo_csj);

                try {
                    VisaNet.authorization(DireccionEntrega.this, data, custom);
                } catch (Exception e) {
                    mostrarAlerta(e.getMessage());
                }
            } else {
                alertDialog.dismiss();
                mostrarAlerta(errorWS);
            }
        }
    }

    private void obtenerVISANET_PURCHASE() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/VISANET/obtenerVISANET_PURCHASE";
        JsonRequest<Integer> jsonObjectRequest = new JsonRequest<Integer>(Request.Method.GET, url, null, response -> {
            alertDialog.dismiss();
            visanetPurchase = response.toString();
            mostrarPreVenta();

        }, error -> {
            if (alertDialog != null) {
                alertDialog.dismiss();
            }
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse!= null){
                String errorMessage = new String(networkResponse.data);
                mostrarAlerta(errorMessage);
            }
            else{
                mostrarAlerta(error.toString());
            }
        }) {
            @Override
            protected com.android.volley.Response<Integer> parseNetworkResponse(NetworkResponse response) {
                try {
                    String responseBody = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                    int intValue = Integer.parseInt(responseBody);
                    return success(intValue, HttpHeaderParser.parseCacheHeaders(response));
                } catch (NumberFormatException | UnsupportedEncodingException e) {
                    return error(new ParseError(e));
                }
            }
        };
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void guardarPago(String JSONString) {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/VISANET/registrarPago";
        Gson gson = new Gson();
        JSONObject jsonBody = null;
        try {
//            Type keySuccessType = new TypeToken<keySuccess>() {
//            }.getType();
            fromJsonSuccess = gson.fromJson(JSONString, keySuccess.class);
            jsonBody = new JSONObject(gson.toJson(fromJsonSuccess));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody, response -> {
                fromJsonSuccess = gson.fromJson(response.toString(), keySuccess.class);
                //crearPedido(); METODO ANTIGUO PARA GUARDAR PEDIDO
                enviarPedido();
            }, error -> {
                alertDialog.dismiss();
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse!= null){
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                }
                else{
                    mostrarAlerta(error.toString());
                }
            }){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
        } catch (Exception e) {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (alertDialog != null){
            alertDialog.dismiss();
        }
        if (requestCode == VisaNet.VISANET_AUTHORIZATION) {
            if (data != null) {
                if (resultCode == RESULT_OK) {
                    String JSONString = data.getExtras().getString("keySuccess");
                    guardarPago(JSONString);
                } else {
                    String JSONString = data.getExtras().getString("keyError");
                    JSONString = JSONString != null ? JSONString : "";
                    if (JSONString != ""){
                        Gson gson = new Gson();
//                        Type errorType = new TypeToken<error>() {
//                        }.getType();
                        fromJsonError = gson.fromJson(JSONString, error.class);

                        alertDialog.dismiss();

                        mostrarError();
                    }
                }
            } else {
                Toast toast1 = Toast.makeText(getApplicationContext(), "Cancelado...", Toast.LENGTH_LONG);
                toast1.show();
            }
        }
    }

    private void enviarCorreo(int idCp) {
        String url = getString(R.string.connection) + "/EnviarCorreo/?idCp="+ idCp;

        try {
            StringRequest jsonObjectRequest = new StringRequest(Request.Method.GET, url,response -> {
                Log.d(TAG, "enviarCorreo: Correo OK");
            }, error -> {
                /*alertDialog.dismiss();
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse!= null){
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                }
                else{
                    mostrarAlerta(error.toString());
                }*/
                Log.d(TAG, "enviarCorreo: Correo ERROR");
            }){
                @Override
                public Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
                    return super.setRetryPolicy(new DefaultRetryPolicy(5000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                }
            };

            Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
        } catch (Exception e) {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        recargar();
    }
}