package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceLookupResultPayload {
    private String id;
    private String keyword;
    private Integer selectedCount;
    private List<PriceResultGroupPayload> groups = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getSelectedCount() { return selectedCount; }
    public void setSelectedCount(Integer selectedCount) { this.selectedCount = selectedCount; }
    public List<PriceResultGroupPayload> getGroups() { return groups; }
    public void setGroups(List<PriceResultGroupPayload> groups) { this.groups = groups; }
}
