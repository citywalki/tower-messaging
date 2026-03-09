package io.iamcyw.tower.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class NullSafetyTest {

    @Test
    void getOrDefaultWithNonNullValue() {
        Assertions.assertThat(NullSafety.getOrDefault("value", "default")).isEqualTo("value");
    }

    @Test
    void getOrDefaultWithNullValue() {
        Assertions.assertThat(NullSafety.getOrDefault(null, "default")).isEqualTo("default");
    }

    @Test
    void getOrDefaultWithSupplier() {
        Supplier<String> supplier = () -> "computed";
        Assertions.assertThat(NullSafety.getOrDefault(null, supplier)).isEqualTo("computed");
    }

    @Test
    void getNonEmptyOrDefaultWithNonEmptyValue() {
        Assertions.assertThat(NullSafety.getNonEmptyOrDefault("value", "default")).isEqualTo("value");
    }

    @Test
    void getNonEmptyOrDefaultWithEmptyValue() {
        Assertions.assertThat(NullSafety.getNonEmptyOrDefault("", "default")).isEqualTo("default");
    }

    @Test
    void getNonEmptyOrDefaultWithNullValue() {
        Assertions.assertThat(NullSafety.getNonEmptyOrDefault(null, "default")).isEqualTo("default");
    }

}
