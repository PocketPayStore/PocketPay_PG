package pocketpaystore.pocketpay_pg.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import pocketpaystore.pocketpay_pg.dto.ApprovalRequest;
import pocketpaystore.pocketpay_pg.dto.CancelRequest;
import pocketpaystore.pocketpay_pg.service.MockPgService;

@RestController
@RequestMapping("/mock-pg")
@RequiredArgsConstructor
public class MockPgController {

	private final MockPgService mockPgService;

	@PostMapping("/approve")
	public ResponseEntity<?> approve(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@RequestBody ApprovalRequest request
	) {
		return mockPgService.approve(idempotencyKey, request);
	}

	@PostMapping("/cancel")
	public ResponseEntity<?> cancel(@RequestBody CancelRequest request) {
		return mockPgService.cancel(request);
	}

	@GetMapping("/transactions/{txId}")
	public ResponseEntity<?> getTransaction(@PathVariable String txId) {
		return mockPgService.getTransaction(txId);
	}

}
