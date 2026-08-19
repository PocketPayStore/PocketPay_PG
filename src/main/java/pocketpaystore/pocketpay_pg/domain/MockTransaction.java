package pocketpaystore.pocketpay_pg.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MockTransaction {

	private String pgTransactionId;
	private String idempotencyKey;
	private String orderNumber;
	private Long amount;
	private Long canceledAmount;
	private MockTransactionStatus status;
	private LocalDateTime approvedAt;
	private LocalDateTime canceledAt;

}
