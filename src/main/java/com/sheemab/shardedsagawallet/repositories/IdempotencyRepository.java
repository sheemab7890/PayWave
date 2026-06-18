package com.sheemab.shardedsagawallet.repositories;


import com.sheemab.shardedsagawallet.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IdempotencyRepository
        extends JpaRepository<IdempotencyRecord, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.createdAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}