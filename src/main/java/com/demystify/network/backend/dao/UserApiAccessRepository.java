package com.demystify.network.backend.dao;

import com.demystify.network.backend.model.userapiaccess.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApiAccessRepository extends CrudRepository<User, Long> {

  User findByPassword(String password);
}
