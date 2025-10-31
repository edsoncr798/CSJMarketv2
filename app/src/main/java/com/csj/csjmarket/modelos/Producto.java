package com.csj.csjmarket.modelos;

import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

public class Producto implements Serializable {

    private int id;
    private String codigo;
    private String nombre;
    private int factor;
    private int idUnidadBase;
    private int idUnidadReferencia;
    private String unidadBase;
    private String unidadReferencia;
    private Double precioUnidadBase;
    private Double precioUnidadRef;
    private Double peso;
    private String proveedor;
    private int idProveedor;
    @SerializedName(value = "Observacion", alternate = {"observacion"})
    private String observacion;

    @SerializedName(value = "PrecioUnidadAntes", alternate = {"precioUnidadAntes"})
    private Double precioUnidadAntes;
    //private String imagen;

    @SerializedName(value = "TieneBonificacion", alternate = {"tieneBonificacion"})
    private boolean tieneBonificacion;

    @SerializedName(value = "stockFisico", alternate = {"StockFisico"})
    private int stockFisico;

    @SerializedName(value = "stockPorEntregar", alternate = {"StockPorEntregar"})
    private int stockPorEntregar;

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getFactor() {
        return factor;
    }

    public int getIdUnidadBase() {
        return idUnidadBase;
    }

    public int getIdUnidadReferencia() {
        return idUnidadReferencia;
    }

    public String getUnidadBase() {
        return unidadBase;
    }

    public String getUnidadReferencia() {
        return unidadReferencia;
    }

    public Double getPrecioUnidadBase() {
        return precioUnidadBase;
    }

    public Double getPrecioUnidadRef() {
        return precioUnidadRef;
    }

    public Double getPeso() {
        return peso;
    }

    public String getProveedor() {
        return proveedor;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    // nuevos getters
    public boolean isTieneBonificacion() {
        return tieneBonificacion;
    }

    public int getStockFisico() {
        return stockFisico;
    }

    public int getStockPorEntregar() {
        return stockPorEntregar;
    }

    public int getStockDisponible() {
        return Math.max(stockFisico - stockPorEntregar, 0);
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Double getPrecioUnidadAntes() {
        return precioUnidadAntes;
    }

    public void setPrecioUnidadAntes(Double precioUnidadAntes) {
        this.precioUnidadAntes = precioUnidadAntes;
    }
}
