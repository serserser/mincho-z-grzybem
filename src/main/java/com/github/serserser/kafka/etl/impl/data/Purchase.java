package com.github.serserser.kafka.etl.impl.data;

public class Purchase {

    private Integer purchaseId;
    private Integer clientId;
    private Integer commodityId;
    private Integer quantity;
    private Integer posId;

    public Purchase(Integer purchaseId, Integer clientId, Integer commodityId, Integer quantity, Integer posId) {
        this.purchaseId = purchaseId;
        this.clientId = clientId;
        this.commodityId = commodityId;
        this.quantity = quantity;
        this.posId = posId;
    }

    public Purchase() {
    }

    public Integer getPurchaseId() {
        return purchaseId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public Integer getCommodityId() {
        return commodityId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getPosId() {
        return posId;
    }

    public void setPosId(Integer posId) {
        this.posId = posId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
