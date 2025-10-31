package com.csj.csjmarket;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "actualizaciones_channel";
    private static final String CHANNEL_NAME = "Actualizaciones de App";
    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.csj.csjmarket";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Verificar si el mensaje contiene datos
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Data Payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage);
        }

        // Verificar si el mensaje contiene notificación
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage);
        }
    }

    private void handleDataMessage(RemoteMessage remoteMessage) {
        try {
            String tipo = remoteMessage.getData().get("tipo");
            String titulo = remoteMessage.getData().get("titulo");
            String mensaje = remoteMessage.getData().get("mensaje");

            if ("actualizacion".equals(tipo)) {
                mostrarNotificacionActualizacion(titulo, mensaje);
            } else {
                // Para otros tipos de notificaciones
                mostrarNotificacionGenerica(titulo, mensaje);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar mensaje de datos: " + e.getMessage());
        }
    }

    private void handleNotificationMessage(RemoteMessage remoteMessage) {
        String titulo = remoteMessage.getNotification().getTitle();
        String mensaje = remoteMessage.getNotification().getBody();

        // Si la notificación contiene "actualización" en el título o mensaje
        if ((titulo != null && titulo.toLowerCase().contains("actualización")) ||
            (mensaje != null && mensaje.toLowerCase().contains("actualización"))) {
            mostrarNotificacionActualizacion(titulo, mensaje);
        } else {
            mostrarNotificacionGenerica(titulo, mensaje);
        }
    }

    private void mostrarNotificacionActualizacion(String titulo, String mensaje) {
        // Crear canal de notificación (necesario para Android O y superior)
        crearCanalNotificacion();

        // Crear intent para abrir Play Store usando market:// con fallback a https
        Intent intent;
        try {
            String packageName = getPackageName();
            Uri marketUri = Uri.parse("market://details?id=" + packageName);
            intent = new Intent(Intent.ACTION_VIEW, marketUri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Fallback a https si no hay handler para market://
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
        } catch (Exception e) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construir la notificación
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download) // Icono de descarga
                .setContentTitle(titulo != null ? titulo : "¡Nueva actualización disponible!")
                .setContentText(mensaje != null ? mensaje : "Toca para actualizar CSJ Market")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(mensaje != null ? mensaje : "Toca para actualizar CSJ Market con las últimas mejoras y funciones."));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1001, notificationBuilder.build());
    }

    private void mostrarNotificacionGenerica(String titulo, String mensaje) {
        crearCanalNotificacion();

        // Intent para abrir la app normalmente
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo != null ? titulo : "CSJ Market")
                .setContentText(mensaje != null ? mensaje : "Tienes una nueva notificación")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1002, notificationBuilder.build());
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones para actualizaciones de la aplicación");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM: " + token);
        // Aquí puedes enviar el token a tu servidor si es necesario
        enviarTokenAlServidor(token);
    }

    private void enviarTokenAlServidor(String token) {
        // Implementar lógica para enviar el token a tu backend
        Log.d(TAG, "Token enviado al servidor: " + token);
    }
}
