package com.csj.csjmarket;

import android.app.Application;
import android.content.Intent;

public class CsjMarketApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // BonificacionPreloadService desactivado para evitar lentitud en arranque
    }
}