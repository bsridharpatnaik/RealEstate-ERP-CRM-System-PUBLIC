package com.ec.application.IDGenerator;

import com.ec.application.config.SpringContextHolder;
import com.ec.application.model.ProductCodeSequence;
import com.ec.application.repository.ProductCodeSequenceRepository;

import java.time.Year;

public class ProductCodeGenerator {

    private static ProductCodeSequenceRepository repository;

    private static ProductCodeSequenceRepository repo() {
        if (repository == null) {
            repository = SpringContextHolder.getBean(ProductCodeSequenceRepository.class);
        }
        return repository;
    }

    public static String nextProductCode() {
        String year = String.valueOf(Year.now().getValue()); // 2026
        String prefix = year + "MG";

        ProductCodeSequence seq = repo().findById(year)
                .orElseGet(() -> {
                    ProductCodeSequence s = new ProductCodeSequence();
                    s.setYear(year);
                    s.setLastNumber(0L);
                    return s;
                });

        seq.setLastNumber(seq.getLastNumber() + 1);
        repo().save(seq);

        return prefix + seq.getLastNumber(); // 2026MG1
    }
}
