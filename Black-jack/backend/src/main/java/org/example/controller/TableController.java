package org.example.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.GameStateEvent;
import org.example.dto.JoinTableRequest;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableLeaveMessage;
import org.example.dto.TableListResponse;
import org.example.dto.TableResponse;
import org.example.dto.TableStateEvent;
import org.example.entity.TableStatus;
import org.example.security.AuthSupport;
import org.example.service.GameRoundService;
import org.example.service.TableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final GameRoundService gameRoundService;

    public TableController(
            TableService tableService, AuthSupport authSupport, GameRoundService gameRoundService) {
        this.tableService = tableService;
        this.authSupport = authSupport;
        this.gameRoundService = gameRoundService;
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

    @GetMapping("/{tableId}/state")
    public TableStateEvent getTableState(@PathVariable UUID tableId) {
        return tableService.getTableState(tableId);
    }

    @GetMapping("/{tableId}/game-state")
    public ResponseEntity<GameStateEvent> getGameState(@PathVariable UUID tableId) {
        GameStateEvent state = gameRoundService.getGameState(tableId);
        if (state == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{tableId}/join")
    public TableStateEvent joinTable(
            @PathVariable UUID tableId, @Valid @RequestBody JoinTableRequest request) {
        return tableService.joinTable(
                authSupport.currentUser(),
                new TableJoinMessage(tableId, request.seatIndex()));
    }

    @DeleteMapping("/{tableId}/leave")
    public TableStateEvent leaveTable(@PathVariable UUID tableId) {
        return tableService.leaveTable(
                authSupport.currentUser(), new TableLeaveMessage(tableId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse createTable(@Valid @RequestBody CreateTableRequest request) {
        authSupport.currentUser();
        return tableService.createTable(request);
    }
}
