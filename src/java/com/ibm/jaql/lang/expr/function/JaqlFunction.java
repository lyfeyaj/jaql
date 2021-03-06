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
package com.ibm.jaql.lang.expr.function;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import com.ibm.jaql.json.type.JsonUtil;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.lang.core.Var;
import com.ibm.jaql.lang.core.VarMap;
import com.ibm.jaql.lang.expr.core.BindingExpr;
import com.ibm.jaql.lang.expr.core.ConstExpr;
import com.ibm.jaql.lang.expr.core.DoExpr;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.core.VarExpr;
import com.ibm.jaql.lang.rewrite.VarTagger;
import com.ibm.jaql.lang.rewrite.VarTagger.TagGenerator;
import com.ibm.jaql.lang.util.JaqlUtil;
import com.ibm.jaql.lang.walk.PostOrderExprWalker;
import com.ibm.jaql.util.FastPrintBuffer;

/** 
 * Value that stores a function implemented in Jaql.
 */
public class JaqlFunction extends Function
{
  /** Number of intialized arguments. */
  private Integer numArgs = -1;
  
  /** Parameters of the function. */
  private VarParameters parameters;
  
  /** Body of the function. */
  private Expr body;
  
  /** Function-local bindings. Contains values for free variables in the default values and 
   * body. */
  private Map<Var, JsonValue> localBindings;

  
  // -- construction ------------------------------------------------------------------------------
  
  /** Constructs from the specified parameters and body. The body refers to a certain parameter
   * by using the variable corresponding to that parameter. */
  public JaqlFunction(VarParameters parameters, Expr body)
  {
    this.localBindings = new HashMap<Var, JsonValue>();
    this.parameters = parameters;
    this.body = body;
  }
  
  /** Constructs from the specified parameters, variable assignment, and body. The body refers to 
   * a certain parameter by using the variable corresponding to that parameter. */
  public JaqlFunction(Map<Var, JsonValue> localBindings, VarParameters parameters, Expr body)
  {
    this.localBindings = Collections.unmodifiableMap(localBindings);
    this.parameters = parameters;
    this.body = body;
  }
  
  // -- self-description --------------------------------------------------------------------------

  @Override
  public VarParameters getParameters()
  {
    return parameters;
  }

  @Override
  public String formatError(String msg)
  {
    String result = "In call of Jaql function with signature fn(";  
    String sep="";
    for(int i=0; i<parameters.numParameters(); i++)
    {
      VarParameter p = parameters.get(i);
      result += sep + p.getName();
      if (p.isOptional())
      {
        result += "=??";
      }
      sep = ", ";
    }
    result += "): " + msg;
    return result;
  }
  
  /** Returns the body of this function. */
  public Expr body()
  {
    return body;
  }
  
  // -- evaluation / inlining ---------------------------------------------------------------------

  /** Returns the function-local bindings. Contains values for free variables in the default 
   * values and body. */
  public Map<Var, JsonValue> getLocalBindings()
  {
    return localBindings;
  }
  
  /** Returns a copy of this function obtained by replacing all references to function-local
   * variables by their values. */
  public JaqlFunction inlineLocalBindings()
  {
    VarMap varMap = new VarMap();
    
    // copy parameters
    int n = parameters.numParameters();
    VarParameter[] newParams = new VarParameter[n]; 
    for (int i=0; i<n; i++)
    {
      VarParameter p = parameters.get(i);
      Var newVar = new Var(p.var.taggedName(), p.var.getSchema());
      
      VarParameter newP;
      if (p.isRequired())
      {
        newP = new VarParameter(newVar); 
      }
      else if (p.isOptional())
      {
        Expr e = inlineLocalBindings(p.defaultValue, varMap);
        newP = new VarParameter(newVar, e);
      }
      else
      {
        throw new IllegalStateException("Jaql functions can only have required or optional parameters");
      }

      varMap.put(p.var, newVar);
      newParams[i] = newP;
    }
       
    // copy body
    Expr newBody = inlineLocalBindings(body, varMap);
    
    // return the function
    return new JaqlFunction(new VarParameters(newParams), newBody);
  }
  
