package com.csj.csjmarket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import com.bumptech.glide.Glide;
import com.csj.csjmarket.databinding.ActivityProductosCategoriaBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.ui.adaptadores.ProductoAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductosCategoria extends AppCompatActivity {

    private final ArrayList<Producto> productos = new ArrayList<>();
    private ArrayList<Producto> productosFiltrado = new ArrayList<>();
    private ActivityProductosCategoriaBinding binding;
    private AlertDialog alertDialog;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;
    private String carritoString;
    private GridLayoutManager layoutManager;
    private String rucProveedor;
    private String idProveedor;
    private int lastVisibleItemPosition;
    private ProductoAdapter productoAdapter;

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
        productoAdapter = new ProductoAdapter(productos);
        binding.provRvListaProducto.setAdapter(productoAdapter);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = productoAdapter.getItemViewType(position);
                return viewType == 2 ? layoutManager.getSpanCount() : 1;
            }
        });

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
            String idPersona = getIntent().getStringExtra("idPersona");
            if (idPersona == null || idPersona.isEmpty()) {
                try {
                    com.csj.csjmarket.modelos.ValidarCorreo vc = (com.csj.csjmarket.modelos.ValidarCorreo) getIntent().getSerializableExtra("validarCorreo");
                    if (vc != null && vc.getId() != null) {
                        idPersona = String.valueOf(vc.getId());
                    }
                } catch (Exception ignored) {}
                if (idPersona == null || idPersona.isEmpty()) {
                    idPersona = getIntent().getStringExtra("id");
                }
            }
            if (idPersona != null && !idPersona.isEmpty()) {
                intent.putExtra("idPersona", idPersona);
            }
            intent.putExtra("email", getIntent().getStringExtra("email"));
            intent.putExtra("docIden", getIntent().getStringExtra("docIden"));
            intent.putExtra("diasUltCompra", getIntent().getStringExtra("diasUltCompra"));
            intent.putExtra("nombre", getIntent().getStringExtra("nombre"));
            this.startActivity(intent);
        });

        // Paginación eliminada: sin scroll infinito
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
        String url = getString(R.string.connection) + "/api/productoscategoria/v2?idProveedor=" + idProveedor;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, responseStr -> {
            if (alertDialog != null) alertDialog.dismiss();
            try {
                ArrayList<Producto> nuevos;
                if (responseStr != null && responseStr.trim().startsWith("{")) {
                    JSONObject obj = new JSONObject(responseStr);
                    JSONArray arr = obj.optJSONArray("productos");
                    Type productListType = new TypeToken<List<Producto>>() {}.getType();
                    nuevos = gson.fromJson(arr != null ? arr.toString() : "[]", productListType);
                } else {
                    JSONArray arr = new JSONArray(responseStr);
                    Type productListType = new TypeToken<List<Producto>>() {}.getType();
                    nuevos = gson.fromJson(arr.toString(), productListType);
                }

                productos.clear();
                productos.addAll(nuevos);
                productoAdapter.productos = productos;
                productoAdapter.notifyDataSetChanged();

                // Persistir mapa de stock desde productos para el carrito
                SharedPreferences sp = context.getSharedPreferences("stockInfo", MODE_PRIVATE);
                java.util.Map<Integer, StockInfo> map = new java.util.HashMap<>();
                for (Producto p : productos) {
                    map.put(p.getId(), new StockInfo(p.getId(), p.getStockFisico(), p.getStockPorEntregar()));
                }
                sp.edit().putString("stockMap", new Gson().toJson(map)).apply();
            } catch (Exception e) {
                mostrarAlerta(e.getMessage());
            }
        }, error -> {
            if (alertDialog != null) alertDialog.dismiss();
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
        }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setShouldCache(false);
        Volley.newRequestQueue(context).add(stringRequest);
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
        String textoBusqueda = binding.provTxtBuscarProducto.getText().toString().toUpperCase().trim();
        if (textoBusqueda.isEmpty()) {
            // Mostrar lista completa con scroll infinito
            binding.provRvListaProducto.setAdapter(productoAdapter);
            productoAdapter.notifyDataSetChanged();
            productoAdapter.setMostrarLoaderAlFinal(false);
            return;
        }

        productosFiltrado = (ArrayList<Producto>) productos
                .stream()
                .filter(x -> x.getNombre().toUpperCase().contains(textoBusqueda)
                        || (x.getCodigo() != null && x.getCodigo().toUpperCase().contains(textoBusqueda)))
                .collect(Collectors.toList());

        ProductoAdapter adaptadorFiltrado = new ProductoAdapter(productosFiltrado);
        binding.provRvListaProducto.setAdapter(adaptadorFiltrado);
        adaptadorFiltrado.notifyDataSetChanged();
        // Ocultar loader al filtrar
        productoAdapter.setMostrarLoaderAlFinal(false);
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