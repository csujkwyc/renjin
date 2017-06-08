/**
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2016 BeDataDriven Groep B.V. and contributors
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
package org.renjin.compiler.builtins;

import org.renjin.compiler.codegen.EmitContext;
import org.renjin.compiler.ir.ArgumentBounds;
import org.renjin.compiler.ir.ValueBounds;
import org.renjin.compiler.ir.tac.IRArgument;
import org.renjin.invoke.model.JvmMethod;
import org.renjin.primitives.Primitives;
import org.renjin.repackaged.asm.Type;
import org.renjin.repackaged.asm.commons.InstructionAdapter;
import org.renjin.repackaged.guava.collect.Lists;
import org.renjin.sexp.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Specialization for builtins that are marked {@link org.renjin.invoke.annotations.DataParallel} and
 * whose arguments are "recycled" for multiple calls.
 */
public class DataParallelCall implements Specialization {

  private final String name;
  private final JvmMethod method;
  private List<ArgumentBounds> argumentBounds;
  private final ValueBounds resultBounds;
  private final Type type;

  public DataParallelCall(Primitives.Entry primitive, JvmMethod method, List<ArgumentBounds> argumentBounds) {
    this.name = primitive.name;
    this.method = method;
    this.argumentBounds = argumentBounds;
    this.resultBounds = computeBounds(argumentBounds);
    this.type = resultBounds.storageType();
  }

  
  private ValueBounds computeBounds(List<ArgumentBounds> argumentBounds) {
    
    List<ArgumentBounds> recycledArguments = recycledArgumentBounds(argumentBounds);

    int resultLength = computeResultLength(this.argumentBounds);

    ValueBounds.Builder bounds = new ValueBounds.Builder();
    bounds.setType(method.getReturnType());
    bounds.setNA(anyNAs(argumentBounds));
    bounds.setLength(resultLength);
    
    switch (method.getPreserveAttributesStyle()) {
      case NONE:
        bounds.setEmptyAttributes();
        break;
      case STRUCTURAL:
        buildStructuralBounds(bounds, recycledArguments, resultLength);
        break;
      case ALL:
        buildAllBounds(bounds, recycledArguments, resultLength);
        break;
    }
    
    return bounds.build();
  }

  private int anyNAs(List<ArgumentBounds> argumentBounds) {
    for (ArgumentBounds argumentBound : argumentBounds) {
      if(argumentBound.getValueBounds().getNA() == ValueBounds.MAY_HAVE_NA) {
        return ValueBounds.MAY_HAVE_NA;
      }
    }
    return ValueBounds.NO_NA;
  }

  /**
   * Makes a list of {@link ValueBounds} for @Recycled arguments.
   */
  private List<ArgumentBounds> recycledArgumentBounds(List<ArgumentBounds> argumentBounds) {
    List<ArgumentBounds> list = Lists.newArrayList();
    Iterator<ArgumentBounds> argumentIt = argumentBounds.iterator();
    for (JvmMethod.Argument formal : method.getFormals()) {
      if (formal.isRecycle()) {
        list.add(argumentIt.next());
      }
    }
    return list;
  }
  
  private int computeResultLength(List<ArgumentBounds> argumentBounds) {
    Iterator<ArgumentBounds> it = argumentBounds.iterator();
    int resultLength = 0;
    
    while(it.hasNext()) {
      int argumentLength = it.next().getValueBounds().getLength();
      if(argumentLength == ValueBounds.UNKNOWN_LENGTH) {
        return ValueBounds.UNKNOWN_LENGTH;
      }
      if(argumentLength == 0) {
        return 0;
      }
      resultLength = Math.max(resultLength, argumentLength);
    }

    return resultLength;
  }
  
  private void buildStructuralBounds(ValueBounds.Builder bounds, List<ArgumentBounds> argumentBounds, int resultLength) {

    Map<Symbol, SEXP> attributes = new HashMap<>();
    attributes.put(Symbols.DIM, combineAttribute(Symbols.DIM, argumentBounds, resultLength));
    attributes.put(Symbols.DIMNAMES, combineAttribute(Symbols.DIM, argumentBounds, resultLength));
    attributes.put(Symbols.NAMES, combineAttribute(Symbols.DIM, argumentBounds, resultLength));
    bounds.setClosedAttributes(attributes);
    
  }
  
