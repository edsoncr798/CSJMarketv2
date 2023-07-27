package com.csj.csjmarket.modelos;

public class CabeceraPedido {
    private int idPersona;
    private int idDireccionEntrega;
    private double totalVenta;
    private double totalPeso;

    public void setIdPersona(int idPersona) {
        this.idPersona = idPersona;
    }

    public void setIdDireccionEntrega(int idDireccionEntrega) {
        this.idDireccionEntrega = idDireccionEntrega;
    }

    public void setTotalVenta(double totalVenta) {
        this.totalVenta = totalVenta;
    }

    public void setTotalPeso(double totalPeso) {
        this.totalPeso = totalPeso;
    }
}
