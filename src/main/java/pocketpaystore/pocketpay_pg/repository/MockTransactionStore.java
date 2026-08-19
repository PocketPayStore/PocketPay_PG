package pocketpaystore.pocketpay_pg.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import pocketpaystore.pocketpay_pg.domain.MockTransaction;

@Component
public class MockTransactionStore {

	private final Map<String, MockTransaction> byTransactionId = new ConcurrentHashMap<>();
	private final Map<String, MockTransaction> byIdempotencyKey = new ConcurrentHashMap<>();

	public void save(MockTransaction transaction) {
		byTransactionId.put(transaction.getPgTransactionId(), transaction);
		if (transaction.getIdempotencyKey() != null) {
			byIdempotencyKey.put(transaction.getIdempotencyKey(), transaction);
		}
	}

	public Optional<MockTransaction> findByTransactionId(String pgTransactionId) {
		return Optional.ofNullable(byTransactionId.get(pgTransactionId));
	}

	public Optional<MockTransaction> findByIdempotencyKey(String idempotencyKey) {
		return Optional.ofNullable(byIdempotencyKey.get(idempotencyKey));
	}

}
