package com.csj.csjmarket.modelos;

import java.io.Serializable;

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

    //private String imagen;

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
}
