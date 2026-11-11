package com.eventpro.repository;

import com.eventpro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    // used on update: the email may stay the same for the same account
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
