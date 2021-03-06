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
package com.ibm.jaql.io.hbase;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.mapred.TableOutputFormat;
import org.apache.hadoop.mapred.JobConf;

import com.ibm.jaql.io.AdapterStore;
import com.ibm.jaql.io.hadoop.HadoopSerializationDefault;
import com.ibm.jaql.io.hadoop.InitializableConfSetter;
import com.ibm.jaql.io.hadoop.JsonHolderDefault;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.DefaultJsonComparator;

/**
 * 
 */
public class TableOutputConfigurator implements InitializableConfSetter
{
  protected String location;

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.hadoop.ConfSetter#init(java.lang.Object)
   */
  public void init(JsonValue options) throws Exception
  {
    location = AdapterStore.getStore().getLocation((JsonRecord) options);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.hadoop.ConfSetter#setParallel(org.apache.hadoop.mapred.JobConf)
   */
  public void setParallel(JobConf conf) throws Exception
  {
    conf.set(TableOutputFormat.OUTPUT_TABLE, location);
    conf.setOutputKeyClass(JsonHolderDefault.class);
    conf.setOutputValueClass(JsonHolderDefault.class);
    HadoopSerializationDefault.register(conf);
    conf.setOutputKeyComparatorClass(DefaultJsonComparator.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.lang.ExprConfigurator#setupExpr(org.apache.hadoop.mapred.JobConf,
   *      java.lang.String, com.ibm.jaql.lang.JRecord)
   */
  public void setSequential(JobConf conf) throws Exception
  {
    HBaseConfiguration hbConf = new HBaseConfiguration();
    HBaseAdmin admin = new HBaseAdmin(hbConf);

    // check if table exists, if not, create it
    if (!admin.tableExists(location))
    {
      HTableDescriptor desc = new HTableDescriptor(location);
      desc.addFamily(HBaseStore.Util.DEFAULT_COLUMN_FAMILY);
      admin.createTable(desc);
      admin.enableTable(location);
    }
    // FIXME: this should be redundant
    setParallel(conf);
  }
}
