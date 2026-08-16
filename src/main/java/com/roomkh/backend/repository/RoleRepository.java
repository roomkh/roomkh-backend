package com.roomkh.backend.repository;

import com.roomkh.backend.entity.Role;
import com.roomkh.backend.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}