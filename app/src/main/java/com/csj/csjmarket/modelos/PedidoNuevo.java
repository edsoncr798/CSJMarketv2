package com.csj.csjmarket.modelos;

import java.util.ArrayList;
import java.util.List;

public class PedidoNuevo {
    private CabeceraPedido pedido;
    private ArrayList<ItemPedido> items;

    public void setCabeceraPedido(CabeceraPedido cabeceraPedido) {
        this.pedido = cabeceraPedido;
    }

    public void setItemPedidos(ArrayList<ItemPedido> itemPedidos) {
        this.items = itemPedidos;
    }
}
