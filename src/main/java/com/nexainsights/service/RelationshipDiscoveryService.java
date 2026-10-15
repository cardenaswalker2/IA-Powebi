package com.nexainsights.service;

import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.Dataset;
import com.nexainsights.model.DatasetRelationship;
import com.nexainsights.repository.DatasetRelationshipRepository;
import com.nexainsights.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RelationshipDiscoveryService {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DatasetRelationshipRepository relationshipRepository;

    /**
     * Descubre coincidencias y claves compartidas entre datasets dentro de un proyecto
     */
    public List<DatasetRelationship> discoverRelationships(String projectId) {
        List<Dataset> datasets = datasetRepository.findByProjectId(projectId);
        List<DatasetRelationship> proposedRelationships = new ArrayList<>();

        if (datasets.size() < 2) {
            return proposedRelationships;
        }

        for (int i = 0; i < datasets.size(); i++) {
            Dataset ds1 = datasets.get(i);
            for (int j = i + 1; j < datasets.size(); j++) {
                Dataset ds2 = datasets.get(j);

                for (ColumnDefinition col1 : ds1.getColumns()) {
                    for (ColumnDefinition col2 : ds2.getColumns()) {
                        // Comprobar coincidencia por nombre o semántica
                        String n1 = normalizeName(col1.getOriginalName());
                        String n2 = normalizeName(col2.getOriginalName());

                        boolean nameMatch = n1.equals(n2) || n1.contains(n2) || n2.contains(n1);
                        boolean typeMatch = col1.getEffectiveType() == col2.getEffectiveType();

                        if (nameMatch && typeMatch) {
                            DatasetRelationship rel = new DatasetRelationship(
                                    projectId,
                                    ds1.getId(), ds1.getName(), col1.getKey(), col1.getOriginalName(),
                                    ds2.getId(), ds2.getName(), col2.getKey(), col2.getOriginalName()
                            );

                            rel.setRelationshipType((col1.isIdentifier() && col2.isIdentifier()) ? "ONE_TO_ONE" : "ONE_TO_MANY");
                            rel.setMatchConfidence(n1.equals(n2) ? 0.95 : 0.75);
                            proposedRelationships.add(rel);
                        }
                    }
                }
            }
        }

        // Guardar o actualizar relaciones propuestas
        for (DatasetRelationship rel : proposedRelationships) {
            relationshipRepository.save(rel);
        }

        return proposedRelationships;
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replace("codigo", "id")
                .replace("identificador", "id");
    }
}
