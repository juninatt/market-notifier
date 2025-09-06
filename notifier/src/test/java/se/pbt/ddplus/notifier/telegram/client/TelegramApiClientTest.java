package se.pbt.ddplus.notifier.telegram.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelegramApiClient")
class TelegramApiClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("POSTs correct JSON to /bot{token}/sendMessage and completes")
    void sendMessage_postsJsonToSendMessage_andCompletes() throws InterruptedException {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        String baseUrl = server.url("/").toString(); // ends with '/'
        String token = "TESTTOKEN";
        TelegramApiClient client = new TelegramApiClient(baseUrl, token);

        // when / then
        StepVerifier.create(client.sendMessage(123L, "hello"))
                .verifyComplete();

        // verify request
        var recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/bot" + token + "/sendMessage");
        assertThat(recorded.getHeader("Content-Type")).contains("application/json");

        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"chat_id\":123");
        assertThat(body).contains("\"text\":\"hello\"");
        assertThat(body).contains("\"parse_mode\":\"MarkdownV2\"");
    }

    @Test
    @DisplayName("Propagates error when server returns 5xx")
    void sendMessage_propagatesErrorOnServerFailure() {
        // given
        server.enqueue(new MockResponse().setResponseCode(500));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "TOKEN");

        // when / then
        StepVerifier.create(client.sendMessage(1L, "x"))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Works when baseUrl lacks a trailing slash")
    void sendMessage_handlesBaseUrlWithoutTrailingSlash_andCompletes() throws InterruptedException {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        String baseUrlWithSlash = server.url("/").toString();
        String baseUrlNoSlash = baseUrlWithSlash.substring(0, baseUrlWithSlash.length() - 1);
        String token = "TOKEN";
        TelegramApiClient client = new TelegramApiClient(baseUrlNoSlash, token);

        // when
        StepVerifier.create(client.sendMessage(42L, "hi")).verifyComplete();

        // then
        var recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/bot" + token + "/sendMessage");
    }

    @Test
    @DisplayName("Properly escapes quotes and backslashes in JSON body")
    void sendMessage_jsonEscapesQuotesAndBackslash() throws InterruptedException {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "T");
        String text = "a \"quote\" and a backslash \\";

        // when
        StepVerifier.create(client.sendMessage(7L, text)).verifyComplete();

        // then
        var recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"text\":\"a \\\"quote\\\" and a backslash \\\\\"");
    }


    @ParameterizedTest(name = "Preserves Unicode sample → {0}")
    @MethodSource("unicodeSamples")
    @DisplayName("Preserves Unicode/emoji (UTF-8) across scripts and sequences")
    void sendMessage_preservesVariousUnicodeSamples(String sample) throws InterruptedException {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "T");

        // when
        StepVerifier.create(client.sendMessage(1L, sample)).verifyComplete();

        // then
        var recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains(sample);
    }

    static Stream<String> unicodeSamples() {
        return Stream.of(
                "Hej åäö 😀",                 // Swedish + emoji
                "Café naïve coöperate",       // Latin with diacritics
                "Привет мир",                 // Cyrillic
                "γειά σου κόσμε",             // Greek
                "שלום עולם",                  // Hebrew (RTL)
                "مرحبا بالعالم",              // Arabic (RTL)
                "नमस्ते दुनिया",              // Devanagari
                "你好，世界",                    // Chinese
                "こんにちは世界",                 // Japanese
                "👨‍💻👩🏽‍💻",                   // ZWJ + skin tone
                "🇸🇪🇺🇳",                        // Regional indicators
                "e\u0301 vs é"               // Combining mark vs precomposed
        );
    }
}
