package com.csj.csjmarket.modelos;

import java.sql.Timestamp;

public class RespuestaPedido {
    private int IdCp;
    private int IdCpInventario;
    private String NumCp;
    private String Fecha;
    private String RequestId;
    private String ImagenProducto;

    public int getIdCp() {
        return IdCp;
    }

    public int getIdCpInventario() {
        return IdCpInventario;
    }

    public String getNumCp() {
        return NumCp;
    }

    public String getFecha() {
        return Fecha;
    }

    public String getRequestId() {
        return RequestId;
    }

    public String getImagenProducto() {
        return ImagenProducto;
    }
}
