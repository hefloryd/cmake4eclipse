/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.lsp;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible to match the first argument (the tool command) of a
 * command-line.
 *
 * @author Martin Weber
 */
public class DefaultToolDetectionParticipant implements IToolDetectionParticipant {
  /** start of the named capturing group that holds the command name (w/o quotes, if any) */
  protected static final String REGEX_GROUP_CMD = "cmd";

  /** pattern part that matches file-system paths with forward slashes */
  protected static final String REGEX_CMD_PATH_SLASH = String.format(Locale.ROOT, "\\A(?<%s>(\\S*?%s)?",
      REGEX_GROUP_CMD, "/");
  /** end of pattern part that matches file-system paths with forward slashes */
  protected static final String REGEX_CMD_PATH_SLASH_END = ")\\s";

  /** pattern part that matches file-system paths with forward slashes and is in quotes */
  protected static final String REGEX_CMD_PATH_SLASH_QUOTE = String.format(Locale.ROOT, "\\A([\"'])(?<%s>(?:(?!\\1).)*?%s",
      REGEX_GROUP_CMD, "/");
  /** end of pattern part that matches file-system paths with forward slashes and is in quotes */
  protected static final String REGEX_CMD_PATH_SLASH_QUOTE_END = ")\\1\\s";

  /** pattern part that matches win32 file-system paths */
  protected static final String REGEX_CMD_PATH_BSLASH = String.format(Locale.ROOT, "\\A(?<%s>(\\S*?%s)?",
      REGEX_GROUP_CMD, Pattern.quote("\\"));
  /** end of pattern part that matches win32 file-system paths */
  protected static final String REGEX_CMD_PATH_BSLASH_END = ")\\s";

  /** pattern part that matches file-system paths with back slashes and is in quotes */
  protected static final String REGEX_CMD_PATH_BSLASH_QUOTE = String.format(Locale.ROOT, "\\A([\"'])(?<%s>(?:(?!\\1).)*?%s",
      REGEX_GROUP_CMD, Pattern.quote("\\"));
  /** end of pattern part that matches file-system paths with back slashes and is in quotes */
  protected static final String REGEX_CMD_PATH_BSLASH_QUOTE_END = ")\\1\\s";

  /**
   * the Matchers to match the name of the tool (including its path, BUT WITHOUT its filename extension) on a given
   * command-line
   */
  private final Matcher[] toolNameMatchers;
  private final Matcher[] toolNameMatchersBackslash;

  /**
   * the Matcher that matches the name of the tool (including its path AND its
   * filename extension) on a given command-line or {@code null}
   */
  protected final Matcher[] toolNameMatchersExt;
  protected final Matcher[] toolNameMatchersExtBackslash;

  /**
   * the corresponding parser for the tool arguments
   */
  private final IToolCommandlineParser parser;

  protected final String basenameRegex;
  protected final String extensionRegex;

  /**
   * whether this object can handle NTFS file system paths in the compiler argument in addition to a Linux path (which
   * has forward slashes to separate path name components). If {@code true} the detection logic will also try to match
   * path name with backslashes and will try to expand windows short paths like <code>C:/pr~1/aa~1.exe</code>.
   */
  protected final boolean alsoHandleNtfsPaths;


  /**
   * Creates a {@code DefaultToolDetectionParticipant} that matches linux paths in the tool
   * name.
   *
   * @param basenameRegex
   *          a regular expression that matches the base name of the tool to detect. The specified expression should be
   *          non-greedy.
   * @param parser
   *          the corresponding parser for the tool arguments
   * @throws NullPointerException
   *           if one of the arguments is {@code null}
   */
  public DefaultToolDetectionParticipant(String basenameRegex, IToolCommandlineParser parser) {
    this(basenameRegex, false, parser);
  }

  /**
   * Creates a {@code DefaultToolDetectionParticipant}.
   *
   * @param basenameRegex
   *          a regular expression that matches the base name of the tool to detect. The specified expression should be
   *          non-greedy.
   * @param alsoHandleNtfsPaths
   *          whether this object can handle NTFS file system paths in the compiler argument in addition to a Linux
   *          path (which has forward slashes to separate path name components). If {@code true} the detection logic will also try to match path name
   *          with backslashes and will try to expand windows short paths like <code>C:/pr~1/aa~1.exe</code>.
   * @param parser
   *          the corresponding parser for the tool arguments
   */
  public DefaultToolDetectionParticipant(String basenameRegex, boolean alsoHandleNtfsPaths, IToolCommandlineParser parser) {
    this(basenameRegex, alsoHandleNtfsPaths, null, parser);
 }

