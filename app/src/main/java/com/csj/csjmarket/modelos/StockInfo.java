package com.csj.csjmarket.modelos;

import java.io.Serializable;

public class StockInfo implements Serializable {
    private int Id;
    private int StockFisico;
    private int StockPorEntregar;

    public StockInfo() {}

    public StockInfo(int id, int stockFisico, int stockPorEntregar) {
        this.Id = id;
        this.StockFisico = stockFisico;
        this.StockPorEntregar = stockPorEntregar;
    }

    public int getId() {
        return Id;
    }

    public int getStockFisico() {
        return StockFisico;
    }

    public int getStockPorEntregar() {
        return StockPorEntregar;
    }

    public int getStockDisponible() {
        return StockFisico - StockPorEntregar;
    }

    public boolean isDisponible() {
        return getStockDisponible() > 0;
    }
}