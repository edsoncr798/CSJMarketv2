package com.csj.csjmarket.modelos;

public class filtroProveedor {
    private int id;
    private String nombre;
    private boolean estado = false;

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
