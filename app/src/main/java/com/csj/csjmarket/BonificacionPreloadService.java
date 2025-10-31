package com.csj.csjmarket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.csj.csjmarket.cache.BonificacionCacheManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BonificacionPreloadService extends Service {
    private static final String TAG = "BonificacionPreload";
    private ExecutorService executor;
    private BonificacionCacheManager cacheManager;
    private static final int PRELOAD_LIMIT = 80;
    private static final int MAX_RETRIES = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        cacheManager = new BonificacionCacheManager(getApplicationContext());
        executor.submit(this::startPreload);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { executor.shutdownNow(); } catch (Exception ignore) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startPreload() {
        try {
            // 1) Build candidate list: recently viewed/popular + current catalog first page
            Deque<Integer> queue = new ArrayDeque<>();

            // Add from shared popular registry in Application if exists
            try {
                Set<Integer> popular = CsjMarketPopularRegistry.get();
                for (Integer id : popular) {
                    if (id != null) queue.add(id);
                    if (queue.size() >= PRELOAD_LIMIT) break;
                }
            } catch (Exception ignore) {}

            // Fallback: fetch first page of catalog to seed queue
            if (queue.isEmpty()) {
                List<Integer> catalogIds = fetchFirstPageProductIds();
                for (Integer id : catalogIds) {
                    queue.add(id);
                    if (queue.size() >= PRELOAD_LIMIT) break;
                }
            }

            // 2) Process queue: request bonus endpoint and cache response
            while (!queue.isEmpty()) {
                Integer idProducto = queue.poll();
                if (idProducto == null) continue;
                if (cacheManager.getBonificacion(idProducto) != null) {
                    continue; // already cached within TTL
                }
                preloadBonificacion(idProducto, 0);
                // small delay to avoid hammering server
                try { Thread.sleep(150); } catch (InterruptedException ignore) {}
            }
        } catch (Exception e) {
            Log.e(TAG, "Preload error: ", e);
        }
    }

    private List<Integer> fetchFirstPageProductIds() {
        List<Integer> ids = new ArrayList<>();
        try {
            String base = getString(R.string.connection);
            String url = base + "/api/productos/v2/listar";
            final Object lock = new Object();

            com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(Request.Method.GET, url, responseStr -> {
                try {
                    List<Integer> temp = new ArrayList<>();
                    if (responseStr != null && responseStr.trim().startsWith("{")) {
                        JSONObject obj = new JSONObject(responseStr);
                        JSONArray arr = obj.optJSONArray("productos");
                        if (arr == null) arr = obj.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;
                                int id = o.optInt("id", o.optInt("Id", 0));
                                if (id > 0) temp.add(id);
                            }
                        }
                    } else if (responseStr != null && responseStr.trim().startsWith("[")) {
                        JSONArray arr = new JSONArray(responseStr);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;
                            int id = o.optInt("id", o.optInt("Id", 0));
                            if (id > 0) temp.add(id);
                        }
                    }
                    for (int id : temp) {
                        ids.add(id);
                        if (ids.size() >= PRELOAD_LIMIT) break;
                    }
                } catch (Exception ignore) {}
                synchronized (lock) { lock.notify(); }
            }, error -> {
                synchronized (lock) { lock.notify(); }
            });
            req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            req.setShouldCache(false);
            Volley.newRequestQueue(getApplicationContext()).add(req);
            synchronized (lock) { lock.wait(12000); }
        } catch (Exception ignore) {}
        return ids;
    }

    private void preloadBonificacion(int idProducto, int attempt) {
        String url = getString(R.string.connection) + "/api/productos/bonificacion/v2/" + idProducto;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                cacheManager.saveBonificacion(idProducto, response);
            } catch (Exception e) {
                Log.e(TAG, "Cache save error", e);
            }
        }, error -> {
            if (attempt < MAX_RETRIES) {
                int next = attempt + 1;
                long delay = TimeUnit.SECONDS.toMillis(2L * next);
                executor.submit(() -> {
                    try { Thread.sleep(delay); } catch (InterruptedException ignore) {}
                    preloadBonificacion(idProducto, next);
                });
            } else {
                NetworkResponse networkResponse = error.networkResponse;
                String emsg;
                if (error instanceof TimeoutError) {
                    emsg = "timeout";
                } else if (error instanceof NoConnectionError) {
                    emsg = "no_connection";
                } else if (networkResponse != null && networkResponse.data != null) {
                    emsg = new String(networkResponse.data);
                } else {
                    emsg = String.valueOf(error);
                }
                Log.w(TAG, "Fail preload id=" + idProducto + ": " + emsg);
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(12000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        req.setShouldCache(false);
        Volley.newRequestQueue(getApplicationContext()).add(req);
    }
}