package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.dto;

import java.util.List;

// Response DTO for GET /v1/agents from the core service
public record AgentsResponse(List<AgentItem> agents) {

    public record AgentItem(String code, String name, String subscriberId) {}
}
