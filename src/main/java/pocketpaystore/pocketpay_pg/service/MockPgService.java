package pocketpaystore.pocketpay_pg.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pocketpaystore.pocketpay_pg.domain.MockTransaction;
import pocketpaystore.pocketpay_pg.domain.MockTransactionStatus;
import pocketpaystore.pocketpay_pg.dto.ApprovalRequest;
import pocketpaystore.pocketpay_pg.dto.ApprovalResponse;
import pocketpaystore.pocketpay_pg.dto.CancelRequest;
import pocketpaystore.pocketpay_pg.dto.CancelResponse;
import pocketpaystore.pocketpay_pg.dto.TransactionStatusResponse;
import pocketpaystore.pocketpay_pg.repository.MockTransactionStore;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockPgService {

	private final MockTransactionStore transactionStore;
	private final WebhookSender webhookSender;

	public ResponseEntity<?> approve(String idempotencyKey, ApprovalRequest request) {
		log.info("[MockPg] approve 요청 수신: idempotencyKey={}, paymentKey={}, orderNumber={}",
				idempotencyKey, request.getPaymentKey(), request.getOrderNumber());

		MockTransaction existing = transactionStore.findByIdempotencyKey(idempotencyKey).orElse(null);
		if (existing != null) {
			log.info("[MockPg] 기존 거래 재반환(멱등): idempotencyKey={}, pgTransactionId={}",
					idempotencyKey, existing.getPgTransactionId());
			return ResponseEntity.ok(toApprovalResponse(existing));
		}

		FaultTrigger trigger = FaultTrigger.from(request.getPaymentKey());
		sleepIfNeeded(trigger.delayMillis());
		if (trigger.httpStatus() != null) {
			return errorResponse(trigger.httpStatus(), "장애 주입 트리거로 강제 실패했습니다.");
		}

		String pgTransactionId = "MOCK-" + UUID.randomUUID();
		LocalDateTime approvedAt = LocalDateTime.now();

		MockTransaction transaction = MockTransaction.builder()
				.pgTransactionId(pgTransactionId)
				.idempotencyKey(idempotencyKey)
				.orderNumber(request.getOrderNumber())
				.amount(request.getAmount())
				.canceledAmount(0L)
				.status(MockTransactionStatus.APPROVED)
				.approvedAt(approvedAt)
				.build();
		transactionStore.save(transaction);

		ApprovalResponse response = toApprovalResponse(transaction);
		webhookSender.sendApproved(response);
		return ResponseEntity.ok(response);
	}

	public ResponseEntity<?> cancel(CancelRequest request) {
		FaultTrigger trigger = FaultTrigger.from(request.getMerchantCancelId());
		sleepIfNeeded(trigger.delayMillis());
		if (trigger.httpStatus() != null) {
			return errorResponse(trigger.httpStatus(), "장애 주입 트리거로 강제 실패했습니다.");
		}

		MockTransaction transaction = transactionStore.findByTransactionId(request.getPgTransactionId()).orElse(null);
		if (transaction == null) {
			return errorResponse(HttpStatus.NOT_FOUND, "존재하지 않는 거래입니다.");
		}

		long canceledAmount = transaction.getCanceledAmount() + request.getCancelAmount();
		LocalDateTime canceledAt = LocalDateTime.now();
		transaction.setCanceledAmount(canceledAmount);
		transaction.setStatus(canceledAmount >= transaction.getAmount()
				? MockTransactionStatus.CANCELED
				: MockTransactionStatus.PARTIAL_CANCELED);
		transaction.setCanceledAt(canceledAt);
		transactionStore.save(transaction);

		return ResponseEntity.ok(new CancelResponse(transaction.getPgTransactionId(), "0000", canceledAt));
	}

	public ResponseEntity<?> getTransaction(String pgTransactionId) {
		sleepRandomly(3000, 5000);

		MockTransaction transaction = transactionStore.findByTransactionId(pgTransactionId).orElse(null);
		if (transaction == null) {
			return errorResponse(HttpStatus.NOT_FOUND, "존재하지 않는 거래입니다.");
		}
		return ResponseEntity.ok(new TransactionStatusResponse(
				transaction.getPgTransactionId(), transaction.getStatus().name(), transaction.getApprovedAt()));
	}

	private void sleepRandomly(long minimumMillis, long maximumMillis) {
		long delayMillis = ThreadLocalRandom.current().nextLong(minimumMillis, maximumMillis + 1);
		sleepIfNeeded(delayMillis);
	}

	private ApprovalResponse toApprovalResponse(MockTransaction transaction) {
		return new ApprovalResponse(transaction.getPgTransactionId(), "0000", "OK", transaction.getApprovedAt());
	}

	private void sleepIfNeeded(long delayMillis) {
		if (delayMillis <= 0) {
			return;
		}
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private ResponseEntity<?> errorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ErrorBody(status.value(), message));
	}

	private record ErrorBody(int resultCode, String resultMessage) {
	}

}
