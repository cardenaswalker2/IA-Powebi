package com.nexainsights.repository;

import com.nexainsights.model.DashboardConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardConfigRepository extends MongoRepository<DashboardConfig, String> {
    Optional<DashboardConfig> findByDatasetId(String datasetId);
    void deleteByDatasetId(String datasetId);
}
