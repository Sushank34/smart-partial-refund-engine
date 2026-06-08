package com.refund.web;

import com.refund.domain.Refund;
import com.refund.service.RefundService;
import com.refund.web.dto.CreateRefundRequest;
import com.refund.web.dto.RefundResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints to issue a partial refund and to retrieve an order's full refund history. */
@RestController
@RequestMapping("/api/orders/{orderId}/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<RefundResponse> create(@PathVariable String orderId,
                                                 @Valid @RequestBody CreateRefundRequest request) {
        Refund refund = refundService.requestRefund(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RefundResponse.from(refund));
    }

    @GetMapping
    public List<RefundResponse> history(@PathVariable String orderId) {
        return refundService.getHistory(orderId).stream()
                .map(RefundResponse::from)
                .toList();
    }
}
