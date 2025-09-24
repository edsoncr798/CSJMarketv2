package com.csj.csjmarket;

import static com.behaviosec.pppppdd.TAG;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.csj.csjmarket.databinding.ActivityLoginBinding;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.gson.Gson;

import java.util.Objects;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private AlertDialog alertDialog;
    private ValidarCorreo validarCorreo;
    private CredentialManager credentialManager;

    private AppUpdateManager appUpdateManager;
    private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(getBaseContext());

        binding.loginBtnGoogle.setOnClickListener(view -> signIn());

        appUpdateManager = AppUpdateManagerFactory.create(this);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        Log.e("ERROR ACTUALIZACION: ", "La actualización FALLO: " + result.getResultCode());
                        // El usuario canceló o falló la actualización
                        // Para "IMMEDIATE" normalmente bloqueas el uso o vuelves a pedir la actualización
                        showForceUpdateDialog();
                    }
                }
        );

        checkForImmediateUpdate();
        solicitarPermisoNotificaciones();
    }

    private void checkForImmediateUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                    .build()
                    );
                } catch (Exception e) {
                    Log.e("ERROR", Objects.requireNonNull(e.getMessage()));
                }
            }
        });
    }

    private void showForceUpdateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Actualización obligatoria")
                .setMessage("Debes actualizar la aplicación para continuar usando el servicio.")
                .setCancelable(false) //
                .setPositiveButton("Actualizar", (dialog, which) -> checkForImmediateUpdate())
                .setNegativeButton("Salir", (dialog, which) -> finishAffinity())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            if (appUpdateInfo.updateAvailability()
                                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an in-app update is already running, resume the update.
                                appUpdateManager.startUpdateFlowForResult(
                                        appUpdateInfo,
                                        activityResultLauncher,
                                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
                            }
                        });
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
                // Permiso concedido
                Log.e("PERMISO NOTIFICACIONES: ", "CONCEDIDO");
            } else {
                // Permiso denegado
                solicitarPermisoNotificaciones();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            mostrarLoader();
            updateUI(currentUser);
        }
    }

    private void signIn() {
        // [START create_credential_manager_request]
        // Instantiate a Google sign-in request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();
        // [END create_credential_manager_request]

        // Launch Credential Manager UI
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

    // [START handle_sign_in]
    private void handleSignIn(Credential credential) {
        // Verificar si es un CustomCredential
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                // Crear Google ID Token
                Bundle credentialData = customCredential.getData();
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credentialData);

                // Iniciar sesión en Firebase con el token
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            } else {
                mostrarAlerta("Algo salió mal, inténtelo nuevamente.");
            }
        } else {
            mostrarAlerta("Ocurrio un error al validar los datos de Google.");
        }
    }
    // [END handle_sign_in]

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
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
        //mostrarLoader();
        //String url = getString(R.string.connection) + "/api/validarCorreos?correo=" + correo;
        String url = getString(R.string.connection) + "/api/validarCorreos/nuevo?correo=" + correo;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Gson gson = new Gson();
//            Type validartListType = new TypeToken<ValidarCorreo>() {
//            }.getType();
            validarCorreo = gson.fromJson(response.toString(), ValidarCorreo.class);
            if (alertDialog != null){
                alertDialog.dismiss();
            }

            if (validarCorreo != null) {  // validar que gson devolvió algo
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