package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceSearchApiResponse {
    private boolean success;
    private String message;
    private List<PriceSearchResultPayload> results = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<PriceSearchResultPayload> getResults() { return results; }
    public void setResults(List<PriceSearchResultPayload> results) { this.results = results; }
}
