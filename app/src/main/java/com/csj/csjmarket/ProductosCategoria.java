package com.csj.csjmarket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.csj.csjmarket.databinding.ActivityProductosCategoriaBinding;
import com.csj.csjmarket.databinding.ActivityVerProductoBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.ui.adaptadores.ProductoAdapter;
import com.csj.csjmarket.ui.vistas.inicio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductosCategoria extends AppCompatActivity {

    private ArrayList<Producto> productos = new ArrayList<>();
    private ArrayList<Producto> productosFiltrado = new ArrayList<>();
    private ActivityProductosCategoriaBinding binding;
    private AlertDialog alertDialog;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;
    private String carritoString;
    private LinearLayoutManager layoutManager;
    private String rucProveedor;
    private String idProveedor;
    private int lastVisibleItemPosition;
    private Map<Integer, StockInfo> stockMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos_categoria);

        binding = ActivityProductosCategoriaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rucProveedor = getIntent().getStringExtra("proveedor");
        idProveedor = getIntent().getStringExtra("idProveedor");

        sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);

        layoutManager = new GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false);
        binding.provRvListaProducto.setLayoutManager(layoutManager);

        Glide.with(this)
                .load(this.getString(R.string.connection) + "/proveedor/" + rucProveedor + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(binding.provImgProveedor);

        binding.provBtnRegresar.setOnClickListener(view -> {
            onBackPressed();
        });

        if (savedInstanceState != null) {
            lastVisibleItemPosition = savedInstanceState.getInt("lastVisibleItemPosition", 0);
        }

        binding.provTxtBuscarProducto.addTextChangedListener(new ProductosCategoria.validacionTextWatcher(binding.provTxtBuscarProducto));

        binding.provFab.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        binding.provFab.setOnClickListener(view1 -> {
            Intent intent = new Intent(this, Carrito.class);
            intent.putExtra("idPersona", getIntent().getStringExtra("idPersona"));
            intent.putExtra("email", getIntent().getStringExtra("email"));
            intent.putExtra("docIden", getIntent().getStringExtra("docIden"));
            intent.putExtra("diasUltCompra", getIntent().getStringExtra("diasUltCompra"));
            intent.putExtra("nombre", getIntent().getStringExtra("nombre"));
            this.startActivity(intent);
        });

        recargar(this);
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

    private void recargar(Context context) {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/productoscategoria?idProveedor=" + idProveedor;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Type productListType = new TypeToken<List<Producto>>() {
            }.getType();
            productos = gson.fromJson(response.toString(), productListType);
            ProductoAdapter productoAdapter = new ProductoAdapter(productos, stockMap);
            binding.provRvListaProducto.setAdapter(productoAdapter);
            productoAdapter.notifyDataSetChanged();
            alertDialog.dismiss();
            cargarStock(context);
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
        Volley.newRequestQueue(context).add(jsonArrayRequest);
    }

    private void cargarStock(Context context) {
        String url = "https://api.comsanjuan.com:8443/api/products/stock";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                JSONArray data = response.getJSONArray("data");
                Map<Integer, StockInfo> nuevoStock = new HashMap<>();
                for (int i = 0; i < data.length(); i++) {
                    JSONArray inner = data.getJSONArray(i);
                    for (int j = 0; j < inner.length(); j++) {
                        JSONObject obj = inner.getJSONObject(j);
                        int id = obj.getInt("Id");
                        int fisico = obj.getInt("StockFisico");
                        int porEntregar = obj.getInt("StockPorEntregar");
                        nuevoStock.put(id, new StockInfo(id, fisico, porEntregar));
                    }
                }
                stockMap = nuevoStock;
                // Guardar mapa de stock en SharedPreferences para uso en Carrito
                android.content.SharedPreferences sp = context.getSharedPreferences("stockInfo", android.content.Context.MODE_PRIVATE);
                sp.edit().putString("stockMap", new com.google.gson.Gson().toJson(stockMap)).apply();
                
                if (binding.provRvListaProducto.getAdapter() != null) {
                    ProductoAdapter productoAdapter = new ProductoAdapter(productos, stockMap);
                    binding.provRvListaProducto.setAdapter(productoAdapter);
                    productoAdapter.notifyDataSetChanged();
                }
            } catch (JSONException e) {
                // Ignorar parse por ahora
            }
        }, error -> {
            // Silencioso
        });
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    public class validacionTextWatcher implements TextWatcher {
        private View view;

        private validacionTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.prov_txtBuscarProducto:
                    filtrarXNombre();
                    break;
            }
        }
    }

    private void filtrarXNombre(){
        String textoBusqueda = binding.provTxtBuscarProducto.getText().toString().toUpperCase();
        productosFiltrado = (ArrayList<Producto>) productos
                .stream()
                .filter(x -> x.getNombre().toUpperCase().contains(textoBusqueda)
                        || (x.getCodigo() != null && x.getCodigo().toUpperCase().contains(textoBusqueda)))
                .collect(Collectors.toList());

        ProductoAdapter productoAdapter = new ProductoAdapter(productosFiltrado, stockMap);
        binding.provRvListaProducto.setAdapter(productoAdapter);
        productoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (layoutManager != null) {
            lastVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            outState.putInt("lastVisibleItemPosition", lastVisibleItemPosition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (layoutManager != null) {
            lastVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            if (layoutManager != null) {
                layoutManager.scrollToPosition(lastVisibleItemPosition);
            }

            filtrarXNombre();

            carritoString = sharedPreferences.getString("carrito", "");
            if (!carritoString.isEmpty()){
                Type typeS = new TypeToken<List<MiCarrito>>() {
                }.getType();
                ArrayList<MiCarrito> miCarrito = gson.fromJson(carritoString, typeS);
                Integer numeroItems = miCarrito.size();

                if (numeroItems == 0){
                    binding.provTxtCarritoItemCount.setVisibility(View.GONE);
                }
                else {
                    Animation animation = AnimationUtils.loadAnimation(binding.getRoot().getContext(), R.anim.zoom_out);
                    binding.provTxtCarritoItemCount.startAnimation(animation);
                    binding.provTxtCarritoItemCount.setVisibility(View.VISIBLE);
                    binding.provTxtCarritoItemCount.setText(numeroItems.toString());
                }
            }
            else {
                binding.provTxtCarritoItemCount.setVisibility(View.GONE);
            }

        }catch (Exception ex){
            ex.getMessage();
        }
    }
}