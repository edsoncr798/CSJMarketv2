package com.csj.csjmarket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.csj.csjmarket.databinding.ActivityCarritoBinding;
import com.csj.csjmarket.databinding.ActivityVerProductoBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.ui.ItemTouchHelper.SimpleCallback;
import com.csj.csjmarket.ui.adaptadores.itemCarritoAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Carrito extends AppCompatActivity implements itemCarritoAdapter.OnItemChangedListener {
    private ActivityCarritoBinding binding;
    private ArrayList<MiCarrito> carrito;
    private itemCarritoAdapter adaptadorCarrito;
    private SharedPreferences sharedPreferences;
    private Double montoTotal = 0.0;
    private Double pesoTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrito);

        binding = ActivityCarritoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.vpBtnRegresar.setOnClickListener(view -> finish());

        binding.carritoBtnComprar.setOnClickListener(view -> {
            Intent intent = new Intent(Carrito.this, DireccionEntrega.class);
            intent.putExtra("idPersona", getIntent().getStringExtra("idPersona"));
            intent.putExtra("totalVenta", montoTotal);
            intent.putExtra("totalPeso", pesoTotal);
            startActivity(intent);
        });
    }

    private void calcularTotales(){
        montoTotal = 0.0;
        pesoTotal = 0.0;
        for (MiCarrito item:carrito) {
            montoTotal += item.getTotal();
            pesoTotal += item.getPesoTotal();
        }
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        String totalFormateado = decimalFormat.format(montoTotal);
        binding.carritoTxtSubTotal.setText(totalFormateado);
        binding.carritoTxtTotal.setText(totalFormateado);

        if (carrito.size() > 0){
            binding.carritoTxtCantItems.setText("Tienes " + carrito.size() + " items");

            if (montoTotal >= 50){
                binding.carritoBtnComprar.setEnabled(true);
                binding.carritoMensajeMontoMinimo.setVisibility(View.GONE);
            }
            else {
                binding.carritoBtnComprar.setEnabled(false);
                binding.carritoMensajeMontoMinimo.setVisibility(View.VISIBLE);
            }
        }
        else {
            binding.carritoTxtCantItems.setText("Su carrito está vacío");
        }
    }

    private void cargarCarrito(){
        String carritojson = sharedPreferences.getString("carrito", "");

        Gson gson = new Gson();
        Type tipoLista = new TypeToken<ArrayList<MiCarrito>>(){}.getType();
        carrito = gson.fromJson(carritojson, tipoLista);
    }

    @Override
    public void onItemChanged() {
        cargarCarrito();
        calcularTotales();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        cargarCarrito();
        if (carrito!=null) {
            binding.rvListaCarrito.setLayoutManager(new LinearLayoutManager(this));
            adaptadorCarrito = new itemCarritoAdapter(carrito, this);
            adaptadorCarrito.setOnItemChangedListener(this);
            binding.rvListaCarrito.setAdapter(adaptadorCarrito);
            adaptadorCarrito.notifyDataSetChanged();

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SimpleCallback.SwipeToDeleteCallback(adaptadorCarrito));
            itemTouchHelper.attachToRecyclerView(binding.rvListaCarrito);

            calcularTotales();
        }
        else{
            binding.rvListaCarrito.setAdapter(null);
            binding.carritoTxtCantItems.setText("Su carrito está vacío");
            binding.carritoTxtTotal.setText("00.00");
            binding.carritoTxtSubTotal.setText("00.00");
            binding.carritoBtnComprar.setEnabled(false);
            binding.carritoMensajeMontoMinimo.setVisibility(View.VISIBLE);
        }
    }
}