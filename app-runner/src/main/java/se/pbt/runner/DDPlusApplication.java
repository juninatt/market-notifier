package se.pbt.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import se.pbt.core.StarterCore;

@SpringBootApplication
public class DDPlusApplication {
    public static void main(String[] args) {
        SpringApplication.run(DDPlusApplication.class, args);

        StarterCore greetingService = new StarterCore();
        System.out.println(greetingService.getGreeting());
    }
}

