package com.foodapp.repository;

import com.foodapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<Entity, IdType> gives us save(), findById(), findAll(),
// delete(), etc. for free - no implementation needed, Spring generates it.
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data derives the SQL from the method name itself:
    // "findByEmail" -> SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
