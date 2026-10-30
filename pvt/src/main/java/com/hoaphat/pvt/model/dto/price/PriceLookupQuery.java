package com.hoaphat.pvt.model.dto.price;

import java.util.ArrayList;
import java.util.List;

public class PriceLookupQuery {
    private String id;
    private String keyword;
    private List<PriceSelectedCandidate> selected = new ArrayList<>();

    public PriceLookupQuery() { }
    public PriceLookupQuery(String id, String keyword, List<PriceSelectedCandidate> selected) {
        this.id = id;
        this.keyword = keyword;
        this.selected = selected;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public List<PriceSelectedCandidate> getSelected() { return selected; }
    public void setSelected(List<PriceSelectedCandidate> selected) { this.selected = selected; }
}
