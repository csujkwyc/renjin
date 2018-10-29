/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2018 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.parser;

/**
 * Opaque position class emitted by the Lexer
 * and consumed by the Parser.
 * <p/>
 * Corresponds to the C class {@code yyltype }
 */
public class Position {

  private int line;
  private int column;
  private int charIndex;

  public Position() {
  }

  public Position(int line, int column, int charIndex) {
    this.line = line;
    this.column = column;
    this.charIndex = charIndex;
  }

  @Override
  public Position clone() {
    return new Position(getLine(), getColumn(), getCharIndex());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Position position = (Position) o;

    if (getCharIndex() != position.getCharIndex()) {
      return false;
    }
    if (getColumn() != position.getColumn()) {
      return false;
    }
    if (getLine() != position.getLine()) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = getLine();
    result = 31 * result + getColumn();
    result = 31 * result + getCharIndex();
    return result;
  }

  @Override
  public String toString() {
    return "line " + (getLine() + 1) + " byte " + (getCharIndex() + 1) + " col " + (getColumn() + 1);
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public int getCharIndex() {
    return charIndex;
  }

}
