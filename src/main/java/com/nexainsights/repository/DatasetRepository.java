package com.nexainsights.repository;

import com.nexainsights.model.Dataset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends MongoRepository<Dataset, String> {
    List<Dataset> findByProjectId(String projectId);
    long countByProjectId(String projectId);
}
