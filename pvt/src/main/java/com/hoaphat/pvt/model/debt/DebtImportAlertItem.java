package com.hoaphat.pvt.model.debt;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "debt_import_alert_item")
public class DebtImportAlertItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer importAlertItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_snapshot_id", nullable = false)
    private DebtAlertEmployeeSnapshot employeeSnapshot;

    @Column(name = "alert_key", nullable = false, length = 64)
    private String alertKey;

    @Column(name = "section_name", length = 30)
    private String section;

    @Column(name = "item_order", length = 50)
    private String itemOrder;

    @Column(name = "supplier_name", length = 500)
    private String supplier;

    @Column(name = "invoice_number", length = 200)
    private String invoiceNumber;

    @Column(name = "remaining_amount_display", length = 200)
    private String remainingAmountDisplay;

    @Column(name = "bill_of_lading_date")
    private LocalDate billOfLadingDate;

    @Column(name = "customs_declaration_date")
    private LocalDate customsDeclarationDate;

    @Column(name = "regulatory_deadline", nullable = false)
    private LocalDate regulatoryDeadline;

    @Column(name = "days_remaining", nullable = false)
    private Integer daysRemaining;

    public Integer getImportAlertItemId() { return importAlertItemId; }
    public void setImportAlertItemId(Integer importAlertItemId) { this.importAlertItemId = importAlertItemId; }
    public DebtAlertEmployeeSnapshot getEmployeeSnapshot() { return employeeSnapshot; }
    public void setEmployeeSnapshot(DebtAlertEmployeeSnapshot employeeSnapshot) { this.employeeSnapshot = employeeSnapshot; }
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
    public LocalDate getBillOfLadingDate() { return billOfLadingDate; }
    public void setBillOfLadingDate(LocalDate billOfLadingDate) { this.billOfLadingDate = billOfLadingDate; }
    public LocalDate getCustomsDeclarationDate() { return customsDeclarationDate; }
    public void setCustomsDeclarationDate(LocalDate customsDeclarationDate) { this.customsDeclarationDate = customsDeclarationDate; }
    public LocalDate getRegulatoryDeadline() { return regulatoryDeadline; }
    public void setRegulatoryDeadline(LocalDate regulatoryDeadline) { this.regulatoryDeadline = regulatoryDeadline; }
    public Integer getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }
}
