package com.nexainsights.repository;

import com.nexainsights.model.DataRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataRecordRepository extends MongoRepository<DataRecord, String> {
    Page<DataRecord> findByDatasetId(String datasetId, Pageable pageable);
    List<DataRecord> findTop50ByDatasetId(String datasetId);
    void deleteByDatasetId(String datasetId);
    long countByDatasetId(String datasetId);
}
