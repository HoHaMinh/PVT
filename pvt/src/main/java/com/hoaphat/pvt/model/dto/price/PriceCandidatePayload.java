package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceCandidatePayload {
    private String name;
    private Integer startRow;
    private Integer rowCount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStartRow() { return startRow; }
    public void setStartRow(Integer startRow) { this.startRow = startRow; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
}
