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
package com.ibm.jaql.io.serialization.text.def;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.jaql.io.serialization.text.TextBasicSerializer;
import com.ibm.jaql.io.serialization.text.TextFullSerializer;
import com.ibm.jaql.json.schema.SchemaFactory;
import com.ibm.jaql.json.type.JsonUtil;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.lang.core.SystemNamespace;
import com.ibm.jaql.lang.core.Var;
import com.ibm.jaql.lang.expr.function.BuiltInFunction;
import com.ibm.jaql.lang.expr.function.BuiltInFunctionDescriptor;
import com.ibm.jaql.lang.expr.function.Function;
import com.ibm.jaql.lang.expr.function.JaqlFunction;
import com.ibm.jaql.lang.expr.function.JavaUdfFunction;
import com.ibm.jaql.lang.expr.function.VarParameter;
import com.ibm.jaql.lang.expr.function.VarParameters;
import com.ibm.jaql.lang.expr.system.ExternalFnFunction;
import com.ibm.jaql.util.FastPrinter;

public class FunctionSerializer extends TextBasicSerializer<Function>
{

  @Override
  public void write(FastPrinter out, Function value, int indent)
      throws IOException
  {
    if (value instanceof BuiltInFunction)
    {
      BuiltInFunction f = (BuiltInFunction)value;
      BuiltInFunctionDescriptor d = f.getDescriptor();
      if (SystemNamespace.getInstance().isSystemExpr(d.getImplementingClass()))
      {
        out.print(SystemNamespace.NAME + "::" + d.getName());
      }
      else
      {
        out.print("#builtin('" + d.getClass().getName() + "')");
      }
    }
    else if (value instanceof JaqlFunction)
    {
      try
      {
        JaqlFunction f = (JaqlFunction)value;
        Map<Var, JsonValue> localBindings = f.getLocalBindings();
        String sep="";
        
        // write captures
        if (!localBindings.isEmpty())
        {
          out.print("system::const((");
          for (Entry<Var, JsonValue> e : localBindings.entrySet())
          {
            out.print(sep);
            out.print(e.getKey().taggedName());
            out.print("=");
            JsonUtil.print(out, e.getValue());
            sep = ", ";
          }
        }
        
        // write parameters
        VarParameters pars = f.getParameters();
        HashSet<Var> capturedVars = new HashSet<Var>();
        out.print(sep);
        out.print("fn(");
        String del = "";
        for (int i=0; i<pars.numParameters(); i++)
        {
          VarParameter par = pars.get(i);
          out.print(del);
          if (par.getSchema() != SchemaFactory.anySchema()) // identity check should be ok, if not, doesnt matter
          {
            out.print(par.getSchema().toString());
            out.print(" ");
          }
          out.print(par.getVar().taggedName());
          if (par.isOptional())
          {
            out.print("=");
            par.getDefaultValue().decompile(out, capturedVars);
          }
          del = ", ";
        }
        out.print(") (");
        
        // write body
        f.body().decompile(out, capturedVars);
        out.print(")");
        
        // endig parens for captures
        if (!localBindings.isEmpty())
        {
          out.print("))");
        }
        return;
      } catch (Exception e)
      {
        throw new IOException(e);
      }
    }
    else if (value instanceof JavaUdfFunction) {
      JavaUdfFunction f = (JavaUdfFunction)value;
      out.print("system::javaudf('" + f.getImplementingClass().getName() + "')");
    }
    else if (value instanceof ExternalFnFunction) {
        ExternalFnFunction f = (ExternalFnFunction)value;
        out.print("system::externalfn(");
        TextFullSerializer.getDefault().write(out, f.getExternalOpts());
        out.print(")");
      }
    else
    {
      throw new IllegalStateException("Unknown function class: " + value.getClass());
    }
  }
}
