package se.pbt.tvm.telegram.service;

import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;
import se.pbt.tvm.core.subscription.SchedulePreset;
import se.pbt.tvm.core.subscription.TelegramSubscribeCommand;
import se.pbt.tvm.telegram.client.TelegramApiClient;
import se.pbt.tvm.telegram.config.TelegramMsgProperties;
import se.pbt.tvm.telegram.config.TelegramStorageProperties;
import se.pbt.tvm.telegram.format.TelegramInputParser;
import se.pbt.tvm.telegram.model.TelegramCommand;
import se.pbt.tvm.subscription.contract.SubscriptionMapper;
import se.pbt.tvm.subscription.model.Subscription;
import se.pbt.tvm.subscription.service.SubscriptionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TelegramService}.
 * <p>
 * Each nested class corresponds to a Telegram command or related scenario.
 * Test methods follow the format {@code scenario_withCondition_expectedBehavior()}.
 */
class TelegramServiceTest {

    private static final long CHAT_ID = 123L;
    private static final String TEST_STORAGE_PATH = "subscriptions/telegram-subscriptions.yml";

    private static TelegramMsgProperties messageProperties;
    private static TelegramStorageProperties storageProperties;

    private TelegramApiClient apiClient;
    private TelegramInputParser commandParser;
    private SubscriptionService subscriptionService;
    private SubscriptionMapper<TelegramSubscribeCommand> mapper;
    private TelegramService service;

    @BeforeAll
    static void init_static_resources() {
        storageProperties = new TelegramStorageProperties();
        storageProperties.setSubscriptions(TEST_STORAGE_PATH);
        messageProperties = new TelegramMsgProperties();
    }

    @BeforeEach
    void setup_mocks_and_service() {
        apiClient = mock(TelegramApiClient.class);
        commandParser = mock(TelegramInputParser.class);
        subscriptionService = mock(SubscriptionService.class);
        mapper = mock(SubscriptionMapper.class);

        when(apiClient.sendMessage(anyLong(), anyString())).thenReturn(Mono.empty());
        when(mapper.map(any(), any())).thenReturn(new Subscription());

        messageProperties.setHelp("HELP");
        messageProperties.setUnknownCommand("UNKNOWN");

        TelegramMsgProperties.SubscriptionMessage subMsg = new TelegramMsgProperties.SubscriptionMessage();
        subMsg.setSaved("SAVED");
        subMsg.setInvalidFormat("INVALID");
        messageProperties.setSubscriptionMessage(subMsg);

        TelegramMsgProperties.ManagementMessage mgmtMsg = new TelegramMsgProperties.ManagementMessage();
        mgmtMsg.setNone("NONE");
        mgmtMsg.setList("LIST: {0}");
        mgmtMsg.setRemoved("REMOVED");
        mgmtMsg.setNotFound("NOT_FOUND");
        messageProperties.setManagementMessage(mgmtMsg);

        TelegramMsgProperties.ErrorMessage errMsg = new TelegramMsgProperties.ErrorMessage();
        errMsg.setUnexpected("UNEXPECTED");
        messageProperties.setError(errMsg);

        service = new TelegramService(apiClient, commandParser, subscriptionService, mapper, messageProperties, storageProperties);
    }

    //  /help and /start command tests

    @Nested
    @DisplayName("/help and /start commands")
    class HelpCommandTests {

        @Test
        @DisplayName("When /help is received, bot replies with help text")
        void helpCommand_withValidInput_repliesWithHelpText() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/help"));
            verify(apiClient).sendMessage(CHAT_ID, "HELP");
        }

