package com.hoaphat.pvt.model.dto.price;

public class PriceSelectedCandidate {
    private String name;
    private Integer startRow;
    private Integer rowCount;

    public PriceSelectedCandidate() { }
    public PriceSelectedCandidate(String name, Integer startRow, Integer rowCount) {
        this.name = name;
        this.startRow = startRow;
        this.rowCount = rowCount;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStartRow() { return startRow; }
    public void setStartRow(Integer startRow) { this.startRow = startRow; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
}
