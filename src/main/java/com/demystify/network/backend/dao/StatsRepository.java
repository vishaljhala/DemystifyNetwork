package com.demystify.network.backend.dao;

import com.demystify.network.backend.model.userapiaccess.Stats;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsRepository extends CrudRepository<Stats, Long> {

}
