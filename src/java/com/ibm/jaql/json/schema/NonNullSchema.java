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
package com.ibm.jaql.json.schema;

import com.ibm.jaql.json.type.JsonType;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.util.Bool3;

/** Schema that accepts any value but null */
public final class NonNullSchema extends Schema
{
  NonNullSchema()
  {    
  }

  // -- Schema methods ----------------------------------------------------------------------------
  
  @Override
  public SchemaType getSchemaType()
  {
    return SchemaType.NON_NULL;
  }
  
  @Override
  public Bool3 is(JsonType type, JsonType ... types)
  {
    if (type != JsonType.NULL) return Bool3.UNKNOWN;
    // test whether all are null
    for (int i=0; i<types.length; i++)
    {
      if (types[i] != JsonType.NULL) return Bool3.UNKNOWN;
    }
    return Bool3.FALSE;
  }
  
  @Override
  public boolean isConstant()
  {
    return false;
  }

  @Override
  public JsonValue getConstant()
  {
    return null;
  }
  
  @Override
  public boolean hasModifiers()
  {
    return false;
  }
  
  @Override
  public Bool3 isEmpty(JsonType type, JsonType ... types)
  {
    return is(type, types).and(Bool3.UNKNOWN);
  }

  @SuppressWarnings("unchecked")
  @Override 
  public Class<? extends JsonValue>[] matchedClasses()
  {
    return new Class[] { JsonValue.class }; // means everything 
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.schema.Schema#matches(com.ibm.jaql.json.type.Item)
   */
  @Override
  public boolean matches(JsonValue value)
  {
    return value != null;
  }
  
  // -- merge -------------------------------------------------------------------------------------

  /** 
   * Return <code>any</code> if <code>other</code> does not match null and <code>any?</code>
   * otherwise/
   */
  @Override
  protected Schema merge(Schema other)
  {
    if (other.is(JsonType.NULL).maybe())
    {
      return SchemaFactory.anySchema();
    }
    else
    {
      return this;
    }
  }
  
  // -- introspection -----------------------------------------------------------------------------
  
  @Override
  public Schema elements()
  {
    // if the actual value has elements, they could have any schema
    return SchemaFactory.anySchema(); 
  }

  @Override
  public Bool3 hasElement(JsonValue which)
  {
    return Bool3.UNKNOWN;
  }

  @Override
  public Schema element(JsonValue which)
  {
    // if the actual value has elements, they could have any schema
    return SchemaFactory.anySchema(); 
  }
  
  // -- comparison --------------------------------------------------------------------------------
  
  @Override
  public int compareTo(Schema other)
  {
    int c = this.getSchemaType().compareTo(other.getSchemaType());
    if (c != 0) return c;
    
    assert other instanceof NonNullSchema;
    return 0;
  }  
}
