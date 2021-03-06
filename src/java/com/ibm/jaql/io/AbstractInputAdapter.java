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
package com.ibm.jaql.io;

import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonValue;

/** Superclass for input adapters that take a {@link JsonRecord} for initialization. */
public abstract class AbstractInputAdapter implements InputAdapter
{
  /**
   * 
   */
  protected JsonRecord args;

  /**
   * 
   */
  protected String  location;

  /**
   * 
   */
  protected JsonRecord options;

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.lang.DataStoreAdapter#initializeFrom(com.ibm.jaql.lang.JRecord)
   */
  @Override
  public void init(JsonValue args) throws Exception
  {
    this.args = (JsonRecord)args;
    // set the location
    this.location = AdapterStore.getStore().getLocation((JsonRecord)args);

    // set the options
    this.options = AdapterStore.getStore().input.getOption((JsonRecord)args);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.lang.DataStoreAdapter#open()
   */
  public void open() throws Exception
  {
    // nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.lang.DataStoreAdapter#close()
   */
  public void close() throws Exception
  {
    // nothing
  }
}
