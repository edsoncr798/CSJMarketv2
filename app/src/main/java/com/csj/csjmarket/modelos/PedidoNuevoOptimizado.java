package com.csj.csjmarket.modelos;

import java.util.ArrayList;
import com.google.gson.annotations.SerializedName;

public class PedidoNuevoOptimizado {
    private PedidoActual pedido;
    private ArrayList<ProductoItem> items;
    @SerializedName("RequestId")
    private String requestId;

    public void setPedido(PedidoActual pedido) {
        this.pedido = pedido;
    }

    public void setItems(ArrayList<ProductoItem> items) {
        this.items = items;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}