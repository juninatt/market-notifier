package se.pbt.ddplus.notifier.telegram.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.pbt.ddplus.notifier.telegram.config.TelegramConfig.escapeMarkdownV2;

@DisplayName("TelegramConfig:")
class TelegramConfigTest {

    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("cases")
    @DisplayName("Escapes text correctly for MarkdownV2")
    void escapeCases(String label, String input, String expected) { // Label is needed
        assertEquals(expected, escapeMarkdownV2(input));
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of("Null → empty string", null, ""),
                Arguments.of("Empty → empty string", "", ""),

                Arguments.of("No specials unchanged", "Hello World", "Hello World"),
                Arguments.of("Digits and letters unchanged", "abc123", "abc123"),

                Arguments.of(
                        "All specials are escaped",
                        "_*[]()~`>#+-=|{}.!\\",
                        "\\_\\*\\[\\]\\(\\)\\~\\`\\>\\#\\+\\-\\=\\|\\{\\}\\.\\!\\\\"
                ),

                Arguments.of(
                        "Mixed text with percent",
                        "Price is 10%_off_*today*",
                        "Price is 10%\\_off\\_\\*today\\*"
                ),

                Arguments.of(
                        "Backslash is escaped",
                        "Path: C:\\Users\\",
                        "Path: C:\\\\Users\\\\"
                ),

                Arguments.of(
                        "Already escaped underscore becomes triple-backslash + underscore",
                        "\\_Hello\\_",
                        "\\\\\\_Hello\\\\\\_"
                )
        );
    }
}
