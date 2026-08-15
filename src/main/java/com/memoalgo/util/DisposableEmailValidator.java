package com.memoalgo.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class DisposableEmailValidator {

    private Set<String> blockedDomains = Collections.emptySet();

    @PostConstruct
    void loadBlocklist(){
        Set<String> domains = new HashSet<>();
        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new ClassPathResource("disposable_email_blocklist.conf").getInputStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            String line;
            while((line = reader.readLine()) != null){
                String trimmed = line.trim().toLowerCase();
                if(!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    domains.add(trimmed);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load disposable email blocklist - "+ "all emails will be allowed through", e);
        }

        blockedDomains = Collections.unmodifiableSet(domains);
        log.info("Loaded {} disposable email domains into blocklist", blockedDomains.size());
    }

    public boolean isDisposable(String email) {
        if(email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
        return blockedDomains.contains(domain);
    }
}
