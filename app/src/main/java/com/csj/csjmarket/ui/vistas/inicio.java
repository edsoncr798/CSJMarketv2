package com.csj.csjmarket.ui.vistas;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.csj.csjmarket.Carrito;
import com.csj.csjmarket.R;
import com.csj.csjmarket.databinding.FragmentInicioBinding;
import com.csj.csjmarket.modelos.CantidadBanner;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.modelos.filtroProveedor;
import com.csj.csjmarket.ui.adaptadores.ProductoAdapter;
import com.csj.csjmarket.ui.adaptadores.itemFiltroProveedorAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class inicio extends Fragment implements itemFiltroProveedorAdapter.OnItemChangedListener {

    private ArrayList<Producto> productos = new ArrayList<>();
    private ArrayList<Producto> productosFiltrado = new ArrayList<>();
    private ArrayList<Producto> productosFiltradoProv = new ArrayList<>();
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
    private ProductoAdapter productoAdapter;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutos

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


        layoutManager = new GridLayoutManager(view.getContext(), 2, LinearLayoutManager.VERTICAL, false);
        binding.rvListaProducto.setLayoutManager(layoutManager);

        // Inicializar adapter con lista vacía (o caché si está disponible)
        productoAdapter = new ProductoAdapter(productos);
        binding.rvListaProducto.setAdapter(productoAdapter);

        mostrarCarrousel(view.getContext());
        cargarDesdeCache(view.getContext());
        recargar(view.getContext());

        // Paginación eliminada: sin scroll infinito

        binding.txtBuscarProducto.addTextChangedListener(new validacionTextWatcher(binding.txtBuscarProducto));

        binding.fab.setOnClickListener(view1 -> {
            Intent intent = new Intent(view.getContext(), Carrito.class);
            String idPersona = requireActivity().getIntent().getStringExtra("idPersona");
            if (idPersona == null || idPersona.isEmpty()) {
                try {
                    com.csj.csjmarket.modelos.ValidarCorreo vc = (com.csj.csjmarket.modelos.ValidarCorreo) getActivity().getIntent().getSerializableExtra("validarCorreo");
                    if (vc != null && vc.getId() != null) {
                        idPersona = String.valueOf(vc.getId());
                    }
                } catch (Exception ignored) {}
                if (idPersona == null || idPersona.isEmpty()) {
                    idPersona = requireActivity().getIntent().getStringExtra("id");
                }
            }
            if (idPersona != null && !idPersona.isEmpty()) {
                intent.putExtra("idPersona", idPersona);
            }
            intent.putExtra("email", getActivity().getIntent().getStringExtra("email"));
            intent.putExtra("docIden", getActivity().getIntent().getStringExtra("docIden"));
            intent.putExtra("diasUltCompra", getActivity().getIntent().getStringExtra("diasUltCompra"));
            intent.putExtra("nombre", getActivity().getIntent().getStringExtra("nombre"));
            view.getContext().startActivity(intent);
        });

        binding.layoutFiltro.setOnClickListener(v -> cargarProveedorFiltro(view.getContext()));
        // Añadimos listener para borrar filtro
        binding.layoutBorrarFiltro.setOnClickListener(v -> limpiarFiltros());

        binding.fab.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        if (savedInstanceState != null) {
            lastVisibleItemPosition = savedInstanceState.getInt("lastVisibleItemPosition", 0);
        }

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
                    filtrarXCategoria();
                    break;
            }
        }
    }

    private void filtrarXNombre() {
        productosFiltrado = (ArrayList<Producto>) productos
                .stream()
                .filter(x -> x.getNombre().toUpperCase().contains(binding.txtBuscarProducto.getText().toString().toUpperCase()))
                .collect(Collectors.toList());

        // Usar el adapter de campo para mantener sincronización con recargar()
        productoAdapter.productos = productosFiltrado;
        binding.rvListaProducto.setAdapter(productoAdapter);
        binding.txtNumeroProductos.setText(String.valueOf(productosFiltrado.size()));
        productoAdapter.notifyDataSetChanged();
    }

    private void filtrarXCategoria() {
        if (filtroProveedoresSel.isEmpty()) {
            productosFiltrado = productos;
            if (!binding.txtBuscarProducto.getText().isEmpty()) {
                String textoBusqueda = binding.txtBuscarProducto.getText().toString().toUpperCase();
                productosFiltrado = (ArrayList<Producto>) productosFiltrado
                        .stream()
                        .filter(x -> x.getNombre().toUpperCase().contains(textoBusqueda)
                                || (x.getCodigo() != null && x.getCodigo().toUpperCase().contains(textoBusqueda)))
                        .collect(Collectors.toList());
            }

            // Usar el adapter de campo, no crear uno nuevo
            productoAdapter.productos = productosFiltrado;
            binding.rvListaProducto.setAdapter(productoAdapter);
            binding.txtNumeroProductos.setText(String.valueOf(productosFiltrado.size()));
            productoAdapter.notifyDataSetChanged();
        } else {
            productosFiltrado = new ArrayList<>();
            productosFiltradoProv = new ArrayList<>();
            for (filtroProveedor filtro : filtroProveedoresSel) {
                productosFiltradoProv = (ArrayList<Producto>) productos
                        .stream()
                        .filter(x -> x.getIdProveedor() == filtro.getId())
                        .collect(Collectors.toList());

                productosFiltrado.addAll(productosFiltradoProv);
            }

            if (!binding.txtBuscarProducto.getText().isEmpty()) {
                String textoBusqueda = binding.txtBuscarProducto.getText().toString().toUpperCase();
                productosFiltrado = (ArrayList<Producto>) productosFiltrado
                        .stream()
                        .filter(x -> x.getNombre().toUpperCase().contains(textoBusqueda)
                                || (x.getCodigo() != null && x.getCodigo().toUpperCase().contains(textoBusqueda)))
                        .collect(Collectors.toList());
            }

            // Usar el adapter de campo, no crear uno nuevo
            productoAdapter.productos = productosFiltrado;
            binding.rvListaProducto.setAdapter(productoAdapter);
            binding.txtNumeroProductos.setText(String.valueOf(productosFiltrado.size()));
            productoAdapter.notifyDataSetChanged();
        }
    }

    // Limpia todos los criterios de filtro y restaura la lista completa cargada
    private void limpiarFiltros() {
        try {
            if (filtroProveedoresSel != null) filtroProveedoresSel.clear();
            if (filtroProveedoresTmp != null) filtroProveedoresTmp.clear();
        } catch (Exception ignored) {}
        filtroDesde = 0.0;
        filtroHasta = 0.0;
        binding.txtFiltro.setText("");
        binding.txtBuscarProducto.setText("");

        // Restaurar lista completa cargada hasta ahora (incluye páginas ya paginadas)
        productoAdapter.productos = productos;
        binding.rvListaProducto.setAdapter(productoAdapter);
        binding.txtNumeroProductos.setText(String.valueOf(productos.size()));
        productoAdapter.notifyDataSetChanged();
    }

    // Método legacy: delega a la versión paginada
    private void recargar(Context context) {
        try {
            if (productos.isEmpty()) {
                mostrarLoader();
            }
        } catch (Exception ignore) {}

        String url = getString(R.string.connection) + "/api/productos/v2/listar";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, responseStr -> {
            try { if (alertDialog != null) alertDialog.dismiss(); } catch (Exception ignore) {}
            try {
                ArrayList<Producto> nuevos;
                if (responseStr != null && responseStr.trim().startsWith("{")) {
                    JSONObject obj = new JSONObject(responseStr);
                    JSONArray arr = obj.optJSONArray("productos");
                    Type productListType = new TypeToken<List<Producto>>() {}.getType();
                    nuevos = gson.fromJson(arr != null ? arr.toString() : "[]", productListType);
                } else {
                    Type productListType = new TypeToken<List<Producto>>() {}.getType();
                    nuevos = gson.fromJson(responseStr != null ? responseStr : "[]", productListType);
                }

                productos.clear();
                productos.addAll(nuevos);
                productoAdapter.productos = productos;
                binding.rvListaProducto.setAdapter(productoAdapter);
                productoAdapter.notifyDataSetChanged();
                binding.txtNumeroProductos.setText(String.valueOf(productos.size()));

                // Actualizar caché completa
                SharedPreferences sp = context.getSharedPreferences("inicioCache", MODE_PRIVATE);
                sp.edit()
                        .putString("productos", new Gson().toJson(productos))
                        .putLong("ts", System.currentTimeMillis())
                        .apply();

                // Persistir mapa de stock
                SharedPreferences spStock = context.getSharedPreferences("stockInfo", MODE_PRIVATE);
                Map<Integer, StockInfo> map = new HashMap<>();
                String mapStr = spStock.getString("stockMap", "");
                if (mapStr != null && !mapStr.isEmpty()) {
                    Type stockType = new TypeToken<Map<Integer, StockInfo>>() {}.getType();
                    map = new Gson().fromJson(mapStr, stockType);
                }
                for (Producto p : productos) {
                    map.put(p.getId(), new StockInfo(p.getId(), p.getStockFisico(), p.getStockPorEntregar()));
                }
                spStock.edit().putString("stockMap", new Gson().toJson(map)).apply();
            } catch (Exception e) {
                mostrarAlerta(e.getMessage());
            }
        }, error -> {
            try { if (alertDialog != null) alertDialog.dismiss(); } catch (Exception ignore) {}
            // Fallback: si falla y no hay productos, intentar cache sin TTL
            if (productos == null || productos.isEmpty()) {
                boolean cargadoStale = cargarDesdeCacheIgnorandoTTL(context);
                if (cargadoStale) {
                    mostrarAlerta("Sin conexión. Mostrando datos guardados (pueden estar desactualizados).");
                    return;
                }
            }

            if (error instanceof TimeoutError) {
                mostrarAlerta("Tiempo de espera agotado (30s). Verifica tu conexión o que el servidor esté disponible.");
            } else if (error instanceof NoConnectionError) {
                mostrarAlerta("Sin conexión. Revisa internet o permisos de red del emulador.");
            } else {
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null) {
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                } else {
                    mostrarAlerta(error.toString());
                }
            }
        });

        int timeoutMs = 30000; // 30 segundos
        int maxRetries = 1;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                timeoutMs,
                maxRetries,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        stringRequest.setShouldCache(false);
        Volley.newRequestQueue(context).add(stringRequest);
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
            if (error instanceof TimeoutError) {
                mostrarAlerta("Tiempo de espera agotado (30s). Revisa tu conexión o disponibilidad del servidor.");
            } else if (error instanceof NoConnectionError) {
                mostrarAlerta("Sin conexión al cargar proveedores. Revisa internet.");
            } else {
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null) {
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                } else {
                    mostrarAlerta(error.toString());
                }
            }
        });
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        jsonArrayRequest.setShouldCache(false);
        Volley.newRequestQueue(context).add(jsonArrayRequest);
    }

    private void mostrarFiltro() {
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    filtroDesde = s.toString().isEmpty() ? 0.0 : Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    filtroDesde = 0.0;
                }
            }
        });

        txtHasta.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
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

    private void mostrarCarrousel(Context context) {
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
            if (numeroImagenesBanner > 0) {
                for (int i = 1; i <= numeroImagenesBanner; i++) {
                    list.add(
                            new CarouselItem(
                                    view.getContext().getString(R.string.connection) + "/banner/" + i + ".jpg"
                            )
                    );
                }

                carousel.setData(list);
            } else {
                carousel.setVisibility(View.GONE);
            }

        }, error -> {
            // No cerrar loaders globales aquí; solo ocultar el carrusel si falla
            carousel.setVisibility(View.GONE);
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

    // (removed) private void cargarStock(Context context) { /* Eliminado */ }
    @Override
    public void onStart() {
        super.onStart();
        String nombreUsuario = "Cliente";
        try {
            com.csj.csjmarket.modelos.ValidarCorreo vc = (com.csj.csjmarket.modelos.ValidarCorreo) requireActivity().getIntent().getSerializableExtra("validarCorreo");
            if (vc != null && vc.getPrimerNombre() != null && !vc.getPrimerNombre().trim().isEmpty()) {
                nombreUsuario = vc.getPrimerNombre();
            }
        } catch (Exception ignored) {}
        binding.txtBienvenida.setText("Hola " + nombreUsuario + ", ¿Qué compramos hoy?");
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
            if (!carritoString.isEmpty()) {
                Type typeS = new TypeToken<List<MiCarrito>>() {
                }.getType();
                ArrayList<MiCarrito> miCarrito = gson.fromJson(carritoString, typeS);
                Integer numeroItems = miCarrito.size();

                if (numeroItems == 0) {
                    binding.txtCarritoItemCount.setVisibility(View.GONE);
                } else {
                    Animation animation = AnimationUtils.loadAnimation(binding.getRoot().getContext(), R.anim.zoom_out);
                    binding.txtCarritoItemCount.startAnimation(animation);
                    binding.txtCarritoItemCount.setVisibility(View.VISIBLE);
                    binding.txtCarritoItemCount.setText(numeroItems.toString());
                }
            } else {
                binding.txtCarritoItemCount.setVisibility(View.GONE);
            }

        } catch (Exception ex) {
            ex.getMessage();
        }
    }



    // Cargar datos de caché si son válidos (TTL)
    private boolean cargarDesdeCache(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences("inicioCache", MODE_PRIVATE);
            long ts = sp.getLong("ts", 0L);
            String productosStr = sp.getString("productos", "");
            boolean valido = ts > 0 && productosStr != null && !productosStr.isEmpty() && (System.currentTimeMillis() - ts) < CACHE_TTL_MS;
            if (valido) {
                Type productListType = new TypeToken<List<Producto>>() {
                }.getType();
                ArrayList<Producto> cached = gson.fromJson(productosStr, productListType);
                productos.clear();
                productos.addAll(cached);
                productoAdapter.productos = productos;
                binding.rvListaProducto.setAdapter(productoAdapter);
                productoAdapter.notifyDataSetChanged();
                binding.rvListaProducto.invalidateItemDecorations();
                binding.rvListaProducto.invalidate();
                binding.txtNumeroProductos.setText(String.valueOf(productos.size()));
                return true;
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    // Fallback: Cargar datos de caché ignorando TTL
    private boolean cargarDesdeCacheIgnorandoTTL(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences("inicioCache", MODE_PRIVATE);
            String productosStr = sp.getString("productos", "");
            if (productosStr != null && !productosStr.isEmpty()) {
                Type productListType = new TypeToken<List<Producto>>() {
                }.getType();
                ArrayList<Producto> cached = gson.fromJson(productosStr, productListType);
                productos.clear();
                productos.addAll(cached);
                productoAdapter.productos = productos;
                binding.rvListaProducto.setAdapter(productoAdapter);
                productoAdapter.notifyDataSetChanged();
                binding.txtNumeroProductos.setText(String.valueOf(productos.size()));
                return true;
            }
        } catch (Exception ignore) {
        }
        return false;
    }
}