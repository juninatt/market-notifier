package se.pbt.mn.telegram.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper mapper = new ObjectMapper();

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
    void sendMessage_postsJsonToSendMessage_andCompletes() throws Exception {
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

        // verify JSON structure semantically
        JsonNode json = mapper.readTree(recorded.getBody().readUtf8());
        assertThat(json.get("chat_id").asLong()).isEqualTo(123L);
        assertThat(json.get("text").asString()).isEqualTo("hello");
        assertThat(json.get("parse_mode").asString()).isEqualTo("MarkdownV2");
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
    @DisplayName("Handles baseUrl without trailing slash correctly")
    void sendMessage_handlesBaseUrlWithoutTrailingSlash_andCompletes() throws Exception {
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
    void sendMessage_jsonEscapesQuotesAndBackslash() throws Exception {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "T");
        String text = "a \"quote\" and a backslash \\";

        // when
        StepVerifier.create(client.sendMessage(7L, text)).verifyComplete();

        // then
        var recorded = server.takeRequest();
        JsonNode json = mapper.readTree(recorded.getBody().readUtf8());

        assertThat(json.get("chat_id").asLong()).isEqualTo(7L);
        String expected = "a \"quote\" and a backslash \\";
        assertThat(json.get("text").asString()).isEqualTo(expected);
        assertThat(json.get("parse_mode").asString()).isEqualTo("MarkdownV2");
    }

    @ParameterizedTest(name = "Preserves Unicode sample → {0}")
    @MethodSource("unicodeSamples")
    @DisplayName("Preserves Unicode and emoji correctly (UTF-8 safe)")
    void sendMessage_preservesVariousUnicodeSamples(String sample) throws Exception {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "T");

        // when
        StepVerifier.create(client.sendMessage(1L, sample)).verifyComplete();

        // then
        var recorded = server.takeRequest();
        JsonNode json = mapper.readTree(recorded.getBody().readUtf8());
        assertThat(json.get("text").asString()).isEqualTo(sample);
    }

    @Test
    @DisplayName("sendFormattedMessage POSTs text as-is, without MarkdownV2 escaping")
    void sendFormattedMessage_sendsTextUnescaped() throws Exception {
        // given
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "TOKEN");
        String preFormatted = "*Bold title*\n[Läs mer](https://example.com)";

        // when
        StepVerifier.create(client.sendFormattedMessage(9L, preFormatted)).verifyComplete();

        // then
        var recorded = server.takeRequest();
        JsonNode json = mapper.readTree(recorded.getBody().readUtf8());
        assertThat(json.get("chat_id").asLong()).isEqualTo(9L);
        assertThat(json.get("text").asString()).isEqualTo(preFormatted);
        assertThat(json.get("parse_mode").asString()).isEqualTo("MarkdownV2");
    }

    @Test
    @DisplayName("sendFormattedMessage propagates error when server returns 5xx")
    void sendFormattedMessage_propagatesErrorOnServerFailure() {
        // given
        server.enqueue(new MockResponse().setResponseCode(500));
        TelegramApiClient client = new TelegramApiClient(server.url("/").toString(), "TOKEN");

        // when / then
        StepVerifier.create(client.sendFormattedMessage(1L, "*x*"))
                .expectError()
                .verify();
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
