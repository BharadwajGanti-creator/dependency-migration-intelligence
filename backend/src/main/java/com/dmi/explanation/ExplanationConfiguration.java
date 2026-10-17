package com.dmi.explanation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExplanationProperties.class)
public class ExplanationConfiguration {}
