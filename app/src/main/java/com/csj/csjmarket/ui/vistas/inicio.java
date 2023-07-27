package com.csj.csjmarket.ui.vistas;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.Carrito;
import com.csj.csjmarket.EnlazarCliente;
import com.csj.csjmarket.LoginActivity;
import com.csj.csjmarket.R;
import com.csj.csjmarket.databinding.FragmentInicioBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.ui.adaptadores.ProductoAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class inicio extends Fragment {

    private ArrayList<Producto> productos = new ArrayList<>();
    private ArrayList<Producto> productosFiltrado = new ArrayList<>();
    private FragmentInicioBinding binding;
    private AlertDialog alertDialog;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;
    private String carritoString;
    private View view;
    private LinearLayoutManager layoutManager;
    private int lastVisibleItemPosition;

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
        Bundle bundle = getArguments();

        layoutManager = new GridLayoutManager(view.getContext(), 2, LinearLayoutManager.VERTICAL, false);
        binding.rvListaProducto.setLayoutManager(layoutManager);

        recargar(view.getContext());

        binding.txtBuscarProducto.addTextChangedListener(new validacionTextWatcher(binding.txtBuscarProducto));

        binding.fab.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        binding.fab.setOnClickListener(view1 -> {
            Intent intent = new Intent(view.getContext(), Carrito.class);
            intent.putExtra("idPersona", bundle.getString("id"));
            view.getContext().startActivity(intent);
        });

        if (savedInstanceState != null) {
            lastVisibleItemPosition = savedInstanceState.getInt("lastVisibleItemPosition", 0);
        }
        return view;
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
                    filtrarXNombre();
                    break;
            }
        }
    }

    private void filtrarXNombre(){
        productosFiltrado = (ArrayList<Producto>) productos
                .stream()
                .filter(x -> x.getNombre().toUpperCase().contains(binding.txtBuscarProducto.getText().toString().toUpperCase()))
                .collect(Collectors.toList());

        ProductoAdapter productoAdapter = new ProductoAdapter(productosFiltrado);
        binding.rvListaProducto.setAdapter(productoAdapter);
        productoAdapter.notifyDataSetChanged();
    }

    private void recargar(Context context) {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/productos";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Type productListType = new TypeToken<List<Producto>>() {
            }.getType();
            productos = gson.fromJson(response.toString(), productListType);
            ProductoAdapter productoAdapter = new ProductoAdapter(productos);
            binding.rvListaProducto.setAdapter(productoAdapter);
            productoAdapter.notifyDataSetChanged();
            alertDialog.dismiss();
        }, error -> {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente. Si el problema persiste póngase en contacto con el desarrollador.");
        });
        Volley.newRequestQueue(context).add(jsonArrayRequest);
    }

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

    @Override
    public void onStart() {
        super.onStart();
        try {
            filtrarXNombre();

            carritoString = sharedPreferences.getString("carrito", "");
            if (!carritoString.isEmpty()){
                Type typeS = new TypeToken<List<MiCarrito>>() {
                }.getType();
                ArrayList<MiCarrito> miCarrito = gson.fromJson(carritoString, typeS);
                Integer numeroItems = miCarrito.size();

                Animation animation = AnimationUtils.loadAnimation(binding.getRoot().getContext(), R.anim.zoom_out);
                binding.txtCarritoItemCount.startAnimation(animation);
                binding.txtCarritoItemCount.setVisibility(View.VISIBLE);
                binding.txtCarritoItemCount.setText(numeroItems.toString());
            }
            else {
                binding.txtCarritoItemCount.setVisibility(View.GONE);
            }
        }catch (Exception ex){

        }
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
    public void onResume() {
        super.onResume();
        if (layoutManager != null) {
            layoutManager.scrollToPosition(lastVisibleItemPosition);
        }
    }
}