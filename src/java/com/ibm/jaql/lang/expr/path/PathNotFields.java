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
package com.ibm.jaql.lang.expr.path;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.ibm.jaql.json.schema.RecordSchema;
import com.ibm.jaql.json.schema.Schema;
import com.ibm.jaql.json.schema.SchemaFactory;
import com.ibm.jaql.json.type.JsonString;
import com.ibm.jaql.lang.core.Context;
import com.ibm.jaql.lang.core.Var;
import com.ibm.jaql.lang.expr.core.ConstExpr;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.core.VarExpr;
import com.ibm.jaql.lang.expr.metadata.MappingTable;
import com.ibm.jaql.util.Bool3;
import com.ibm.jaql.util.FastPrinter;


/**
 * 
 */
public class PathNotFields extends PathFields
{
  private static Expr[] makeArgs(ArrayList<PathStep> fields)
  {
    Expr[] es = fields.toArray(new Expr[fields.size()+1]);
    es[fields.size()] = new PathReturn();
    return es;
  }

  /**
   * @param exprs
   */
  public PathNotFields(Expr[] exprs)
  {
    super(exprs);
  }

  /**
   */
  public PathNotFields(ArrayList<PathStep> fields)
  {
    super(makeArgs(fields));
  }

  /**
   * 
   */
  public void decompile(FastPrinter exprText, HashSet<Var> capturedVars)
  throws Exception
  {
    exprText.print("* -");
    String sep = " ";
    int n = exprs.length - 1;
    for(int i = 0 ; i < n ; i++)
    {
      exprText.print(sep);
      exprs[i].decompile(exprText, capturedVars);
      sep = ", ";
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.jaql.lang.expr.core.PathFields#matches(com.ibm.jaql.lang.core.Context, com.ibm.jaql.json.type.JString)
   */
  @Override
  public boolean matches(Context context, JsonString name) throws Exception
  {
    
    int n = exprs.length - 1;
    for(int i = 0 ; i < n ; i++)
    {
      PathOneField f = (PathOneField)exprs[i];
      if( f.matches(context, name) )
      {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Return the mapping table.
   * Since a subset of the fields are excluded, we do the mapping as following:
   * 	1) add a generic mapping record that matches any field, i.e., ($ --mapsTo--> $). This record has SafeMapping flag set to True.
   *    2) add one mapping record for each of the excluded fields that overrides the generic mapping record. These records have SafeMapping flag set to False.
   *  Since excluded fields have mapping entries with SafeMapping flag set to false, then any conditions on these fields will not be pushed down.
   *  Conditions on the included fields will match the generic mapping record and will be pushed down. 
   */
  @Override
  public MappingTable getMappingTable()
  {
	  MappingTable mt = new MappingTable();
	    
	  //Find the iteration variable used outside the PathALLFields
	  Var bindVar = null;
	  Expr parent = this.parent(); 
	  if ((!(parent instanceof PathRecord)) || (!(parent.parent() instanceof PathExpr)))
		  return mt;
	  PathExpr peParent = (PathExpr)parent.parent();
	  VarExpr veParent = MappingTable.findVarInExpr(peParent.input());
	  if (veParent == null)
		  return mt;
	  else
		  bindVar = veParent.var();
		  
	  //Construct the L.H.S of the mapping	
	  VarExpr veL = new VarExpr(new Var(MappingTable.DEFAULT_PIPE_VAR));
	  PathExpr peL = new PathExpr(veL, new PathReturn());

	  //Construct the R.H.S of the mapping (pathExpr)
	  VarExpr veR = new VarExpr(bindVar);
	  PathExpr peR = new PathExpr(veR, new PathReturn());
	  
	  //Add the generic mapping record
	  mt.add(peL, peR, true);
	  
	  //Loop over the excluded fields and add one record with SafeMapping flag set to False.
	  ConstExpr fName = null;
	  for(int i = 0 ; i < exprs.length - 1 ; i++)
	  {
		  PathOneField f = (PathOneField)exprs[i];
		  if (f.nameExpr() instanceof ConstExpr)
		  {
			  fName = (ConstExpr)f.nameExpr();
		  }
		  else
		  {
			  //the column name to be excluded is not static. In this case, we should not do any mapping
			  mt.clear();
			  return mt;
		  }
		  
		  //Construct the L.H.S of the mapping	
		  veL = new VarExpr(new Var(MappingTable.DEFAULT_PIPE_VAR));
		  peL = new PathExpr(veL, new PathFieldValue(fName.clone(null)));

		  //Construct the R.H.S of the mapping (pathExpr)
		  veR = new VarExpr(bindVar);
		  peR = new PathExpr(veR, new PathFieldValue(fName.clone(null)));
		  
		  //Add unSafe mapping record
		  mt.add(peL, peR, false);
	  }
	    
	  return mt;
  }
  
  
  // -- schema ------------------------------------------------------------------------------------
  
  // special case: resulting fields nested in PathStepSchema.schema
  @Override
  public PathStepSchema getSchema(Schema inputSchema)
  {
    if (inputSchema instanceof RecordSchema)
    {
      RecordSchema inRec = (RecordSchema)inputSchema;
      
      HashSet<JsonString> oldFields = new HashSet<JsonString>();
      
      for (RecordSchema.Field field : ((RecordSchema)inputSchema).getFieldsByPosition())
      {
        oldFields.add(field.getName());
      }
      
      // Remove known names from the list of fields.
      // If any removed field name is not statically known, then all fields
      // become optional (we don't know if they will be present or not).
      boolean allOptional = false;
      for(int i = 0 ; i < exprs.length-1; i++)
      {
        PathOneField f = (PathOneField)exprs[i];
        Expr ne = f.nameExpr();
        if( ne instanceof ConstExpr )
        {
          JsonString fname = (JsonString)((ConstExpr)ne).value;
          oldFields.remove(fname);
        }
        else
        {
          allOptional = true;
        }
      }
      
      // and copy the input schema w/o those names
      List<RecordSchema.Field> newFields = new LinkedList<RecordSchema.Field>();
      for( RecordSchema.Field field : inRec.getFieldsByPosition() )
      {
        if( oldFields.contains(field.getName()) )
        {
          newFields.add(new RecordSchema.Field(field.getName(), field.getSchema(),
              allOptional || field.isOptional())); // unresolved fields might match this field
        }
      }
      return new PathStepSchema(new RecordSchema(newFields, inRec.getAdditionalSchema()) , Bool3.TRUE);
    }
    return new PathStepSchema(SchemaFactory.recordSchema(), Bool3.TRUE);
  }
}
