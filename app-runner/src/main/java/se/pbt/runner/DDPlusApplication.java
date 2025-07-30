package se.pbt.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "se.pbt")
public class DDPlusApplication {
    public static void main(String[] args) {
        SpringApplication.run(DDPlusApplication.class, args);
    }
}
