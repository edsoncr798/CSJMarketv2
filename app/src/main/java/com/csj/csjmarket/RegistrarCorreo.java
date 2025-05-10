package com.csj.csjmarket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.databinding.ActivityLoginBinding;
import com.csj.csjmarket.databinding.ActivityRegistrarCorreoBinding;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class RegistrarCorreo extends AppCompatActivity {
    private ActivityRegistrarCorreoBinding binding;
    private FirebaseAuth mAuth;
    private AlertDialog alertDialog;
    private ValidarCorreo validarCorreo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_correo);

        binding = ActivityRegistrarCorreoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnRegisterMail.setOnClickListener(view -> {
            if (validarFormatoCorreo() && validarContraseña()){
                mostrarLoader();
                mAuth.createUserWithEmailAndPassword(binding.registerTxtCorreo.getText().toString().trim(),
                                binding.registerTxtPass.getText().toString().trim())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                try {
                                    alertDialog.dismiss();

                                    if (task.isSuccessful()) {
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        user.sendEmailVerification();
                                        updateUI(user);
                                    } else {
                                        String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                                        switch (errorCode) {
                                            case "ERROR_INVALID_CUSTOM_TOKEN":
                                                mostrarAlerta("El formato del token personalizado es incorrecto. Por favor revisa la documentación.");
                                                break;
                                            case "ERROR_CUSTOM_TOKEN_MISMATCH":
                                                mostrarAlerta("El token personalizado corresponde a una audiencia diferente.");
                                                break;
                                            case "ERROR_INVALID_CREDENTIAL":
                                                mostrarAlerta("La credencial de autenticación proporcionada tiene un formato incorrecto o ha caducado.");
                                                break;
                                            case "ERROR_INVALID_EMAIL":
                                                mostrarAlerta("La dirección de correo electrónico está mal formateada.");
                                                break;
                                            case "ERROR_WRONG_PASSWORD":
                                                mostrarAlerta("La contraseña no es válida o el usuario no tiene contraseña.");
                                                break;
                                            case "ERROR_USER_MISMATCH":
                                                mostrarAlerta("Las credenciales proporcionadas no corresponden al usuario que inició sesión anteriormente.");
                                                break;
                                            case "ERROR_REQUIRES_RECENT_LOGIN":
                                                mostrarAlerta("Esta operación es confidencial y requiere autenticación reciente. Inicie sesión nuevamente antes de volver a intentar esta solicitud.");
                                                break;
                                            case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                                                mostrarAlerta("Ya existe una cuenta con la misma dirección de correo electrónico pero con credenciales de inicio de sesión diferentes. Inicie sesión utilizando un proveedor asociado con esta dirección de correo electrónico.");
                                                break;
                                            case "ERROR_EMAIL_ALREADY_IN_USE":
                                                mostrarAlerta("La dirección de correo electrónico ya está en uso en otra cuenta.");
                                                break;
                                            case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                                                mostrarAlerta("Esta credencial ya está asociada con una cuenta de usuario diferente.");
                                                break;
                                            case "ERROR_USER_DISABLED":
                                                mostrarAlerta("La cuenta de usuario ha sido deshabilitada por un administrador.");
                                                break;
                                            case "ERROR_USER_TOKEN_EXPIRED":
                                                mostrarAlerta("La credencial del usuario ya no es válida. El usuario debe iniciar sesión nuevamente.");
                                                break;
                                            case "ERROR_USER_NOT_FOUND":
                                                mostrarAlerta("No existe ningún registro de usuario correspondiente a este identificador. Es posible que el usuario haya sido eliminado.");
                                                break;
                                            case "ERROR_INVALID_USER_TOKEN":
                                                mostrarAlerta("La credencial del usuario ya no es válida. El usuario debe iniciar sesión nuevamente.");
                                                break;
                                            case "ERROR_OPERATION_NOT_ALLOWED":
                                                mostrarAlerta("Esta operación no está permitida. Debes habilitar este servicio en la consola.");
                                                break;
                                            case "ERROR_WEAK_PASSWORD":
                                                mostrarAlerta("La contraseña proporcionada no es válida.");
                                                break;
                                            case "ERROR_MISSING_EMAIL":
                                                mostrarAlerta("Se debe proporcionar una dirección de correo electrónico.");
                                                break;
                                        }
                                    }
                                }catch (Exception ex){
                                    mostrarAlerta("Algo salio mal, por favor intenta nuevamente ahora o vuelve más tarde.");
                                }
                            }
                        });
            }
        });

        binding.registerTxtCorreo.addTextChangedListener(new validacionTextWatcher(binding.registerTxtCorreo));
        binding.registerTxtPass.addTextChangedListener(new validacionTextWatcher(binding.registerTxtPass));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(RegistrarCorreo.this, LoginActivity.class);
        startActivity(intent);
        finish();
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
                case R.id.register_txtCorreo:
                    validarFormatoCorreo();
                    break;
                case R.id.register_txtPass:
                    validarContraseña();
                    break;
            }
        }
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private boolean validarFormatoCorreo() {
        String direccionCorreo = binding.registerTxtCorreo.getText().toString().trim();
        if (direccionCorreo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(direccionCorreo).matches()) {
            binding.registerTilCorreo.setError("Por favor, ingrese un correo electrónico válido.");
            requestFocus(binding.registerTilCorreo);
            return false;
        }else {
            binding.registerTilCorreo.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarContraseña() {
        String contraseñaCorreo = binding.registerTxtPass.getText().toString().trim();
        if (contraseñaCorreo.isEmpty()) {
            binding.registerTilPass.setError("Este campo no puede estar vacío.");
            requestFocus(binding.registerTilPass);
            return false;
        }else if(contraseñaCorreo.length() < 6) {
            binding.registerTilPass.setError("La contraseña debe tener 6 dígitos como mínimo.");
            requestFocus(binding.registerTilPass);
            return false;
        }else {
            binding.registerTilPass.setErrorEnabled(false);
        }
        return true;
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (!user.isEmailVerified()){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(R.drawable.ic_baseline_info_24);
                builder.setTitle("Ya falta poco");
                builder.setMessage("Hemos enviado un mensaje de correo electrónico a " + user.getEmail() + " para asegurarnos de que eres el propietario. Por favor, compruebe su bandeja de entrada.");
                builder.setPositiveButton("Aceptar", (dialogInterface, i) -> {
                    mAuth.signOut();
                    onBackPressed();
                });
                builder.setCancelable(false);
                alertDialog = builder.create();
                alertDialog.show();
            }
            else{
                acceder(user.getEmail(), user.getDisplayName());
            }
        }
    }

    private void irMain(String correo, String nombre, ValidarCorreo validarCorreo) {
        Intent intent = new Intent(RegistrarCorreo.this, MainActivity.class);
        intent.putExtra("correo", correo);
        intent.putExtra("nombre", nombre);
        intent.putExtra("validarCorreo", validarCorreo);
        startActivity(intent);
        finish();
    }

    private void irEnlazar(String correo, String nombre) {
        Intent intent = new Intent(RegistrarCorreo.this, EnlazarCliente.class);
        intent.putExtra("correo", correo);
        intent.putExtra("nombre", nombre);
        startActivity(intent);
        finish();
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

    private void acceder(String correo, String nombre) {
        //mostrarLoader();
        String url = getString(R.string.connection) + "/api/validarCorreos?correo=" + correo;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            Type validartListType = new TypeToken<ValidarCorreo>() {
            }.getType();
            validarCorreo = gson.fromJson(response.toString(), validartListType);
            if (alertDialog != null){
                alertDialog.dismiss();
            }
            if (validarCorreo.getId() != 0) {
                irMain(correo, nombre, validarCorreo);
            } else {
                irEnlazar(correo, nombre);
            }
        }, error -> {
            if (alertDialog != null){
                alertDialog.dismiss();
            }
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse!= null){
                String errorMessage = new String(networkResponse.data);
                mostrarAlerta(errorMessage);
            }
            else{
                mostrarAlerta(error.toString());
            }
        });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }
}