  /**
   * a {@code ParserDetectorExt} that matches linux paths in the tool name.
   *
   * @param basenameRegex
   *          a regular expression that matches the base name of the tool to detect. The specified expression should be
   *          non-greedy.
   * @param extensionRegex
   *          a regular expression that matches the filename extension of the
   *          tool to detect.
   * @param parser
   *          the corresponding parser for the tool arguments
   */
  public DefaultToolDetectionParticipant(String basenameRegex, String extensionRegex, IToolCommandlineParser parser) {
    this(basenameRegex, false, extensionRegex, parser);
  }

  /**
   * Creates a {@code DefaultToolDetectionParticipant}.
   *
   * @param basenameRegex
   *          a regular expression that matches the base name of the tool to detect. The specified expression should be
   *          non-greedy.
   * @param alsoHandleNtfsPaths
   *          whether this object can handle NTFS file system paths in the compiler argument in addition to a Linux
   *          path (which has forward slashes to separate path name components). If {@code true} the detection logic will also try to match path name
   *          with backslashes and will try to expand windows short paths like <code>C:/pr~1/aa~1.exe</code>.
   * @param extensionRegex
   *          a regular expression that matches the filename extension of the
   *          tool to detect.
   * @param parser
   *          the corresponding parser for the tool arguments
   */
  public DefaultToolDetectionParticipant(String basenameRegex, boolean alsoHandleNtfsPaths, String extensionRegex,
      IToolCommandlineParser parser) {
    this.basenameRegex = basenameRegex;
    this.parser = parser;
    this.alsoHandleNtfsPaths = alsoHandleNtfsPaths;
    this.extensionRegex = extensionRegex;

    this.toolNameMatchers = new Matcher[] {
        Pattern.compile(String.format(Locale.ROOT, "%s%s%s", REGEX_CMD_PATH_SLASH_QUOTE, basenameRegex,
            REGEX_CMD_PATH_SLASH_QUOTE_END)).matcher(""),
        Pattern
            .compile(
                String.format(Locale.ROOT, "%s%s%s", REGEX_CMD_PATH_SLASH, basenameRegex, REGEX_CMD_PATH_SLASH_END))
            .matcher("") };
    if (alsoHandleNtfsPaths) {
      this.toolNameMatchersBackslash = new Matcher[] {
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s", REGEX_CMD_PATH_BSLASH_QUOTE, basenameRegex,
              REGEX_CMD_PATH_BSLASH_QUOTE_END)).matcher(""),
          Pattern.compile(
              String.format(Locale.ROOT, "%s%s%s", REGEX_CMD_PATH_BSLASH, basenameRegex, REGEX_CMD_PATH_BSLASH_END))
              .matcher("") };
    } else {
      this.toolNameMatchersBackslash = new Matcher[] {};
    }

    if(extensionRegex != null) {
      this.toolNameMatchersExt = new Matcher[] {
          Pattern.compile(String.format(Locale.ROOT, "%s%s\\.%s%s", REGEX_CMD_PATH_SLASH_QUOTE, basenameRegex,
              extensionRegex, REGEX_CMD_PATH_SLASH_QUOTE_END)).matcher(""),
          Pattern.compile(String.format(Locale.ROOT, "%s%s\\.%s%s", REGEX_CMD_PATH_SLASH, basenameRegex,
              extensionRegex, REGEX_CMD_PATH_SLASH_END)).matcher("") };
      if (alsoHandleNtfsPaths) {
        this.toolNameMatchersExtBackslash = new Matcher[] {
            Pattern.compile(String.format(Locale.ROOT, "%s%s\\.%s%s", REGEX_CMD_PATH_BSLASH_QUOTE, basenameRegex,
                extensionRegex, REGEX_CMD_PATH_BSLASH_QUOTE_END)).matcher(""),
            Pattern.compile(String.format(Locale.ROOT, "%s%s\\.%s%s", REGEX_CMD_PATH_BSLASH, basenameRegex,
                extensionRegex, REGEX_CMD_PATH_BSLASH_END)).matcher("") };
      } else {
        this.toolNameMatchersExtBackslash = new Matcher[] {};
      }
    } else {
      this.toolNameMatchersExt = new Matcher[] {};
      this.toolNameMatchersExtBackslash = new Matcher[] {};
    }
  }

  @Override
  public IToolCommandlineParser getParser() {
    return parser;
  }

  @Override
  public boolean canHandleNtfsPaths() {
    return alsoHandleNtfsPaths;
  }

  @Override
  public DefaultToolDetectionParticipant.MatchResult basenameMatches(String commandLine, boolean matchBackslash) {
    if(matchBackslash && !canHandleNtfsPaths()) {
      return null;
    }
    for (Matcher matcher : matchBackslash ? toolNameMatchersBackslash : toolNameMatchers) {
      DefaultToolDetectionParticipant.MatchResult result = matcherMatches(matcher, commandLine);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public DefaultToolDetectionParticipant.MatchResult basenameWithVersionMatches(String commandLine, boolean matchBackslash, String versionRegex) {
    if(matchBackslash && !canHandleNtfsPaths()) {
      return null;
    }

    Matcher[] toolNameMatchers;
    if (matchBackslash) {
      toolNameMatchers = new Matcher[] {
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s%s", REGEX_CMD_PATH_BSLASH_QUOTE, basenameRegex,
              versionRegex, REGEX_CMD_PATH_BSLASH_QUOTE_END)).matcher(""),
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s%s", REGEX_CMD_PATH_BSLASH, basenameRegex, versionRegex,
              REGEX_CMD_PATH_BSLASH_END)).matcher("") };
    } else {
      toolNameMatchers = new Matcher[] {
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s%s", REGEX_CMD_PATH_SLASH_QUOTE, basenameRegex,
              versionRegex, REGEX_CMD_PATH_SLASH_QUOTE_END)).matcher(""),
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s%s", REGEX_CMD_PATH_SLASH, basenameRegex, versionRegex,
              REGEX_CMD_PATH_SLASH_END)).matcher("") };
    }

    for (Matcher matcher : toolNameMatchers) {
      DefaultToolDetectionParticipant.MatchResult result = matcherMatches(matcher, commandLine);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public DefaultToolDetectionParticipant.MatchResult basenameWithExtensionMatches(String commandLine, boolean matchBackslash) {
    if(matchBackslash && !canHandleNtfsPaths()) {
      return null;
    }
    for (Matcher matcher : matchBackslash ? toolNameMatchersExtBackslash: toolNameMatchersExt) {
      DefaultToolDetectionParticipant.MatchResult result = matcherMatches(matcher, commandLine);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public DefaultToolDetectionParticipant.MatchResult basenameWithVersionAndExtensionMatches(String commandLine, boolean matchBackslash, String versionRegex) {
    if(matchBackslash && !canHandleNtfsPaths()) {
      return null;
    }

    Matcher[] toolNameMatchers;
    if (matchBackslash) {
      toolNameMatchers = new Matcher[] {
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s\\.%s%s",
              REGEX_CMD_PATH_BSLASH_QUOTE , basenameRegex , versionRegex , extensionRegex
                  , REGEX_CMD_PATH_BSLASH_QUOTE_END))
              .matcher(""),
          Pattern
              .compile(String.format(Locale.ROOT, "%s%s%s\\.%s%s",
                  REGEX_CMD_PATH_BSLASH , basenameRegex , versionRegex , extensionRegex , REGEX_CMD_PATH_BSLASH_END))
              .matcher("") };
    } else {
      toolNameMatchers = new Matcher[] {
          Pattern.compile(String.format(Locale.ROOT, "%s%s%s\\.%s%s",
              REGEX_CMD_PATH_SLASH_QUOTE , basenameRegex , versionRegex , extensionRegex
                  , REGEX_CMD_PATH_SLASH_QUOTE_END))
              .matcher(""),
          Pattern
              .compile(String.format(Locale.ROOT, "%s%s%s\\.%s%s",
                  REGEX_CMD_PATH_SLASH , basenameRegex , versionRegex , extensionRegex , REGEX_CMD_PATH_SLASH_END))
              .matcher("") };
    }

    for (Matcher matcher : toolNameMatchers) {
      DefaultToolDetectionParticipant.MatchResult result = matcherMatches(matcher, commandLine);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Gets, whether the specified Matcher for the tool arguments can properly
   * parse the specified command-line string. If so, the remaining arguments
   * of the command-line are returned.
   *
   * @param matcher
   *          the matcher that performs the match a regular expression that
   *          matches the version string in the name of the tool to detect.
   * @param commandLine
   *          the command-line to match
   * @return {@code null} if the matcher did not match the tool name in the
   *         command-line string. Otherwise, if the tool name matches, a
   *         MatchResult holding the de-composed command-line is returned.
   */
  private DefaultToolDetectionParticipant.MatchResult matcherMatches(Matcher matcher, String commandLine) {
    matcher.reset(commandLine);
    if (matcher.lookingAt()) {
      return new DefaultToolDetectionParticipant.MatchResult(matcher.group(REGEX_GROUP_CMD), commandLine.substring(matcher.end()));
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [basenameRegex=" + basenameRegex + ", extensionRegex=" + extensionRegex + "]";
  }


} // DefaultToolDetectionParticipant