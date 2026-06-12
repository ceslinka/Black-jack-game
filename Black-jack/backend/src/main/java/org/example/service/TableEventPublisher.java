package org.example.service;

import org.example.dto.RoundCannotStartEvent;
import org.example.dto.TableStateEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TableEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public TableEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishRoundCannotStart(RoundCannotStartEvent event) {
        publishToTable(event.tableId(), event);
    }

    public void publishToTable(UUID tableId, Object event) {
        messagingTemplate.convertAndSend("/topic/table/" + tableId, event);
    }

    public void publishTableState(TableStateEvent event) {
        publishToTable(event.tableId(), event);
        messagingTemplate.convertAndSend("/topic/lobby", event);
    }
}
