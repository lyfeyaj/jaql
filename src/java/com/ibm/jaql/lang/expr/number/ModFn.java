/*
 * Copyright (C) IBM Corp. 2008.
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
package com.ibm.jaql.lang.expr.number;

import java.math.BigDecimal;
import java.math.MathContext;

import com.ibm.jaql.json.schema.Schema;
import com.ibm.jaql.json.type.JsonDecimal;
import com.ibm.jaql.json.type.JsonDouble;
import com.ibm.jaql.json.type.JsonLong;
import com.ibm.jaql.json.type.JsonNumber;
import com.ibm.jaql.lang.core.Context;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.core.MathExpr;
import com.ibm.jaql.lang.expr.function.DefaultBuiltInFunctionDescriptor;

/**
 * @jaqlDescription Return the modulus of a and b, both a and b are numeric values 
 * 
 * Usage:
 * number mod(number a, number b)
 * 
 * @jaqlExample mod(3,2)
 * 1
 *  
 */
public class ModFn extends Expr
{
  public static class Descriptor extends DefaultBuiltInFunctionDescriptor.Par22
  {
    public Descriptor()
    {
      super("mod", ModFn.class);
    }
  }
  
  /**
   * @param exprs
   */
  public ModFn(Expr[] exprs)
  {
    super(exprs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.lang.expr.core.Expr#eval(com.ibm.jaql.lang.core.Context)
   */
  public JsonNumber eval(final Context context) throws Exception
  {
    JsonNumber value1 = (JsonNumber)exprs[0].eval(context);
    if (value1 == null)
    {
      return null;
    }
    JsonNumber value2 = (JsonNumber)exprs[1].eval(context);
    if (value2 == null)
    {
      return null;
    }

    if( value1 instanceof JsonDecimal || value2 instanceof JsonDecimal )
    {
      BigDecimal x = value1.decimalValue();
      BigDecimal y = value2.decimalValue();
      BigDecimal z = x.remainder(y, MathContext.DECIMAL128);
      return new JsonDecimal(z);
    }
    if( value1 instanceof JsonDouble || value2 instanceof JsonDouble )
    {
      double x = value1.doubleValue();
      double y = value2.doubleValue();
      return new JsonDouble(x % y);
    }
    long x = value1.longValueExact();
    long y = value2.longValueExact();
    return new JsonLong(x % y);
  }
  
  @Override
  public Schema getSchema()
  {
    Schema s1 = exprs[0].getSchema();
    Schema s2 = exprs[1].getSchema();
    
    Schema result = MathExpr.promote(s1, s2);
    if (result == null)
    {
      throw new RuntimeException("Operation mod not defined for input types.");
    }
    return result;
  }

}
