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
package com.ibm.jaql.json.meta;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.type.MutableJsonLong;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.util.BaseUtil;

/**
 * 
 */
public class IntMetaArray extends MetaArray
{

  /**
   * 
   */
  public IntMetaArray()
  {
    super(int[].class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaArray#makeItem()
   */
  @Override
  public MutableJsonLong makeValue()
  {
    return new MutableJsonLong();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaArray#count(java.lang.Object)
   */
  @Override
  public long count(Object obj)
  {
    int[] arr = (int[]) obj;
    return arr.length;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaArray#iter(java.lang.Object)
   */
  @Override
  public JsonIterator iter(Object obj) throws Exception
  {
    final int[] arr = (int[]) obj;
    final MutableJsonLong jlong = new MutableJsonLong();
    return new JsonIterator(jlong) {
      int   i     = 0;

      @Override
      public boolean moveNext() throws Exception
      {
        if (i < arr.length)
        {
          jlong.set(arr[i++]);
          return true; // currentValue == jlong
        }
        return false;
      }
    };
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaArray#nth(java.lang.Object, long,
   *      com.ibm.jaql.json.type.Item)
   */
  @Override
  public JsonValue nth(Object obj, long n, JsonValue target) throws Exception
  {
    int[] arr = (int[]) obj;
    if (n >= 0 && n < arr.length)
    {
      ((MutableJsonLong) target).set(arr[(int) n]);
      return target;
    }
    else
    {
      return null;
    }
  }

  public static final int[] emptyArray = new int[0];

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaValue#newInstance()
   */
  @Override
  public Object newInstance()
  {
    return emptyArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaValue#read(java.io.DataInput,
   *      java.lang.Object)
   */
  @Override
  public Object read(DataInput in, Object obj) throws IOException
  {
    int[] arr = (int[]) obj;
    int len = BaseUtil.readVUInt(in);
    if (len != arr.length)
    {
      arr = new int[len];
    }
    for (int i = 0; i < len; i++)
    {
      arr[i] = in.readInt();
    }
    return arr;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaValue#write(java.io.DataOutput,
   *      java.lang.Object)
   */
  @Override
  public void write(DataOutput out, Object obj) throws IOException
  {
    int[] arr = (int[]) obj;
    BaseUtil.writeVUInt(out, arr.length);
    for (int i = 0; i < arr.length; i++)
    {
      out.writeInt(arr[i]);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.json.meta.MetaArray#copy(java.lang.Object,
   *      java.lang.Object)
   */
  @Override
  public Object copy(Object toValue, Object fromValue)
  {
    int[] to = (int[]) toValue;
    int[] from = (int[]) fromValue;
    if (to.length != from.length)
    {
      to = new int[from.length];
    }
    for (int i = 0; i < from.length; i++)
    {
      to[i] = from[i];
    }
    return to;
  }
}
