package com.pixora.repository;

import com.pixora.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository interface for managing database operations on Package entities
@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    // Retrieve all photography packages that are currently marked as active
    List<Package> findByIsActiveTrue();
}