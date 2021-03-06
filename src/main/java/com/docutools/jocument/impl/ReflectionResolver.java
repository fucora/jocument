package com.docutools.jocument.impl;

import com.docutools.jocument.CustomPlaceholderRegistry;
import com.docutools.jocument.PlaceholderData;
import com.docutools.jocument.PlaceholderResolver;
import com.docutools.jocument.annotations.Format;
import com.docutools.jocument.annotations.Image;
import com.docutools.jocument.annotations.Money;
import com.docutools.jocument.annotations.Numeric;
import com.docutools.jocument.annotations.Percentage;
import com.docutools.jocument.impl.word.placeholders.ImagePlaceholderData;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Takes a {@link java.lang.Object} of any type and resolves placeholder names with reflective access to its type.
 *
 * @author codecitizen
 * @see com.docutools.jocument.PlaceholderResolver
 * @since 2020-02-19
 */
public class ReflectionResolver implements PlaceholderResolver {

  private static final String SELF_REFERENCE = "this";

  private static final Logger logger = LogManager.getLogger();

  private final Object bean;
  private final PropertyUtilsBean pub = new PropertyUtilsBean();
  private final CustomPlaceholderRegistry customPlaceholderRegistry;

  public ReflectionResolver(Object value) {
    this.bean = value;
    this.customPlaceholderRegistry = new CustomPlaceholderRegistryImpl(); //NoOp CustomPlaceholderRegistry
  }

  public ReflectionResolver(Object value, CustomPlaceholderRegistry customPlaceholderRegistry) {
    this.bean = value;
    this.customPlaceholderRegistry = customPlaceholderRegistry;
  }

  private static boolean isFieldAnnotatedWith(Class<?> clazz, String fieldName, Class<? extends Annotation> annotation) {
    try {
      return clazz.getDeclaredField(fieldName)
          .getDeclaredAnnotation(annotation) != null;
    } catch (Exception e) {
      logger.debug("Class %s not annotated with %s".formatted(clazz, fieldName), e);
      return false;
    }
  }

  private static NumberFormat toNumberFormat(Percentage percentage, Locale locale) {
    var format = NumberFormat.getPercentInstance(locale);
    if (percentage.maxFractionDigits() > -1) {
      format.setMaximumFractionDigits(percentage.maxFractionDigits());
    }
    return format;
  }

  private static NumberFormat toNumberFormat(Money money, Locale locale) {
    var currency = !money.currencyCode().isBlank()
        ? Currency.getInstance(money.currencyCode()) :
        Currency.getInstance(locale);
    var format = NumberFormat.getCurrencyInstance(locale);
    format.setCurrency(currency);
    return format;
  }

  private static NumberFormat toNumberFormat(Numeric numeric, Locale locale) {
    var format = NumberFormat.getNumberInstance(locale);
    if (numeric.maxFractionDigits() != -1) {
      format.setMaximumFractionDigits(numeric.maxFractionDigits());
    }
    if (numeric.minFractionDigits() != -1) {
      format.setMinimumFractionDigits(numeric.minFractionDigits());
    }
    if (numeric.maxIntDigits() != -1) {
      format.setMaximumIntegerDigits(numeric.maxIntDigits());
    }
    if (numeric.minIntDigits() != -1) {
      format.setMinimumIntegerDigits(numeric.minIntDigits());
    }
    if (!numeric.currencyCode().equals("")) {
      format.setCurrency(Currency.getInstance(numeric.currencyCode()));
    }
    format.setGroupingUsed(numeric.groupingUsed());
    format.setParseIntegerOnly(numeric.parseIntegerOnly());
    if (numeric.roundingMode() != RoundingMode.UNNECESSARY) {
      format.setRoundingMode(numeric.roundingMode());
    }
    return format;
  }

  private static DateTimeFormatter toDateTimeFormatter(Format format) {
    var formatter = DateTimeFormatter.ofPattern(format.value());
    if (!format.zone().isBlank()) {
      formatter = formatter.withZone(ZoneId.of(format.zone()));
    }
    if (!format.locale().isBlank()) {
      formatter = formatter.withLocale(Locale.forLanguageTag(format.locale()));
    }
    return formatter;
  }

  @Override
  public Optional<PlaceholderData> resolve(String placeholderName, Locale locale) {
    logger.debug("Trying to resolve placeholder {}", placeholderName);
    Optional<PlaceholderData> result = Optional.empty();
    for (String property : placeholderName.split("\\.")) {
      result = result.isEmpty() ? doResolve(property, locale) :
          result
              .flatMap(r -> r.stream().findAny())
              .flatMap(r -> r.resolve(property, locale));
    }
    return result;
  }

