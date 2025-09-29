package se.pbt.ddplus.notifier.telegram.polling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;
import se.pbt.ddplus.notifier.telegram.client.TelegramApiClient;
import se.pbt.ddplus.notifier.telegram.config.TelegramProperties;
import se.pbt.ddplus.notifier.telegram.model.TelegramCommand;
import se.pbt.ddplus.notifier.telegram.service.TelegramService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TelegramLongPollingRunner:")
class TelegramLongPollingRunnerTest {

    private static final String BOT_TOKEN = "token";
    private static final String TELEGRAM_BASE_URL = "https://api.telegram.org";

    private static final long AWAIT_SHORT_MS = 1000;
    private static final long AWAIT_RECOVERY_MS = 2500;

    private TelegramProperties props;
    private TelegramService telegramService;
    private TelegramApiClient apiClient;
    private TelegramLongPollingRunner runner;

    final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        props = new TelegramProperties();
        props.setEnabled(true);
        props.setBotToken(BOT_TOKEN);
        props.setBaseUrl(TELEGRAM_BASE_URL);
        props.setLongPollTimeoutSeconds(1);
        props.setInitialOffset(0);

        telegramService = mock(TelegramService.class);
        apiClient = mock(TelegramApiClient.class);

        runner = new TelegramLongPollingRunner(props, telegramService, apiClient);
    }

    @AfterEach
    void tearDown() {
        if (runner != null && runner.isRunning()) runner.stop();
    }


    @Nested
    @DisplayName("start() & stop():")
    class LifecycleTests {

        @Test
        @DisplayName("Starts a worker when enabled and token present")
        void startStartsWorker() throws Exception {
            CountDownLatch called = new CountDownLatch(1);
            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer((Answer<Mono<JsonNode>>) inv -> {
                        called.countDown();
                        // Return empty result so the loop continues
                        return okResult("[]");
                    });

            runner.start();
            assertTrue(runner.isRunning(), "runner should be running after start");
            assertTrue(called.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS), "getUpdates should be called at least once");

            runner.stop();
            assertFalse(runner.isRunning(), "runner should not be running after stop");
        }

        @Test
        @DisplayName("Does nothing when disabled")
        void startDoesNothingWhenDisabled() {
            props.setEnabled(false);
            TelegramLongPollingRunner disabled = new TelegramLongPollingRunner(props, telegramService, apiClient);

            disabled.start();
            assertFalse(disabled.isRunning(), "should remain stopped when disabled");
            verify(apiClient, never()).getUpdates(anyInt(), anyLong());
        }

        @Test
        @DisplayName("Does nothing when token is missing")
        void startDoesNothingWhenTokenMissing() {
            props.setBotToken("");
            TelegramLongPollingRunner missingToken = new TelegramLongPollingRunner(props, telegramService, apiClient);

            missingToken.start();
            assertFalse(missingToken.isRunning(), "should remain stopped when token is missing");
            verify(apiClient, never()).getUpdates(anyInt(), anyLong());
        }
    }


    // Polling behavior

    @Nested
    @DisplayName("pollLoop():")
    class PollingBehaviorTests {

        @Test
        @DisplayName("delivers TelegramCommand to service for valid text message and advances offset")
        void deliversCommandAndAdvancesOffset() throws Exception {
            CountDownLatch serviceCalled = new CountDownLatch(1);
            CountDownLatch advancedOffset = new CountDownLatch(1);

            doAnswer(inv -> { serviceCalled.countDown(); return null; })
                    .when(telegramService).handleTelegramCommand(any(TelegramCommand.class));

            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer((Answer<Mono<JsonNode>>) inv -> {
                        long offset = inv.getArgument(1, Long.class);
                        if (offset == 0L) {
                            return okResult("""
                          [{
                            "update_id": 100,
                            "message": { "chat": { "id": 12345 }, "text": "Hello" }
                          }]
                          """);
                        } else if (offset >= 101L) {
                            advancedOffset.countDown();
                            return okResult("[]");
                        }
                        return okResult("[]");
                    });

            runner.start();

            assertTrue(serviceCalled.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS), "Service should be called for the text message");
            assertTrue(advancedOffset.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS), "Runner should poll again with offset=101");

            ArgumentCaptor<TelegramCommand> cmdCap = ArgumentCaptor.forClass(TelegramCommand.class);
            verify(telegramService, times(1)).handleTelegramCommand(cmdCap.capture());
            TelegramCommand cmd = cmdCap.getValue();
            assertEquals(12345L, cmd.chatId());
            assertEquals("Hello", cmd.message());
        }

        @Test
        @DisplayName("does not call service when message has no text but still advances offset")
        void noTextNoServiceButOffsetAdvances() throws Exception {
            CountDownLatch advancedOffset = new CountDownLatch(1);

            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer((Answer<Mono<JsonNode>>) inv -> {
                        long offset = inv.getArgument(1, Long.class);
                        if (offset == 0L) {
                            return okResult("""
                          [{
                            "update_id": 200,
                            "message": { "chat": { "id": 99999 } }
                          }]
                          """);
                        } else if (offset >= 201L) {
                            advancedOffset.countDown();
                            return okResult("[]");
                        }
                        return okResult("[]");
                    });

            runner.start();

            assertTrue(advancedOffset.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS),
                    "Runner should poll again with offset=201 after processing update_id=200");

            verify(telegramService, never()).handleTelegramCommand(any());
        }

        @Test
        @DisplayName("ignores result.ok=false responses and keeps polling")
        void ignoresNotOkResponses() throws Exception {
            CountDownLatch secondPoll = new CountDownLatch(1);

            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer(new Answer<Mono<JsonNode>>() {
                        int calls = 0;

                        @Override
                        public Mono<JsonNode> answer(org.mockito.invocation.InvocationOnMock inv) throws Throwable {
                            calls++;
                            if (calls == 1) {
                                return notOkResult();
                            } else {
                                secondPoll.countDown();
                                return okResult("[]");
                            }
                        }
                    });

            runner.start();
            assertTrue(secondPoll.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS),
                    "Runner should continue polling after not-ok response");
        }

        @Test
        @DisplayName("tolerates errors from apiClient and continues polling")
        void toleratesErrorsAndContinues() throws Exception {
            CountDownLatch recovered = new CountDownLatch(1);

            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer(new Answer<Mono<JsonNode>>() {
                        int calls = 0;

                        @Override
                        public Mono<JsonNode> answer(org.mockito.invocation.InvocationOnMock inv) throws Throwable {
                            calls++;
                            if (calls == 1) {
                                return Mono.error(new RuntimeException("boom"));
                            } else {
                                recovered.countDown();
                                return okResult("[]");
                            }
                        }
                    });

            runner.start();

            assertTrue(recovered.await(AWAIT_RECOVERY_MS, TimeUnit.MILLISECONDS),
                    "Runner should recover and call getUpdates again after an error");
        }

        @Test
        @DisplayName("handles multiple updates in one response (mix of valid/invalid) and advances offset to latest+1")
        void handlesMultipleUpdatesAndAdvancesToLatestPlusOne() throws Exception {
            CountDownLatch handledTwo = new CountDownLatch(2);
            CountDownLatch advancedOffset = new CountDownLatch(1);

            doAnswer(inv -> { handledTwo.countDown(); return null; })
                    .when(telegramService).handleTelegramCommand(any(TelegramCommand.class));

            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer((Answer<Mono<JsonNode>>) inv -> {
                        long offset = inv.getArgument(1, Long.class);
                        if (offset == 0L) {
                            return okResult("""
                          [
                            { "update_id": 9,  "message": { "chat": { "id": 7 } } },
                            { "update_id": 10, "message": { "chat": { "id": 1 }, "text": "A" } },
                            { "update_id": 12, "message": { "chat": { "id": 2 }, "text": "B" } }
                          ]
                          """);
                        } else if (offset >= 13L) { // latest+1 (12+1)
                            advancedOffset.countDown();
                            return okResult("[]");
                        }
                        return okResult("[]");
                    });

            runner.start();

            assertTrue(handledTwo.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS),
                    "Both valid updates should produce commands");
            assertTrue(advancedOffset.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS),
                    "Runner should advance to latest+1 and poll with offset=13");

            ArgumentCaptor<TelegramCommand> cmdCap = ArgumentCaptor.forClass(TelegramCommand.class);
            verify(telegramService, times(2)).handleTelegramCommand(cmdCap.capture());
            assertEquals(1L, cmdCap.getAllValues().get(0).chatId());
            assertEquals("A", cmdCap.getAllValues().get(0).message());
            assertEquals(2L, cmdCap.getAllValues().get(1).chatId());
            assertEquals("B", cmdCap.getAllValues().get(1).message());
        }
    }


    @Nested
    @DisplayName("Start conditions:")
    class StartConditionsParamTests {

        @ParameterizedTest(name = "[{index}] enabled={0}, token='{1}' -> starts={2}")
        @MethodSource("startInputs")
        @DisplayName("runner starts only when enabled and token is present")
        void startOnlyWhenEnabledAndToken(boolean enabled, String token, boolean shouldStart) throws Exception {
            props.setEnabled(enabled);
            props.setBotToken(token);

            CountDownLatch called = new CountDownLatch(1);
            when(apiClient.getUpdates(anyInt(), anyLong()))
                    .thenAnswer((Answer<Mono<JsonNode>>) inv -> {
                        called.countDown();
                        return okResult("[]");
                    });

            runner = new TelegramLongPollingRunner(props, telegramService, apiClient);
            runner.start();

            assertEquals(shouldStart, runner.isRunning(), "running state mismatch");

            if (shouldStart) {
                assertTrue(called.await(AWAIT_SHORT_MS, TimeUnit.MILLISECONDS), "getUpdates should be called");
                runner.stop();
                assertFalse(runner.isRunning());
            } else {
                verify(apiClient, never()).getUpdates(anyInt(), anyLong());
            }
        }

        static Stream<Arguments> startInputs() {
            return Stream.of(
                    Arguments.of(true, BOT_TOKEN, true),
                    Arguments.of(true, "", false),
                    Arguments.of(true, "   ", false),
                    Arguments.of(true, null, false),
                    Arguments.of(false, BOT_TOKEN, false),
                    Arguments.of(false, "", false)
            );
        }
    }

    //  Helpers

    /**
     * Provides a minimal successful Telegram response to focus tests on poll-loop behavior
     * rather than JSON boilerplate.
     */
    private Mono<JsonNode> okResult(String resultArrayJson) throws Exception {
        String json = """
          {
            "ok": true,
            "result": %s
          }
          """.formatted(resultArrayJson);
        return Mono.just(mapper.readTree(json));
    }

    /**
     * Provides a minimal unsuccessful Telegram response to focus tests on poll-loop behavior
     * rather than JSON boilerplate.
     */
    private Mono<JsonNode> notOkResult() throws Exception {
        String json = """
          {
            "ok": false,
            "result": []
          }
          """;
        return Mono.just(mapper.readTree(json));
    }
}
