package org.example.config;

import org.example.repository.GameTableRepository;
import org.example.service.TableService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {

    @Bean
    @Profile("!test")
    CommandLineRunner seedTables(GameTableRepository gameTableRepository, TableService tableService) {
        return args -> {
            if (gameTableRepository.count() > 0) {
                return;
            }
            org.example.dto.CreateTableRequest table1 =
                    new org.example.dto.CreateTableRequest("Stół VIP", 2, 10, 500);
            org.example.dto.CreateTableRequest table2 =
                    new org.example.dto.CreateTableRequest("Stół Classic", 2, 5, 200);
            tableService.createTable(table1);
            tableService.createTable(table2);
        };
    }
}
