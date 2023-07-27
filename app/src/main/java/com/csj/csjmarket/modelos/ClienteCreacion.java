package com.csj.csjmarket.modelos;

import java.io.Serializable;

public class ClienteCreacion implements Serializable {
    private String p_Prefijo;
    private String p_Nombre;
    private String p_ApellidoPaterno;
    private String p_ApellidoMaterno;
    private String p_PrimerNombre;
    private String p_Email;
    private String p_DocIdentidad;
    private String p_Direccion1;
    private String p_Distrito1;
    private String p_TelefonoGeneral;

    public ClienteCreacion(String p_Prefijo, String p_Nombre, String p_ApellidoPaterno, String p_ApellidoMaterno, String p_PrimerNombre, String p_Email, String p_DocIdentidad, String p_Direccion1, String p_Distrito1, String p_TelefonoGeneral) {
        this.p_Prefijo = p_Prefijo;
        this.p_Nombre = p_Nombre;
        this.p_ApellidoPaterno = p_ApellidoPaterno;
        this.p_ApellidoMaterno = p_ApellidoMaterno;
        this.p_PrimerNombre = p_PrimerNombre;
        this.p_Email = p_Email;
        this.p_DocIdentidad = p_DocIdentidad;
        this.p_Direccion1 = p_Direccion1;
        this.p_Distrito1 = p_Distrito1;
        this.p_TelefonoGeneral = p_TelefonoGeneral;
    }
}
