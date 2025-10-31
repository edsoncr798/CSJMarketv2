package com.csj.csjmarket.modelos;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class PedidoNuevo {
    private CabeceraPedido pedido;
    private ArrayList<ItemPedido> items;
    @SerializedName("RequestId")
    private String requestId;

    public void setCabeceraPedido(CabeceraPedido cabeceraPedido) {
        this.pedido = cabeceraPedido;
    }

    public void setItemPedidos(ArrayList<ItemPedido> itemPedidos) {
        this.items = itemPedidos;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
