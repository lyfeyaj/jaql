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
package com.ibm.jaql.lang.expr.array;

import com.ibm.jaql.json.schema.ArraySchema;
import com.ibm.jaql.json.schema.Schema;
import com.ibm.jaql.json.schema.SchemaFactory;
import com.ibm.jaql.json.schema.SchemaTransformation;
import com.ibm.jaql.json.type.BufferedJsonArray;
import com.ibm.jaql.json.type.JsonNumber;
import com.ibm.jaql.json.type.MutableJsonLong;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.lang.core.Context;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.core.IterExpr;
import com.ibm.jaql.lang.expr.function.DefaultBuiltInFunctionDescriptor;

/**
 * @jaqlDescription Take an input an array of any type and returns an array of pairs, one pair per input value. 
 * Each pair will list the ordinal value of the array value (e.g., its index in the array), along with the value 
 * of the array.
 * 
 * Usage:
 * [ [long, T],* ] enumerate( [ T, * ] )
 * 
 * @jaqlExample enumerate( ["a", "b", "c"]);
 * [ [ 0, "a"] , [1, "b"], [2, "c"] ]
 * 
 */
public final class EnumerateExpr extends IterExpr
{
  public static class Descriptor extends DefaultBuiltInFunctionDescriptor.Par12
  {
    public Descriptor()
    {
      super("enumerate", EnumerateExpr.class);
    }
  }
  
  /**
   * Expr array
   * 
   * @param exprs
   */
  public EnumerateExpr(Expr[] exprs)
  {
    super(exprs);
  }

  /**
   * @param arrayExpr
   */
  public EnumerateExpr(Expr arrayExpr)
  {
    this(new Expr[]{arrayExpr});
  }

  @Override
  public Schema getSchema()
  {
    Schema s = exprs[0].getSchema();
    s = SchemaTransformation.restrictToArrayOrNull(s);
    if( s == null )
    {
      throw new IllegalArgumentException("array expected"); 
    }
    s = s.elements();
    if( s == null )   
    {
      s = SchemaFactory.emptyArraySchema();
    }
    else
    {
      s = new ArraySchema(new Schema[] { SchemaFactory.longSchema(), s });
      s = new ArraySchema(null, s);
    }
    return s;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.lang.expr.core.IterExpr#iter(com.ibm.jaql.lang.core.Context)
   */
  public JsonIterator iter(final Context context) throws Exception
  {
    JsonNumber jstart = exprs.length <= 1 ? null : (JsonNumber)exprs[1].eval(context);
    long start = jstart == null ? 0 : jstart.longValueExact();
    final JsonIterator iter = exprs[0].iter(context);
    final BufferedJsonArray pair = new BufferedJsonArray(2);
    final MutableJsonLong counter = new MutableJsonLong(start - 1);
    pair.set(0, counter);

    return new JsonIterator(pair) {
      public boolean moveNext() throws Exception
      {
        if (!iter.moveNext()) {
          return false;
        }
        counter.set(counter.get()+1);
        pair.set(1, iter.current());
        return true;
      }
    };
  }

}
