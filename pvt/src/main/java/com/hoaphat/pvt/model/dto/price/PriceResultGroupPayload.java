package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceResultGroupPayload {
    private String group;
    private String material;
    private String unit;
    private String date;
    @JsonDeserialize(using = FlexibleBigDecimalDeserializer.class)
    private BigDecimal price;
    private String supplier;
    private boolean found;

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }
}
