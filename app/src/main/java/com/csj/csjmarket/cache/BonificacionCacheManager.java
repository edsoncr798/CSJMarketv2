package com.csj.csjmarket.cache;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BonificacionCacheManager {

    private static final String PREF_NAME = "bonificacion_cache";
    private static final String KEY_CACHE = "bonificacion_data";
    private static final String KEY_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(2); // 2 horas

    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private Map<Integer, BonificacionData> cache;
    private long lastUpdate;

    public BonificacionCacheManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadCache();
    }

    private void loadCache() {
        String json = sharedPreferences.getString(KEY_CACHE, "{}");
        Type type = new TypeToken<HashMap<Integer, BonificacionData>>(){}.getType();
        cache = gson.fromJson(json, type);
        if (cache == null) {
            cache = new HashMap<>();
        }
        lastUpdate = sharedPreferences.getLong(KEY_TIMESTAMP, 0);
    }

    private void saveCache() {
        String json = gson.toJson(cache);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CACHE, json);
        editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public BonificacionData getBonificacion(int idProducto) {
        BonificacionData data = cache.get(idProducto);
        if (data != null && !isCacheExpired()) {
            return data;
        }
        return null;
    }

    public void saveBonificacion(int idProducto, JSONObject response) {
        BonificacionData data = BonificacionData.fromJson(response);
        cache.put(idProducto, data);
        saveCache();
    }

    private boolean isCacheExpired() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdate) > CACHE_DURATION;
    }

    public void forceRefresh() {
        lastUpdate = 0;
    }

    public void clearCache() {
        cache.clear();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CACHE);
        editor.remove(KEY_TIMESTAMP);
        editor.apply();
    }

    public boolean hasCache() {
        return !cache.isEmpty();
    }

    public static class BonificacionData {
        public boolean tieneBonificacion;
        public String mensajePromocional;
        public String requisitoMinimo;
        public String tipoCondicion;
        public String valorDesde;
        public String valorHasta;
        public double porCada;
        public String nombreProductoObsequiado;
        public String codigoProductoObsequiado;
        public String urlProductoObsequiado; // URL pública de imagen del producto obsequiado
        public int cantidadObsequiado;
        public boolean hayStockDisponible;
        public int stockDisponible;
        public String fechaInicio;
        public String fechaFin;
        public String reglaBonificacion;
        public String mensajeStock;
        public String mensajeCondicion;
        public double precioObsequio;

        public static BonificacionData fromJson(JSONObject response) {
            BonificacionData data = new BonificacionData();
            JSONObject dataObj = response.optJSONObject("data");
            if (dataObj != null) {
                data.tieneBonificacion = dataObj.optBoolean("TieneBonificacion", false);
                data.mensajePromocional = dataObj.optString("MensajePromocional", "");
                data.requisitoMinimo = dataObj.optString("RequisitoMinimo", "");
                data.tipoCondicion = dataObj.optString("TipoCondicion", "");
                data.valorDesde = dataObj.optString("ValorDesde", "");
                data.valorHasta = dataObj.optString("ValorHasta", "");
                // Manejo seguro de PorCada cuando viene null
                if (!dataObj.isNull("PorCada")) {
                    try {
                        data.porCada = dataObj.optDouble("PorCada", 1.0);
                    } catch (Exception e) {
                        data.porCada = 1.0;
                    }
                } else {
                    data.porCada = 1.0;
                }
                data.nombreProductoObsequiado = dataObj.optString("NombreProductoObsequiado", "");
                data.codigoProductoObsequiado = dataObj.optString("CodigoProductoObsequiado", "");
                // Intentar capturar posibles nombres de clave para la URL pública del obsequio
                String url = dataObj.optString("UrlProductoObsequiado", "").trim();
                if (url.isEmpty()) url = dataObj.optString("URLProductoObsequiado", "").trim();
                if (url.isEmpty()) url = dataObj.optString("ImagenProductoObsequiado", "").trim();
                if (url.isEmpty()) url = dataObj.optString("UrlPublicaProductoObsequiado", "").trim();
                if (url.isEmpty()) url = dataObj.optString("ImagenProducto", "").trim();
                if (url.isEmpty()) url = dataObj.optString("imagenProducto", "").trim();
                data.urlProductoObsequiado = url;

                data.cantidadObsequiado = dataObj.optInt("CantidadObsequiado", 0);
                data.hayStockDisponible = dataObj.optBoolean("HayStockDisponible", false);
                data.stockDisponible = dataObj.optInt("StockDisponible", 0);
                data.fechaInicio = dataObj.optString("FechaInicio", "");
                data.fechaFin = dataObj.optString("FechaFin", "");
                data.reglaBonificacion = dataObj.optString("ReglaBonificacion", "");
                data.mensajeStock = dataObj.optString("MensajeStock", "");
                data.mensajeCondicion = dataObj.optString("MensajeCondicion", "");
                // Algunos JSON traen PrecioObsequio capitalizado diferente
                data.precioObsequio = dataObj.has("PrecioObsequio") ? dataObj.optDouble("PrecioObsequio", 0.0) : dataObj.optDouble("precioObsequio", 0.0);
            } else {
                // Fallback si la API no envió nodo "data"
                data.tieneBonificacion = response.optBoolean("tieneBonificacion", false);
                data.mensajePromocional = response.optString("mensajePromocional", "");
                data.requisitoMinimo = response.optString("requisitoMinimo", "");
                data.nombreProductoObsequiado = response.optString("nombreProductoObsequiado", "");
                data.codigoProductoObsequiado = response.optString("codigoProductoObsequiado", "");
                String url = response.optString("UrlProductoObsequiado", "").trim();
                if (url.isEmpty()) url = response.optString("URLProductoObsequiado", "").trim();
                if (url.isEmpty()) url = response.optString("ImagenProductoObsequiado", "").trim();
                if (url.isEmpty()) url = response.optString("UrlPublicaProductoObsequiado", "").trim();
                if (url.isEmpty()) url = response.optString("ImagenProducto", "").trim();
                if (url.isEmpty()) url = response.optString("imagenProducto", "").trim();
                data.urlProductoObsequiado = url;
                data.cantidadObsequiado = response.optInt("cantidadObsequiado", 0);
                data.hayStockDisponible = response.optBoolean("hayStockDisponible", false);
                data.mensajeCondicion = response.optString("mensajeCondicion", "");
                data.porCada = response.optDouble("porCada", 1.0);
                data.fechaInicio = response.optString("fechaInicio", "");
                data.fechaFin = response.optString("fechaFin", "");
                data.precioObsequio = response.optDouble("precioObsequio", 0.0);
            }
            return data;
        }
    }
}