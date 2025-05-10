package com.csj.csjmarket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.modelos.DireccionNueva;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.csj.csjmarket.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import android.Manifest;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private AlertDialog alertDialog;
    private String idPersona;
    private ActivityMapsBinding binding;
    double latitude = 0.0;
    double longitude = 0.0;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    boolean tienePermisos = false;
    //private Marker initialLocationMarker;
    private Marker currentLocationMarker;
    private Boolean locationPermissionGranted = true;

    private DireccionNueva direccionNueva = new DireccionNueva();

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.provBtnRegresar.setOnClickListener(view -> finish());

        idPersona = getIntent().getStringExtra("idPersona");

        /*
        mostrarLoader();
        verificarPermisos();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Obtener la ubicación actual
                    if (locationPermissionGranted){
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        new GetAddressTask().execute(latitude, longitude);
                        LatLng currentLocation = new LatLng(latitude, longitude);
                        // Mover la cámara a la ubicación actual y establecer un nivel de zoom
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                        currentLocationMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Aquí"));
                        locationPermissionGranted = false;
                    }
                }
            }
        };

        if (tienePermisos) {

        } else {
            verificarPermisos();
        }

        */
        binding.mapsBtnGuardar.setOnClickListener(view -> {
            direccionNueva.setIdPersona(Integer.parseInt(idPersona));
            direccionNueva.setDescripcion(binding.mapsTxtDireccion.getText().toString());
            direccionNueva.setLatitud(latitude);
            direccionNueva.setLongitud(longitude);

            crearDireccion();
        });

    }

    private class GetAddressTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... params) {
            Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
            latitude = params[0];
            longitude = params[1];

            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 2);

                if (addresses != null && addresses.size() > 0) {
                    alertDialog.dismiss();
                    Address address = addresses.get(1);
                    // Aquí obtienes la dirección en formato legible
                    String direccion = "";
                    if (address.getSubLocality() != null){
                        direccion += address.getSubLocality();
                    }
                    if (address.getThoroughfare() != null){
                        direccion += " " + address.getThoroughfare();
                    }
                    if (address.getSubThoroughfare() != null){
                        direccion += " # " + address.getSubThoroughfare();
                    }

                    return direccion;
                }
            } catch (IOException e) {
                Log.e("GetAddressTask", "Error obtaining address", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String address) {
            // Aquí puedes utilizar la dirección obtenida
            if (address != null) {
                binding.mapsTxtDireccion.setText(address);
            } else {
                Log.e("Address", "No address found");
            }
        }
    }

    private void verificarPermisos() {
        if (!tienePermisos) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            } else {
                tienePermisos = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El permiso de ubicación fue concedido, puedes obtener la ubicación aquí
                tienePermisos = true;
                //startLocationUpdates();
            } else {
                // El permiso de ubicación fue denegado, maneja esta situación apropiadamente
                tienePermisos = false;
                onBackPressed();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        this.mMap.setOnMapClickListener(this);
        this.mMap.setOnMapLongClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        //startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (fusedLocationClient != null) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
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

    private void crearDireccion() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/direccion";
        Gson gson = new Gson();
        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject(gson.toJson(direccionNueva));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody, response -> {
                onBackPressed();
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
            }){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
        } catch (Exception e) {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente. Si el problema persiste póngase en contacto con el desarrollador.\nDetalle: " + e.getMessage());
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        latitude = latLng.latitude;
        longitude = latLng.longitude;
        LatLng currentLocation = new LatLng(latitude, longitude);

        if (currentLocationMarker != null){
            currentLocationMarker.setPosition(currentLocation);
        }
        else {
            currentLocationMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Aquí"));
        }
        new GetAddressTask().execute(latitude, longitude);
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        latitude = latLng.latitude;
        longitude = latLng.longitude;
        LatLng currentLocation = new LatLng(latitude, longitude);

        if (currentLocationMarker != null){
            currentLocationMarker.setPosition(currentLocation);
        }
        else {
            currentLocationMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Aquí"));
        }
        new GetAddressTask().execute(latitude, longitude);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Llamar a startLocationUpdates solo cuando sea necesario
        if (locationPermissionGranted) {
            startLocationUpdates();
        }
    }
}