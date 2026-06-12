package org.example.websocket;

import jakarta.validation.Valid;
import java.util.UUID;
import org.example.dto.ActionMessage;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableLeaveMessage;
import org.example.dto.TableStateEvent;
import org.example.dto.WebSocketErrorEvent;
import org.example.entity.User;
import org.example.exception.ApiException;
import org.example.repository.UserRepository;
import org.example.security.JwtService;
import org.example.service.GameRoundService;
import org.example.service.TableService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class TableWebSocketController {

    private final TableService tableService;
    private final GameRoundService gameRoundService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public TableWebSocketController(
            TableService tableService,
            GameRoundService gameRoundService,
            UserRepository userRepository,
            JwtService jwtService) {
        this.tableService = tableService;
        this.gameRoundService = gameRoundService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @MessageMapping("/table.join")
    public TableStateEvent join(@Payload @Valid TableJoinMessage message, java.security.Principal principal) {
        User user = resolveUser(principal);
        return tableService.joinTable(user, message);
    }

    @MessageMapping("/table.leave")
    public TableStateEvent leave(@Payload @Valid TableLeaveMessage message, java.security.Principal principal) {
        User user = resolveUser(principal);
        return tableService.leaveTable(user, message);
    }

    @MessageMapping("/action.hit")
    public void hit(@Payload @Valid ActionMessage message, java.security.Principal principal) {
        User user = resolveUser(principal);
        gameRoundService.hit(user, message.tableId(), message.handId());
    }

    @MessageMapping("/action.stand")
    public void stand(@Payload @Valid ActionMessage message, java.security.Principal principal) {
        User user = resolveUser(principal);
        gameRoundService.stand(user, message.tableId(), message.handId());
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorEvent handleApiException(ApiException ex) {
        return new WebSocketErrorEvent("error", ex.getCode(), ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorEvent handleException(Exception ex) {
        return new WebSocketErrorEvent("error", "INTERNAL_ERROR", ex.getMessage());
    }

    private User resolveUser(java.security.Principal principal) {
        if (principal == null) {
            throw new ApiException("UNAUTHORIZED", "WebSocket authentication required");
        }
        UUID userId = jwtService.parseUserId(principal.getName());
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "User not found"));
    }
}
