package com.pixora.repository;

import com.pixora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity data access operations.
 * Extends JpaRepository to provide standard CRUD functionalities
 */

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    //Retrieves a user by their email address.
    Optional<User> findByEmail(String email);

    //Retrieves a user by their phone number
    Optional<User> findByPhone(String phone);

    //Checks whether a user exists with the specified email address.
    boolean existsByEmail(String email);

    //Retrieves a list of all users who have a specific role.
    List<User> findByRole(User.Role role);

    //Retrieves a list of users filtered by both their role and account status.
    List<User> findByRoleAndAccountStatus(User.Role role, User.AccountStatus status);
}
