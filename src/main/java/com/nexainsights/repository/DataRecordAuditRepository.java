package com.nexainsights.repository;

import com.nexainsights.model.DataRecordAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataRecordAuditRepository extends MongoRepository<DataRecordAudit, String> {
    List<DataRecordAudit> findByDatasetIdOrderByTimestampDesc(String datasetId);
    List<DataRecordAudit> findByDatasetIdAndRowNumberOrderByTimestampDesc(String datasetId, int rowNumber);
    long countByDatasetId(String datasetId);
}
