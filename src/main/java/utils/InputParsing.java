/*

Copyright 2024 Massimo Santini

This file is part of "Programmazione 2 @ UniMI" teaching material.

This is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This material is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this file.  If not, see <https://www.gnu.org/licenses/>.

*/

package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for input parsing.
 *
 * <p>This class provides methods to analyze input lines and support the creation of the entities
 * required by the project. It uses three tools to achieve this:
 *
 * <ul>
 *   <li>Three <em>descriptors</em> for the three entities <strong>index</strong>,
 *       <strong>column</strong>, and <strong>table</strong>;
 *   <li>A <em>parser</em> to convert an input line into one of these descriptors;
 *   <li>A <em>parser</em> to convert an input line into an array of values, where each element is
 *       of the most specific possible type.
 * </ul>
 *
 * <h2>Descriptors and their parsing</h2>
 *
 * The descriptors provided by this class are:
 *
 * <ul>
 *   <li>{@link IndexDescriptor} which contains the number of rows and the name of the index;
 *   <li>{@link ColumnDescriptor} which contains the number of rows, the type, and the name of the
 *       column;
 *   <li>{@link TableDescriptor} which contains the number of rows, the number of columns, and the
 *       type of the table.
 * </ul>
 *
 * <p>The {@link #parseDescriptor(String)} method parses a string and returns the corresponding
 * descriptor object. If the line does not contain a descriptor (e.g., it do not begin with <code>#
 * </code> followed by <code>index</code>, <code>column</code>, or <code>table</code>), the method
 * returns {@code null}. This fact can be used, for instance, when parsing a column, to decide if
 * the line following the column descriptor is an index descriptor, or if the index is implicitly
 * assumed to be a numeric indice, hence the line contains values.
 *
 * <h2>Parsing values</h2>
 *
 * <p>Once a line has been read, it can be parsed using the {@link #parseValues(String, int)}
 * method. This method takes a line and the number of values to parse, and returns an array of
 * objects. The method uses a {@link Scanner} to read the input line and attempts to parse each
 * value based on its type. The array can be used (casted as needed) to create the corresponding
 * entity.
 *
 * <h2>Example: read a column</h2>
 *
 * The following code snippet shows how to read a column from a {@link Scanner} (for instance, in
 * one of the clients). The part between the <code>... here ...</code> comments is left to the user,
 * as it depends on the specific constructors for the column and index classes he designed.
 *
 * <pre>
 * String line = s.nextLine();
 * ColumnDescriptor cd = (ColumnDescriptor) InputParsing.parseDescriptor(line);
 * if (cd == null) throw new IllegalArgumentException("Invalid column descriptor: " + line);
 * line = s.nextLine();
 * IndexDescriptor id = (IndexDescriptor) InputParsing.parseDescriptor(line);
 * if (id != null) {
 * Object[] labels = InputParsing.parseValues(s.nextLine(), cd.rows());
 * ... here create the index using the labels ...
 * line = s.nextLine();
 * }
 * Object[] values = InputParsing.parseValues(line, cd.rows());
 * ... here create the column using the values and index ...
 * </pre>
 */
public class InputParsing {

  /** The regular expression used in {@link #parseDescriptor(String)} */
  static final Pattern DESCRIPTOR_RE =
      Pattern.compile("#(\\w+)\\[(\\d+)(?:\\s*,\\s*([^,\\]]+))?(?:\\s*,\\s*([^\\]]+))?\\]");

  /**
   * The {@link DateTimeFormatter} to parse and convert from <code>datetime</code> to {@link String}
   */
  static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  /** To prevent instantiation */
  private InputParsing() {}

  /** Interface that collects specific descriptors, does not have its own methods. */
  public interface Descriptor {}

  /**
   * Converts a string representation of a type to its corresponding {@link Class} object.
   *
   * <p>Supported types:
   *
   * <ul>
   *   <li><code>string</code> -> {@link String}
   *   <li><code>boolean</code> -> {@link Boolean}
   *   <li><code>number</code> -> {@link Number}
   *   <li><code>integer</code> -> {@link Integer}
   *   <li><code>double</code> -> {@link Double}
   *   <li><code>datetime</code> -> {@link LocalDateTime}
   *   <li>Any other string -> {@link Object}
   * </ul>
   *
   * @param typeName the string representation of the type
   * @return the corresponding {@link Class} object
   */
  private static Class<?> stringToType(String typeName) {
    if (typeName == null) return Object.class;
    return switch (typeName.trim().toLowerCase()) {
      case "string" -> String.class;
      case "boolean" -> Boolean.class;
      case "number" -> Number.class;
      case "integer" -> Integer.class;
      case "double" -> Double.class;
      case "datetime" -> LocalDateTime.class;
      default -> Object.class;
    };
  }

  /**
   * A record representing an index descriptor with a specified length and name.
   *
   * @param len the length of the index
   * @param name the name of the index
   */
  public record IndexDescriptor(int len, String name) implements Descriptor {
    /**
     * Constructs an {@link IndexDescriptor} with the specified length and name.
     *
     * @param len the length of the index
     * @param name the name of the index (will be automatically trimmed of leading and trailing
     *     whitespace if not {@code null})
     * @throws IllegalArgumentException if {@code len} is less than or equal to 0
     */
    public IndexDescriptor(int len, String name) {
      if (len <= 0) throw new IllegalArgumentException("Length must be positive");
      this.len = len;
      this.name = name == null ? null : name.trim();
    }
  }

  /**
   * A record representing a column descriptor with a specified number of rows, type, and name.
   *
   * @param rows the number of rows in the column
   * @param type the type of the column
   * @param name the name of the column
   */
  public record ColumnDescriptor(int rows, Class<?> type, String name) implements Descriptor {
    /**
     * Constructs a {@link ColumnDescriptor} with the specified number of rows, type, and name.
     *
     * @param rows the number of rows in the column
     * @param type the type of the column (will be automatically set to {@link Object} if null)
     * @param name the name of the column (will be automatically trimmed of leading and trailing
     *     whitespace if not {@code null})
     * @throws IllegalArgumentException if {@code rows} is less than or equal to 0
     */
    public ColumnDescriptor(int rows, Class<?> type, String name) {
      if (rows <= 0) throw new IllegalArgumentException("Row number must be positive");
      this.rows = rows;
      this.type = type == null ? Object.class : type;
      this.name = name == null ? null : name.trim();
    }
  }

  /**
   * A record representing a table descriptor with a specified number of rows, columns, and type.
   *
   * @param rows the number of rows in the table
   * @param cols the number of columns in the table
   * @param type the type of the table (defaults to {@link Object} if null)
   */
  public record TableDescriptor(int rows, int cols, Class<?> type) implements Descriptor {
    /**
     * Constructs a {@link TableDescriptor} with the specified number of rows, columns, and type.
     *
     * @param rows the number of rows in the table
     * @param cols the number of columns in the table
     * @param type the type of the table (will be automatically set to {@link Object} if null)
     * @throws IllegalArgumentException if {@code rows} or {@code cols} is less than or equal to 0
     */
    public TableDescriptor(int rows, int cols, Class<?> type) {
      if (rows <= 0) throw new IllegalArgumentException("Row number must be positive");
      if (cols <= 0) throw new IllegalArgumentException("Column number must be positive");
      this.rows = rows;
      this.cols = cols;
      this.type = type == null ? Object.class : type;
    }
  }

  /**
   * Parses a descriptor string and returns the corresponding descriptor object.
   *
   * <p>Supported descriptor types:
   *
   * <ul>
   *   <li><b>index</b>: Represents an index descriptor with a specified length and optional name.
   *       Example: {@code #index[10, name]}
   *   <li><b>column</b>: Represents a column descriptor with a specified number of rows, type, and
   *       optional name. Example: {@code #column[5, integer, columnName]}
   *   <li><b>table</b>: Represents a table descriptor with a specified number of rows, columns, and
   *       type. Example: {@code #table[3, 4, string]}
   * </ul>
   *
   * @param line the descriptor string to parse
   * @return the parsed {@link Descriptor} object, or {@code null} if the input does not match the
   *     pattern
   * @throws NullPointerException if {@code line} is {@code null}
   * @throws IllegalArgumentException if the descriptor type is unknown or if parsing fails
   */
  public static Descriptor parseDescriptor(String line) {
    Matcher matcher = DESCRIPTOR_RE.matcher(Objects.requireNonNull(line));
    if (matcher.matches())
      try {
        return switch (matcher.group(1)) {
          case "index" -> new IndexDescriptor(Integer.parseInt(matcher.group(2)), matcher.group(3));
          case "column" ->
              new ColumnDescriptor(
                  Integer.parseInt(matcher.group(2)),
                  stringToType(matcher.group(3)),
                  matcher.groupCount() == 3 ? null : matcher.group(4));
          case "table" ->
              new TableDescriptor(
                  Integer.parseInt(matcher.group(2)),
                  Integer.parseInt(matcher.group(3)),
                  stringToType(matcher.group(4)));
          default ->
              throw new IllegalArgumentException("Unknown descriptor type: " + matcher.group(1));
        };
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Error parsing descriptor: " + line, e);
      }
    return null;
  }

  /**
   * Parses a line of input and returns an array of values.
   *
   * <p>This method uses a {@link Scanner} to read the input line and attempts to parse each value
   * based on its type. The types supported are:
   *
   * <ul>
   *   <li>{@link Boolean}
   *   <li>{@link Integer}
   *   <li>{@link Double}
   *   <li>{@link String} (converting the string
   *       <pre>null</pre>
   *       to {@code null} value)
   *   <li>{{@link LocalDateTime} (formatted as ISO_DATE_TIME)
   * </ul>
   *
   * @param line the input line to parse
   * @param n the number of values to parse
   * @return an array of parsed values
   * @throws NullPointerException if {@code line} is {@code null}
   */
  public static Object[] parseValues(String line, int n) {
    Object[] result = new Object[n];
    try (Scanner s = new Scanner(Objects.requireNonNull(line))) {
      for (int i = 0; i < n; i++) {
        Object object = null;
        if (s.hasNextBoolean()) object = s.nextBoolean();
        else if (s.hasNextInt()) object = s.nextInt();
        else if (s.hasNextDouble()) object = s.nextDouble();
        else {
          String str = s.next();
          try {
            object = LocalDateTime.parse(str, DATE_FORMATTER);
          } catch (DateTimeParseException e) {
            object = str.equalsIgnoreCase("null") ? null : str;
          }
        }
        result[i] = object;
      }
    }
    return result;
  }
}
