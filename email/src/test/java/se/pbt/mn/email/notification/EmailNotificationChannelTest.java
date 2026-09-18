package se.pbt.mn.email.notification;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.email.config.EmailApiProperties;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("EmailNotificationChannel")
class EmailNotificationChannelTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private MockWebServer server;
    private EmailNotificationChannel channel;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();

        var properties = new EmailApiProperties();
        properties.setFromAddress("news@example.com");

        channel = new EmailNotificationChannel(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("id() returns 'email'")
    void id_returnsEmail() {
        assertThat(channel.id()).isEqualTo("email");
    }

    @Test
    @DisplayName("Does nothing when the recipient address is blank")
    void send_withBlankRecipient_doesNothing() {
        channel.send("  ", new Notification("Title", "Body", null, null, null, null));

        assertThat(server.getRequestCount()).isZero();
    }

    @Nested
    @DisplayName("Request payload")
    class RequestPayload {

        @Test
        @DisplayName("POSTs from/to/subject/html to /emails")
        void send_postsExpectedPayload() throws Exception {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"abc\"}"));

            channel.send("user@outlook.com", new Notification("Tesla rallies", "Shares jump.", "https://example.com",
                    "Reuters", Instant.parse("2026-08-08T12:00:00Z"), List.of("TSLA")));

            var recorded = server.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("POST");
            assertThat(recorded.getPath()).isEqualTo("/emails");

            JsonNode json = mapper.readTree(recorded.getBody().readUtf8());
            assertThat(json.get("from").asString()).isEqualTo("news@example.com");
            assertThat(json.get("to").get(0).asString()).isEqualTo("user@outlook.com");
            assertThat(json.get("subject").asString()).contains("Tesla rallies");
            assertThat(json.get("html").asString()).contains("<h2>Tesla rallies</h2>");
            assertThat(json.get("html").asString()).contains("Shares jump.");
            assertThat(json.get("html").asString()).contains("TSLA");
            assertThat(json.get("html").asString()).contains("Reuters");
            assertThat(json.get("html").asString()).contains("https://example.com");
        }

        @Test
        @DisplayName("Escapes HTML special characters in dynamic text")
        void send_escapesHtmlInTitle() throws Exception {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"abc\"}"));

            channel.send("user@outlook.com", new Notification("<script>alert(1)</script>", null, null, null, null, null));

            var recorded = server.takeRequest();
            JsonNode json = mapper.readTree(recorded.getBody().readUtf8());
            assertThat(json.get("html").asString()).doesNotContain("<script>");
            assertThat(json.get("html").asString()).contains("&lt;script&gt;");
        }
    }

    @Test
    @DisplayName("When the API call fails, the exception does not propagate")
    void send_withApiFailure_doesNotPropagateException() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertDoesNotThrow(() ->
                channel.send("user@outlook.com", new Notification("Title", "Body", null, null, null, null)));
    }
}
