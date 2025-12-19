package com.csj.csjmarket.modelos;

import com.google.gson.annotations.SerializedName;

public class ProductoItem {
    private int idCp;
    private int idCpInventario;
    private int idProducto;
    private int idUnidad;
    private double peso;
    private String descripcion;
    private int cantidad;
    private double precio;
    private double total;
    @SerializedName(value = "TieneBono", alternate = {"tieneBono"})
    private boolean tieneBono;
    @SerializedName(value = "TieneDescuento", alternate = {"tieneDescuento"})
    private boolean tieneDescuento;
    @SerializedName(value = "IdDefinicionDescuento", alternate = {"idDefinicionDescuento", "IDDefinicionDescuento"})
    private Integer idDefinicionDescuento;

    public void setIdCp(int idCp) {
        this.idCp = idCp;
    }

    public void setIdCpInventario(int idCpInventario) {
        this.idCpInventario = idCpInventario;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public void setIdUnidad(int idUnidad) {
        this.idUnidad = idUnidad;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void setTieneBono(boolean tieneBono) {
        this.tieneBono = tieneBono;
    }

    // NUEVO: Getter para tieneBono
    public boolean isTieneBono() {
        return tieneBono;
    }

    // NUEVO: setter/getter para tieneDescuento
    public void setTieneDescuento(boolean tieneDescuento) {
        this.tieneDescuento = tieneDescuento;
    }

    public boolean isTieneDescuento() {
        return tieneDescuento;
    }

    public Integer getIdDefinicionDescuento() {
        return idDefinicionDescuento;
    }

    public void setIdDefinicionDescuento(Integer idDefinicionDescuento) {
        this.idDefinicionDescuento = idDefinicionDescuento;
    }
}