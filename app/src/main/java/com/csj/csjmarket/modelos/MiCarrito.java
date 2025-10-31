package com.csj.csjmarket.modelos;

import android.os.Parcelable;

public class MiCarrito {
    private Integer idProducto;
    private String codigo;
    private Integer idUnidad;
    private String nombre;
    private Double peso;
    private String unidad;
    private Integer cantidad;
    private Double precio;
    private Double total;
    private Double pesoTotal;
    private boolean esBonificacion; // indica si es un item de regalo/bonificación
    private Integer idProductoPrincipal; // id del producto principal que genera el regalo
    private Integer bonusStepUnidades; // unidades requeridas por paso (porCada)
    private Integer bonusCantidadPorPaso; // cantidad de obsequios por paso
    private String bonusNombreObsequio; // nombre del producto obsequiado
    private String codigoProductoObsequiado; // código del producto obsequiado
    private String imagenUrlObsequio; // URL pública de imagen del producto obsequiado
    // Nuevo: flag del producto principal indicando si tiene bonificación disponible
    private boolean tieneBonificacion;
    // Nuevo: factor de conversión del producto principal (p.ej. 1 tira = 12 unidades)
    private Integer factor;

    public Integer getIdProducto() {
        return idProducto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(Integer idUnidad) {
        this.idUnidad = idUnidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public Double getPesoTotal() {
        return pesoTotal;
    }

    public void setPesoTotal(Double pesoTotal) {
        this.pesoTotal = pesoTotal;
    }

    // Getter/Setter de bonificación
    public boolean isEsBonificacion() {
        return esBonificacion;
    }

    public void setEsBonificacion(boolean esBonificacion) {
        this.esBonificacion = esBonificacion;
    }

    // Nuevo Getter/Setter: tieneBonificacion
    public boolean isTieneBonificacion() {
        return tieneBonificacion;
    }

    public void setTieneBonificacion(boolean tieneBonificacion) {
        this.tieneBonificacion = tieneBonificacion;
    }

    // Relación con producto principal y reglas de bonificación
    public Integer getIdProductoPrincipal() {
        return idProductoPrincipal;
    }

    public void setIdProductoPrincipal(Integer idProductoPrincipal) {
        this.idProductoPrincipal = idProductoPrincipal;
    }

    public Integer getBonusStepUnidades() {
        return bonusStepUnidades;
    }

    public void setBonusStepUnidades(Integer bonusStepUnidades) {
        this.bonusStepUnidades = bonusStepUnidades;
    }

    public Integer getBonusCantidadPorPaso() {
        return bonusCantidadPorPaso;
    }

    public void setBonusCantidadPorPaso(Integer bonusCantidadPorPaso) {
        this.bonusCantidadPorPaso = bonusCantidadPorPaso;
    }

    public String getBonusNombreObsequio() {
        return bonusNombreObsequio;
    }

    public void setBonusNombreObsequio(String bonusNombreObsequio) {
        this.bonusNombreObsequio = bonusNombreObsequio;
    }

    public String getCodigoProductoObsequiado() {
        return codigoProductoObsequiado;
    }

    public void setCodigoProductoObsequiado(String codigoProductoObsequiado) {
        this.codigoProductoObsequiado = codigoProductoObsequiado;
    }

    public String getImagenUrlObsequio() {
        return imagenUrlObsequio;
    }

    public void setImagenUrlObsequio(String imagenUrlObsequio) {
        this.imagenUrlObsequio = imagenUrlObsequio;
    }

    // Nuevo getter/setter: factor
    public Integer getFactor() {
        return factor;
    }

    public void setFactor(Integer factor) {
        this.factor = factor;
    }
}
