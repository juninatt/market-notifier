package se.pbt.ddplus.newsprovider.common;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MappingUtils — parse/validate helpers")
class MappingUtilsTest {

    @Test
    @DisplayName("validateTitle: falls back to '(no title)' when blank")
    void validateTitle_returnsFallbackWhenBlank() {
        assertEquals("(no title)", MappingUtils.validateTitle(""));
        assertEquals("Hello", MappingUtils.validateTitle("Hello"));
    }

    @Test
    @DisplayName("parseUri: returns null for blank/invalid input; parses valid URI")
    void parseUri_returnsNullForInvalidOrBlank() {
        assertNull(MappingUtils.parseUri(null));
        assertNull(MappingUtils.parseUri(""));
        assertNull(MappingUtils.parseUri("::bad-uri"));
        assertEquals(URI.create("http://test.com"), MappingUtils.parseUri("http://test.com"));
    }

    @Test
    @DisplayName("parseInstant: parses ISO-8601 string; returns null when blank/invalid")
    void parseInstant_handlesValidAndInvalid() {
        Instant now = Instant.now();
        assertEquals(now, MappingUtils.parseInstant(now.toString()));
        assertNull(MappingUtils.parseInstant(""));
        assertNull(MappingUtils.parseInstant("not-a-date"));
    }

    @Test
    @DisplayName("parseEpochSeconds: returns Instant.EPOCH for <= 0; converts positive seconds")
    void epochSecondsOrEpoch_returnsEpochForZeroOrNegativeSeconds() {
        assertEquals(Instant.EPOCH, MappingUtils.parseEpochSeconds(0));
        assertEquals(Instant.ofEpochSecond(100), MappingUtils.parseEpochSeconds(100));
    }

    @Test
    @DisplayName("parseTextField: returns field text; null when missing/blank")
    void textOrNull_readsNonBlankText() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("field", "value");
        assertEquals("value", MappingUtils.parseTextField(node, "field"));
        node.put("field", " ");
        assertNull(MappingUtils.parseTextField(node, "field"));
    }

    @Test
    @DisplayName("parseCsvToUppercaseList: uppercase output, removes duplicates, preserves order")
    void parseCsvToUppercaseListUppercaseList() {
        List<String> result = MappingUtils.parseCsvToUppercaseList("a,B,a");
        assertEquals(List.of("A", "B"), result);
    }

    @Test
    @DisplayName("parseFieldValuesToUppercaseList: reads array field to uppercase unique list (order-preserving)")
    void parseFieldValuesToUppercaseList() {
        ObjectNode e1 = JsonNodeFactory.instance.objectNode().put("symbol", "aapl");
        ObjectNode e2 = JsonNodeFactory.instance.objectNode().put("symbol", " msft ");
        ObjectNode e3 = JsonNodeFactory.instance.objectNode().put("symbol", "AAPL");
        var array = JsonNodeFactory.instance.arrayNode().add(e1).add(e2).add(e3);

        List<String> result = MappingUtils.parseFieldValuesToUppercaseList(array, "symbol");
        assertEquals(List.of("AAPL", "MSFT"), result);
    }

    @Test
    @DisplayName("putIfHasText: adds only entries with non-blank values")
    void putIfHasText_addsOnlyNonBlank() {
        Map<String, String> map = new HashMap<>();
        MappingUtils.putIfHasText(map, "key1", "value");
        MappingUtils.putIfHasText(map, "key2", " ");
        assertEquals(1, map.size());
        assertEquals("value", map.get("key1"));
    }
}