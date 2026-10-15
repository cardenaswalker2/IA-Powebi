package com.nexainsights.controller;

import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/explorer")
public class DataExplorerController {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping
    public String showExplorer(@RequestParam(required = false) String datasetId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "15") int size,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String columnFilter,
                               @RequestParam(required = false) String filterValue,
                               @RequestParam(defaultValue = "rowNumber") String sortBy,
                               @RequestParam(defaultValue = "asc") String sortDir,
                               Model model) {

        List<Dataset> datasets = datasetRepository.findAll();
        if (datasets.isEmpty()) {
            return "redirect:/import";
        }

        Dataset selectedDataset;
        if (datasetId != null && !datasetId.isEmpty()) {
            selectedDataset = datasetRepository.findById(datasetId).orElse(datasets.get(0));
        } else {
            selectedDataset = datasets.get(0);
        }

        // Búsqueda filtrada o paginada en MongoDB
        Query query = new Query(Criteria.where("datasetId").is(selectedDataset.getId()));

        if (search != null && !search.trim().isEmpty()) {
            Pattern regex = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
            List<Criteria> searchCriteria = new ArrayList<>();
            for (ColumnDefinition col : selectedDataset.getColumns()) {
                searchCriteria.add(Criteria.where("data." + col.getKey()).regex(regex));
            }
            if (!searchCriteria.isEmpty()) {
                query.addCriteria(new Criteria().orOperator(searchCriteria.toArray(new Criteria[0])));
            }
        }

        if (columnFilter != null && !columnFilter.isEmpty() && filterValue != null && !filterValue.isEmpty()) {
            Pattern regex = Pattern.compile(Pattern.quote(filterValue.trim()), Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("data." + columnFilter).regex(regex));
        }

        long totalFiltered = mongoTemplate.count(query, DataRecord.class);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = sortBy.equals("rowNumber") ? "rowNumber" : ("data." + sortBy);

        query.with(PageRequest.of(page, size, Sort.by(direction, sortProperty)));
        List<DataRecord> records = mongoTemplate.find(query, DataRecord.class);

        int totalPages = (int) Math.ceil((double) totalFiltered / size);

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDataset", selectedDataset);
        model.addAttribute("records", records);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalFiltered", totalFiltered);
        model.addAttribute("search", search);
        model.addAttribute("columnFilter", columnFilter);
        model.addAttribute("filterValue", filterValue);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "explorer/index";
    }
}
