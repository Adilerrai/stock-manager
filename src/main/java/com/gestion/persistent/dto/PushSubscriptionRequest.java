package com.gestion.persistent.dto;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionRequest(@NotBlank String endpoint, @NotBlank String p256dh, @NotBlank String auth) {

    public record Retrait(@NotBlank String endpoint) {
    }
}
