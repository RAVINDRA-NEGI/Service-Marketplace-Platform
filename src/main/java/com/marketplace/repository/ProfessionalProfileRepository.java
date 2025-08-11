package com.marketplace.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

@Repository
public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, Long> {
    Optional<ProfessionalProfile> findByUser(User user);
    boolean existsByUser(User user);
    
    @Query("SELECT p FROM ProfessionalProfile p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:city IS NULL OR LOWER(p.serviceAreaCity) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:minRating IS NULL OR p.averageRating >= :minRating)")
    List<ProfessionalProfile> findProfessionalsByFilters(
        @Param("categoryId") Long categoryId,
        @Param("city") String city,
        @Param("minRating") Double minRating
    );
    
    List<ProfessionalProfile> findByCategoryId(Long categoryId);
}