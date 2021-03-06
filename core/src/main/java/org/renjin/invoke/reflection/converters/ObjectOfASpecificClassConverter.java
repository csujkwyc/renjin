/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2019 BeDataDriven Groep B.V. and contributors
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
package org.renjin.invoke.reflection.converters;

import org.renjin.sexp.ExternalPtr;
import org.renjin.sexp.Null;
import org.renjin.sexp.SEXP;


/**
 * Converter for a class of object that we don't do anything special
 * with: not java.lang.Object or java.lang.String, or java.lang.Number.
 */
public class ObjectOfASpecificClassConverter implements Converter<Object> {

  private Class clazz;

  public ObjectOfASpecificClassConverter(Class clazz) {
    this.clazz = clazz;
  }

  @Override
  public SEXP convertToR(Object value) {
    if(value == null) {
      return Null.INSTANCE;
    }
    return new ExternalPtr(value);
  }

  @Override
  public Object convertToJava(SEXP exp) {

    // try to simply unwrap 
    if(exp instanceof ExternalPtr) {
      ExternalPtr ptr = (ExternalPtr)exp;
      if(clazz.isAssignableFrom(ptr.getInstance().getClass())) {
        return ptr.getInstance();
      }
    }
    throw new ConversionException();
  }

  @Override
  public boolean acceptsSEXP(SEXP exp) {
    try {
      convertToJava(exp);
      return true;

    } catch(ConversionException e) {
      return false;
    }
  }

  @Override
  public int getSpecificity() {
    return Specificity.SPECIFIC_OBJECT;
  }
}
