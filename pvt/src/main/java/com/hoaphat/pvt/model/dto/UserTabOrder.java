package com.hoaphat.pvt.model.dto;
import javax.persistence.*;
@Entity
public class UserTabOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String managerUsername;
    private String accountName;
    private Integer displayOrder;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getManagerUsername() { return managerUsername; }
    public void setManagerUsername(String managerUsername) { this.managerUsername = managerUsername; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}