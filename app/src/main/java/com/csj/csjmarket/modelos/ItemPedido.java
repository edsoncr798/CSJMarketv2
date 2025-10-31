package com.csj.csjmarket.modelos;

public class ItemPedido {
    private int idCp;
    private int idCpInventario;
    private int idProducto;
    private int idUnidad;
    private double peso;
    private String descripcion;
    private int cantidad;
    private double precio;
    private double total;
    private boolean tieneBono; // NUEVO: Indica si el producto tiene bonificación

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

    // NUEVO: Setter para tieneBono
    public void setTieneBono(boolean tieneBono) {
        this.tieneBono = tieneBono;
    }

    // NUEVO: Getter para tieneBono
    public boolean isTieneBono() {
        return tieneBono;
    }
}
