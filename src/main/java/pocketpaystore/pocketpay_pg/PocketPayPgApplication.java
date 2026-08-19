package pocketpaystore.pocketpay_pg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PocketPayPgApplication {

	public static void main(String[] args) {
		SpringApplication.run(PocketPayPgApplication.class, args);
	}

}
