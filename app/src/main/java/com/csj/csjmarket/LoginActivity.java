package com.csj.csjmarket;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.Manifest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.databinding.ActivityLoginBinding;
import com.csj.csjmarket.modelos.ValidarCorreo;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private AlertDialog alertDialog;
    private ValidarCorreo validarCorreo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPreferences = this.getSharedPreferences("carritoInfo", MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.loginBtnGoogle.setOnClickListener(view -> {
            mostrarLoader();
            signIn();
        });

        solicitarPermisoNotificaciones();

        /*binding.txtCrearCuenta.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegistrarCorreo.class);
            startActivity(intent);
            finish();
        });

        binding.loginBtnMail.setOnClickListener(view -> {
            if (validarFormatoCorreo() && validarContraseña()){
                mostrarLoader();
                mAuth.signInWithEmailAndPassword(binding.loginTxtCorreo.getText().toString().trim(),
                                binding.loginTxtPass.getText().toString().trim())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                try {
                                    if (task.isSuccessful()) {
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        updateUI(user);
                                    } else {
                                        alertDialog.dismiss();
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

        binding.txtRecuperarCuenta.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RecuperarCorreo.class);
            startActivity(intent);
            finish();
        });

        binding.loginTxtCorreo.addTextChangedListener(new validacionTextWatcher(binding.loginTxtCorreo));
        binding.loginTxtPass.addTextChangedListener(new validacionTextWatcher(binding.loginTxtPass));
*/
    }

    private void solicitarPermisoNotificaciones(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1);
            } else {
                // Permiso ya otorgado, puedes mostrar notificaciones
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
            } else {
                // Permiso denegado
                solicitarPermisoNotificaciones();
            }
        }
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
                /*case R.id.login_txtCorreo:
                    validarFormatoCorreo();
                    break;
                case R.id.login_txtPass:
                    validarContraseña();
                    break;*/
            }
        }
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    /*private boolean validarFormatoCorreo() {
        String direccionCorreo = binding.loginTxtCorreo.getText().toString().trim();
        if (direccionCorreo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(direccionCorreo).matches()) {
            binding.loginTilCorreo.setError("Por favor, ingrese un correo electrónico válido.");
            requestFocus(binding.loginTilCorreo);
            return false;
        }else {
            binding.loginTilCorreo.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validarContraseña() {
        String contraseñaCorreo = binding.loginTxtPass.getText().toString().trim();
        if (contraseñaCorreo.isEmpty()) {
            binding.loginTilPass.setError("Por favor, ingrese su contraseña.");
            requestFocus(binding.loginTilPass);
            return false;
        }else {
            binding.loginTilPass.setErrorEnabled(false);
        }
        return true;
    }*/

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
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(new Intent(signInIntent));
    }

    ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK){
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    // Google Sign In failed, update UI appropriately
                    String a = e.getMessage();
                    a.length();
                }
            }
            else
            {
                alertDialog.dismiss();
                mostrarAlerta("Ocurrio un error al validar los datos de google.");
            }
        }
    });

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (user.isEmailVerified()) {
                acceder(user.getEmail(),
                        user.getDisplayName() == null || user.getDisplayName() == "" ? user.getEmail() : user.getDisplayName(),
                        user.getPhotoUrl().toString());
            }
            else {
                if (alertDialog != null){
                    alertDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(R.drawable.ic_baseline_info_24);
                builder.setTitle("Aún no haz verificado tu correo");
                builder.setMessage("Hemos enviado un mensaje de correo electrónico a " + user.getEmail() + " para asegurarnos de que eres el propietario. Por favor, comprueba su bandeja de entrada.");
                builder.setPositiveButton("Aceptar", (dialogInterface, i) -> {
                    mAuth.signOut();
                });
                builder.setCancelable(false);
                alertDialog = builder.create();
                alertDialog.show();
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
            Type validartListType = new TypeToken<ValidarCorreo>() {
            }.getType();
            validarCorreo = gson.fromJson(response.toString(), validartListType);
            if (alertDialog != null){
                alertDialog.dismiss();
            }
            if (validarCorreo.getId() != 0) {
                irMain(correo, nombre, validarCorreo, imagen);
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