package se.pbt.tvm.subscription.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.tvm.subscription.testutil.SubscriptionTestFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SubscriptionIdGenerator")
class SubscriptionIdGeneratorTest {

    private final SubscriptionIdGenerator generator = new SubscriptionIdGenerator();

    @Nested
    @DisplayName("ID generation")
    class IdGeneration {

        @Test
        @DisplayName("Generates base id from chatId and first keyword")
        void generates_baseId_fromChatAndFirstKeyword() {
            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of());
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Appends -2 for duplicate subscription in same chat")
            // overlaps with increments_suffixBeyondSecond(), kept for completeness
        void appends_suffix_onCollision() {
            var existing = SubscriptionTestFactory.subscription("sub-123-ai");
            existing.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(existing));
            assertEquals("sub-123-ai-2", id);
        }

        @Test
        @DisplayName("Increments suffix when -2 already exists")
        void increments_suffixBeyondSecond() {
            var e1 = SubscriptionTestFactory.subscription("sub-123-ai");
            var e2 = SubscriptionTestFactory.subscription("sub-123-ai-2");
            e1.setChatId(123L);
            e2.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(e1, e2));
            assertEquals("sub-123-ai-3", id);
        }

        @Test
        @DisplayName("Produces same ID for identical input when no collisions exist")
            // overlaps with generates_baseId_fromChatAndFirstKeyword(), kept for completeness
        void produces_deterministic_baseId() {
            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub1 = SubscriptionTestFactory.subscription(null, filter, true);
            var sub2 = SubscriptionTestFactory.subscription(null, filter, true);
            sub1.setChatId(123L);
            sub2.setChatId(123L);

            var id1 = generator.generateUniqueId(sub1, List.of());
            var id2 = generator.generateUniqueId(sub2, List.of());

            assertEquals("sub-123-ai", id1);
            assertEquals(id1, id2);
        }

        @Test
        @DisplayName("Picks first free numeric suffix when a gap exists")
        void picks_firstFreeSuffix_whenGapExists() {
            var e1 = SubscriptionTestFactory.subscription("sub-123-ai");
            var e3 = SubscriptionTestFactory.subscription("sub-123-ai-3");
            e1.setChatId(123L);
            e3.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(e1, e3));
            assertEquals("sub-123-ai-2", id);
        }

        @Test
        @DisplayName("Result does not depend on order of existing IDs")
        void existingOrder_doesNotAffect_result() {
            var e1 = SubscriptionTestFactory.subscription("sub-123-ai");
            var e2 = SubscriptionTestFactory.subscription("sub-123-ai-2");
            e1.setChatId(123L);
            e2.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var a = generator.generateUniqueId(sub, List.of(e1, e2));
            var b = generator.generateUniqueId(sub, List.of(e2, e1));
            assertEquals(a, b);
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("Throws when subscription has no keywords")
        void generate_withNoKeywords_throwsException() {
            var filter = SubscriptionTestFactory.filter(List.of(), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            assertThrows(IllegalArgumentException.class,
                    () -> generator.generateUniqueId(sub, List.of()));
        }

        @Test
        @DisplayName("Throws when first keyword contains only whitespace")
        void generate_withOnlyWhitespaceKeyword_throwsException() {
            var filter = SubscriptionTestFactory.filter(List.of("   \t\n  "), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            assertThrows(IllegalArgumentException.class,
                    () -> generator.generateUniqueId(sub, List.of()));
        }
    }

    @Nested
    @DisplayName("Slug transformation")
    class SlugTransformation {

        @Nested
        @DisplayName("Basic cleanup")
        class BasicCleanup {

            @Test
            @DisplayName("Throws when slug becomes empty after cleanup")
            void slug_becomesEmpty_afterCleanup_throws() {
                var filter = SubscriptionTestFactory.filter(List.of("+++"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                assertThrows(IllegalArgumentException.class,
                        () -> generator.generateUniqueId(sub, List.of()));
            }

            @Test
            @DisplayName("Removes non-alphanumeric characters and lowercases")
                // overlaps with lowercases_allLetters
            void slug_removesNonAlnum_andLowercases() {
                var filter = SubscriptionTestFactory.filter(List.of("AI/ML+++"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-ai-ml", id);
            }

            @Test
            @DisplayName("Lowercases all letters in the slug")
            void lowercases_allLetters() {
                var filter = SubscriptionTestFactory.filter(List.of("Ai-ML"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-ai-ml", id);
            }
        }

        @Nested
        @DisplayName("Whitespace and separators")
        class SeparatorHandling {

            @Test
            @DisplayName("Collapses multiple whitespaces to a single dash")
                // overlaps with collapses_mixedSeparators_toSingleDash(), kept for completeness
            void collapses_whitespace_toSingleDash() {
                var filter = SubscriptionTestFactory.filter(List.of("  AI    ML  "), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-ai-ml", id);
            }

            @Test
            @DisplayName("Collapses mixed separators to single dashes")
            void collapses_mixedSeparators_toSingleDash() {
                var filter = SubscriptionTestFactory.filter(List.of("AI...//__++ ML"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-ai-ml", id);
            }

            @Test
            @DisplayName("Removes leading and trailing dashes")
            void removes_leadingAndTrailing_dashes() {
                var filter = SubscriptionTestFactory.filter(List.of("--AI--"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-ai", id);
            }
        }

        @Nested
        @DisplayName("Language and symbols")
        class CharacterHandling {

            @Test
            @DisplayName("Drops diacritics")
            void drops_diacritics() {
                var filter = SubscriptionTestFactory.filter(List.of("Göteborg"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-gteborg", id);
            }

            @Test
            @DisplayName("Drops emoji and symbols without leaving artifacts")
            void drops_emoji_andSymbols() {
                var filter = SubscriptionTestFactory.filter(List.of("AI🔥™"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-ai", id);
            }

            @Test
            @DisplayName("Drops non-ASCII letters consistently")
            void drops_nonAsciiLetters_consistently() {
                var filter = SubscriptionTestFactory.filter(List.of("München"), List.of(), "en");
                var sub = SubscriptionTestFactory.subscription(null, filter, true);
                sub.setChatId(123L);

                var id = generator.generateUniqueId(sub, List.of());
                assertEquals("sub-123-mnchen", id);
            }
        }
    }

    @Nested
    @DisplayName("Collision handling")
    class CollisionHandling {

        @Test
        @DisplayName("Ignores collisions from subscriptions in different chats")
            // overlaps with sameKeyword_acrossDifferentChats_doesNotCollide(), kept for completeness
        void ignores_collisions_acrossChats() {
            var otherChat = SubscriptionTestFactory.subscription("sub-999-ai");
            otherChat.setChatId(999L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(otherChat));
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Ignores existing subscriptions with null IDs in collision checks")
        void ignores_nullIds_inExisting() {
            var e1 = SubscriptionTestFactory.subscription((String) null);
            e1.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(e1));
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Does not add suffix when keyword differs in same chat")
            // overlaps with caseSensitive_existingId_doesNotCollide
        void noSuffix_when_differentKeyword_inSameChat() {
            var existing = SubscriptionTestFactory.subscription("sub-123-ai");
            existing.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("ML"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(existing));
            assertEquals("sub-123-ml", id);
        }

        @Test
        @DisplayName("String match is case-sensitive")
        void caseSensitive_existingId_doesNotCollide() {
            var existing = SubscriptionTestFactory.subscription("sub-123-AI");
            existing.setChatId(123L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(existing));
            assertEquals("sub-123-ai", id);
        }

        @Test
        @DisplayName("Same keyword in different chats never collides or adds suffix")
        void sameKeyword_acrossDifferentChats_doesNotCollide() {
            var otherChat1 = SubscriptionTestFactory.subscription("sub-111-ai");
            var otherChat2 = SubscriptionTestFactory.subscription("sub-222-ai");
            otherChat1.setChatId(111L);
            otherChat2.setChatId(222L);

            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of(), "en");
            var sub = SubscriptionTestFactory.subscription(null, filter, true);
            sub.setChatId(123L);

            var id = generator.generateUniqueId(sub, List.of(otherChat1, otherChat2));
            assertEquals("sub-123-ai", id);
        }
    }
}
