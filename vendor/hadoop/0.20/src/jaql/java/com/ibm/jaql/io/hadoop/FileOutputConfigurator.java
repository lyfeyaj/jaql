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
package com.ibm.jaql.io.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.mapred.FileOutputCommitter;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCommitter;

import com.ibm.jaql.io.AdapterStore;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonValue;

/**
 * A Configurator that specifically writes the JobConf for OutputFormat
 */
public class FileOutputConfigurator implements InitializableConfSetter
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
   * @see com.ibm.jaql.io.hadoop.ConfSetter#setSequential(org.apache.hadoop.mapred.JobConf)
   */
  public void setSequential(JobConf conf) throws Exception
  {
    registerSerializers(conf);
    
    // For an expression, the location is the final file name
    Path outPath = new Path(location);
    FileSystem fs = outPath.getFileSystem(conf);
    outPath = outPath.makeQualified(fs);
    if( fs.exists(outPath))
    {
      // TODO: Jaql currently has overwrite semantics; add flag to control this
      if( fs.isFile(outPath))
      {
        fs.delete(outPath, false); 
      }
      else
      {
        // Look for a map-reduce output directory
        FileStatus[] nonMR = fs.listStatus(outPath, new PathFilter() {
          boolean onlyOne = true;
          public boolean accept(Path path)
          {
            String name = path.getName();
            if( name.matches("([.][.]?)|([.]part-[0-9]+.crc)|(part-[0-9]+)") )
            {
              return false;
            }
            if( onlyOne )
            {
              onlyOne = false;
              return true;
            }
            return false;
          }
        });
        if( nonMR.length > 0 )
        {
          throw new IOException("directory exists and is not a map-reduce output directory: "+nonMR[0].getPath());
        }
        fs.delete(outPath, true); 
      }
    }

    // In sequential mode, we will write directly to the output file
    // and bypass the _temporary directory and rename of the standard 
    // FileOutputCommitter by using our own DirectFileOutputCommitter.
    FileOutputFormat.setOutputPath(conf, outPath.getParent());
    conf.setClass("mapred.output.committer.class", DirectFileOutputCommiter.class, OutputCommitter.class);
  }

  protected void registerSerializers(JobConf conf)
  {
    conf.setOutputKeyClass(JsonHolderDefault.class);
    conf.setOutputValueClass(JsonHolderDefault.class);
    HadoopSerializationDefault.register(conf);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.hadoop.ConfSetter#setParallel(org.apache.hadoop.mapred.JobConf)
   */
  public void setParallel(JobConf conf) throws Exception
  {
    registerSerializers(conf);

    // For map-reduce, multiple files can be produced, so the location is their
    // parent directory.
    Path outPath = new Path(location);
    FileSystem fs = outPath.getFileSystem(conf);
    fs.delete(outPath, true); // TODO: Jaql currently has overwrite semantics; add flag to control this
    FileOutputFormat.setOutputPath(conf, outPath);
  }
}
