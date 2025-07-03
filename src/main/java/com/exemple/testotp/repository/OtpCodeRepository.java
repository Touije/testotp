package com.exemple.testotp.repository;


import com.exemple.testotp.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    @Query("SELECT o FROM OtpCode o WHERE o.phoneNumber = :phoneNumber AND o.used = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpCode> findValidOtpByPhoneNumber(@Param("phoneNumber") String phoneNumber, @Param("now") LocalDateTime now);

    void deleteByPhoneNumberAndUsed(String phoneNumber, boolean used);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}