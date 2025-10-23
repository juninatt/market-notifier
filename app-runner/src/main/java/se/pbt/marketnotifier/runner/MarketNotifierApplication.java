package se.pbt.marketnotifier.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the Market Notifier application.
 * Boots Spring and scans all modules (core, newsprovider, notifier, scheduler).
 */

@SpringBootApplication
@ComponentScan(basePackages = "se.pbt")
public class MarketNotifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketNotifierApplication.class, args);
    }
}
