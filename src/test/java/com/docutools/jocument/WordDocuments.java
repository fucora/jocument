package com.docutools.jocument;

import com.docutools.jocument.impl.ReflectionResolver;
import com.docutools.jocument.sample.model.SampleModelData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@DisplayName("Generating Word Documents")
public class WordDocuments {

  @Test
  @DisplayName("Load word templates from classpath.")
  void shouldLoadWordTemplateFromCP() {
    // Act
    Template template = Template.fromClassPath("/templates/word/UserProfileTemplate.docx")
            .orElseThrow();

    // Assert
    assertThat(template.getMimeType(), is(MimeType.DOCX));
  }

  @Test
  @DisplayName("Return empty value when given classpath resource does not exist.")
  void shouldReturnEmptyWhenCpNotExists() {
    // Act
    var result = Template.fromClassPath("/does/not/exist.docx");

    // Assert
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  @DisplayName("Template should assume systems default Locale, if none is passed with resource.")
  void shouldAssumeDefaultLocale() {
    // Act
    var result = Template.fromClassPath("/templates/word/UserProfileTemplate.docx")
            .orElseThrow();

    // Assert
    assertThat(result.getLocale(), equalTo(Locale.getDefault()));
  }

  @Test
  @DisplayName("Generate a document from a simple template.")
  void shouldGenerateSimpleDocument() throws InterruptedException, IOException {
    // Arrange
    Template template = Template.fromClassPath("/templates/word/UserProfileTemplate.docx")
            .orElseThrow();
    PlaceholderResolver resolver = new ReflectionResolver(SampleModelData.PICARD_PERSON);

    // Act
    Document document = template.startGeneration(resolver);
    document.blockUntilCompletion(60000L); // 1 minute

    // Assert
    assertThat(document.completed(), is(true));

    Desktop.getDesktop().open(document.getPath().toFile());
  }

  @Test
  @DisplayName("Replace placeholders in tables.")
  void shouldReplacePlaceholdersInTables() throws InterruptedException, IOException {
    // Arrange
    Template template = Template.fromClassPath("/templates/word/UserProfileWithTableTemplate.docx")
            .orElseThrow();
    PlaceholderResolver resolver = new ReflectionResolver(SampleModelData.PICARD_PERSON);

    // Act
    Document document = template.startGeneration(resolver);
    document.blockUntilCompletion(60000L); // 1 minute

    // Assert
    assertThat(document.completed(), is(true));

    Desktop.getDesktop().open(document.getPath().toFile());
  }

  @Test
  @DisplayName("Resolve collection placeholders.")
  void shouldResolveCollectionPlaceholders() throws InterruptedException, IOException {
    // Arrange
    Template template = Template.fromClassPath("/templates/word/CollectionsTemplate.docx")
            .orElseThrow();
    PlaceholderResolver resolver = new ReflectionResolver(SampleModelData.PICARD);

    // Act
    Document document = template.startGeneration(resolver);
    document.blockUntilCompletion(60000L); // 1 minute

    // Assert
    assertThat(document.completed(), is(true));

    Desktop.getDesktop().open(document.getPath().toFile());
  }

  @Test
  @DisplayName("Apply custom word placeholder.")
  void shouldApplyCustomWordPlaceholder() throws InterruptedException, IOException {
    // Arrange
    Template template = Template.fromClassPath("/templates/word/CustomPlaceholderTemplate.docx")
            .orElseThrow();
    PlaceholderResolver resolver = new ReflectionResolver(SampleModelData.PICARD);

    // Act
    Document document = template.startGeneration(resolver);
    document.blockUntilCompletion(60000L); // 1 minute

    // Assert
    assertThat(document.completed(), is(true));

    Desktop.getDesktop().open(document.getPath().toFile());
  }
}