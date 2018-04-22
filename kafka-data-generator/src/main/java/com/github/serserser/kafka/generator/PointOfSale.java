package com.github.serserser.kafka.generator;

public class PointOfSale {

    private Integer shopId;
    private Integer countryId;

    public PointOfSale(Integer shopId, Integer countryId) {
        this.shopId = shopId;
        this.countryId = countryId;
    }

    public Integer getShopId() {
        return shopId;
    }

    public Integer getCountryId() {
        return countryId;
    }
}
