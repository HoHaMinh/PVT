package com.hoaphat.pvt.model.price;

import javax.persistence.*;

@Entity
@Table(name = "price_lookup_candidate")
public class PriceLookupCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer candidateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private PriceLookupItem item;

    @Column(name = "material_name", nullable = false, length = 2000)
    private String materialName;

    @Column(name = "cache_start_row", nullable = false)
    private Integer startRow;

    @Column(name = "cache_row_count", nullable = false)
    private Integer rowCount;

    @Column(name = "is_selected", nullable = false)
    private boolean selected;

    public Integer getCandidateId() { return candidateId; }
    public void setCandidateId(Integer candidateId) { this.candidateId = candidateId; }
    public PriceLookupItem getItem() { return item; }
    public void setItem(PriceLookupItem item) { this.item = item; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public Integer getStartRow() { return startRow; }
    public void setStartRow(Integer startRow) { this.startRow = startRow; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
