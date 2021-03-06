/*
 * Copyright (C) IBM Corp. 2009.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ibm.jaql.lang.expr.agg;

import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.lang.core.Context;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.function.DefaultBuiltInFunctionDescriptor;


/**
 * @jaqlDescription ensure that an array has only one element, otherwise, throw an exception
 * Usage:
 * T singleton( [T] );
 * 
 * @jaqlExample [1] -> singleton
 * 1
 * 
 * @jaqlExample [1,2] -> singleton // throws an exception
 *
 */
public final class SingletonAgg extends AlgebraicAggregate // TODO: should this preserve nulls?
{
  protected JsonValue saved;
  
  public static class Descriptor extends DefaultBuiltInFunctionDescriptor.Par11
  {
    public Descriptor()
    {
      super("singleton", SingletonAgg.class);
    }
  }
  
  /**
   * @param exprs
   */
  public SingletonAgg(Expr[] exprs)
  {
    super(exprs);
  }

  /**
   * @param expr
   */
  public SingletonAgg(Expr expr)
  {
    super(expr);
  }

  @Override
  public void init(Context context) throws Exception
  {
    saved = null;
  }

  /**
   * This aggregate keeps nested arrays as a single item, but it does discard nulls.
   */
  public void processInitial(Context context, JsonValue value) throws Exception
  {
    value = exprs[0].eval(context);
    if( value != null )
    {
      accumulate(value);
    }
  }

  @Override
  public void accumulate(JsonValue value) throws Exception
  {
    if( saved != null )
    {
      throw new RuntimeException("multiple items passed to singleton");
    }
    if( value != null )
    {
      saved = value.getCopy(null);
    }
  }

  @Override
  public JsonValue getPartial() throws Exception
  {
    return saved;
  }

  @Override
  public void combine(JsonValue value) throws Exception
  {
    accumulate(value);
  }

  @Override
  public JsonValue getFinal() throws Exception
  {
    return saved;
  }
}


//public class SingletonExpr extends Expr
//{
//  /**
//   * Expr input
//   * 
//   * @param exprs
//   */
//  public SingletonExpr(Expr[] exprs)
//  {
//    super(exprs);
//  }
//
//  /**
//   * @param input
//   */
//  public SingletonExpr(Expr input)
//  {
//    super(input);
//  }
//
//  /*
//   * (non-Javadoc)
//   * 
//   * @see com.ibm.jaql.lang.expr.core.Expr#decompile(java.io.PrintStream,
//   *      java.util.HashSet)
//   */
//  public void decompile(PrintStream exprText, HashSet<Var> capturedVars)
//      throws Exception
//  {
//    exprs[0].decompile(exprText, capturedVars);
//    exprText.print("\n-> singleton");
//  }
//
//  /*
//   * (non-Javadoc)
//   * 
//   * @see com.ibm.jaql.lang.expr.core.IterExpr#iter(com.ibm.jaql.lang.core.Context)
//   */
//  public Item eval(final Context context) throws Exception
//  {
//    Iter iter = exprs[0].iter(context);
//    Item item = iter.next();
//    if( item == null )
//    {
//      return Item.nil;
//    }
//    Item result = new Item();
//    result.copy(item);
//    if( iter.next() != null )
//    {
//      throw new RuntimeException("more than one item in singleton pipe");
//    }
//    return result;
//  }
//
//}
