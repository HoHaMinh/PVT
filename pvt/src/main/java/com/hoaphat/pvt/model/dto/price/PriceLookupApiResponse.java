package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceLookupApiResponse {
    private boolean success;
    private String message;
    private String searchedAt;
    private List<PriceLookupResultPayload> results = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSearchedAt() { return searchedAt; }
    public void setSearchedAt(String searchedAt) { this.searchedAt = searchedAt; }
    public List<PriceLookupResultPayload> getResults() { return results; }
    public void setResults(List<PriceLookupResultPayload> results) { this.results = results; }
}
