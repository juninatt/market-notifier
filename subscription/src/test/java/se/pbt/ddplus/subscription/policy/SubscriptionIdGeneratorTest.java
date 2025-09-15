package se.pbt.ddplus.subscription.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.ddplus.core.schedule.SchedulePreset;
import se.pbt.ddplus.subscription.model.Subscription;
import se.pbt.ddplus.subscription.model.SubscriptionFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SubscriptionIdGenerator")
class SubscriptionIdGeneratorTest {

    private final SubscriptionIdGenerator generator = new SubscriptionIdGenerator();

    @Nested
    @DisplayName("Id generation:")
    class IdGeneration {

        @Test
        @DisplayName("Generates base id from chatId and first keyword")
        void generatesBaseIdFromChatAndFirstKeyword() {
            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Appends -2 on for same subscription within the same chat")
        void appendsSuffixOnCollision() {
            Subscription existing = new Subscription();
            existing.setId("sub-123-ai");
            existing.setChatId(123L);

            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(existing));
            assertEquals("sub-123-ai-2", id);
        }

        @Test
        @DisplayName("Increments suffix to the next free value")
        void incrementsSuffixBeyondSecond() {
            Subscription e1 = withIdAndChat("sub-123-ai", 123L);
            Subscription e2 = withIdAndChat("sub-123-ai-2", 123L);

            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(e1, e2));
            assertEquals("sub-123-ai-3", id);
        }

        @Test
        @DisplayName("Produces the same ID for identical input when no collisions exist")
        void producesDeterministicBaseId() {
            Subscription sub1 = makeSub(123L, List.of("AI"));
            Subscription sub2 = makeSub(123L, List.of("AI"));
            String id1 = generator.generateUniqueId(sub1, List.of());
            String id2 = generator.generateUniqueId(sub2, List.of());
            assertEquals("sub-123-ai", id1);
            assertEquals(id1, id2);
        }

        @Test
        @DisplayName("Picks the first free numeric suffix when there is a gap")
        void picksFirstFreeSuffixWhenGapExists() {
            Subscription e1 = withIdAndChat("sub-123-ai", 123L);
            Subscription e3 = withIdAndChat("sub-123-ai-3", 123L);
            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(e1, e3));
            assertEquals("sub-123-ai-2", id);
        }

        @Test
        @DisplayName("Result does not depend on the order of existing IDs")
        void existingOrderDoesNotAffectResult() {
            Subscription e1 = withIdAndChat("sub-123-ai", 123L);
            Subscription e2 = withIdAndChat("sub-123-ai-2", 123L);
            Subscription sub = makeSub(123L, List.of("AI"));

            String a = generator.generateUniqueId(sub, List.of(e1, e2));
            String b = generator.generateUniqueId(sub, List.of(e2, e1));
            assertEquals(a, b);
        }

    }

    @Nested
    @DisplayName("Keyword validation:")
    class KeywordValidation {

        @Test
        @DisplayName("Throws when subscription has no keywords")
        void throwsWhenNoKeywords() {
            Subscription sub = makeSub(123L, List.of());
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generateUniqueId(sub, List.of()));
        }

        @Test
        @DisplayName("Throws when first keyword contains only whitespace")
        void throwsWhenKeywordIsOnlyWhitespace() {
            Subscription sub = makeSub(123L, List.of("   \t\n  "));
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generateUniqueId(sub, List.of()));
        }
    }

    @Nested
    @DisplayName("Slug transformation:")
    class SlugTransformation {

        @Test
        @DisplayName("Throws when slug becomes empty after trimming")
        void throwsWhenSlugBecomesEmpty() {
            Subscription sub = makeSub(123L, List.of("+++"));
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generateUniqueId(sub, List.of()));
        }

        @Test
        @DisplayName("Slug removes non-alphanumeric characters and lowercases")
        void slugRemovesNonAlnumAndLowercases() {
            Subscription sub = makeSub(123L, List.of("AI/ML+++"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai-ml", id);
        }

        @Test
        @DisplayName("Slug drops diacritics")
        void slugDropsDiacritics() {
            Subscription sub = makeSub(123L, List.of("Göteborg"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-gteborg", id);
        }

        @Test
        @DisplayName("Collapses multiple whitespaces to a single dash and trims edges")
        void collapsesWhitespaceToSingleDash() {
            Subscription sub = makeSub(123L, List.of("  AI    ML  "));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai-ml", id);
        }

        @Test
        @DisplayName("Collapses mixed separators to single dashes between tokens")
        void collapsesMixedSeparators() {
            Subscription sub = makeSub(123L, List.of("AI...//__++ ML"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai-ml", id);
        }

        @Test
        @DisplayName("Removes leading and trailing dashes produced by cleanup")
        void removesLeadingAndTrailingDashes() {
            Subscription sub = makeSub(123L, List.of("--AI--"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Lowercases all letters in the slug")
        void lowercasesAllLetters() {
            Subscription sub = makeSub(123L, List.of("Ai-ML"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai-ml", id);
        }

        @Test
        @DisplayName("Drops emoji and symbols without leaving artifacts")
        void dropsEmojiAndSymbols() {
            Subscription sub = makeSub(123L, List.of("AI🔥™"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Drops non-ASCII letters consistently")
        void dropsNonAsciiLettersConsistently() {
            Subscription sub = makeSub(123L, List.of("München"));
            String id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-mnchen", id);
        }
    }

    @Nested
    @DisplayName("Collision handling:")
    class CollisionHandling {

        @Test
        @DisplayName("Ignores collisions from subscriptions in different chats")
        void ignoresCollisionsAcrossChats() {
            Subscription otherChat = withIdAndChat("sub-999-ai", 999L);

            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(otherChat));
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Ignores existing subscriptions with null IDs in collision checks")
        void ignoresNullIdsInExisting() {
            Subscription e1 = withIdAndChat(null, 123L);
            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(e1));
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Does not add suffix when keyword differs in the same chat")
        void noSuffixWhenDifferentKeywordInSameChat() {
            Subscription existing = withIdAndChat("sub-123-ai", 123L);
            Subscription sub = makeSub(123L, List.of("ML"));
            String id = generator.generateUniqueId(sub, List.of(existing));
            assertEquals("sub-123-ml", id);
        }

        @Test
        @DisplayName("String match is case-sensitive")
        void caseSensitiveExistingIdDoesNotCollide() {
            Subscription existing = withIdAndChat("sub-123-AI", 123L);
            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(existing));
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Same keyword in different chats never collides or adds suffix")
        void sameKeywordAcrossDifferentChatsDoesNotCollide() {
            Subscription otherChat1 = withIdAndChat("sub-111-ai", 111L);
            Subscription otherChat2 = withIdAndChat("sub-222-ai", 222L);

            Subscription sub = makeSub(123L, List.of("AI"));
            String id = generator.generateUniqueId(sub, List.of(otherChat1, otherChat2));
            assertEquals("sub-123-ai", id);
        }
    }

    // --- Helpers ---

    private Subscription makeSub(long chatId, List<String> keywords) {
        Subscription sub = new Subscription();
        sub.setChatId(chatId);
        SubscriptionFilter f = new SubscriptionFilter();
        f.setKeywords(keywords);
        sub.setFilter(f);
        sub.setSchedule(SchedulePreset.EVENING);
        return sub;
    }

    private Subscription withIdAndChat(String id, long chatId) {
        Subscription s = new Subscription();
        s.setId(id);
        s.setChatId(chatId);
        return s;
    }
}
