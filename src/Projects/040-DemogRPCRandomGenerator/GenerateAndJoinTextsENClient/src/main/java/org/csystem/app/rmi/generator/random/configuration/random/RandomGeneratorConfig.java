package org.csystem.app.rmi.generator.random.configuration.random;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.random.RandomGenerator;

@Configuration
public class RandomGeneratorConfig {
    @Bean
    public RandomGenerator createRandomGenerator()
    {
        return new Random();
    }
}
