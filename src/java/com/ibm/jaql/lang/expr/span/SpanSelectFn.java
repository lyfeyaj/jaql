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
package com.ibm.jaql.lang.expr.span;

import com.ibm.jaql.json.schema.Schema;
import com.ibm.jaql.json.schema.SchemaFactory;
import com.ibm.jaql.json.type.JsonLong;
import com.ibm.jaql.json.type.JsonSpan;
import com.ibm.jaql.json.type.JsonString;
import com.ibm.jaql.lang.core.Context;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.function.DefaultBuiltInFunctionDescriptor;
import com.ibm.jaql.util.Bool3;

public class SpanSelectFn extends Expr
{
	public static final JsonString BEGIN = new JsonString("begin");
	public static final JsonString END = new JsonString("end");
	
	public static class Descriptor extends DefaultBuiltInFunctionDescriptor.Par22
	{
		public Descriptor()
		{
			super("span_select", SpanSelectFn.class);
		}
	}
	
	/**
	 * @param exprs
	 */
	public SpanSelectFn(Expr[] exprs)
	{
		super(exprs);
	}

	
	/* (non-Javadoc)
	 * @see com.ibm.jaql.lang.expr.core.Expr#getSchema()
	 */
	@Override
	public Schema getSchema() {
		return SchemaFactory.longOrNullSchema();
	}

	/* (non-Javadoc)
	 * @see com.ibm.jaql.lang.expr.core.Expr#evaluatesChildOnce(int)
	 */
	@Override
	public Bool3 evaluatesChildOnce(int i) {
		
		return Bool3.TRUE;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.jaql.lang.expr.core.Expr#eval(com.ibm.jaql.lang.core.Context)
	 */
	public JsonLong eval(final Context context) throws Exception
	{
		// get the span to select from
		JsonSpan spn = (JsonSpan) exprs[0].eval(context);
		if (spn == null)
		{
			return null;
		}
		
		// get the selector key
		JsonString key = (JsonString) exprs[1].eval(context);
		long val;
		if(key == null) 
		{
			return null;
		}
		if(key.equals(BEGIN)) {
			val = spn.begin;
		} else if(key.equals(END)) {
			val = spn.end;
		} else {
			return null;
		}
		
		return new JsonLong(val);
	}
}