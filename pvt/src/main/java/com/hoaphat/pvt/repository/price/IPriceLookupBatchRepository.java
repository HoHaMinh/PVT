package com.hoaphat.pvt.repository.price;

import com.hoaphat.pvt.model.price.PriceLookupBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPriceLookupBatchRepository extends JpaRepository<PriceLookupBatch, Integer> {
}
