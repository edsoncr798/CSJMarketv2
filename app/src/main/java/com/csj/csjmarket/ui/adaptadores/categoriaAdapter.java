package com.csj.csjmarket.ui.adaptadores;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.LoginActivity;
import com.csj.csjmarket.MainActivity;
import com.csj.csjmarket.ProductosCategoria;
import com.csj.csjmarket.R;
import com.csj.csjmarket.modelos.Categorias;
import com.csj.csjmarket.modelos.ValidarCorreo;

import java.util.List;

public class categoriaAdapter extends ArrayAdapter<Categorias> {

    public categoriaAdapter(@NonNull Context context, @NonNull List<Categorias> objects, String idPersona, String email, String docIden
            , String diasUltCompra, String nombre) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Categorias categoria = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_imagebutton, parent, false);
        }

        ImageButton imageButton = convertView.findViewById(R.id.imageButtonCategoria);

        Glide.with(this.getContext())
                .load(getContext().getString(R.string.connection) + "/proveedor/" + categoria.getProveedor() + ".jpg")
                .into(imageButton);

        imageButton.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), ProductosCategoria.class);
            intent.putExtra("proveedor", categoria.getProveedor());
            intent.putExtra("idProveedor", categoria.getId());
            getContext().startActivity(intent);
        });

        return convertView;
    }
}
