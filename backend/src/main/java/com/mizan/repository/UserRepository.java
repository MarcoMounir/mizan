package com.mizan.repository;

import com.mizan.entity.User;
import com.mizan.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuthProviderAndProviderUserId(AuthProvider provider, String providerUserId);
    Optional<User> findByEmailAndAuthProvider(String email, AuthProvider provider);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
