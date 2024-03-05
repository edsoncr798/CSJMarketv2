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

import com.csj.csjmarket.databinding.ActivityLoginBinding;
import com.csj.csjmarket.databinding.ActivityRecuperarCorreoBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class RecuperarCorreo extends AppCompatActivity {

    private ActivityRecuperarCorreoBinding binding;
    private FirebaseAuth mAuth;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_correo);

        binding = ActivityRecuperarCorreoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnRecoveryMail.setOnClickListener(view -> {
            if (validarFormatoCorreo()){
                mostrarLoader();
                mAuth.sendPasswordResetEmail(binding.recoveryTxtCorreo.getText().toString()).addOnCompleteListener(task -> {
                    if (alertDialog != null){
                        alertDialog.dismiss();
                    }
                    if (task.isSuccessful()){
                        mostrarMensaje();
                    }
                    else{
                        mostrarAlerta("No se pudo enviar el correo electrónico para restablecer su contraseña.");
                    }
                });
            }
        });

        binding.recoveryTxtCorreo.addTextChangedListener(new validacionTextWatcher(binding.recoveryTxtCorreo));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(RecuperarCorreo.this, LoginActivity.class);
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

    private void mostrarMensaje(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle("Ya falta poco");
        builder.setMessage("Hemos enviado un mensaje de correo electrónico a " + binding.recoveryTxtCorreo.getText().toString() + " para restablecer su contraseña. Por favor, compruebe su bandeja de entrada.");
        builder.setPositiveButton("Aceptar", (dialogInterface, i) -> {
            onBackPressed();
        });
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
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
                case R.id.recovery_txtCorreo:
                    validarFormatoCorreo();
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
        String direccionCorreo = binding.recoveryTxtCorreo.getText().toString().trim();
        if (direccionCorreo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(direccionCorreo).matches()) {
            binding.recoveryTilCorreo.setError("Por favor, ingrese un correo electrónico válido.");
            requestFocus(binding.recoveryTilCorreo);
            return false;
        }else {
            binding.recoveryTilCorreo.setErrorEnabled(false);
        }
        return true;
    }
}