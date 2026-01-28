package org.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByExternalId(String externalId);

}