package com.csj.csjmarket.modelos;

public class PedidoActual {
    private int idPersona;
    private int idDireccionEntrega;
    private double totalVenta;
    private double peso;
    private int tipoCp;

    public void setIdPersona(int idPersona) {
        this.idPersona = idPersona;
    }

    public void setIdDireccionEntrega(int idDireccionEntrega) {
        this.idDireccionEntrega = idDireccionEntrega;
    }

    public void setTotalVenta(double totalVenta) {
        this.totalVenta = totalVenta;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public void setTipoCp(int tipoCp) {
        this.tipoCp = tipoCp;
    }
}