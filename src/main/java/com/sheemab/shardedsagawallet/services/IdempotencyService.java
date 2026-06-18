package com.sheemab.shardedsagawallet.services;



import com.sheemab.shardedsagawallet.entities.IdempotencyRecord;
import com.sheemab.shardedsagawallet.enums.IdempotencyStatus;
import com.sheemab.shardedsagawallet.exception.DuplicateRequestException;
import com.sheemab.shardedsagawallet.repositories.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    // If a record is stuck in PROCESSING for more than 10 minutes,
    // the server probably crashed mid-saga — safe to allow retry
    private static final int STALE_PROCESSING_MINUTES = 10;

    private final IdempotencyRepository repository;

    /**
     * STEP 1 — Called at the very start of every transfer request.
     *
     * THREE outcomes:
     *
     * A) Key never seen before      → return empty  → proceed with saga
     * B) Key seen, saga COMPLETED   → return record → return cached result
     * C) Key seen, saga PROCESSING  → throw         → reject duplicate
     *
     * FAILED records are deleted here so client can retry with same key.
     * Stale PROCESSING records (server crashed) are also deleted for retry.
     */
    @Transactional
    public Optional<IdempotencyRecord> checkIdempotency(String key) {

        Optional<IdempotencyRecord> found = repository.findById(key);

        // Case A — key never seen, fresh request
        if (found.isEmpty()) {
            log.info("[Idempotency] Fresh key: {}", key);
            return Optional.empty();
        }

        IdempotencyRecord record = found.get();

        // Case B — saga already completed, return cached result
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            log.info("[Idempotency] Duplicate detected. Returning cached sagaInstanceId: {}",
                    record.getSagaInstanceId());
            return Optional.of(record);
        }

        // Case C — saga failed previously, delete record and allow retry
        if (record.getStatus() == IdempotencyStatus.FAILED) {
            log.info("[Idempotency] Previous attempt failed. Clearing record, allowing retry.");
            repository.delete(record);
            return Optional.empty();
        }

        // Case D — PROCESSING: could be concurrent duplicate OR server crash recovery
        if (record.getStatus() == IdempotencyStatus.PROCESSING) {

            boolean isStale = record.getCreatedAt()
                    .isBefore(LocalDateTime.now().minusMinutes(STALE_PROCESSING_MINUTES));

            if (isStale) {
                // Server crashed while saga was running — safe to retry
                log.warn("[Idempotency] Stale PROCESSING record found (>{}min). Clearing for retry.",
                        STALE_PROCESSING_MINUTES);
                repository.delete(record);
                return Optional.empty();
            }

            // Genuine concurrent duplicate — reject it
            log.warn("[Idempotency] Request already in progress for key: {}", key);
            throw new DuplicateRequestException(
                    "This request is already being processed. Please wait and retry."
            );
        }

        return Optional.empty();
    }

    /**
     * STEP 2 — Called immediately after checkIdempotency() returns empty.
     *
     * WHY REQUIRES_NEW?
     * This opens a BRAND NEW transaction and commits it IMMEDIATELY,
     * independent of the outer saga transaction.
     *
     * If we didn't do this, two concurrent threads could both pass
     * checkIdempotency() before either one inserts the PROCESSING record.
     * Both would then run the saga — double debit.
     *
     * With REQUIRES_NEW:
     * Thread A inserts PROCESSING → commits to DB immediately
     * Thread B tries to insert → DB unique constraint fires → rejected
     * Only ONE saga runs.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(String key) {
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.PROCESSING)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.saveAndFlush(record);
            // saveAndFlush = save + immediately write to DB within this tx
            // The REQUIRES_NEW tx commits when this method exits

            log.info("[Idempotency] Key {} marked as PROCESSING", key);

        } catch (DataIntegrityViolationException e) {
            // Another thread already inserted this key — concurrent duplicate
            throw new DuplicateRequestException(
                    "Concurrent duplicate detected for key: " + key
            );
        }
    }

    /**
     * STEP 3A — Called when saga completes successfully.
     *
     * Stores the sagaInstanceId so future duplicate requests
     * get the same result back without running the saga again.
     *
     * REQUIRES_NEW — commits immediately, independent of outer tx.
     * If outer tx rolls back, this record stays committed.
     * That's intentional — saga completed, we want this persisted.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, Long sagaInstanceId) {
        IdempotencyRecord record = repository.findById(key)
                .orElseThrow(() -> new RuntimeException(
                        "[Idempotency] Record missing for key: " + key + " — this is a bug"
                ));

        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setSagaInstanceId(sagaInstanceId);
        repository.saveAndFlush(record);

        log.info("[Idempotency] Key {} marked COMPLETED with sagaInstanceId: {}", key, sagaInstanceId);
    }

    /**
     * STEP 3B — Called when saga fails.
     *
     * Marks as FAILED instead of deleting.
     * Why? So the next call to checkIdempotency() can detect FAILED,
     * log it, and clean it up — giving us an audit trail of what happened.
     *
     * REQUIRES_NEW — commits immediately even if outer tx rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        repository.findById(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            repository.saveAndFlush(record);
        });

        log.info("[Idempotency] Key {} marked FAILED — client may retry with same key", key);
    }

    /**
     * Runs at 2AM daily.
     * Deletes records older than 30 days — prevents table growing forever.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        repository.deleteOlderThan(cutoff);
        log.info("[Idempotency] Cleaned up records older than 30 days");
    }
}