  private Optional<PlaceholderData> doResolve(String placeholderName, Locale locale) {
    try {
      if (customPlaceholderRegistry.governs(placeholderName)) {
        return customPlaceholderRegistry.resolve(placeholderName);
      }
      var property = SELF_REFERENCE.equals(placeholderName) ? bean : pub.getProperty(bean, placeholderName);
      if (property == null) {
        return Optional.empty();
      }
      if (property instanceof Number number) {
        var numberFormat = findNumberFormat(placeholderName, locale);
        return Optional.of(new ScalarPlaceholderData(numberFormat.format(number)));
      } else if (property instanceof Enum || property instanceof String || ReflectionUtils.isWrapperType(property.getClass())) {
        return Optional.of(new ScalarPlaceholderData(property.toString()));
      } else if (property instanceof Collection<?> collection) {
        List<PlaceholderResolver> list = collection.stream()
            .map(object -> new ReflectionResolver(object, customPlaceholderRegistry))
            .collect(Collectors.toList());
        return Optional.of(new IterablePlaceholderData(list, list.size()));
      } else if (property instanceof Temporal temporal) {
        return formatTemporal(placeholderName, temporal, locale);
      } else if (property instanceof Path path && isFieldAnnotatedWith(bean.getClass(), placeholderName, Image.class)) {
        return ReflectionUtils.findFieldAnnotation(bean.getClass(), placeholderName, Image.class)
            .map(image -> new ImagePlaceholderData(path)
                .withMaxWidth(image.maxWidth()));
      }
      if (bean.equals(property)) {
        return Optional.of(new IterablePlaceholderData(List.of(new ReflectionResolver(bean)), 1));
      } else {
        var value = pub.getProperty(bean, placeholderName);
        return Optional.of(new IterablePlaceholderData(List.of(new ReflectionResolver(value, customPlaceholderRegistry)), 1));
      }
    } catch (NoSuchMethodException | IllegalArgumentException e) {
      logger.debug("Did not find placeholder {}", placeholderName);
      return Optional.empty();
    } catch (IllegalAccessException | InvocationTargetException e) {
      logger.error("Could not resolve placeholder %s".formatted(placeholderName), e);
      throw new IllegalStateException("Could not resolve placeholderName against type.", e);
    } catch (InstantiationException e) {
      logger.warn("InstantiationException when trying to resolve placeholder %s".formatted(placeholderName), e);
      return Optional.empty();
    }
  }

  private Optional<PlaceholderData> formatTemporal(String placeholderName, Temporal time, Locale locale) {
    Optional<DateTimeFormatter> formatter;
    if (isFieldAnnotatedWith(bean.getClass(), placeholderName, Format.class)) {
      formatter = ReflectionUtils.findFieldAnnotation(bean.getClass(), placeholderName, Format.class)
          .map(ReflectionResolver::toDateTimeFormatter);
    } else {
      if (time instanceof LocalDate) {
        formatter = Optional.of(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
      } else if (time instanceof LocalTime) {
        formatter = Optional.of(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
      } else if (time instanceof LocalDateTime) {
        formatter = Optional.of(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
      } else {
        logger.warn("Failed to format placeholder {} as temporal {}", placeholderName, time);
        formatter = Optional.empty();
      }
      formatter = formatter.map(dateTimeFormatter -> dateTimeFormatter.withLocale(locale));
    }
    return formatter.map(dateTimeFormatter -> new ScalarPlaceholderData(dateTimeFormatter.format(time)));
  }

  private NumberFormat findNumberFormat(String fieldName, Locale locale) {
    return ReflectionUtils.findFieldAnnotation(bean.getClass(), fieldName, Percentage.class)
        .map(percentage -> toNumberFormat(percentage, locale))
        .or(() -> ReflectionUtils.findFieldAnnotation(bean.getClass(), fieldName, Money.class)
            .map(money -> toNumberFormat(money, locale)))
        .or(() -> ReflectionUtils.findFieldAnnotation(bean.getClass(), fieldName, Numeric.class)
            .map(numeric -> toNumberFormat(numeric, locale)))
        .orElseGet(() -> {
          logger.info("Did not find formatting directive for {}, formatting according to locale {}", fieldName, locale);
          return NumberFormat.getInstance(locale);
        });
  }
}
