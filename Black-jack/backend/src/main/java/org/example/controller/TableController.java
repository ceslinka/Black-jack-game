package org.example.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.TableListResponse;
import org.example.dto.TableResponse;
import org.example.entity.TableStatus;
import org.example.security.AuthSupport;
import org.example.service.TableService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tables")
public class TableController {

    private final TableService tableService;
    private final AuthSupport authSupport;

    public TableController(TableService tableService, AuthSupport authSupport) {
        this.tableService = tableService;
        this.authSupport = authSupport;
    }

    @GetMapping
    public TableListResponse listTables(@RequestParam(required = false) TableStatus status) {
        List<TableResponse> tables = tableService.listTables(status);
        return new TableListResponse(tables);
    }

    @GetMapping("/{tableId}")
    public TableResponse getTable(@PathVariable UUID tableId) {
        return tableService.getTable(tableId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse createTable(@Valid @RequestBody CreateTableRequest request) {
        authSupport.currentUser();
        return tableService.createTable(request);
    }
}
