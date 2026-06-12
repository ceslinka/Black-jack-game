package org.example.dto;

import java.util.List;
import java.util.UUID;

public record CardDealtEvent(String type, UUID tableId, String target, List<String> cards, boolean hidden) {}
