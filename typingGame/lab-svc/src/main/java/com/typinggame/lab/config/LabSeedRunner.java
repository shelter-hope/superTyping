package com.typinggame.lab.config;

import com.typinggame.lab.service.LabCatalogService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LabSeedRunner implements ApplicationRunner {
    private final LabCatalogService catalog;

    public LabSeedRunner(LabCatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public void run(ApplicationArguments args) {
        catalog.ensureSeed();
    }
}
