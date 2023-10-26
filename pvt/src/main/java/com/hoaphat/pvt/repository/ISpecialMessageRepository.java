package com.hoaphat.pvt.repository;

import com.hoaphat.pvt.model.event.SpecialMessage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ISpecialMessageRepository extends CrudRepository<SpecialMessage,Integer> {
}
