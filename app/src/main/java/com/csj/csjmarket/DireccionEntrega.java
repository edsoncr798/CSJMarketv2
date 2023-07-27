package com.csj.csjmarket;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.databinding.ActivityDireccionEntregaBinding;
import com.csj.csjmarket.modelos.CabeceraPedido;
import com.csj.csjmarket.modelos.Direccion;
import com.csj.csjmarket.modelos.ItemPedido;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.RespuestaPedido;
import com.csj.csjmarket.ui.adaptadores.itemDireccionAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DireccionEntrega extends AppCompatActivity implements itemDireccionAdapter.OnItemChangedListener{
    private ActivityDireccionEntregaBinding binding;
    private AlertDialog alertDialog;
    private ArrayList<Direccion> direcciones = new ArrayList<>();
    private CabeceraPedido cabeceraPedido = new CabeceraPedido();
    private ArrayList<ItemPedido> itemsPedido  = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private ArrayList<MiCarrito> carrito;
    private RespuestaPedido respuestaPedido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direccion_entrega);

        binding = ActivityDireccionEntregaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.vpBtnRegresar.setOnClickListener(view -> finish());

        binding.rvListaDireccion.setLayoutManager(new LinearLayoutManager(this));

        recargar();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_baseline_good);
        builder.setTitle("Aviso");
        builder.setMessage("Su pedido se guardo exitosamente. El número de pedido generado es: " + respuestaPedido.getNumCp());
        builder.setPositiveButton("Aceptar", (dialogInterface, i) -> {
            finish();
        });
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarRegistro(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogo_registro, null);
        TextView txtNumPedido = view.findViewById(R.id.dr_txtNumPedido);
        txtNumPedido.setText(respuestaPedido.getNumCp());
        builder.setTitle("Aviso");
        builder.setView(view).setPositiveButton("Aceptar", (dialogInterface, i) -> {finish();})
                .setCancelable(false);
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
            mostrarAlerta(error.toString());
        });
        Volley.newRequestQueue(this).add(jsonArrayRequest);
    }

    private void crearPedido() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/pedido";
        Gson gson = new Gson();
        JSONObject jsonBody = null;
        try {
            cabeceraPedido.setIdPersona(Integer.parseInt(getIntent().getStringExtra("idPersona")));
            cabeceraPedido.setTotalVenta(getIntent().getDoubleExtra("totalVenta", 0.0));
            cabeceraPedido.setTotalPeso(getIntent().getDoubleExtra("totalPeso", 0.0));
            jsonBody = new JSONObject(gson.toJson(cabeceraPedido));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody, response -> {
                Type productListType = new TypeToken<RespuestaPedido>() {
                }.getType();
                respuestaPedido = gson.fromJson(response.toString(), productListType);
                alertDialog.dismiss();
                insertarItemsPedido();
            }, error -> {
                alertDialog.dismiss();
                mostrarAlerta("Error al guardar el pedido, por favor notifique al desarrollador de la aplicación.");
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
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente. Si el problema persiste póngase en contacto con el desarrollador.");
        }
    }

    private void insertarItemsPedido(){
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/pedido/itemPedido";
        Gson gson = new Gson();
        JSONArray jsonBody = null;
        try {
            cargarCarrito();
            for (MiCarrito item:carrito) {
                ItemPedido itemPedido = new ItemPedido();
                itemPedido.setIdCp(respuestaPedido.getIdCp());
                itemPedido.setIdCpInventario(respuestaPedido.getIdCpInventario());
                itemPedido.setIdProducto(item.getIdProducto());
                itemPedido.setIdUnidad(item.getIdUnidad());
                itemPedido.setPeso(item.getPesoTotal());
                itemPedido.setDescripcion(item.getNombre());
                itemPedido.setCantidad(item.getCantidad());
                itemPedido.setPrecio(item.getPrecio());
                itemPedido.setTotal(item.getTotal());
                itemsPedido.add(itemPedido);
            }
            jsonBody = new JSONArray(gson.toJson(itemsPedido));
            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.POST, url, jsonBody, response -> {
                alertDialog.dismiss();
                sharedPreferences.edit().clear().commit();
                mostrarRegistro();


            }, error -> {
                alertDialog.dismiss();
                mostrarAlerta("Error al guardar los items en el pedido, por favor notifique al desarrollador de la aplicación.");
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
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente. Si el problema persiste póngase en contacto con el desarrollador.");
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
        crearPedido();
    }
}