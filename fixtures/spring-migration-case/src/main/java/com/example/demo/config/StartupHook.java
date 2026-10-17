package com.example.demo.config;

import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StartupHook {

    @PostConstruct
    public void init() {
        // Runs after bean construction
    }
}
