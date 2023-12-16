package com.demystify_network.backend.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.demystify_network.backend.model.userapiaccess.Stats;

@Repository
public interface StatsRepository extends CrudRepository<Stats, Long> {

}
