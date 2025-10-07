package com.csj.csjmarket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int MAX_UPDATE_RETRIES = 3;
    private static final long SPLASH_DELAY_MS = 2000; // 2 segundos mínimo de splash
    // MODO DESARROLLO: poner en false antes de subir a producción
    private static final boolean DEBUG_MODE = true;

    private AppUpdateManager appUpdateManager;
    private ActivityResultLauncher<IntentSenderRequest> updateLauncher;
    private int updateRetryCount = 0;
    // private TextView loadingText;
    private Handler splashHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG, "=== SPLASH INICIADO ===");
        
        // Inicializar vistas y animaciones
        splashHandler = new Handler();
        android.view.View logo = findViewById(R.id.logo_image);
        android.view.View ring = findViewById(R.id.pulse_ring);
        android.view.View appName = findViewById(R.id.app_name_text);
        android.view.View welcome = findViewById(R.id.welcome_text);

        android.view.animation.Animation logoAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.splash_logo_in);
        logo.startAnimation(logoAnim);

        // Fade-in para "Bienvenido a"
        android.view.animation.Animation welcomeAnim = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        if (welcome != null) {
            welcome.startAnimation(welcomeAnim);
        }

        // Fade-in para "CSJ Market" con leve retraso para encadenar
        android.view.animation.Animation textAnim = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        textAnim.setStartOffset(150);
        if (appName != null) {
            appName.startAnimation(textAnim);
        }

        // Versión dinámica desde BuildConfig
        android.view.View versionTextView = findViewById(R.id.version_text);
        android.view.animation.Animation animationVersion = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        if (versionTextView instanceof TextView) {
            ((TextView) versionTextView).setText("v" + BuildConfig.VERSION_NAME);
            versionTextView.startAnimation(animationVersion);
        }

        android.view.animation.Animation pulseAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse);
        ring.setAlpha(1f);
        ring.startAnimation(pulseAnim);

        // Inicializar AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this);

        // Configurar el launcher para el resultado de la actualización
        updateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "Actualización aceptada por el usuario");
                        // La actualización fue aceptada, esperar a que se complete
                    } else {
                        Log.e(TAG, "Actualización rechazada o fallida. Código: " + result.getResultCode());
                        handleUpdateFailure("La actualización es obligatoria para continuar");
                    }
                }
        );

        // Iniciar el proceso de verificación
        startUpdateVerification();
    }

    private void startUpdateVerification() {
        Log.d(TAG, "Iniciando verificación de actualizaciones...");
        // oculto: verificación de actualizaciones

        // Verificar actualizaciones disponibles
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            Log.d(TAG, "Información de actualización recibida");
            handleUpdateInfo(appUpdateInfo);
        });

        appUpdateInfoTask.addOnFailureListener(e -> {
            Log.e(TAG, "Error al obtener información de actualización", e);
            handleUpdateError("Error al verificar actualizaciones");
        });
    }

    private void handleUpdateInfo(AppUpdateInfo appUpdateInfo) {
        Log.d(TAG, "Disponibilidad de actualización: " + appUpdateInfo.updateAvailability());
        Log.d(TAG, "Tipo de actualización permitido: " + appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE));

        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            
            Log.d(TAG, "Actualización disponible y requerida");
            // oculto: texto "Actualización disponible..."
            
            // Intentar iniciar la actualización
            try {
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar el flujo de actualización", e);
                showForceUpdateDialog();
            }
            
        } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            
            Log.d(TAG, "Actualización en progreso");
            // oculto: texto "Actualización en progreso..."
            // Si ya hay una actualización en progreso, continuar con ella
            continueUpdateIfInProgress(appUpdateInfo);
            
        } else {
            Log.d(TAG, "No hay actualizaciones disponibles o requeridas");
            proceedToLogin();
        }
    }

    private void continueUpdateIfInProgress(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error al continuar actualización en progreso", e);
            handleUpdateError("Error al continuar actualización");
        }
    }

    private void showForceUpdateDialog() {
        Log.d(TAG, "Mostrando diálogo de actualización forzada");
        // oculto: texto "Actualización requerida"
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Actualización Requerida")
                .setMessage("Para continuar usando CSJ Market, debes actualizar a la última versión.")
                .setCancelable(false)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    Log.d(TAG, "Usuario aceptó actualizar desde diálogo");
                    redirectToPlayStore();
                })
                .setNegativeButton("Salir", (dialog, which) -> {
                    Log.d(TAG, "Usuario rechazó actualizar");
                    finishAffinity(); // Cerrar completamente la app
                })
                .show();
    }

    private void redirectToPlayStore() {
        try {
            String packageName = getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
            finishAffinity(); // Cerrar app después de redirigir
        } catch (Exception e) {
            Log.e(TAG, "Error al redirigir a Play Store", e);
            // Sin mostrar mensaje al usuario para mantener splash limpio
            finishAffinity();
        }
    }

    private void handleUpdateFailure(String message) {
        updateRetryCount++;
        Log.d(TAG, "Manejando fallo de actualización. Intento: " + updateRetryCount);
        
        if (updateRetryCount < MAX_UPDATE_RETRIES) {
            Log.d(TAG, "Reintentando verificación de actualización");
            // oculto: texto "Reintentando..."
            
            // Esperar un momento antes de reintentar
            splashHandler.postDelayed(() -> {
                startUpdateVerification();
            }, 2000);
            
        } else {
            Log.e(TAG, "Máximo de reintentos alcanzado");
            showForceUpdateDialog();
        }
    }

    private void handleUpdateError(String message) {
        updateRetryCount++;
        Log.e(TAG, "Manejando error de actualización. Intento: " + updateRetryCount);
        
        if (updateRetryCount < MAX_UPDATE_RETRIES) {
            Log.d(TAG, "Reintentando después de error");
            // oculto: texto "Reintentando..."
            
            splashHandler.postDelayed(() -> {
                startUpdateVerification();
            }, 2000);
            
        } else {
            Log.e(TAG, "Máximo de reintentos alcanzado por error");
            // Si no podemos verificar actualizaciones, permitir continuar (fallback seguro)
            Toast.makeText(this, "No se pudo verificar actualizaciones", Toast.LENGTH_SHORT).show();
            proceedToLogin();
        }
    }

    private void proceedToLogin() {
        Log.d(TAG, "Procediendo al login - sin actualizaciones requeridas");
        // oculto: texto "Iniciando..."
        
        // Asegurar tiempo mínimo de splash para mejor UX
        splashHandler.postDelayed(() -> {
            if (DEBUG_MODE) {
                Log.d(TAG, "DEBUG_MODE activo: Navegando directo a MainActivity con datos dummy");
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("nombre", "Usuario Debug");
                intent.putExtra("correo", "debug@csj.com");
                intent.putExtra("imagen", "");
                intent.putExtra("validarCorreo", new com.csj.csjmarket.modelos.ValidarCorreo());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error al iniciar MainActivity en modo debug", e);
                    Toast.makeText(SplashActivity.this, "Error al iniciar vista principal en modo debug", Toast.LENGTH_SHORT).show();
                    Intent fallback = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(fallback);
                }
            } else {
                Log.d(TAG, "Navegando a LoginActivity");
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            
            // Transición suave
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Cerrar splash
            finish();
        }, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
        Log.d(TAG, "=== SPLASH DESTRUIDO ===");
    }

    @Override
    public void onBackPressed() {
        // Prevenir que el usuario cierre el splash con el botón atrás
        super.onBackPressed();
        Log.d(TAG, "Botón atrás presionado en splash - ignorado");
        // No hacer nada
    }
}