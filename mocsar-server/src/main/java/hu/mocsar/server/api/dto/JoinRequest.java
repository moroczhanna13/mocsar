package hu.mocsar.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(@NotBlank @Size(max = 32) String nickname) {
}
