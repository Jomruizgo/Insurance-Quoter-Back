package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.dto;

import java.util.List;

// Response DTO for GET /v1/subscribers from the core service
public record SubscribersResponse(List<SubscriberItem> subscribers) {

    public record SubscriberItem(String id, String name) {}
}
