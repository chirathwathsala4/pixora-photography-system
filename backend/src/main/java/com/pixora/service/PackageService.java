package com.pixora.service;

import com.pixora.dto.PackageDto;
import com.pixora.entity.Package;
import com.pixora.exception.ResourceNotFoundException;
import com.pixora.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Service implementation managing CRUD operations and status toggling for photography packages
@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;

    // Retrieve all photography packages from the database and map to DTOs
    @Transactional(readOnly = true)
    public List<PackageDto> getAllPackages() {
        return packageRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // Retrieve only active photography packages from the database and map to DTOs
    @Transactional(readOnly = true)
    public List<PackageDto> getActivePackages() {
        return packageRepository.findByIsActiveTrue().stream().map(this::toDto).collect(Collectors.toList());
    }

    // Create and save a new photography package
    @Transactional
    public PackageDto createPackage(PackageDto dto) {
        // Build package entity with default active state if unspecified
        Package pkg = Package.builder()
                .packageName(dto.getPackageName())
                .priceLkr(dto.getPriceLkr())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        // Persist entity and return mapped DTO response
        return toDto(packageRepository.save(pkg));
    }

    // Update existing photography package details by package ID
    @Transactional
    public PackageDto updatePackage(Long id, PackageDto dto) {
        // Retrieve target package or throw ResourceNotFoundException
        Package pkg = findPackage(id);
        // Update package attributes with provided DTO values
        pkg.setPackageName(dto.getPackageName());
        pkg.setPriceLkr(dto.getPriceLkr());
        pkg.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) pkg.setIsActive(dto.getIsActive());
        // Save updated entity and return DTO
        return toDto(packageRepository.save(pkg));
    }

    // Toggle active status (activate/deactivate) of a package by ID
    @Transactional
    public PackageDto toggleActive(Long id) {
        // Fetch package entity
        Package pkg = findPackage(id);
        // Invert the current active status flag
        pkg.setIsActive(!pkg.getIsActive());
        // Save status change and return DTO
        return toDto(packageRepository.save(pkg));
    }

    // Remove a package record from the database by ID
    @Transactional
    public void deletePackage(Long id) {
        packageRepository.delete(findPackage(id));
    }

    // Helper method to retrieve a Package entity by ID or throw exception if missing
    private Package findPackage(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));
    }

    // Mapping method to convert Package entity into PackageDto
    public PackageDto toDto(Package p) {
        return PackageDto.builder()
                .packageId(p.getPackageId())
                .packageName(p.getPackageName())
                .priceLkr(p.getPriceLkr())
                .description(p.getDescription())
                .isActive(p.getIsActive())
                .build();
    }
}