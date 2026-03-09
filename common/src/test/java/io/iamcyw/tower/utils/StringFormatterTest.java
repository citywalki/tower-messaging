package io.iamcyw.tower.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StringFormatterTest {

    @Test
    void format() {
        Assertions.assertThat(StringFormatter.format("a{}b{}", "a", "b")).isEqualTo("aabb");
    }

    @Test
    void formatWithNoPlaceholders() {
        Assertions.assertThat(StringFormatter.format("hello", "a")).isEqualTo("hello");
    }

    @Test
    void formatWithNullText() {
        Assertions.assertThat(StringFormatter.format(null, "a")).isNull();
    }

    @Test
    void formatWithEmptyReplacements() {
        Assertions.assertThat(StringFormatter.format("a{}b{}")).isEqualTo("a{}b{}");
    }

}
