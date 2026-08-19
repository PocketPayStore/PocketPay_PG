package pocketpaystore.pocketpay_pg.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.ObjectMapper;

import pocketpaystore.pocketpay_pg.dto.ApprovalResponse;

@Slf4j
@Component
public class WebhookSender {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String webhookUrl;
	private final String webhookSecret;

	public WebhookSender(ObjectMapper objectMapper,
						  @Value("${pocketpay-core.webhook-url:http://localhost:8080/api/webhooks/pg}") String webhookUrl,
						  @Value("${mock-pg.webhook.secret:mock-pg-webhook-secret}") String webhookSecret) {
		this.restClient = RestClient.create();
		this.objectMapper = objectMapper;
		this.webhookUrl = webhookUrl;
		this.webhookSecret = webhookSecret;
	}

	@Async
	public void sendApproved(ApprovalResponse response) {
		try {
			String payload = objectMapper.writeValueAsString(response);
			restClient.post()
					.uri(webhookUrl)
					.header("X-PG-Signature", sign(payload))
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
		} catch (Exception e) {
			log.error("[MockPg] 웹훅 발송 실패(best-effort): pgTransactionId={}", response.getPgTransactionId(), e);
		}
	}

	private String sign(String payload) throws Exception {
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
		byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(computed);
	}

}
