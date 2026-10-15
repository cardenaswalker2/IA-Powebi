package com.nexainsights.repository;

import com.nexainsights.model.DatasetRelationship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRelationshipRepository extends MongoRepository<DatasetRelationship, String> {
    List<DatasetRelationship> findByProjectId(String projectId);
    void deleteByProjectId(String projectId);
}
