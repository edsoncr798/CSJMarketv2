package com.csj.csjmarket;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.databinding.ActivityVerProductoBinding;
import com.csj.csjmarket.modelos.MiCarrito;
import com.csj.csjmarket.modelos.Producto;
import com.csj.csjmarket.modelos.StockInfo;
import com.csj.csjmarket.ui.Ayudas;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VerProducto extends AppCompatActivity {
    private ActivityVerProductoBinding binding;
    private Producto producto;
    private Integer cantidad = 1;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private ArrayList<MiCarrito> miCarrito;
    private boolean editar = false;
    private StockInfo stockInfo;
    private int stockFisico = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_producto);

        binding = ActivityVerProductoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        Type typeS = new TypeToken<List<MiCarrito>>(){}.getType();
        miCarrito = gson.fromJson(sharedPreferences.getString("carrito", ""), typeS);

        producto = (Producto) getIntent().getSerializableExtra("producto");
        stockInfo = (StockInfo) getIntent().getSerializableExtra("stockInfo");
        if (stockInfo != null) {
            int disponibleUnidades = Math.max(stockInfo.getStockFisico() - stockInfo.getStockPorEntregar(), 0);
            stockFisico = disponibleUnidades;
        } else {
            stockFisico = 0;
        }
        // Ajuste de cantidad visible acorde al stock disponible
        if (stockFisico <= 0) {
            cantidad = 0;
            binding.vpTxtCantidad.setText(cantidad.toString());
        } else if (binding.vpTxtCantidad.getText().toString().equals("")) {
            cantidad = 1;
            binding.vpTxtCantidad.setText(cantidad.toString());
        }
        if (producto.getFactor() == 1){
            binding.seccionUnidades.setVisibility(View.GONE);
        }

        binding.vpNombreProducto.setText(new Ayudas().capitalize(producto.getNombre()));
        binding.vpTxtUnidad.setText("S/ " + producto.getPrecioUnidadBase() + " x " + producto.getUnidadBase());
        binding.vpTxtPrecio.setText(producto.getUnidadBase());
        Glide.with(this)
                .load(getString(R.string.connection) + "/imagenes/" + producto.getCodigo() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(binding.vpImagenProducto);

        // Mostrar stock disponible en la vista de cantidad
        binding.vpTxtStockDisponible.setText("Stock: " + stockFisico);
        // Mostrar código del producto debajo del stock
        binding.vpTxtCodigoProducto.setText("Código: " + producto.getCodigo());

        binding.vpBtnAumentar.setOnClickListener(view -> {
            if (binding.vpTxtCantidad.getText().toString().equals("")){
                cantidad = 1;
                binding.vpTxtCantidad.setText(cantidad.toString());
            }else {
                cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                if (cantidad >= stockFisico) {
                    mostrarMaximoStock();
                } else {
                    cantidad++;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }
            }
            verificarBotonIncremento();
        });

        binding.vpBtnDisminuir.setOnClickListener(view -> {
            if (binding.vpTxtCantidad.getText().toString().equals("") || binding.vpTxtCantidad.getText().toString().equals("1")){
                cantidad = 0;
                binding.vpTxtCantidad.setText(cantidad.toString());
            }else {
                if (cantidad > 0) {
                    cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                    cantidad--;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }
            }
            verificarBotonIncremento();
        });

        binding.vpTxtCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.vpTxtCantidad.getText().toString().equals("")){
                    cantidad = 0;
                }
                else{
                    cantidad = Integer.parseInt(binding.vpTxtCantidad.getText().toString());
                }

                if (cantidad < 0) {
                    cantidad = 0;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                }
                if (cantidad > stockFisico) {
                    cantidad = stockFisico;
                    binding.vpTxtCantidad.setText(cantidad.toString());
                    mostrarMaximoStock();
                }
                verificarBotonIncremento();
            }
        });

        binding.vpBtnRegresar.setOnClickListener(view -> finish());

        binding.vpBtnUnidad.setOnClickListener(view -> {
            binding.vpTxtUnidad.setText(producto.getUnidadBase());
            binding.vpTxtPrecio.setText("S/ " + producto.getPrecioUnidadBase().toString());
            binding.vpBtnUnidad.setBackground(getDrawable(R.drawable.estilo_boton2));
            binding.vpBtnPaquete.setBackground(getDrawable(R.drawable.estilo_boton1));
        });

        binding.vpBtnPaquete.setOnClickListener(view -> {
            binding.vpTxtUnidad.setText(producto.getUnidadReferencia());
            binding.vpTxtPrecio.setText("S/ " + producto.getPrecioUnidadRef().toString());
            binding.vpBtnUnidad.setBackground(getDrawable(R.drawable.estilo_boton1));
            binding.vpBtnPaquete.setBackground(getDrawable(R.drawable.estilo_boton2));
        });

        binding.vpBtnComprar.setOnClickListener(view -> {
            if (cantidad > 0){
                if (miCarrito != null){
                    for (MiCarrito item:miCarrito) {
                        if (item.getIdProducto() == producto.getId()){
                            item.setCantidad(cantidad);
                            item.setTotal(cantidad * producto.getPrecioUnidadBase());
                            editar = true;
                        }
                    }
                }
                else {
                    miCarrito = new ArrayList<>();
                }

                if (!editar){
                    MiCarrito item = new MiCarrito();
                    item.setIdProducto(producto.getId());
                    item.setCodigo(producto.getCodigo());
                    item.setNombre(producto.getNombre());
                    item.setIdUnidad(producto.getIdUnidadBase());
                    item.setPrecio(producto.getPrecioUnidadBase());
                    item.setCantidad(cantidad);
                    item.setUnidad(producto.getUnidadBase());
                    item.setPeso(producto.getPeso());
                    item.setTotal(cantidad * producto.getPrecioUnidadBase());
                    item.setPesoTotal(cantidad * producto.getPeso());
                    miCarrito.add(item);
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("carrito", gson.toJson(miCarrito));
                editor.commit();
            }
            finish();
        });

        verificarBotonIncremento();
    }

    private void mostrarSinStock() {
        Toast.makeText(this, "Producto sin stock", Toast.LENGTH_SHORT).show();
    }

    private void mostrarMaximoStock() {
        if (stockFisico > 0) {
            Toast.makeText(this, "Stock máximo: " + stockFisico, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Stock no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarBotonIncremento() {
        boolean habilitar = cantidad < stockFisico;
        binding.vpBtnAumentar.setEnabled(habilitar);
    }
}