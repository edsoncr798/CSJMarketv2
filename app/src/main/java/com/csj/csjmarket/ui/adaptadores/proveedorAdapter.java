package com.csj.csjmarket.ui.adaptadores;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.ProductosCategoria;
import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.Categorias;

import java.util.List;

public class proveedorAdapter extends BaseAdapter {
    private Context context;
    private List<Categorias> categorias;
    private String idPersona, email, docIden, diasUltCompra, nombre;

    public proveedorAdapter(Context context, List<Categorias> categorias, String idPersona, String email, String docIden, String diasUltCompra, String nombre) {
        this.context = context;
        this.categorias = categorias;
        this.idPersona = idPersona;
        this.email = email;
        this.docIden = docIden;
        this.diasUltCompra = diasUltCompra;
        this.nombre = nombre;
    }

    @Override
    public int getCount() {
        return categorias.size();
    }

    @Override
    public Object getItem(int i) {
        return categorias.get(i);
    }

    @Override
    public long getItemId(int i) {
        return categorias.get(i).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_imagebutton, viewGroup, false);
        }

        ImageButton imageButton = view.findViewById(R.id.imageButtonCategoria);

        Glide.with(context)
                .load(context.getString(R.string.connection) + "/proveedor/" + categorias.get(i).getProveedor() + ".jpg")
                .placeholder(R.drawable.default_image)
                .into(imageButton);

        imageButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(context, ProductosCategoria.class);
            intent.putExtra("proveedor", categorias.get(i).getProveedor());
            intent.putExtra("idProveedor", categorias.get(i).getId().toString());
            intent.putExtra("idPersona", idPersona);
            intent.putExtra("email", email);
            intent.putExtra("docIden", docIden);
            intent.putExtra("diasUltCompra", diasUltCompra);
            intent.putExtra("nombre", nombre);
            context.startActivity(intent);
        });
        return view;
    }
}
