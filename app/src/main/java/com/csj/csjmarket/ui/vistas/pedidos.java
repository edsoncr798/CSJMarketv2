package com.csj.csjmarket.ui.vistas;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.R;
import com.csj.csjmarket.databinding.FragmentInicioBinding;
import com.csj.csjmarket.databinding.FragmentPedidosBinding;
import com.csj.csjmarket.modelos.MisPedidos;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.csj.csjmarket.ui.adaptadores.ProductoAdapter;
import com.csj.csjmarket.ui.adaptadores.itemMisPedidosAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class pedidos extends Fragment {
    private AlertDialog alertDialog;
    private Gson gson = new Gson();
    private FragmentPedidosBinding binding;
    private View view;
    private ValidarCorreo validarCorreo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPedidosBinding.inflate(inflater, container, false);
        view = binding.getRoot();
        binding.rvListaMisPedidos.setLayoutManager(new LinearLayoutManager(view.getContext()));

        validarCorreo = (ValidarCorreo) getActivity().getIntent().getSerializableExtra("validarCorreo");

        recargar(view.getContext());

        return view;
    }

    private void recargar(Context context) {
        mostrarLoader();
        String url = getString(R.string.connection) + "/mispedido?idPersona=" + validarCorreo.getId();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Type miPedidoListType = new TypeToken<List<MisPedidos>>() {
            }.getType();
            ArrayList<MisPedidos> misPedidos = new ArrayList<>();
            misPedidos = gson.fromJson(response.toString(), miPedidoListType);
            itemMisPedidosAdapter misPedidosAdapter = new itemMisPedidosAdapter(misPedidos);
            binding.rvListaMisPedidos.setAdapter(misPedidosAdapter);
            misPedidosAdapter.notifyDataSetChanged();
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
}