  /** Clones <code>expr</code> and replaces all references to function-local variables 
   * by their values */ 
  private Expr inlineLocalBindings(Expr expr, VarMap varMap)
  {
    // check root
    if (expr instanceof VarExpr)
    {
      VarExpr ve = (VarExpr)expr;
      if (localBindings.containsKey(ve.var()))
      {
        return new ConstExpr(localBindings.get(ve.var()));
      }
    }
    
    // traverse children
    Expr clonedExpr = expr.clone(varMap);
    PostOrderExprWalker walker = new PostOrderExprWalker(clonedExpr);    
    Expr e;
    while ((e = walker.next()) != null)
    {
      if (e instanceof VarExpr)
      {
        VarExpr ve = (VarExpr)e;
        if (localBindings.containsKey(ve.var()))
        {
          ve.replaceInParent(new ConstExpr(localBindings.get(ve.var())));
        }
      }
    }
    return clonedExpr;
  }
  
  /** Tags variables used in the default value expressions and body so that decompilation
   * or serialization of this function is safe. Has to be called before decompilation.
   * 
   * @throws IllegalStateException if the function has captures that have to be tagged
   */
  public void tagVars()
  {
    TagGenerator tagGenerator = new VarTagger.NumberTagGenerator();
    
    // scope local bindings
    Map<String, Stack<Var>> scope = new HashMap<String, Stack<Var>>();
    for (Var var : localBindings.keySet())
    {
      var.setTag(null);
      VarTagger.scope(var, scope);
    }
    
    // tag all default value expressions and the function body; also determine the set of
    // environment variables that has to be tagged
    Set<Var> varsToTag = new HashSet<Var>();
    for (int i=0; i<parameters.numParameters(); i++)
    {
      VarParameter par = parameters.get(i);
      if (par.isOptional())
      {
        par.var.setTag(null);
        varsToTag.addAll(VarTagger.tag(par.defaultValue, scope, tagGenerator));
      }
    }
    
    // scope args
    for (int i=0; i<parameters.numParameters(); i++)
    {
      VarTagger.scope(parameters.get(i).getVar(), scope);
    }
    varsToTag.addAll(VarTagger.tag(body, scope, tagGenerator));
    
    // mark all local variables that have a name that occurs more than once to be tagged
    // (this is necessary because the local bindings do not have order, so it is undefined
    //  which one is deserialized first)
    Map<String, Var> namesSeen = new HashMap<String, Var>();
    for (Var var : localBindings.keySet())
    {
      if (namesSeen.containsKey(var.name()))
      {
        // 2nd or more occurrence of the name
        varsToTag.add(namesSeen.get(var.name())); 
        varsToTag.add(var);
      }
      namesSeen.put(var.name(), var);
    }
    
    // tag local bindings
    for (Var var : varsToTag)
    {
      Stack<Var> scoped = scope.get(var.name()); 
      if (scoped == null || !scoped.contains(var))
      {
        throw new IllegalStateException("Function has captures that have to be tagged");
      }
      var.setTag(tagGenerator.nextTag());
    }
  }
  
  /** Determines whether this function captures variables. Function arguments have to be 
   * set using one of the <code>setArguments</code> functions before this method is called. */ 
  public boolean hasCaptures()
  {
    return !getCaptures().isEmpty();
  }
  
