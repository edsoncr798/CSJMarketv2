package com.csj.csjmarket.modelos;

public class Direccion {
    private int id;
    private String descripcion;
    private String ubigeo;

    public int getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUbigeo() {
        return ubigeo;
    }

    public void setUbigeo(String ubigeo) {
        this.ubigeo = ubigeo;
    }
}
