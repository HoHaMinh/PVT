package com.hoaphat.pvt.model.dto.debt;

public class DebtImportAlertPayload {
    private String alertKey;
    private String section;
    private String itemOrder;
    private String supplier;
    private String invoiceNumber;
    private String remainingAmountDisplay;
    private String billOfLadingDate;
    private String customsDeclarationDate;
    private String regulatoryDeadline;
    private Integer daysRemaining;

    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getItemOrder() { return itemOrder; }
    public void setItemOrder(String itemOrder) { this.itemOrder = itemOrder; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getRemainingAmountDisplay() { return remainingAmountDisplay; }
    public void setRemainingAmountDisplay(String remainingAmountDisplay) { this.remainingAmountDisplay = remainingAmountDisplay; }
    public String getBillOfLadingDate() { return billOfLadingDate; }
    public void setBillOfLadingDate(String billOfLadingDate) { this.billOfLadingDate = billOfLadingDate; }
    public String getCustomsDeclarationDate() { return customsDeclarationDate; }
    public void setCustomsDeclarationDate(String customsDeclarationDate) { this.customsDeclarationDate = customsDeclarationDate; }
    public String getRegulatoryDeadline() { return regulatoryDeadline; }
    public void setRegulatoryDeadline(String regulatoryDeadline) { this.regulatoryDeadline = regulatoryDeadline; }
    public Integer getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }
}