  private SEXP combineAttribute(Symbol symbol, List<ArgumentBounds> argumentBounds, int resultLength) {

    // If we don't know the result length, we don't know which 
    // argument to take the attributes from.
    if(resultLength == ValueBounds.UNKNOWN_LENGTH && argumentBounds.size() > 1) {
      return null; // unknown
    }
    
    for (ArgumentBounds argumentBound : argumentBounds) {
      if (argumentBound.getValueBounds().getLength() == resultLength) {

        SEXP value = argumentBound.getValueBounds().getAttributeIfConstant(symbol);
        if (value != Null.INSTANCE) {
          return value;
        }
      }
    }
    return Null.INSTANCE;
  }


  private void buildAllBounds(ValueBounds.Builder bounds, List<ArgumentBounds> argumentBounds, int resultLength) {


    // If we don't know the result length, we don't know which 
    // argument to take the attributes from.
    if(resultLength == ValueBounds.UNKNOWN_LENGTH && argumentBounds.size() > 1) {
      // TOOD: if all argument bounds have closed attribute sets, then we can still 
      // infer SOME information
      return;
    } 

    Map<Symbol, SEXP> attributes = new HashMap<>();

    boolean open = false;
    
    for (ArgumentBounds argumentBound : argumentBounds) {
      if (argumentBound.getValueBounds().getLength() == resultLength) {
        
        if(argumentBound.getValueBounds().isAttributeSetOpen()) {
          open = true;
        }

        for (Map.Entry<Symbol, SEXP> entry : argumentBound.getValueBounds().getAttributeBounds().entrySet()) {
          if(!attributes.containsKey(entry.getKey())) {
            attributes.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    bounds.setAttributeBounds(attributes);
    bounds.setAttributeSetOpen(open);
  }


  public Specialization specializeFurther() {
    if(resultBounds.getLength() == 1) {

      if(ValueBounds.allConstant(argumentBounds)) {
        return evaluateConstant();
      }

      DoubleBinaryOp op = DoubleBinaryOp.trySpecialize(name, method, resultBounds);
      if(op != null) {
        return op;
      }
      if(resultBounds.getNA() == ValueBounds.NO_NA) {
        return new DataParallelScalarCall(method, argumentBounds, resultBounds).trySpecializeFurther();
      }
    }
    return this;
  }

  private Specialization evaluateConstant() {

    assert !method.acceptsArgumentList();

    List<JvmMethod.Argument> formals = method.getAllArguments();
    Object[] args = new Object[formals.size()];
    Iterator<ValueBounds> it = argumentBounds.iterator();
    int argI = 0;
    for (JvmMethod.Argument formal : formals) {
      if(formal.isContextual()) {
        throw new UnsupportedOperationException("in " + method +  ", " + "formal: " + formal);
      } else {
        ValueBounds argument = it.next();
        args[argI++] = convert(argument.getConstantValue(), formal.getClazz());

      }
    }

    Object constantValue;
    try {
      constantValue = method.getMethod().invoke(null, args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new ConstantCall(constantValue);

  }

  private Object convert(SEXP constantValue, Class formalType) {
    if(formalType.equals(double.class)) {
      return constantValue.asReal();
    } else if(formalType.equals(int.class)) {
      return constantValue.asInt();
    } else if(formalType.equals(String.class)) {
      return constantValue.asString();
    } else if(SEXP.class.isAssignableFrom(formalType)) {
      return constantValue;
    } else {
      throw new UnsupportedOperationException("formal type: " + formalType);
    }
  }

  @Override
  public Type getType() {
    return type;
  }

  public ValueBounds getResultBounds() {
    return resultBounds;
  }

  @Override
  public void load(EmitContext emitContext, InstructionAdapter mv, List<IRArgument> arguments) {
    throw new UnsupportedOperationException();
  }
}
