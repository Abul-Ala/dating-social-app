package com.datingsocial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatingSocialApplication {

    public static void main(String[] args) {
        // Automatic JDBC URL adaptation for cloud providers (Railway, Heroku, etc.)
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("postgres://")) {
                System.setProperty("spring.datasource.url", "jdbc:postgresql://" + databaseUrl.substring("postgres://".length()));
            } else if (databaseUrl.startsWith("postgresql://")) {
                System.setProperty("spring.datasource.url", "jdbc:" + databaseUrl);
            }
        }
        SpringApplication.run(DatingSocialApplication.class, args);
    }
}
