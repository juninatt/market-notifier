package se.pbt.tvm.newsprovider.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Common helpers for extracting and preparing values from JSON when mapping news provider data.
 */
public final class MappingUtils {

    private MappingUtils() {}

    /**
     * Makes sure that a title is present and usable.
     * Falls back to "(no title)" if the input is null or blank.
     */
    public static String validateTitle(String title) {
        return hasText(title) ? title : "(no title)";
    }

    /**
     * Adds a key-value pair to the given map only if the value contains text.
     * Used for avoiding empty or placeholder entries in metadata.
     */
    public static void putIfHasText(Map<String, String> map, String key, String value) {
        if (hasText(value)) map.put(key, value);
    }

    /**
     * Attempts to create a URI from the given string.
     * Returns null if the input is blank or not a valid URI format.
     */
    public static URI parseUri(String raw) {
        if (!hasText(raw)) return null;
        try { return new URI(raw); } catch (URISyntaxException e) { return null; }
    }

    /**
     * Parses a date-time string in ISO-8601 format into an Instant.
     * Returns null if the input is blank or cannot be parsed.
     */
    public static Instant parseInstant(String iso) {
        if (!hasText(iso)) return null;
        try { return Instant.parse(iso); } catch (DateTimeParseException e) { return null; }
    }

    /**
     * Converts epoch seconds to an Instant, using Instant.EPOCH as a safe fallback when the value is zero or negative.
     * Used when timestamps may be missing or unset.
     */
    public static Instant parseEpochSeconds(long epochSeconds) {
        return epochSeconds > 0 ? Instant.ofEpochSecond(epochSeconds) : Instant.EPOCH;
    }

    /**
     * Parses the specified text field from a JSON node, or returns null if missing or blank.
     */
    public static String parseTextField(JsonNode node, String field) {
        if (node == null) return null;
        String v = node.path(field).asText(null);
        return hasText(v) ? v : null;
    }

    /**
     * Parses a comma-separated string into an uppercase list of values,
     * removing duplicates and preserving order.
     */
    public static List<String> parseCsvToUppercaseList(String csv) {
        if (!hasText(csv)) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String p : csv.split(",")) {
            String norm = p.trim().toUpperCase(Locale.ROOT);
            if (!norm.isEmpty()) set.add(norm);
        }
        return List.copyOf(set);
    }

    /**
     * Parses the given field from each element in the JSON array into a list of uppercase values,
     * removing duplicates and preserving order.
     */
    public static List<String> parseFieldValuesToUppercaseList(ArrayNode entities, String symbolField) {
        if (entities == null || entities.isEmpty()) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (JsonNode e : entities) {
            String sym = parseTextField(e, symbolField);
            if (sym != null) {
                String norm = sym.trim().toUpperCase(Locale.ROOT);
                if (!norm.isEmpty()) set.add(norm);
            }
        }
        return List.copyOf(set);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