        @Test
        @DisplayName("When /start is received, bot replies with help text")
        void startCommand_withValidInput_repliesWithHelpText() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/start"));
            verify(apiClient).sendMessage(CHAT_ID, "HELP");
        }

        @Test
        @DisplayName("When input is empty, bot replies with unknown command message")
        void helpCommand_withEmptyInput_repliesWithUnknownMessage() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, ""));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }
    }

    //  /subscribe command tests

    @Nested
    @DisplayName("/subscribe command")
    class SubscribeCommandTests {

        @Test
        @DisplayName("When valid command is received, subscription is persisted and confirmation sent")
        void subscribeCommand_withValidInput_persistsSubscriptionAndRepliesSaved() {
            TelegramSubscribeCommand parsedCmd = new TelegramSubscribeCommand(
                    CHAT_ID, "en", 10, List.of("Tesla"), SchedulePreset.MORNING
            );
            Subscription mockSubscription = new Subscription();

            when(commandParser.parseSubscribeCommand(any())).thenReturn(parsedCmd);
            when(mapper.map(any(), any())).thenReturn(mockSubscription);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"Tesla\" en 10"));

            verify(subscriptionService).save(eq(mockSubscription), eq(TEST_STORAGE_PATH));
            verify(apiClient).sendMessage(CHAT_ID, "SAVED");
        }

        @Test
        @DisplayName("When command contains multiple keywords, all are included in subscription")
        void subscribeCommand_withMultipleKeywords_includesAllKeywordsInSubscription() {
            TelegramSubscribeCommand parsedCmd = new TelegramSubscribeCommand(
                    CHAT_ID, "en", 10, List.of("Tesla", "AI", "Nvidia"), SchedulePreset.MORNING
            );
            when(commandParser.parseSubscribeCommand(any())).thenReturn(parsedCmd);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"Tesla AI Nvidia\" en 10"));

            verify(mapper).map(argThat(cmd -> cmd.keywords().size() == 3), any());
            verify(apiClient).sendMessage(CHAT_ID, "SAVED");
        }

        @Test
        @DisplayName("When mapper throws exception, bot replies with 'unexpected' error message")
        void subscribeCommand_withMapperException_repliesUnexpectedError() {
            TelegramSubscribeCommand parsedCmd = new TelegramSubscribeCommand(
                    CHAT_ID, "en", 5, List.of("Tesla"), SchedulePreset.MORNING
            );
            when(commandParser.parseSubscribeCommand(any())).thenReturn(parsedCmd);
            when(mapper.map(any(), any())).thenThrow(new RuntimeException("Mapping failure"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"Tesla\" en 5"));

            verify(apiClient).sendMessage(CHAT_ID, "UNEXPECTED");
        }

        @Test
        @DisplayName("When language is null, subscription is still persisted successfully")
        void subscribeCommand_withNullLanguage_persistsSubscriptionSuccessfully() {
            TelegramSubscribeCommand parsedCmd = new TelegramSubscribeCommand(
                    CHAT_ID, null, 10, List.of("Tesla"), SchedulePreset.MORNING
            );
            Subscription mockSubscription = new Subscription();

            when(commandParser.parseSubscribeCommand(any())).thenReturn(parsedCmd);
            when(mapper.map(any(), any())).thenReturn(mockSubscription);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"Tesla\" 10"));

            verify(subscriptionService).save(eq(mockSubscription), eq(TEST_STORAGE_PATH));
            verify(apiClient).sendMessage(CHAT_ID, "SAVED");
        }

        @Test
        @DisplayName("When command format is invalid, bot replies with 'invalid' and 'help' messages")
        void subscribeCommand_withInvalidFormat_repliesInvalidAndHelp() {
            when(commandParser.parseSubscribeCommand(any())).thenThrow(new IllegalArgumentException("bad format"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe wrongformat"));

            verify(apiClient).sendMessage(eq(CHAT_ID), contains("INVALID"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("HELP"));
        }

        @Test
        @DisplayName("When runtime error occurs, bot replies with 'unexpected'")
        void subscribeCommand_withRuntimeError_repliesUnexpected() {
            when(commandParser.parseSubscribeCommand(any())).thenThrow(new RuntimeException("fail"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"Tesla\" en 10"));

            verify(apiClient).sendMessage(CHAT_ID, "UNEXPECTED");
        }

        @Test
        @DisplayName("When non-numeric maxItems is provided, bot replies with 'invalid' and 'help'")
        void subscribeCommand_withNonNumericMaxItems_repliesInvalidAndHelp() {
            when(commandParser.parseSubscribeCommand(any())).thenThrow(new IllegalArgumentException("bad number"));
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe Tesla en abc"));

            verify(apiClient).sendMessage(eq(CHAT_ID), contains("INVALID"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("HELP"));
        }

        @Test
        @DisplayName("When too few parameters are provided, bot replies with 'invalid' and 'help'")
        void subscribeCommand_withTooFewParameters_repliesInvalidAndHelp() {
            when(commandParser.parseSubscribeCommand(any())).thenThrow(new IllegalArgumentException("missing args"));
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe Tesla"));

            verify(apiClient).sendMessage(eq(CHAT_ID), contains("INVALID"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("HELP"));
        }

        @Test
        @DisplayName("When empty keyword is provided, bot replies with 'invalid' and 'help'")
        void subscribeCommand_withEmptyKeyword_repliesInvalidAndHelp() {
            when(commandParser.parseSubscribeCommand(any())).thenThrow(new IllegalArgumentException("empty keyword"));
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"\" en 10"));

            verify(apiClient).sendMessage(eq(CHAT_ID), contains("INVALID"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("HELP"));
        }
    }

    //  /list command tests

    @Nested
    @DisplayName("/list command")
    class ListCommandTests {

        @Test
        @DisplayName("When no subscriptions exist, bot replies with 'none'")
        void listCommand_withNoSubscriptions_repliesNone() {
            when(subscriptionService.listByChatId(CHAT_ID)).thenReturn(List.of());

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));

            verify(apiClient).sendMessage(CHAT_ID, "NONE");
        }

        @Test
        @DisplayName("When subscriptions exist, bot replies with formatted list")
        void listCommand_withExistingSubscriptions_repliesWithFormattedList() {
            when(subscriptionService.listByChatId(CHAT_ID)).thenReturn(List.of("sub1", "sub2"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));

            verify(apiClient).sendMessage(eq(CHAT_ID), contains("LIST:"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("sub1"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("sub2"));
        }

        @Test
        @DisplayName("When error occurs while listing, bot replies with 'unexpected'")
        void listCommand_withServiceError_repliesUnexpected() {
            when(subscriptionService.listByChatId(CHAT_ID)).thenThrow(new RuntimeException("fail"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));

            verify(apiClient).sendMessage(CHAT_ID, "UNEXPECTED");
        }

        @Test
        @DisplayName("When subscriptions contain special characters, they are rendered correctly")
        void listCommand_withSpecialCharacters_rendersCorrectly() {
            when(subscriptionService.listByChatId(CHAT_ID)).thenReturn(List.of("AI stocks", "Tesla 🚀"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));

            verify(apiClient).sendMessage(eq(CHAT_ID), contains("AI stocks"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("Tesla 🚀"));
        }

        @Test
        @DisplayName("When null is returned from service, bot treats it as empty and replies 'none'")
        void listCommand_withNullResponse_repliesNone() {
            when(subscriptionService.listByChatId(CHAT_ID)).thenReturn(null);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));

            verify(apiClient).sendMessage(CHAT_ID, "NONE");
        }
    }

    //  /unsubscribe command tests

    @Nested
    @DisplayName("/unsubscribe command")
    class UnsubscribeCommandTests {

        @Test
        @DisplayName("When valid keyword is provided, subscription is removed and confirmation sent")
        void unsubscribeCommand_withValidKeyword_removesSubscriptionAndRepliesRemoved() {
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tesla")).thenReturn(true);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe Tesla"));

            verify(apiClient).sendMessage(CHAT_ID, "REMOVED");
        }

        @Test
        @DisplayName("When no matching subscription exists, bot replies with 'not found'")
        void unsubscribeCommand_withNonexistentKeyword_repliesNotFound() {
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tesla")).thenReturn(false);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe Tesla"));

            verify(apiClient).sendMessage(CHAT_ID, "NOT_FOUND");
        }

        @Test
        @DisplayName("When argument is empty, bot replies with 'not found'")
        void unsubscribeCommand_withEmptyArgument_repliesNotFound() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe "));

            verify(apiClient).sendMessage(CHAT_ID, "NOT_FOUND");
        }

        @Test
        @DisplayName("When exception occurs while unsubscribing, bot replies with 'unexpected'")
        void unsubscribeCommand_withServiceException_repliesUnexpected() {
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tesla"))
                    .thenThrow(new RuntimeException("fail"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe Tesla"));

            verify(apiClient).sendMessage(CHAT_ID, "UNEXPECTED");
        }

        @Test
        @DisplayName("When argument has extra whitespace, it is trimmed before lookup")
        void unsubscribeCommand_withExtraWhitespace_trimsArgumentBeforeLookup() {
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tesla")).thenReturn(true);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe    Tesla   "));

            verify(apiClient).sendMessage(CHAT_ID, "REMOVED");
        }

        @Test
        @DisplayName("When partial match is provided, bot replies 'not found'")
        void unsubscribeCommand_withPartialMatch_repliesNotFound() {
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tes")).thenReturn(false);

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe Tes"));

            verify(apiClient).sendMessage(CHAT_ID, "NOT_FOUND");
        }
    }

    //  Unknown command tests

    @Nested
    @DisplayName("Unknown commands")
    class UnknownCommandTests {

        @Test
        @DisplayName("When unknown command is received, bot replies with 'unknown'")
        void unknownCommand_withInvalidCommand_repliesUnknown() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/foobar"));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }

        @Test
        @DisplayName("When command has trailing whitespace, bot still replies with 'unknown'")
        void unknownCommand_withTrailingWhitespace_repliesUnknown() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/foobar   "));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }

        @Test
        @DisplayName("When command has uppercase letters, bot treats it as unknown")
        void unknownCommand_withUppercaseLetters_repliesUnknown() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/SUBSCRIBE Tesla en 10"));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }

        @Test
        @DisplayName("When command has mixed case, bot treats it as unknown")
        void unknownCommand_withMixedCase_repliesUnknown() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/Help"));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }
    }

    //  Helper method tests

    @Nested
    @DisplayName("Helper methods")
    class HelperMethodTests {

        @Test
        @DisplayName("When extractBaseCommand is called with valid text, it returns the first token")
        void extractBaseCommand_withValidText_returnsFirstToken() throws Exception {
            var method = TelegramService.class.getDeclaredMethod("extractBaseCommand", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(service, "/subscribe Tesla en 10");
            assertEquals("/subscribe", result);
        }

        @Test
        @DisplayName("When extractBaseCommand is called with blanks, it returns empty string")
        void extractBaseCommand_withBlanks_returnsEmptyString() throws Exception {
            var method = TelegramService.class.getDeclaredMethod("extractBaseCommand", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(service, "   ");
            assertEquals("", result);
        }

        @Test
        @DisplayName("When renderSubscriptionList is called, it formats a numbered list correctly")
        void renderSubscriptionList_withItems_formatsNumberedList() throws Exception {
            var method = TelegramService.class.getDeclaredMethod("renderSubscriptionList", List.class);
            method.setAccessible(true);
            String result = (String) method.invoke(service, List.of("one", "two"));
            assertEquals("1. one\n2. two", result);
        }

        @Test
        @DisplayName("When renderSubscriptionList is called with empty list, it returns empty string")
        void renderSubscriptionList_withEmptyList_returnsEmptyString() throws Exception {
            var method = TelegramService.class.getDeclaredMethod("renderSubscriptionList", List.class);
            method.setAccessible(true);
            String result = (String) method.invoke(service, List.of());
            assertEquals("", result);
        }
    }

    //  General edge cases

    @Nested
    @DisplayName("General edge cases")
    class GeneralEdgeCaseTests {

        @Test
        @DisplayName("When message is null, bot replies with 'unknown'")
        void handleCommand_withNullMessage_repliesUnknown() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, null));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }

        @Test
        @DisplayName("When message contains only whitespace, bot replies with 'unknown'")
        void handleCommand_withWhitespaceMessage_repliesUnknown() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "   "));
            verify(apiClient).sendMessage(CHAT_ID, "UNKNOWN");
        }

        @Test
        @DisplayName("When safeRun catches an exception, bot replies with 'unexpected'")
        void safeRun_withException_repliesUnexpected() {
            when(subscriptionService.listByChatId(CHAT_ID)).thenThrow(new RuntimeException("boom"));
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));
            verify(apiClient).sendMessage(CHAT_ID, "UNEXPECTED");
        }

        @Test
        @DisplayName("When unsubscribe throws exception, safeRun still replies with 'unexpected'")
        void safeRun_withExceptionDuringUnsubscribe_repliesUnexpected() {
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tesla"))
                    .thenThrow(new RuntimeException("boom"));
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe Tesla"));
            verify(apiClient).sendMessage(CHAT_ID, "UNEXPECTED");
        }

        @Test
        @DisplayName("When reply() emits error signal, it is caught and does not propagate")
        void reply_withErrorSignal_doesNotPropagateException() {
            when(apiClient.sendMessage(anyLong(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("API down")));

            Assertions.assertDoesNotThrow(() ->
                    service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/help"))
            );
        }

        @Test
        @DisplayName("When invalid /subscribe is sent, bot replies twice: 'invalid' and 'help'")
        void subscribeCommand_withInvalidInput_repliesTwiceInvalidAndHelp() {
            when(commandParser.parseSubscribeCommand(any())).thenThrow(new IllegalArgumentException("bad"));

            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe wrongformat"));

            verify(apiClient, times(2)).sendMessage(eq(CHAT_ID), anyString());
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("INVALID"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("HELP"));
        }

        @Test
        @DisplayName("When /unsubscribe is called without argument, bot replies with 'not found'")
        void unsubscribeCommand_withoutArgument_repliesNotFound() {
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe"));
            verify(apiClient).sendMessage(CHAT_ID, "NOT_FOUND");
        }
    }

    //  API failure scenarios

    @Nested
    @DisplayName("API failure scenarios")
    class ApiFailureTests {

        @Test
        @DisplayName("When Telegram API fails, exception does not propagate to caller")
        void apiClient_withSendMessageFailure_doesNotPropagateException() {
            when(apiClient.sendMessage(anyLong(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("API down")));

            Assertions.assertDoesNotThrow(() ->
                    service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/help"))
            );
        }
    }

    //  Integration flow simulation

    @Nested
    @DisplayName("Integration flow simulation")
    class IntegrationFlowTests {

        @Test
        @DisplayName("When user subscribes, lists and unsubscribes, full flow works correctly")
        void fullFlow_withSubscribeListAndUnsubscribe_worksAsExpected() {
            Subscription mockSubscription = new Subscription();
            TelegramSubscribeCommand parsedCmd = new TelegramSubscribeCommand(
                    CHAT_ID, "en", 5, List.of("Tesla"), SchedulePreset.EVENING
            );

            when(commandParser.parseSubscribeCommand(any())).thenReturn(parsedCmd);
            when(mapper.map(any(), any())).thenReturn(mockSubscription);

            // Step 1: Subscribe
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/subscribe \"Tesla\" en 5"));
            verify(subscriptionService).save(eq(mockSubscription), eq(TEST_STORAGE_PATH));
            verify(apiClient).sendMessage(CHAT_ID, "SAVED");

            clearInvocations(apiClient);

            // Step 2: List
            when(subscriptionService.listByChatId(CHAT_ID)).thenReturn(List.of("Tesla"));
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/list"));
            verify(apiClient).sendMessage(eq(CHAT_ID), contains("Tesla"));

            clearInvocations(apiClient);

            // Step 3: Unsubscribe
            when(subscriptionService.removeByIdOrKeyword(CHAT_ID, "Tesla")).thenReturn(true);
            service.handleTelegramCommand(new TelegramCommand(CHAT_ID, "/unsubscribe Tesla"));
            verify(apiClient).sendMessage(CHAT_ID, "REMOVED");
        }
    }
}
