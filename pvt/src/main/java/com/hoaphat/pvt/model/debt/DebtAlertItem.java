package com.hoaphat.pvt.model.debt;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "debt_alert_item")
public class DebtAlertItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer debtAlertItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_snapshot_id", nullable = false)
    private DebtAlertEmployeeSnapshot employeeSnapshot;

    @Column(name = "section_name", length = 30)
    private String section;

    @Column(name = "item_order", length = 50)
    private String itemOrder;

    @Column(name = "supplier_name", length = 500)
    private String supplier;

    @Column(name = "invoice_number", length = 200)
    private String invoiceNumber;

    @Column(name = "amount_display", length = 200)
    private String amountDisplay;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "t_plus")
    private Integer tPlus;

    public Integer getDebtAlertItemId() {
        return debtAlertItemId;
    }

    public void setDebtAlertItemId(Integer debtAlertItemId) {
        this.debtAlertItemId = debtAlertItemId;
    }

    public DebtAlertEmployeeSnapshot getEmployeeSnapshot() {
        return employeeSnapshot;
    }

    public void setEmployeeSnapshot(DebtAlertEmployeeSnapshot employeeSnapshot) {
        this.employeeSnapshot = employeeSnapshot;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(String itemOrder) {
        this.itemOrder = itemOrder;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getAmountDisplay() {
        return amountDisplay;
    }

    public void setAmountDisplay(String amountDisplay) {
        this.amountDisplay = amountDisplay;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getTPlus() {
        return tPlus;
    }

    public void setTPlus(Integer tPlus) {
        this.tPlus = tPlus;
    }
}
