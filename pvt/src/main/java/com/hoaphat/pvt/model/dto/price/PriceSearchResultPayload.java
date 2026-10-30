package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceSearchResultPayload {
    private String id;
    private String keyword;
    private List<PriceCandidatePayload> candidates = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public List<PriceCandidatePayload> getCandidates() { return candidates; }
    public void setCandidates(List<PriceCandidatePayload> candidates) { this.candidates = candidates; }
}
