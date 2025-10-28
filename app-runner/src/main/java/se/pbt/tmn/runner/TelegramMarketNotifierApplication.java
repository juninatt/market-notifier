package se.pbt.tmn.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the Telegram Market Notifier application (TVM).
 */

@SpringBootApplication
@ComponentScan(basePackages = "se.pbt")
public class TelegramMarketNotifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelegramMarketNotifierApplication.class, args);
    }
}
