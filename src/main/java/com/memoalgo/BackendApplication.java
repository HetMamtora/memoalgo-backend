package com.memoalgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * MemoAlgo — DSA Revision Platform
 *
 * @EnableJpaAuditing enables automatic population of
 * @CreatedDate and @LastModifiedDate fields on JPA entities.
 * This is wired up here once and works across the entire application.
 */
@SpringBootApplication
@EnableJpaAuditing
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
