package com.csj.csjmarket.ui.vistas;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.Carrito;
import com.csj.csjmarket.EnlazarCliente;
import com.csj.csjmarket.LoginActivity;
import com.csj.csjmarket.R;
import com.csj.csjmarket.databinding.FragmentInicioBinding;
import com.csj.csjmarket.modelos.CantidadBanner;
import com.csj.csjmarket.modelos.Categorias;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.modelos.filtroCategorias;
import com.csj.csjmarket.modelos.filtroProveedor;
import com.csj.csjmarket.ui.adaptadores.ImageAdapter;
import com.csj.csjmarket.ui.adaptadores.ProductoAdapter;
import com.csj.csjmarket.ui.adaptadores.categoriaAdapter;
import com.csj.csjmarket.ui.adaptadores.itemFiltroCategoriaAdapter;
import com.csj.csjmarket.ui.adaptadores.itemFiltroProveedorAdapter;
import com.csj.csjmarket.ui.adaptadores.proveedorAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class inicio extends Fragment implements itemFiltroProveedorAdapter.OnItemChangedListener{

    private ArrayList<Producto> productos = new ArrayList<>();
    private ArrayList<Producto> productosFiltrado = new ArrayList<>();
    private ArrayList<Producto> productosFiltradoProv = new ArrayList<>();;
    private ArrayList<filtroProveedor> filtroProveedores = new ArrayList<>();
    private ArrayList<filtroProveedor> filtroProveedoresTmp = new ArrayList<>();
    private ArrayList<filtroProveedor> filtroProveedoresSel = new ArrayList<>();
    private Double filtroDesde = 0.0;
    private Double filtroHasta = 0.0;

    private FragmentInicioBinding binding;
    private AlertDialog alertDialog;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;
    private String carritoString;
    private View view;
    private LinearLayoutManager layoutManager;
    private int numeroImagenesBanner = 0;
    private int lastVisibleItemPosition;
    private ImageAdapter adapter;
    private int currentPage = 0;
    private Handler handler;
    private final int delay = 4000;
    private ValidarCorreo validarCorreo;
    private Map<Integer, StockInfo> stockMap = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInicioBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        sharedPreferences = view.getContext().getSharedPreferences("carritoInfo", MODE_PRIVATE);

        validarCorreo = (ValidarCorreo) getActivity().getIntent().getSerializableExtra("validarCorreo");

        layoutManager = new GridLayoutManager(view.getContext(), 2, LinearLayoutManager.VERTICAL, false);
        binding.rvListaProducto.setLayoutManager(layoutManager);

        mostrarCarrousel(view.getContext());
        //obtenerProveedores(view.getContext());
        recargar(view.getContext());

        binding.txtBuscarProducto.addTextChangedListener(new validacionTextWatcher(binding.txtBuscarProducto));

        binding.fab.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        binding.fab.setOnClickListener(view1 -> {
            Intent intent = new Intent(view.getContext(), Carrito.class);
            intent.putExtra("idPersona",validarCorreo.getId().toString());
            intent.putExtra("email", getActivity().getIntent().getStringExtra("correo"));
            intent.putExtra("docIden", validarCorreo.getDocIdentidad());
            intent.putExtra("diasUltCompra", validarCorreo.getDiasUltimaCompra());
            intent.putExtra("nombre", validarCorreo.getPrimerNombre());
            view.getContext().startActivity(intent);
        });

        if (savedInstanceState != null) {
            lastVisibleItemPosition = savedInstanceState.getInt("lastVisibleItemPosition", 0);
        }

        binding.layoutFiltro.setOnClickListener(view1 -> {
            if (filtroProveedores.size() == 0){
                cargarProveedorFiltro(view.getContext());
            }
            else{
                mostrarFiltro();
            }
        });

        binding.layoutBorrarFiltro.setOnClickListener(view1 -> {
            filtroProveedoresSel = new ArrayList<>();
            filtroProveedoresTmp = new ArrayList<>();
            filtroHasta = 0.0;
            filtroDesde = 0.0;

            for (filtroProveedor tmp: filtroProveedores) {
                tmp.setEstado(false);
            }

            binding.txtFiltro.setText("(" + filtroProveedoresSel.size() + ")");

            ProductoAdapter productoAdapter = new ProductoAdapter(productos, stockMap);
            binding.rvListaProducto.setAdapter(productoAdapter);
            binding.txtNumeroProductos.setText(((Integer) productos.size()).toString());
            productoAdapter.notifyDataSetChanged();
        });

        return view;
    }

    @Override
    public void onItemChanged(ArrayList<filtroProveedor> nuevoFiltro) {
        filtroProveedoresTmp = nuevoFiltro;
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
                case R.id.txtBuscarProducto:
                    //filtrarXNombre();
                    filtrarXCategoria();
                    break;
            }
        }
    }

    private void filtrarXNombre(){
        productosFiltrado = (ArrayList<Producto>) productos
                .stream()
                .filter(x -> x.getNombre().toUpperCase().contains(binding.txtBuscarProducto.getText().toString().toUpperCase()))
                .collect(Collectors.toList());

        ProductoAdapter productoAdapter = new ProductoAdapter(productosFiltrado, stockMap);
        binding.rvListaProducto.setAdapter(productoAdapter);
        binding.txtNumeroProductos.setText(((Integer) productosFiltrado.size()).toString());
        productoAdapter.notifyDataSetChanged();
    }

    private void filtrarXCategoria(){
        if (filtroProveedoresSel.size() == 0){
            productosFiltrado = productos;

            if (filtroDesde > 0){
                if (filtroHasta > 0){
                    productosFiltrado = (ArrayList<Producto>) productosFiltrado
                            .stream()
                            .filter(x -> x.getPrecioUnidadBase() >= filtroDesde && x.getPrecioUnidadBase() <= filtroHasta)
                            .collect(Collectors.toList());
                }
                else{
                    productosFiltrado = (ArrayList<Producto>) productosFiltrado
                            .stream()
                            .filter(x -> x.getPrecioUnidadBase() >= filtroDesde)
                            .collect(Collectors.toList());
                }
            }
            else if (filtroHasta > 0){
                productosFiltrado = (ArrayList<Producto>) productosFiltrado
                        .stream()
                        .filter(x -> x.getPrecioUnidadBase() <= filtroHasta)
                        .collect(Collectors.toList());
            }

            if (binding.txtBuscarProducto.getText().length() > 0){
                String textoBusqueda = binding.txtBuscarProducto.getText().toString().toUpperCase();
                productosFiltrado = (ArrayList<Producto>) productosFiltrado
                        .stream()
                        .filter(x -> x.getNombre().toUpperCase().contains(textoBusqueda)
                                || (x.getCodigo() != null && x.getCodigo().toUpperCase().contains(textoBusqueda)))
                        .collect(Collectors.toList());
            }

            ProductoAdapter productoAdapter = new ProductoAdapter(productosFiltrado, stockMap);
            binding.rvListaProducto.setAdapter(productoAdapter);
            binding.txtNumeroProductos.setText(((Integer) productosFiltrado.size()).toString());
            productoAdapter.notifyDataSetChanged();
        }
        else{
            productosFiltrado = new ArrayList<>();
            productosFiltradoProv = new ArrayList<>();
            for (filtroProveedor filtro: filtroProveedoresSel) {
                productosFiltradoProv = (ArrayList<Producto>) productos
                        .stream()
                        .filter(x -> x.getIdProveedor() == filtro.getId())
                        .collect(Collectors.toList());

                productosFiltrado.addAll(productosFiltradoProv);
            }

            if (filtroDesde > 0){
                if (filtroHasta > 0){
                    productosFiltrado = (ArrayList<Producto>) productosFiltrado
                            .stream()
                            .filter(x -> x.getPrecioUnidadBase() >= filtroDesde && x.getPrecioUnidadBase() <= filtroHasta)
                            .collect(Collectors.toList());
                }
                else{
                    productosFiltrado = (ArrayList<Producto>) productosFiltrado
                            .stream()
                            .filter(x -> x.getPrecioUnidadBase() >= filtroDesde)
                            .collect(Collectors.toList());
                }
            }
            else if (filtroHasta > 0){
                productosFiltrado = (ArrayList<Producto>) productosFiltrado
                        .stream()
                        .filter(x -> x.getPrecioUnidadBase() <= filtroHasta)
                        .collect(Collectors.toList());
            }

            if (binding.txtBuscarProducto.getText().length() > 0){
                String textoBusqueda = binding.txtBuscarProducto.getText().toString().toUpperCase();
                productosFiltrado = (ArrayList<Producto>) productosFiltrado
                        .stream()
                        .filter(x -> x.getNombre().toUpperCase().contains(textoBusqueda)
                                || (x.getCodigo() != null && x.getCodigo().toUpperCase().contains(textoBusqueda)))
                        .collect(Collectors.toList());
            }

            ProductoAdapter productoAdapter = new ProductoAdapter(productosFiltrado, stockMap);
            binding.rvListaProducto.setAdapter(productoAdapter);
            binding.txtNumeroProductos.setText(((Integer) productosFiltrado.size()).toString());
            productoAdapter.notifyDataSetChanged();
        }
    }

    private void recargar(Context context) {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/productos";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Type productListType = new TypeToken<List<Producto>>() {
            }.getType();
            productos = gson.fromJson(response.toString(), productListType);
            ProductoAdapter productoAdapter = new ProductoAdapter(productos, stockMap);
            binding.rvListaProducto.setAdapter(productoAdapter);
            productoAdapter.notifyDataSetChanged();
            binding.txtNumeroProductos.setText(((Integer) productos.size()).toString());
            alertDialog.dismiss();
            // Cargar stock en segundo plano y refrescar etiquetas cuando llegue
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

    private void cargarProveedorFiltro(Context context) {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/filtroproveedor";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Type proveedorFiltroType = new TypeToken<List<filtroProveedor>>() {
            }.getType();
            filtroProveedores = gson.fromJson(response.toString(), proveedorFiltroType);
            alertDialog.dismiss();
            mostrarFiltro();
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

    private void mostrarFiltro(){
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialogo_filtro, null);
        builder.setView(dialogView);
        builder.setCancelable(true);

        EditText txtDesde = dialogView.findViewById(R.id.filtro_txtDesde);
        EditText txtHasta = dialogView.findViewById(R.id.filtro_txtHasta);
        RecyclerView rvCategoria = dialogView.findViewById(R.id.filtro_rvCategoria);

        // Inicializar valores actuales
        if (filtroDesde != null && filtroDesde > 0) {
            txtDesde.setText(String.valueOf(filtroDesde.intValue()));
        }
        if (filtroHasta != null && filtroHasta > 0) {
            txtHasta.setText(String.valueOf(filtroHasta.intValue()));
        }

        txtDesde.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    filtroDesde = s.toString().isEmpty() ? 0.0 : Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    filtroDesde = 0.0;
                }
            }
        });

        txtHasta.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    filtroHasta = s.toString().isEmpty() ? 0.0 : Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    filtroHasta = 0.0;
                }
            }
        });

        rvCategoria.setLayoutManager(new LinearLayoutManager(view.getContext()));
        itemFiltroProveedorAdapter adapterFiltro = new itemFiltroProveedorAdapter(filtroProveedores);
        adapterFiltro.setOnItemChangedListener(this);
        rvCategoria.setAdapter(adapterFiltro);

        builder.setPositiveButton("Aplicar", (dialog, which) -> {
            // Guardar selección temporal como selección final
            if (filtroProveedoresTmp != null) {
                filtroProveedoresSel = new ArrayList<>(filtroProveedoresTmp);
            } else {
                filtroProveedoresSel = new ArrayList<>();
            }
            binding.txtFiltro.setText("(" + filtroProveedoresSel.size() + ")");
            // Limpiar texto de búsqueda para evitar conflicto con filtros
            binding.txtBuscarProducto.setText("");
            filtrarXCategoria();
        });
        builder.setNegativeButton("Cancelar", null);

        AlertDialog filtroDialog = builder.create();
        filtroDialog.show();
    }

    private void mostrarCarrousel(Context context){
        // Java
        ImageCarousel carousel = binding.carousel;

        // Register lifecycle. For activity this will be lifecycle/getLifecycle() and for fragments it will be viewLifecycleOwner/getViewLifecycleOwner().
        carousel.registerLifecycle(getLifecycle());
        carousel.setPersistentDrawingCache(0);

        List<CarouselItem> list = new ArrayList<>();

        String url = getString(R.string.connection) + "/api/cantidadImagenesBanner";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Type cantidadType = new TypeToken<CantidadBanner>() {
            }.getType();
            CantidadBanner cantidadBanner = gson.fromJson(response.toString(), cantidadType);

            numeroImagenesBanner = cantidadBanner.getCantidad();
            if (numeroImagenesBanner > 0){
                for (int i = 1; i <= numeroImagenesBanner; i++){
                    list.add(
                            new CarouselItem(
                                    view.getContext().getString(R.string.connection) + "/banner/" + i + ".jpg"
                            )
                    );
                }

                carousel.setData(list);
            }
            else {
                carousel.setVisibility(View.GONE);
            }

        }, error -> {
            alertDialog.dismiss();
        });
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    /*private void obtenerProveedores(Context context) {
        String url = getString(R.string.connection) + "/api/categorias";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Type categoriaType = new TypeToken<List<Categorias>>() {
            }.getType();
            List<Categorias> categorias = gson.fromJson(response.toString(), categoriaType);

            proveedorAdapter adapter = new proveedorAdapter(this.getContext(), categorias, bundle.getString("id"), bundle.getString("email"),
                    bundle.getString("docIden"), bundle.getString("diasUltCompra"), bundle.getString("nombre"));
            binding.lvProveedores.setAdapter(adapter);

            int totalHeight = 0;
            for (int i = 0; i < adapter.getCount(); i++) {
                View listItem = adapter.getView(i, null, binding.lvProveedores);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = binding.lvProveedores.getLayoutParams();
            params.height = totalHeight + (binding.lvProveedores.getDividerHeight() * (adapter.getCount() - 1));
            binding.lvProveedores.setLayoutParams(params);

        }, error -> {
            alertDialog.dismiss();
        });
        Volley.newRequestQueue(context).add(jsonArrayRequest);
    }*/

    private void mostrarLoader() {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        View progress = getLayoutInflater().inflate(R.layout.loader, null);
        builder.setView(progress);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void mostrarAlerta(String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle("Error");
        builder.setMessage(mensaje);
        builder.setPositiveButton("Aceptar", null);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
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
                // Guardar mapa de stock en SharedPreferences para uso posterior (Carrito)
                android.content.SharedPreferences sp = context.getSharedPreferences("stockInfo", android.content.Context.MODE_PRIVATE);
                sp.edit().putString("stockMap", new com.google.gson.Gson().toJson(stockMap)).apply();
                // Refrescar adapter actual con stock
                if (binding.rvListaProducto.getAdapter() != null) {
                    ProductoAdapter productoAdapter = new ProductoAdapter(productos, stockMap);
                    binding.rvListaProducto.setAdapter(productoAdapter);
                    productoAdapter.notifyDataSetChanged();
                }
            } catch (JSONException e) {
                // Ignorar parse errors por ahora
            }
        }, error -> {
            // Silencioso: si falla el stock, solo no se muestra la etiqueta
        });
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.txtBienvenida.setText("Hola " + validarCorreo.getPrimerNombre() + ", ¿Qué compramos hoy?");
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
    public void onPause() {
        if (layoutManager != null) {
            lastVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            if (layoutManager != null) {
                layoutManager.scrollToPosition(lastVisibleItemPosition);
            }

            filtrarXCategoria();
            //filtrarXNombre();

            carritoString = sharedPreferences.getString("carrito", "");
            if (!carritoString.isEmpty()){
                Type typeS = new TypeToken<List<MiCarrito>>() {
                }.getType();
                ArrayList<MiCarrito> miCarrito = gson.fromJson(carritoString, typeS);
                Integer numeroItems = miCarrito.size();

                if (numeroItems == 0){
                    binding.txtCarritoItemCount.setVisibility(View.GONE);
                }
                else {
                    Animation animation = AnimationUtils.loadAnimation(binding.getRoot().getContext(), R.anim.zoom_out);
                    binding.txtCarritoItemCount.startAnimation(animation);
                    binding.txtCarritoItemCount.setVisibility(View.VISIBLE);
                    binding.txtCarritoItemCount.setText(numeroItems.toString());
                }
            }
            else {
                binding.txtCarritoItemCount.setVisibility(View.GONE);
            }

        }catch (Exception ex){
            ex.getMessage();
        }
    }
}