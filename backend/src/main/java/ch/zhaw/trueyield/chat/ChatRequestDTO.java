package ch.zhaw.trueyield.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "Message must not be blank")
        String message
) {
}