  /** Returns the set of variables captured in the body of this function. Function arguments have to be 
   * set using one of the <code>setArguments</code> functions before this method is called. 
   * @return
   */
  public HashSet<Var> getCaptures()
  {
    // TODO: make more efficient
    HashSet<Var> capturedVars = new HashSet<Var>();
    FastPrintBuffer exprText = new FastPrintBuffer();
    try
    {
      body.decompile(exprText, capturedVars);
      for (int i=0; i<parameters.numParameters(); i++)
      {
        VarParameter par = parameters.get(i);
        capturedVars.remove(par.getVar());
        if (par.isOptional())
        {
          par.getDefaultValue().decompile(exprText, capturedVars);
        }
      }
      capturedVars.removeAll(localBindings.keySet());
      return capturedVars;
    }
    catch (Exception e)
    {
      throw JaqlUtil.rethrow(e);
    }
  }

  @Override
  public void prepare(int numArgs)
  {
    this.numArgs = numArgs;
  }

  @Override
  protected void setArgument(int pos, JsonValue value)
  {
    parameters.get(pos).getVar().setValue(value);
  }

  @Override
  protected void setArgument(int pos, JsonIterator it)
  {
    parameters.get(pos).getVar().setIter(it);
  }

  @Override
  protected void setArgument(int pos, Expr expr)
  {
    parameters.get(pos).getVar().setExpr(expr);
  }

  @Override
  protected void setDefault(int pos)
  {
    setArgument(pos, parameters.defaultOf(pos));
  }
  
  @Override
  public Expr inline(boolean forEval)
  {
    if (forEval)
    {
      // initialize function-local environment
      for (Entry<Var, JsonValue> e : localBindings.entrySet())
      {
        Var var = e.getKey();
        var.setValue(e.getValue());
      }
      return body;
    }
    else
    {
      // TODO: Don't inline (potentially) recursive functions
      
      // initialize function-local environment
      int numEnv = localBindings.size();
      Expr[] doExprs = new Expr[numEnv+numArgs+1];
      int i = 0;
      for (Entry<Var, JsonValue> e : localBindings.entrySet())
      {
        doExprs[i] = new BindingExpr(BindingExpr.Type.EQ, e.getKey(), null, new ConstExpr(e.getValue()));
        i++;
      }
      
      // evaluate the args
      for (i=0; i<numArgs; i++)
      {
        Var var = parameters.get(i).getVar();
        assert var.type() == Var.Type.EXPR; // all arguments are expressions at compile time
        doExprs[numEnv+i] = new BindingExpr(BindingExpr.Type.EQ, var, null, var.expr());
      }
      
      // return the inlined functions
      doExprs[numEnv+numArgs] = body;
      return new DoExpr(doExprs);
    }
  }

  // -- copying -----------------------------------------------------------------------------------
  
  @Override
  public JaqlFunction getCopy(JsonValue target)
  {
    VarMap varMap = new VarMap();
    
    // copy environment
    Map<Var, JsonValue> newEnv = new HashMap<Var, JsonValue>();
    for (Entry<Var, JsonValue> e : localBindings.entrySet())
    {
      Var oldVar = e.getKey();
      Var newVar = new Var(oldVar.taggedName(), oldVar.getSchema());
      JsonValue newValue = JsonUtil.getImmutableCopyUnchecked(e.getValue());
      newEnv.put(newVar, newValue);
      varMap.put(oldVar, newVar);
    }
    
    // copy parameters and default values
    int n = parameters.numParameters();
    VarParameter[] newPars = new VarParameter[n];
    for (int i=0; i<n; i++)
    {
      VarParameter par = parameters.get(i); 
      Var oldVar = par.getVar();
      Var newVar = new Var(oldVar.taggedName(), oldVar.getSchema());
      varMap.put(oldVar, newVar);
      if (par.isRequired())
      {
        newPars[i] = new VarParameter(newVar);
      }
      else
      {
        newPars[i] = new VarParameter(newVar, par.getDefaultValue().clone(varMap));
      }
    }
    
    // copy body
    Expr newBody = body.clone(varMap);
    return new JaqlFunction(newEnv, new VarParameters(newPars), newBody);
  }

  @Override
  public Function getImmutableCopy()
  {
    return getCopy(null);
  }
}
