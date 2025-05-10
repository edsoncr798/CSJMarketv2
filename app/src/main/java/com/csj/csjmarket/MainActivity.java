package com.csj.csjmarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.csj.csjmarket.databinding.ActivityMainBinding;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private FirebaseAuth mAuth;
    private TextView txtInicial, txtNombre;
    private ImageView imagen;
    public ValidarCorreo validarCorreo;
    private String nombreUsuario, correoUsuario, rutaImagen;

    private AlertDialog dialogo_creditos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        nombreUsuario = getIntent().getStringExtra("nombre");
        correoUsuario = getIntent().getStringExtra("correo");
        rutaImagen = getIntent().getStringExtra("imagen");
        validarCorreo = (ValidarCorreo) getIntent().getSerializableExtra("validarCorreo");

        mAuth = FirebaseAuth.getInstance();

        //txtInicial = binding.navView.getHeaderView(0).findViewById(R.id.nav_txtInicial);
        imagen = binding.navView.getHeaderView(0).findViewById(R.id.nav_imagen);
        txtNombre = binding.navView.getHeaderView(0).findViewById(R.id.nav_txtNombre);

        //txtInicial.setText(nombreUsuario.substring(0, 1));
        Glide.with(this)
                .load(rutaImagen)
                .placeholder(R.drawable.default_image)
                .into(imagen);
        txtNombre.setText(nombreUsuario);

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_inicio, R.id.nav_voucher, R.id.nav_profile, R.id.nav_politicas)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                switch (navDestination.getId()){
                    /*case R.id.nav_profile:
                        bundle.putString("id", validarCorreo.getId().toString());
                        break;
                    case R.id.nav_inicio:
                        bundle.putString("id", validarCorreo.getId().toString());
                        bundle.putString("email", correoUsuario);
                        bundle.putString("docIden", validarCorreo.getDocIdentidad());
                        bundle.putString("diasUltCompra", validarCorreo.getDiasUltimaCompra().toString());
                        bundle.putString("nombre", nombreUsuario);
                        bundle.putString("primerNombre", validarCorreo.getPrimerNombre());
                        break;
                    case R.id.nav_voucher:
                        bundle.putString("id", validarCorreo.getId().toString());
                        break;*/
                    case R.id.nav_libroReclamaciones:
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://librorec.comsanjuan.com/"));
                        startActivity(intent);
                        break;
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialogo_creditos, null);
            TextView correo = view.findViewById(R.id.correo);
            correo.setOnClickListener(view1 -> {
                String[] recipients = {"fausto@ariasdev.net.pe"}; // Agrega la dirección de correo del destinatario aquí
                String subject = "Quiero un nuevo sistema"; // Asunto del correo
                String body = "Quiero un nuevo sistema para mi empresa"; // Contenido del correo

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_EMAIL, recipients);
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                intent.setType("message/rfc822"); // Esto asegura que se abra el cliente de correo electrónico

                startActivity(Intent.createChooser(intent, "Elige una aplicación de correo:"));
            });
            builder.setView(view).setPositiveButton("Aceptar", null)
                    .setCancelable(true);
            dialogo_creditos = builder.create();
            dialogo_creditos.show();
            return true;
        }
        else if (id == R.id.action_contactUs){
            String msj = "Hola Comercializadora San Juan";
            String numeroTel = "+51965877767";

            try {
                // Crea un URI con el número de teléfono y el mensaje
                Uri uri = Uri.parse("https://wa.me/" + numeroTel + "?text=" + URLEncoder.encode(msj, "UTF-8"));

                // Crea un Intent para abrir WhatsApp
                Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);

                // Inicia la actividad
                startActivity(sendIntent);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return true;
        }
        else if(id == R.id.nav_libroReclamaciones){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://librorec.comsanjuan.com/"));
            startActivity(intent);
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null){
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

}