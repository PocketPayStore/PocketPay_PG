package pocketpaystore.pocketpay_pg.service;

import org.springframework.http.HttpStatus;

record FaultTrigger(HttpStatus httpStatus, long delayMillis) {

	private static final long TIMEOUT_DELAY_MILLIS = 5000;

	static FaultTrigger from(String key) {
		if (key == null) {
			return none();
		}
		if (key.startsWith("FAIL_400")) {
			return new FaultTrigger(HttpStatus.BAD_REQUEST, 0);
		}
		if (key.startsWith("FAIL_500")) {
			return new FaultTrigger(HttpStatus.INTERNAL_SERVER_ERROR, 0);
		}
		if (key.startsWith("TIMEOUT")) {
			return new FaultTrigger(null, TIMEOUT_DELAY_MILLIS);
		}
		if (key.startsWith("DELAY_")) {
			return new FaultTrigger(null, parseDelay(key));
		}
		return none();
	}

	private static long parseDelay(String key) {
		try {
			return Long.parseLong(key.substring("DELAY_".length()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static FaultTrigger none() {
		return new FaultTrigger(null, 0);
	}

}
