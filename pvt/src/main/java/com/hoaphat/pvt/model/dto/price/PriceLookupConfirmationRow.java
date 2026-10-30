package com.hoaphat.pvt.model.dto.price;

import java.util.ArrayList;
import java.util.List;

public class PriceLookupConfirmationRow {
    private Integer itemId;
    private List<Integer> selectedCandidateIds = new ArrayList<>();
    private boolean noSuitableCandidate;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public List<Integer> getSelectedCandidateIds() {
        return selectedCandidateIds;
    }

    public void setSelectedCandidateIds(List<Integer> selectedCandidateIds) {
        this.selectedCandidateIds = selectedCandidateIds == null
                ? new ArrayList<>() : selectedCandidateIds;
    }

    public boolean isNoSuitableCandidate() {
        return noSuitableCandidate;
    }

    public void setNoSuitableCandidate(boolean noSuitableCandidate) {
        this.noSuitableCandidate = noSuitableCandidate;
    }
}
