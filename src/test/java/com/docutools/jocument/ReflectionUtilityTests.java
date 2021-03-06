package com.docutools.jocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.docutools.jocument.impl.ReflectionUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.zip.Adler32;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Reflection Utilities")
public class ReflectionUtilityTests {

  @ParameterizedTest(name = "Detect JSR 310 Types")
  @ValueSource(classes = {ZonedDateTime.class, LocalDateTime.class, LocalDate.class, Instant.class})
  void shouldDetectJSR310Types(Class<?> jsr310Type) {
    // Act
    boolean jsr310 = ReflectionUtils.isJsr310Type(jsr310Type);
    // Assert
    assertThat(jsr310, is(true));
  }

  @ParameterizedTest(name = "Negatively detect non-JSR 310 Types")
  @ValueSource(classes = {Duration.class, Object.class, Adler32.class, String.class})
  void shouldNegativelyDetectNonJsr310Types(Class<?> jsr310Type) {
    // Act
    boolean jsr310 = ReflectionUtils.isJsr310Type(jsr310Type);
    // Assert
    assertThat(jsr310, is(false));
  }

  @Test
  void shouldGetAnnotationFromPrivateField() {
    // Arrange
    var clazz = Clazz.class;
    // Act
    Optional<TheAnnotation> annotation = ReflectionUtils.findFieldAnnotation(clazz, "field", TheAnnotation.class);
    // Assert
    assertThat(annotation, notNullValue());
    assertThat(annotation.isPresent(), is(true));
  }

  @Test
  void shouldGetEmptyResultWhenAnnotationNotOnField() {
    // Act
    var result = ReflectionUtils.findFieldAnnotation(Clazz.class, "field", Override.class);
    // Assert
    assertThat(result, notNullValue());
    assertThat(result.isEmpty(), is(true));
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TheAnnotation {
  }

  static class Clazz {
    @TheAnnotation
    private Object field;
  }
}
