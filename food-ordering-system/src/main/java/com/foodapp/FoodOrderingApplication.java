package com.foodapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. @SpringBootApplication bundles three annotations:
 *  - @Configuration      : this class can define Spring beans
 *  - @EnableAutoConfiguration : Spring Boot auto-configures beans based on
 *                          what's on the classpath (e.g. sees postgres driver
 *                          + spring-data-jpa -> configures a DataSource + EntityManager)
 *  - @ComponentScan      : scans com.foodapp and sub-packages for
 *                          @Component/@Service/@Repository/@RestController beans
 *
 * @EnableScheduling turns on Spring's @Scheduled annotation support,
 * required for OrderCleanupScheduler to actually run.
 */
@SpringBootApplication
@EnableScheduling
public class FoodOrderingApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodOrderingApplication.class, args);
    }
}
