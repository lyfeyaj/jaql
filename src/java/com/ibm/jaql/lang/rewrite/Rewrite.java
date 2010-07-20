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
package com.ibm.jaql.lang.rewrite;

import java.util.ArrayList;

import com.ibm.jaql.json.schema.Schema;
import com.ibm.jaql.json.type.JsonType;
import com.ibm.jaql.lang.core.Var;
import com.ibm.jaql.lang.expr.array.AsArrayFn;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.core.IterExpr;
import com.ibm.jaql.lang.expr.core.VarExpr;
import com.ibm.jaql.lang.expr.function.DefineJaqlFunctionExpr;
import com.ibm.jaql.lang.expr.function.FunctionCallExpr;
import com.ibm.jaql.lang.expr.hadoop.MRAggregate;
import com.ibm.jaql.lang.expr.hadoop.MapReduceBaseExpr;
import com.ibm.jaql.lang.expr.hadoop.MapReduceFn;
import com.ibm.jaql.lang.expr.path.PathExpr;
import com.ibm.jaql.lang.walk.ExprFlow;
import com.ibm.jaql.lang.walk.ExprWalker;
import com.ibm.jaql.util.Bool3;

/**
 * 
 */
public abstract class Rewrite
{
  protected RewriteEngine engine;

  /**
   * @param phase
   * @param fireOn
   */
  public Rewrite(RewritePhase phase, Class<? extends Expr> fireOn)
  {
    this.engine = phase.engine;
    phase.fireOn(this, fireOn);
  }

  /**
   * @param phase
   * @param fireOn
   */
  public Rewrite(RewritePhase phase, Class<? extends Expr>[] fireOn)
  {
    this.engine = phase.engine;
    phase.fireOn(this, fireOn);
  }

  /**
   * @param expr
   * @return
   * @throws Exception
   */
  public abstract boolean rewrite(Expr expr) throws Exception;

  // Rewrite utilities.
  // Is there a better place for these?

  /**
   * @param expr
   * @return
   */
  protected Expr cloneExpr(Expr expr)
  {
    engine.varMap.clear();
    return expr.clone(engine.varMap);
  }


  // FIXME: There is a potential bug here when exprTree == VarExpr that gets replaced!
  /**
   * @param var
   * @param exprTree
   * @param replaceBy
   * @return
   */
  protected int replaceVarUses(Var var, Expr exprTree, Expr replaceBy)
  {
    ExprWalker walker = engine.walker;
    walker.reset(exprTree);
    Expr expr = walker.next();
    int n = 0;
    while (expr != null)
    {
      if (expr instanceof VarExpr)
      {
        VarExpr ve = (VarExpr) expr;
        expr = walker.next();
        if (ve.var() == var)
        {
          ve.replaceInParent(cloneExpr(replaceBy));
          n++;
        }
      }
      else
      {
        expr = walker.next();
      }
    }
    return n;
  }

  // TODO: this needs to exclude the case where one map/reduce is input to another. 
  /**
   * Determine if this Expr could be part of a map, combine, or reduce function
   * to a map/reduce job. Right now it is extremely conservative: it looks to
   * see if this expr is in a function that could be input to one of the
   * map/reduce functions.
   * 
   * @param expr
   * @return
   */
  public boolean maybeInMapReduce(Expr expr)
  {
    ExprFlow flow = engine.flow;
    flow.reset(expr);

    while ((expr = flow.next()) != null)
    {
      if (expr instanceof DefineJaqlFunctionExpr)
      {
        break;
      }
    }

    while ((expr = flow.next()) != null)
    {
      if (expr instanceof MapReduceFn || expr instanceof MRAggregate)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * @param expr
   * @return
   */
  public boolean mightContainMapReduce(Expr expr) // FIXME: replace with an ExprProperty
  {
    ExprWalker walker = engine.walker;
    walker.reset(expr);

    while ((expr = walker.next()) != null)
    {
      if (expr instanceof MapReduceBaseExpr )
      {
        return true;
      }
      if (expr instanceof FunctionCallExpr)
      {
        // FIXME: look deeper into this case.
        // Expr fnExpr = ((FunctionCallExpr)expr).fnExpr();
        // if( fnExpr instanceof ConstExpr ) ... for JaqlFunction look at its body and params, for Expr or Udf ask them
        // if( fnExpr instanceof DefineJaqlFunctionExpr ) ... look at body and params
        return true;
      }
    }
    return false;
  }

  /**
   * @param expr
   * @param var
   * @return
   */
  int countVarUse(Expr expr, Var var)
  {
    int n = 0;
    ExprWalker walker = engine.walker;
    walker.reset(expr);
    while ((expr = walker.next()) != null)
    {
      if (expr instanceof VarExpr)
      {
        VarExpr ve = (VarExpr) expr;
        if (ve.var() == var)
        {
          n++;
        }
      }
    }
    return n;
  }

  /**
   * @param expr
   * @param var
   * @return
   */
  VarExpr findFirstVarUse(Expr expr, Var var)
  {
    ExprWalker walker = engine.walker;
    walker.reset(expr);

    while ((expr = walker.next()) != null)
    {
      if (expr instanceof VarExpr)
      {
        VarExpr ve = (VarExpr) expr;
        if (ve.var() == var)
        {
          return (VarExpr) expr;
        }
      }
    }
    return null;
  }
  
  /**
   * Find PathExprs and VarExprs(that are not children of PathExpr) that contain the given var
   */
  ArrayList<Expr> findMaximalVarOrPathExpr(Expr expr, Var var)
  {
    ExprWalker walker = engine.walker;
    walker.reset(expr);
    ArrayList<Expr> list = new ArrayList<Expr>();
    
    while ((expr = walker.next()) != null)
    {
    	if (expr instanceof VarExpr) 
    	{
    		VarExpr ve = (VarExpr) expr;
    		if (ve.var() == var)
    		{
    			if ((expr.parent() instanceof PathExpr) && (expr.getChildSlot() == 0))
    				list.add((PathExpr) expr.parent());
    			else
    				list.add((VarExpr) expr);
    		}
    	}
    }
    return list;
  }

  /**
   * @return true if expr and all its ancestors are evaluated at most once all the
   * way up to ancestor.
   * @throws NullPointerException if expr is not a decendant of ancestor.
   */
  protected boolean evaluatedOnceTo(Expr expr, Expr ancestor)
  {
    while( expr != ancestor )
    {
      int i = expr.getChildSlot();
      if( expr.parent().evaluatesChildOnce(i).maybeNot() )
      {
        return false;
      }
      expr = expr.parent();
    }
    return true;
  }
  
  /** 
   * Ensure that expr returns an array.
   * If it never returns an array or null, raise a type exception.
   * If it always returns an array, simply return the expr.
   * If it unknown, return asArray(expr).
   */
  public static Expr asArray(Expr expr)
  {
    if( !(expr instanceof IterExpr) )
    {
      Schema s = expr.getSchema();
      Bool3 isArray = s.is(JsonType.ARRAY);
      if( isArray.never() && s.is(JsonType.NULL).never() )
      {
        throw new ClassCastException("expected an array or null. got:"+s+"\nIn expr:"+expr);
      }
      if( isArray.maybeNot() )
      {
        expr = new AsArrayFn(expr);
      }
    }
    return expr;
  }
}
