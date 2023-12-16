package com.demystify_network.backend.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.demystify_network.backend.model.userapiaccess.User;

@Repository
public interface UserApiAccessRepository extends CrudRepository<User, Long> {

  User findByPassword(String password);
}
