package com.example.des.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.des.model.Company;

@Repository
public class CompanyRepository {

    private final Map<String, Company> storage = new ConcurrentHashMap<>();

    public Company save(Company company) {
        storage.put(company.getId(), company);
        return company;
    }

    public Optional<Company> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Collection<Company> findAll() {
        return storage.values();
    }
}
