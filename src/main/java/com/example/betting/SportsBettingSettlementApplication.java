package com.example.betting;

import com.example.betting.config.BettingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BettingProperties.class)
public class SportsBettingSettlementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsBettingSettlementApplication.class, args);
    }
}
