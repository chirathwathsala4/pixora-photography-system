package com.pixora.repository;

import com.pixora.entity.PromoCode;
//It gives us ready-made database methods,save()findById()findAll()delete()deleteById()
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//Used when a promo code may or may not exist
import java.util.Optional;

//Tells Spring that this is a repository/database component.
@Repository
//provides CRUD operations for the PromoCode entity.
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    //The code matches without considering uppercase/lowercase,The promo code is active
    Optional<PromoCode> findByCodeIgnoreCaseAndIsActiveTrue(String code);
    //This searches for a promo code by its code, ignoring uppercase/lowercase,
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    //This checks whether a promo code already exists
    boolean existsByCodeIgnoreCase(String code);
}
