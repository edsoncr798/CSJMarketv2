package com.csj.csjmarket;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.databinding.ActivityEnlazarClienteBinding;
import com.csj.csjmarket.modelos.ClienteCreacion;
import com.csj.csjmarket.modelos.Distrito;
import com.csj.csjmarket.modelos.Provincia;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.csj.csjmarket.modelos.dni;
import com.csj.csjmarket.modelos.ruc;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnlazarCliente extends AppCompatActivity {

    private AlertDialog alertDialog;
    private ActivityEnlazarClienteBinding binding;
    private ArrayList<Provincia> provincias = new ArrayList<>();
    private ArrayList<Distrito> distritos = new ArrayList<>();
    private ValidarCorreo validarCorreo;
    private ClienteCreacion clienteCreacion;
    private String nombreUsuario, correoUsuario;
    private boolean provinciaOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enlazar_cliente);

        binding = ActivityEnlazarClienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        nombreUsuario = getIntent().getStringExtra("nombre");
        correoUsuario = getIntent().getStringExtra("correo");

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.AllCaps();
        binding.enlazarTxtDireccion.setFilters(filters);

        binding.enlazarTxtCorreo.setText(getIntent().getStringExtra("correo"));

        binding.enlazarSpnTipoDocumento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                validarDocumento();
                if (binding.enlazarSpnTipoDocumento.getSelectedItemPosition() == 0) {
                    binding.enlazarContenedorRazonSocial.setVisibility(View.GONE);
                    binding.enlazarContenedorNombreCliente.setVisibility(View.VISIBLE);
                } else {
                    binding.enlazarContenedorRazonSocial.setVisibility(View.VISIBLE);
                    binding.enlazarContenedorNombreCliente.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.direccionSpnProvincia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (provinciaOk){
                    listarDistritos(provincias.get(i).getPkid());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.enlazarBtnContinuar.setOnClickListener(view -> {
            String nombre = "";
            String prefijo = "";
            String nombres = "";
            String apellidoPaterno = "";
            String apellidoMaterno = "";
            boolean clienteValido = true;

            if (binding.enlazarSpnTipoDocumento.getSelectedItemPosition() == 0){
                if (validarDocumento() &&
                        validarApellidoPaterno() &&
                        validarApellidoMaterno() &&
                        validarNombres() &&
                        validarDireccion() &&
                        validarTelefono()) {
                    prefijo = binding.enlazarTxtApellidoPaterno.getText().toString().toUpperCase().trim().substring(0, 1);
                    nombre = binding.enlazarTxtApellidoPaterno.getText().toString().toUpperCase().trim() + " " +
                            binding.enlazarTxtApellidoMaterno.getText().toString().toUpperCase().trim() + " " +
                            binding.enlazarTxtNombres.getText().toString().toUpperCase();
                    nombres = binding.enlazarTxtNombres.getText().toString().toUpperCase();
                    apellidoPaterno = binding.enlazarTxtApellidoPaterno.getText().toString().toUpperCase().trim();
                    apellidoMaterno = binding.enlazarTxtApellidoMaterno.getText().toString().toUpperCase().trim();
                }
                else{
                    clienteValido = false;
                }
            }
            else if (validarDocumento() &&
                    validarNombre() &&
                    validarDireccion() &&
                    validarTelefono() ){
                nombre = binding.enlazarTxtNombre.getText().toString().toUpperCase();
                prefijo = nombre.substring(0, 1);
            }
            else{
                clienteValido = false;
            }

            if (clienteValido){
                clienteCreacion = new ClienteCreacion(
                        prefijo
                        , nombre
                        , apellidoPaterno
                        , apellidoMaterno
                        , nombres
                        , binding.enlazarTxtCorreo.getText().toString().trim()
                        , binding.enlazarTxtNumeroDocumento.getText().toString().trim()
                        , binding.enlazarTxtDireccion.getText().toString()
                        , distritos.get(binding.direccionSpnDistrito.getSelectedItemPosition()).getUbigeo()
                        , binding.enlazarTxtTelefono.getText().toString().trim());

                crearCliente();
            }

        });

        //listarProvincias();

        listarDistritos(138);
        binding.enlazarTxtNumeroDocumento.addTextChangedListener(new validacionTextWatcher(binding.enlazarTxtNumeroDocumento));
        binding.enlazarTxtDireccion.addTextChangedListener(new validacionTextWatcher(binding.enlazarTxtDireccion));
        binding.enlazarTxtTelefono.addTextChangedListener(new validacionTextWatcher(binding.enlazarTxtTelefono));
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
                case R.id.enlazar_txtNumeroDocumento:
                    boolean documentoValido = validarDocumento();
                    if (documentoValido) {
                        //buscarDocumento(binding.enlazarTxtNumeroDocumento.getText().toString());
                    }
                    break;
                case R.id.enlazar_txtDireccion:
                    validarDireccion();
                    break;
                case R.id.enlazar_txtTelefono:
                    validarTelefono();
                    break;
            }
        }
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private boolean validarDireccion() {
        if (binding.enlazarTxtDireccion.getText().toString().trim().isEmpty()) {
            binding.enlazarTilDireccion.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilDireccion);
            return false;
        } else {
            binding.enlazarTilDireccion.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarApellidoPaterno() {
        if (binding.enlazarTxtApellidoPaterno.getText().toString().trim().isEmpty()) {
            binding.enlazarTilApellidoPaterno.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilApellidoPaterno);
            return false;
        } else {
            binding.enlazarTilApellidoPaterno.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarApellidoMaterno() {
        if (binding.enlazarTxtApellidoMaterno.getText().toString().trim().isEmpty()) {
            binding.enlazarTilApellidoMaterno.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilApellidoMaterno);
            return false;
        } else {
            binding.enlazarTilApellidoMaterno.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarNombre() {
        if (binding.enlazarTxtNombre.getText().toString().trim().isEmpty()) {
            binding.enlazarTilNombre.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilNombre);
            return false;
        } else {
            binding.enlazarTilNombre.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarNombres() {
        if (binding.enlazarTxtNombres.getText().toString().trim().isEmpty()) {
            binding.enlazarTilNombres.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilNombres);
            return false;
        } else {
            binding.enlazarTilNombres.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarTelefono() {
        if (binding.enlazarTxtTelefono.getText().toString().trim().isEmpty()) {
            binding.enlazarTilNumeroTelefono.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilNumeroTelefono);
            return false;
        } if (binding.enlazarTxtTelefono.getText().toString().trim().length() != 9) {
            binding.enlazarTilNumeroTelefono.setError("Debe de tener 9 dígitos.");
            requestFocus(binding.enlazarTilNumeroTelefono);
            return false;
        }else {
            binding.enlazarTilNumeroTelefono.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarDocumento() {
        if (binding.enlazarTxtNumeroDocumento.getText().toString().trim().isEmpty()) {
            binding.enlazarTilNumeroDocumento.setError("Este campo no puede estar vacío.");
            requestFocus(binding.enlazarTilNumeroDocumento);
            return false;
        } else if (binding.enlazarSpnTipoDocumento.getSelectedItemPosition() == 0 && binding.enlazarTxtNumeroDocumento.getText().length() != 8) {
            binding.enlazarTilNumeroDocumento.setError("El número de DNI debe tener 8 digitos");
            binding.enlazarTxtApellidoPaterno.setText("");
            binding.enlazarTxtApellidoMaterno.setText("");
            binding.enlazarTxtNombres.setText("");
            requestFocus(binding.enlazarTilNumeroDocumento);
            return false;
        } else if (binding.enlazarSpnTipoDocumento.getSelectedItemPosition() == 1 && binding.enlazarTxtNumeroDocumento.getText().length() != 11) {
            binding.enlazarTilNumeroDocumento.setError("El número de RUC debe tener 11 digitos");
            binding.enlazarTxtNombre.setText("");
            requestFocus(binding.enlazarTilNumeroDocumento);
            return false;
        } else {
            binding.enlazarTilNumeroDocumento.setErrorEnabled(false);
        }
        return true;
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

    private void buscarDocumento(String numeroDucumento) {
        mostrarLoader();
        String url = "";
        if (binding.enlazarSpnTipoDocumento.getSelectedItemPosition() == 0) {
            url = "https://api.apis.net.pe/v1/dni?numero=" + numeroDucumento;
        } else {
            url = "https://api.apis.net.pe/v1/ruc?numero=" + numeroDucumento;
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            if (binding.enlazarSpnTipoDocumento.getSelectedItemPosition() == 0) {
                dni fromJson = gson.fromJson(response.toString(), dni.class);
                binding.enlazarTxtApellidoPaterno.setText(fromJson.getApellidoPaterno());
                binding.enlazarTxtApellidoMaterno.setText(fromJson.getApellidoMaterno());
                binding.enlazarTxtNombres.setText(fromJson.getNombres());
            } else {
                ruc fromJson = gson.fromJson(response.toString(), ruc.class);
                binding.enlazarTxtNombre.setText(fromJson.getNombre());
            }
            alertDialog.dismiss();
        }, error -> {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + error.toString());
        });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void listarProvincias() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/provincia";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            ArrayList<String> nombresProvincias = new ArrayList<>();
            Type provinciaListType = new TypeToken<List<Provincia>>() {
            }.getType();
            provincias = gson.fromJson(response.toString(), provinciaListType);
            for (Provincia tmp : provincias) {
                nombresProvincias.add(tmp.getDescripcion());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, nombresProvincias);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            provinciaOk = true;

            binding.direccionSpnProvincia.setAdapter(adapter);

            listarDistritos(provincias.get(0).getPkid());

        }, error -> {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + error.toString());
        });
        Volley.newRequestQueue(getApplicationContext()).add(jsonArrayRequest);
    }

    private void listarDistritos(int idProvincia) {
        String url = getString(R.string.connection) + "/api/distrito?idProvincia=" + idProvincia;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            ArrayList<String> nombresDistritos = new ArrayList<>();
            Type distritoListType = new TypeToken<List<Distrito>>() {
            }.getType();
            distritos = gson.fromJson(response.toString(), distritoListType);
            for (Distrito tmp : distritos) {
                nombresDistritos.add(tmp.getDescripcion());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, nombresDistritos);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            binding.direccionSpnDistrito.setAdapter(adapter);

            if (alertDialog != null) {
                alertDialog.dismiss();
            }
        }, error -> {
            alertDialog.dismiss();
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + error.toString());
        });
        Volley.newRequestQueue(getApplicationContext()).add(jsonArrayRequest);
    }

    private void crearCliente() {
        mostrarLoader();
        String url = getString(R.string.connection) + "/api/cliente";
        Gson gson = new Gson();
        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject(gson.toJson(clienteCreacion));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody, response -> {
                acceder(correoUsuario, nombreUsuario);
            }, error -> {
                alertDialog.dismiss();
                mostrarAlerta("Error al guardar el nuevo cliente.\nDetalle: " + error.toString());
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
            mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.\nDetalle: " + e.getMessage());
        }
    }

    private void acceder(String correo, String nombre) {
        String url = getString(R.string.connection) + "/api/validarCorreos/nuevo/?correo=" + correo;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            Type validartListType = new TypeToken<ValidarCorreo>() {
            }.getType();
            validarCorreo = gson.fromJson(response.toString(), validartListType);
            alertDialog.dismiss();
            if (validarCorreo.getId() != 0) {
                irMain(correo, nombre, validarCorreo);
            } else {
                mostrarAlerta("Algo salió mal, por favor inténtelo nuevamente.");
            }
        }, error -> {
            alertDialog.dismiss();
            mostrarAlerta(error.toString());
        });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void irMain(String correo, String nombre, ValidarCorreo validarCorreo) {
        Intent intent = new Intent(EnlazarCliente.this, MainActivity.class);
        intent.putExtra("correo", correo);
        intent.putExtra("nombre", nombre);
        intent.putExtra("validarCorreo", validarCorreo);
        startActivity(intent);
        finish();
    }
}