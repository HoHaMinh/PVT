package com.hoaphat.pvt.model.dto.debt;

public class DebtAlertItemView {
    private String section;
    private String itemOrder;
    private String supplier;
    private String invoiceNumber;
    private String amountDisplay;
    private String dueDate;
    private Integer tPlus;
    private String tPlusLabel;
    private String tPlusClass;

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getItemOrder() { return itemOrder; }
    public void setItemOrder(String itemOrder) { this.itemOrder = itemOrder; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getAmountDisplay() { return amountDisplay; }
    public void setAmountDisplay(String amountDisplay) { this.amountDisplay = amountDisplay; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public Integer getTPlus() { return tPlus; }
    public void setTPlus(Integer tPlus) { this.tPlus = tPlus; }
    public String getTPlusLabel() { return tPlusLabel; }
    public void setTPlusLabel(String tPlusLabel) { this.tPlusLabel = tPlusLabel; }
    public String getTPlusClass() { return tPlusClass; }
    public void setTPlusClass(String tPlusClass) { this.tPlusClass = tPlusClass; }
}
