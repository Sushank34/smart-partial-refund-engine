package com.refund.repository;

import com.refund.domain.Refund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, String> {

    /** Full refund history for an order, oldest first. */
    List<Refund> findByOrderIdOrderByCreatedAtAsc(String orderId);

    /** Look up a prior refund by its idempotency key, for replay. */
    Optional<Refund> findByIdempotencyKey(String idempotencyKey);
}
