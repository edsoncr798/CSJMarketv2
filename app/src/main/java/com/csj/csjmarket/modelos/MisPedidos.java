package com.csj.csjmarket.modelos;

public class MisPedidos {
    private String fechaEmision;
    private String numCp;
    private String observacion;
    private double total;
    private String estadoAprobacion;
    private String estadoFacturacion;
    private String estadoEntrega;

    public String getFechaEmision() {
        return fechaEmision;
    }

    public String getNumCp() {
        return numCp;
    }

    public String getObservacion() {
        return observacion;
    }

    public double getTotal() {
        return total;
    }

    public String getEstadoAprobacion() {
        return estadoAprobacion;
    }

    public String getEstadoFacturacion() {
        return estadoFacturacion;
    }

    public String getEstadoEntrega() {
        return estadoEntrega;
    }
}
