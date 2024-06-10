package com.csj.csjmarket.modelos;

import java.io.Serializable;

public class ValidarCorreo implements Serializable {
    private Integer id;
    private String docIdentidad;
    private Integer diasUltimaCompra;
    private String primerNombre;

    public Integer getId() {
        return id;
    }

    public String getDocIdentidad() {
        return docIdentidad;
    }

    public Integer getDiasUltimaCompra() {
        return diasUltimaCompra;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }
}
