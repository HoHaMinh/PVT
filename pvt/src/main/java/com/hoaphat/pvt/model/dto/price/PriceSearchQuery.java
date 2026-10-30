package com.hoaphat.pvt.model.dto.price;

public class PriceSearchQuery {
    private String id;
    private String keyword;

    public PriceSearchQuery() { }
    public PriceSearchQuery(String id, String keyword) {
        this.id = id;
        this.keyword = keyword;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
