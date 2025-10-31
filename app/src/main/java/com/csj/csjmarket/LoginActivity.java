package com.csj.csjmarket;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import com.csj.csjmarket.databinding.ActivityLoginBinding;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.gson.Gson;

import java.util.Objects;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private AlertDialog alertDialog;
    private ValidarCorreo validarCorreo;
    private CredentialManager credentialManager;
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(getBaseContext());

        binding.loginBtnGoogle.setOnClickListener(view -> signIn());

        // Solicitar permisos de notificaciones
        solicitarPermisoNotificaciones();
        
        // Si el usuario ya está autenticado, continuar con el flujo normal
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mostrarLoader();
            updateUI(currentUser);
        }
    }



    private void solicitarPermisoNotificaciones(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("PERMISO NOTIFICACIONES", "CONCEDIDO");
            } else {
                solicitarPermisoNotificaciones();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            mostrarLoader();
            updateUI(currentUser);
        }
    }

    private void signIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Couldn't retrieve user's credentials: " + e.getLocalizedMessage());
                    }
                }
        );
    }

    private void handleSignIn(Credential credential) {
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                Bundle credentialData = customCredential.getData();
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credentialData);

                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            } else {
                mostrarAlerta("Algo salió mal, inténtelo nuevamente.");
            }
        } else {
            mostrarAlerta("Ocurrio un error al validar los datos de Google.");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (user.isEmailVerified()) {
                acceder(user.getEmail(),
                        user.getDisplayName() == null || user.getDisplayName().isEmpty() ? user.getEmail() : user.getDisplayName(),
                        Objects.requireNonNull(user.getPhotoUrl()).toString());
            }
            else {
                if (alertDialog != null){
                    alertDialog.dismiss();
                }

                new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_baseline_info_24)
                .setTitle("Aún no haz verificado tu correo")
                .setMessage("Hemos enviado un mensaje de correo electrónico a " + user.getEmail() + " para asegurarnos de que eres el propietario. Por favor, comprueba su bandeja de entrada.")
                .setPositiveButton("Aceptar", (dialogInterface, i) -> mAuth.signOut())
                .setCancelable(false)
                .show();
            }
        }
    }

    private void irMain(String correo, String nombre, ValidarCorreo validarCorreo, String imagen) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("correo", correo);
        intent.putExtra("nombre", nombre);
        intent.putExtra("validarCorreo", validarCorreo);
        intent.putExtra("imagen", imagen);
        startActivity(intent);
        finish();
    }

    private void irEnlazar(String correo, String nombre) {
        Intent intent = new Intent(LoginActivity.this, EnlazarCliente.class);
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
        builder.setPositiveButton("Aceptar",null);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void acceder(String correo, String nombre, String imagen) {
        String url = getString(R.string.connection) + "/api/validarCorreos/nuevo?correo=" + correo;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
            validarCorreo = gson.fromJson(response.toString(), ValidarCorreo.class);
            if (alertDialog != null){
                alertDialog.dismiss();
            }

            if (validarCorreo != null) {
                if (validarCorreo.getId() != 0) {
                    irMain(correo, nombre, validarCorreo, imagen);
                } else {
                    irEnlazar(correo, nombre);
                }
            } else {
                mostrarAlerta("Respuesta inesperada del servidor");
            }
        }, error -> {
            if (alertDialog != null){
                alertDialog.dismiss();
            }
            if (error instanceof TimeoutError) {
                mostrarAlerta("Tiempo de espera agotado al validar correo.");
            } else if (error instanceof NoConnectionError) {
                mostrarAlerta("Sin conexión al validar correo.");
            } else {
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse!= null && networkResponse.data != null){
                    String errorMessage = new String(networkResponse.data);
                    mostrarAlerta(errorMessage);
                }
                else{
                    mostrarAlerta(error.toString());
                }
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        jsonObjectRequest.setShouldCache(false);
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }
}