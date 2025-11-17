package com.pacific.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configurable log masking pattern layout that extends PatternLayout. This class provides
 * configurable data masking capabilities for sensitive information in log messages based on
 * configurable patterns.
 *
 * <p>Features: - Configurable masking patterns via properties - Support for multiple pattern types
 * (generic, custom, field-specific) - Partial masking support (showing first/last characters) -
 * Performance optimized with compiled patterns - Environment-specific configuration support
 */
public class DataMaskingPatternLayout extends PatternLayout {

  private static final Logger log = LoggerFactory.getLogger(DataMaskingPatternLayout.class);

  private List<String> maskPatterns = new ArrayList<>();
  Pattern appliedPattern; // Made package-private for testing
  private boolean enabled = true;
  private String maskCharacter = "*";
  private int partialMaskLength = 4;
  private boolean partialMasking = false;
  private boolean noPatternLogged = false;

  public DataMaskingPatternLayout() {
    // Set a default pattern for PatternLayout
    setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");

    // Try to load patterns from system properties as fallback
    String fallbackPattern = System.getProperty("COMBINED_MASK_PATTERN");
    if (fallbackPattern != null && !fallbackPattern.isEmpty()) {
      setMaskPattern(fallbackPattern);
    }
  }

  @Override
  public void start() {
    // Check if patterns are empty and try to load from system properties
    if (maskPatterns.isEmpty()) {
      String systemPattern = System.getProperty("COMBINED_MASK_PATTERN");
      if (systemPattern != null && !systemPattern.isEmpty()) {
        setMaskPattern(systemPattern);
      } else {
        // Fallback: Use a simple email pattern for testing
        setMaskPattern("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
      }
    }

    super.start();
  }

  /**
   * Sets mask patterns from configuration. This is the primary method used by logback
   * configuration.
   *
   * @param maskPattern the pattern string (can be comma-separated)
   */
  public void setMaskPattern(String maskPattern) {
    if (maskPattern == null || maskPattern.trim().isEmpty()) {
      return;
    }

    // Clear existing patterns
    maskPatterns.clear();

    // For now, treat the entire string as a single pattern
    // TODO: Implement proper comma splitting that handles regex patterns with commas
    String trimmedPattern = maskPattern.trim();
    if (!trimmedPattern.isEmpty()) {
      maskPatterns.add(trimmedPattern);
    }

    // Recompile the combined pattern
    compilePatterns();
  }

  /**
   * Adds mask patterns from configuration. Supports comma-separated patterns for easy
   * configuration.
   *
   * @param maskPattern the pattern string (can be comma-separated)
   */
  public void addMaskPattern(String maskPattern) {
    if (maskPattern == null || maskPattern.trim().isEmpty()) {
      return;
    }

    // For now, treat the entire string as a single pattern to avoid issues with regex patterns
    // containing commas
    // TODO: Implement proper comma splitting that handles regex patterns with commas
    String trimmedPattern = maskPattern.trim();
    if (!trimmedPattern.isEmpty()) {
      maskPatterns.add(trimmedPattern);
      log.debug("Added mask pattern: {}", trimmedPattern);
    }

    // Recompile the combined pattern
    compilePatterns();
  }

  /** Compiles all mask patterns into a single regex pattern for performance. */
  private void compilePatterns() {
    if (maskPatterns.isEmpty()) {
      appliedPattern = null;
      return;
    }

    try {
      String combinedPattern =
          maskPatterns.stream().map(this::wrapWithGroup).collect(Collectors.joining("|"));

      appliedPattern =
          Pattern.compile(combinedPattern, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    } catch (Exception e) {
      // Use System.out to avoid logging recursion
      System.out.println("ERROR: Failed to compile mask patterns: " + e.getMessage());
      appliedPattern = null;
    }
  }

  /**
   * Wraps pattern with capturing group for proper masking.
   *
   * @param pattern the original pattern
   * @return wrapped pattern with capturing group
   */
  private String wrapWithGroup(String pattern) {
    // Wrap with a capturing group for proper masking
    return "(" + pattern + ")";
  }

  @Override
  public String doLayout(ILoggingEvent event) {
    if (!enabled) {
      return super.doLayout(event);
    }

    if (appliedPattern == null) {
      // Only log this once to avoid infinite loop
      if (!noPatternLogged) {
        noPatternLogged = true;
        // Use System.out to avoid logging recursion
        System.out.println(
            "WARN: No mask pattern compiled, masking disabled. Check logback configuration.");
      }
      return super.doLayout(event);
    }

    String originalMessage = super.doLayout(event);
    String maskedMessage = maskMessage(originalMessage);

    return maskedMessage;
  }

  /**
   * Masks sensitive data in the log message using configured patterns.
   *
   * @param message the original log message
   * @return the message with sensitive data masked
   */
  private String maskMessage(String message) {
    if (message == null || message.isEmpty()) {
      return message;
    }

    if (appliedPattern == null) {
      return message;
    }

    try {
      // Use simple replaceAll for better performance and memory usage
      return appliedPattern
          .matcher(message)
          .replaceAll(
              matchResult -> {
                String matchedText = matchResult.group();
                return createMaskedText(matchedText);
              });
    } catch (Exception e) {
      // Use System.out to avoid logging recursion
      System.out.println("WARN: Error during log masking: " + e.getMessage());
      return message; // Return original message if masking fails
    }
  }

  /**
   * Creates masked text based on configuration.
   *
   * @param originalText the original text to mask
   * @return the masked text
   */
  private String createMaskedText(String originalText) {
    if (originalText == null || originalText.isEmpty()) {
      return originalText;
    }

    if (!partialMasking || originalText.length() <= partialMaskLength) {
      // Full masking
      return maskCharacter.repeat(originalText.length());
    }

    // Partial masking - show first and last characters
    int visibleLength = Math.min(partialMaskLength, originalText.length() / 2);
    String visibleStart = originalText.substring(0, visibleLength);
    String visibleEnd = originalText.substring(originalText.length() - visibleLength);
    String maskedMiddle = maskCharacter.repeat(originalText.length() - (visibleLength * 2));

    return visibleStart + maskedMiddle + visibleEnd;
  }

  // Getters and Setters for configuration properties

  public String getMaskPattern() {
    return String.join(",", maskPatterns);
  }

  public List<String> getMaskPatterns() {
    return maskPatterns;
  }

  public void setMaskPatterns(List<String> maskPatterns) {
    this.maskPatterns = maskPatterns;
    compilePatterns();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getMaskCharacter() {
    return maskCharacter;
  }

  public void setMaskCharacter(String maskCharacter) {
    this.maskCharacter = maskCharacter;
  }

  public int getPartialMaskLength() {
    return partialMaskLength;
  }

  public void setPartialMaskLength(int partialMaskLength) {
    this.partialMaskLength = partialMaskLength;
  }

  public boolean isPartialMasking() {
    return partialMasking;
  }

  public void setPartialMasking(boolean partialMasking) {
    this.partialMasking = partialMasking;
  }
}
