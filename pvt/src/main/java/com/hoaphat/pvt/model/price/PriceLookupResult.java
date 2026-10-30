package com.hoaphat.pvt.model.price;

import javax.persistence.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "price_lookup_result",
        uniqueConstraints = @UniqueConstraint(name = "uk_price_lookup_item_group", columnNames = {"item_id", "group_number"}))
public class PriceLookupResult {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer resultId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private PriceLookupItem item;

    @Column(name = "group_number", nullable = false)
    private Integer groupNumber;

    @Column(name = "group_name", nullable = false, length = 200)
    private String groupName;

    @Column(name = "material_name", length = 2000)
    private String materialName;

    @Column(name = "unit_name", length = 200)
    private String unitName;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 20, scale = 4)
    private BigDecimal purchasePrice;

    @Column(name = "supplier_name", length = 1000)
    private String supplierName;

    @Column(name = "is_found", nullable = false)
    private boolean found;

    public String getPurchaseDateDisplay() { return purchaseDate == null ? "-" : purchaseDate.format(DISPLAY_DATE); }
    public String getPurchasePriceDisplay() {
        if (purchasePrice == null) return "-";
        return new DecimalFormat("#,##0.####", DecimalFormatSymbols.getInstance(Locale.US)).format(purchasePrice);
    }

    public Integer getResultId() { return resultId; }
    public void setResultId(Integer resultId) { this.resultId = resultId; }
    public PriceLookupItem getItem() { return item; }
    public void setItem(PriceLookupItem item) { this.item = item; }
    public Integer getGroupNumber() { return groupNumber; }
    public void setGroupNumber(Integer groupNumber) { this.groupNumber = groupNumber; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }
}
