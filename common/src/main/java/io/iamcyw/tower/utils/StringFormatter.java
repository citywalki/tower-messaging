package io.iamcyw.tower.utils;

import com.google.common.base.Strings;

/**
 * Formats strings with placeholder replacement.
 *
 * <p>This class provides string formatting utilities for replacing placeholders
 * in template strings with actual values. It is primarily used for message
 * formatting in the messaging framework.</p>
 *
 * @since 2.0
 */
public class StringFormatter {

    public static final String PLACEHOLDER = "{}";

    private StringFormatter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Replaces '{}' placeholders in the text with values from the replacement array.
     *
     * <p>Placeholders are replaced in order. If there are more placeholders than
     * replacements, the remaining placeholders are left unchanged.</p>
     *
     * @param text           the template string containing '{}' placeholders
     * @param replacements   the values to substitute for placeholders
     * @return the formatted string with placeholders replaced
     */
    public static String format(String text, Object... replacements) {
        if (Strings.isNullOrEmpty(text) || replacements == null || replacements.length == 0) {
            return text;
        }

        final int indexNotFound = -1;
        int start = 0;
        int end = text.indexOf(PLACEHOLDER, start);

        if (end == indexNotFound) {
            return text;
        }

        final int placeholderLength = PLACEHOLDER.length();
        final StringBuilder buffer = new StringBuilder(text.length() + replacements.length * 2);
        int index = 0;
        while (end != indexNotFound) {
            final String replacement = String.valueOf(replacements[index]);
            buffer.append(text, start, end).append(replacement);
            start = end + placeholderLength;
            end = text.indexOf(PLACEHOLDER, start);
            index++;
        }
        buffer.append(text, start, text.length());

        return buffer.toString();
    }

}
