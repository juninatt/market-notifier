package se.pbt.ddplus.notifier.telegram.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Represents an inbound Telegram message used by the application to process input
 * and decide how to respond. It carries the target chat and the raw text.
 *
 * @param chatId  Telegram chat identifier
 * @param message Raw text received from Telegram
 */
public record TelegramCommand(
        @Positive long chatId,
        @NotBlank String message
) {}
