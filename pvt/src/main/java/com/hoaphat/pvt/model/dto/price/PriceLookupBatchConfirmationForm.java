package com.hoaphat.pvt.model.dto.price;

import java.util.ArrayList;
import java.util.List;

public class PriceLookupBatchConfirmationForm {
    private List<PriceLookupConfirmationRow> items = new ArrayList<>();

    public List<PriceLookupConfirmationRow> getItems() {
        return items;
    }

    public void setItems(List<PriceLookupConfirmationRow> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
