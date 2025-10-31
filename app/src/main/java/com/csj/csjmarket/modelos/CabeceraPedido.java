package com.csj.csjmarket.modelos;

public class CabeceraPedido {
    private int idPersona;
    private int idDireccionEntrega;
    private double totalVenta;
    private double totalPeso;
    private int tipoCp;

    public void setTipoCp(int tipoCp) {
        this.tipoCp = tipoCp;
    }

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

    public int getIdDireccionEntrega() {
        return idDireccionEntrega;
    }
}
