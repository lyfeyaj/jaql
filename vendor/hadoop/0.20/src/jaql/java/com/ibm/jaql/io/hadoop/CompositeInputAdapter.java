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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.ReflectionUtils;

import com.ibm.jaql.io.Adapter;
import com.ibm.jaql.io.AdapterStore;
import com.ibm.jaql.io.ClosableJsonIterator;
import com.ibm.jaql.io.registry.RegistryUtil;
import com.ibm.jaql.json.schema.ArraySchema;
import com.ibm.jaql.json.schema.OrSchema;
import com.ibm.jaql.json.schema.Schema;
import com.ibm.jaql.json.schema.SchemaFactory;
import com.ibm.jaql.json.type.BufferedJsonArray;
import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonLong;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.type.MutableJsonLong;

// TODO: look into factoring some of this code with DefaultHadoopInputAdapter
/** Takes an array of HadoopInputAdapters and operates on the union of their inputs. */
public class CompositeInputAdapter implements HadoopInputAdapter
{
  public static String         CURRENT_IDX_NAME = "com.ibm.jaql.lang.CompositeinputAdapter.currentIdx";
  public static String         ADD_INDEX_NAME = "com.ibm.jaql.lang.CompositeinputAdapter.addIndex";


  private JsonArray               args;
  
  // true => return [inputIndex,value] pairs, else just value.
  // This is triggered by using a record for a descriptor; an array will not add the index.
  // TODO: This is a bit of an ugly way to do this, but it keeps mapReduce working until
  // we decide if we should eliminate direct co-group support from mapReduce()
  private boolean                 addIndex;

  private HadoopInputAdapter[] adapters;
  
  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.Adapter#initializeFrom(com.ibm.jaql.json.type.Item)
   */
  @Override
  public void init(JsonValue val) throws Exception
  {
    // TODO: eliminate
    if(val instanceof JsonArray)
    {
      addIndex = false;
      initializeFrom((JsonArray) val); 
    }
    else if(val instanceof JsonRecord)
    {
      addIndex = true;
      // dig the location out
      JsonRecord rval = (JsonRecord)val;
      JsonArray loc = (JsonArray)rval.get(CompositeOutputAdapter.DESCRIPTORS);
      if( loc == null )
      {
        loc = (JsonArray)rval.get(Adapter.LOCATION_NAME); // TODO: eliminate
      }
      initializeFrom(loc);
    }
    else
    {
      throw new IllegalArgumentException("invalid composite descriptor");
    }
  }

  /**
   * @param args
   * @throws Exception
   */
  private void initializeFrom(JsonArray args) throws Exception
  {
    this.args = (JsonArray)args;

    // 1. make a InputAdapter array of the same size as args
    int numAdapters = (int) this.args.count();
    adapters = new HadoopInputAdapter[numAdapters];

    // 2. instantiate and initialize all StorableInputAdapters
    for (int i = 0; i < numAdapters; i++)
    {
      JsonValue value = this.args.get(i);
      // adapters[i] = AdapterStore.getInputAdapter((JRecord) item.getNonNull(),
      // item);
      adapters[i] = (HadoopInputAdapter) AdapterStore.getStore().input
          .getAdapter(value);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.Adapter#open()
   */
  public void open() throws Exception
  {
    // for each adapter, call its open
    int numAdapters = adapters.length;
    for (int i = 0; i < numAdapters; i++)
    {
      adapters[i].open();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.InputAdapter#getItemReader()
   */
  public ClosableJsonIterator iter() throws Exception
  {
    if( addIndex )
    {
      final MutableJsonLong index = new MutableJsonLong(0);
      final BufferedJsonArray pair = new BufferedJsonArray(2);
      pair.set(0, index);

      return new ClosableJsonIterator(pair) { // TODO: temprory hack until input get updated
        ClosableJsonIterator baseReader = null;

        int        idx        = 0;

        @Override
        public boolean moveNext() throws Exception
        {
          while (true)
          {
            if (baseReader == null)
            {
              if (idx >= adapters.length)
              {
                return false;
              }
              index.set(idx);
              baseReader = adapters[idx++].iter();
            }
            if (baseReader.moveNext())
            {
              pair.set(1, baseReader.current());
              return true;
            }
            else
            {
              baseReader.close();
              baseReader = null;
            }
          }
        }
      };
    }
    else
    {
      return new ClosableJsonIterator() { // TODO: temprory hack until input get updated
        ClosableJsonIterator baseReader = null;

        int        idx        = 0;

        @Override
        public boolean moveNext() throws Exception
        {
          while (true)
          {
            if (baseReader == null)
            {
              if (idx >= adapters.length)
              {
                return false;
              }
              baseReader = adapters[idx++].iter();
            }
            if (baseReader.moveNext())
            {
              currentValue = baseReader.current();
              return true;
            }
            else
            {
              baseReader.close();
              baseReader = null;
            }
          }
        }
      };
    }
  }
  
  public Schema getSchema()
  {
    Schema[] inSchemata = new Schema[adapters.length];
    for (int i=0; i<adapters.length; i++)
    {
      inSchemata[i] = adapters[i].getSchema();
    }
    Schema schema = OrSchema.make(inSchemata);
    if( addIndex )
    {
      schema = schema.elements();
      schema = new ArraySchema( new Schema[]{ SchemaFactory.longSchema(), schema } ); 
      schema = new ArraySchema( null, schema );
    }
    return schema;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.mapred.InputFormat#getRecordReader(org.apache.hadoop.mapred.InputSplit,
   *      org.apache.hadoop.mapred.JobConf, org.apache.hadoop.mapred.Reporter)
   */
  @SuppressWarnings("unchecked")
  public RecordReader<JsonHolder, JsonHolder> getRecordReader(InputSplit split,
      JobConf job, Reporter reporter) throws IOException
  {
    CompositeSplit cSplit = (CompositeSplit) split;

    // 1. get the InputAdapter's array index (i) from the split
    final int idx = cSplit.getAdapterIdx();
    InputSplit baseSplit = cSplit.getSplit();

    try
    {
      // 2. get the ith adapter's args record
      JsonValue value = this.args.get(idx);
      // JRecord baseArgs = (JRecord) item.getNonNull();
      // record the current index to the job conf
      // ASSUMES: in map/reduce, the format's record reader is called *before*
      // the map class is configured
      writeCurrentIndex(job, idx); // FIXME: no longer needed

      // 3. insantiate and initialize the adapter
      HadoopInputAdapter adapter = (HadoopInputAdapter) AdapterStore.getStore().input
          .getAdapter(/** baseArgs, */
          value);

      // 4. create a new JobConf j'
      JobConf jTmp = new JobConf(job);

      // 5. call adapter's setupConf(j')
      // ConfiguratorUtil.writeToConf(adapter, jTmp, item/**baseArgs*/);
      adapter.setParallel(jTmp);

      // 6. configure the adapter from j'
      adapter.configure(jTmp);

      // 7. call adapter's getRecordReader with j'
      final RecordReader<JsonHolder, JsonHolder> reader = (RecordReader<JsonHolder, JsonHolder>)
         adapter.getRecordReader(baseSplit, jTmp, reporter);
      
      if( !addIndex )
      {
        return reader;
      }
      
      return new RecordReader<JsonHolder,JsonHolder>()
      {

        @Override
        public void close() throws IOException
        {
          reader.close();
        }

        @Override
        public JsonHolder createKey()
        {
          return reader.createKey();
        }

        @Override
        public JsonHolder createValue()
        {
          return reader.createValue();
        }

        @Override
        public long getPos() throws IOException
        {
          return reader.getPos();
        }

        @Override
        public float getProgress() throws IOException
        {
          return reader.getProgress();
        }

        @Override
        public boolean next(JsonHolder key, JsonHolder value) throws IOException
        {
          BufferedJsonArray pair = (BufferedJsonArray)value.value;
          if( pair != null )
          {
            value.value = pair.get(1);
          }
          else
          {
            pair = new BufferedJsonArray(2);
            pair.set(0, JsonLong.make(idx));
          }
          
          if( reader.next(key, value) )
          {
            pair.set(1, value.value);
            value.value = pair;
            return true;
          }
          
          return false;
        }
      };
      
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.Adapter#close()
   */
  public void close() throws Exception
  {
    // for each adapter, call its close
    int numAdapters = adapters.length;
    for (int i = 0; i < numAdapters; i++)
    {
      adapters[i].close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.mapred.InputFormat#getSplits(org.apache.hadoop.mapred.JobConf,
   *      int)
   */
  public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException
  {
    // initialize adapters
    try
    {
      initializeFrom(this.args);
    }
    catch (Exception e)
    {
      throw new IOException(e.getMessage());
    }

    // for each adapter
    int numAdapters = adapters.length;
    ArrayList<CompositeSplit> allSplits = new ArrayList<CompositeSplit>();
    for (int i = 0; i < numAdapters; i++)
    {
      JobConf jTmp = new JobConf(job);
      try
      {
        // ConfiguratorUtil.writeToConf((Configurator)adapters[i], jTmp,
        // (JRecord)this.args.nth(i).getNonNull());
        // ConfiguratorUtil.writeToConf((ConfSetter)adapters[i], jTmp,
        // this.args.nth(i));
        adapters[i].setParallel(jTmp);
      }
      catch (Exception e)
      {
        throw new IOException(e.getMessage());
      }
      // TODO: is this needed?
      // ((DefaultHadoopInputAdapter)adapters[i]).configure(jTmp);
      adapters[i].configure(jTmp);

      // get its splits
      InputSplit[] splits = ((InputFormat<?,?>) adapters[i]).getSplits(jTmp,
          numSplits);
      for (int j = 0; j < splits.length; j++)
      {
        // wrap the split with InputAdapter's array index
        allSplits.add(new CompositeSplit(splits[j], i)); // FIXME: memory
      }
    }
    return allSplits.toArray(new InputSplit[allSplits.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.mapred.InputFormat#validateInput(org.apache.hadoop.mapred.JobConf)
   */
  @Deprecated
  public void validateInput(JobConf job) throws IOException
  {
    // 1. read the args array and parse it
    try
    {
      this.args = ConfUtil.readConfArray(job, ConfSetter.CONFINOPTIONS_NAME);

      // instantiate and initialize it
      this.initializeFrom(this.args);

      // 2. for each adapter
      int numAdapters = adapters.length;
      for (int i = 0; i < numAdapters; i++)
      {
        // make a new JobConf j'
        JobConf jTmp = new JobConf(job);

        // call adapter's setupConf(j')
        // ConfiguratorUtil.writeToConf((ConfSetter)adapters[i], jTmp,
        // this.args.nth(i));
        adapters[i].setParallel(jTmp);
        // ConfiguratorUtil.writeToConf((Configurator)adapters[i], jTmp,
        // (JRecord)this.args.nth(i).getNonNull());

        // call adapter's validateInput(j')
//        ((InputFormat<?,?>) adapters[i]).validateInput(jTmp);
      }
    }
    catch (Exception e)
    {
      throw new IOException(e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.hadoop.ConfSetter#setSequential(org.apache.hadoop.mapred.JobConf)
   */
  public void setSequential(JobConf conf) throws Exception
  {
    set(conf);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.jaql.io.hadoop.ConfSetter#setParallel(org.apache.hadoop.mapred.JobConf)
   */
  public void setParallel(JobConf conf) throws Exception
  {
    set(conf);
  }

  // FIXME: the "args" here makes no sense... should be JArray
  /**
   * @param conf
   * @throws Exception
   */
  protected void set(JobConf conf) throws Exception
  {
    conf.setInputFormat(this.getClass());

    // write out the input adapter args array
    ConfUtil.writeConfArray(conf, ConfSetter.CONFINOPTIONS_NAME, this.args);
    conf.set(ADD_INDEX_NAME, Boolean.toString(addIndex));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.mapred.JobConfigurable#configure(org.apache.hadoop.mapred.JobConf)
   */
  public void configure(JobConf conf)
  {
    Globals.setJobConf(conf);
    try
    {
      RegistryUtil.readConf(conf, HadoopAdapter.storeRegistryVarName,
          AdapterStore.getStore());
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    // read in the adapter array from conf
    try
    {
      this.args = ConfUtil.readConfArray(conf, ConfSetter.CONFINOPTIONS_NAME);
      this.addIndex = Boolean.parseBoolean( conf.get(ADD_INDEX_NAME) );
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param job
   * @param idx
   * @throws Exception
   */
  private void writeCurrentIndex(JobConf job, int idx) throws Exception
  {
    job.set(CURRENT_IDX_NAME, String.valueOf(idx));
  }

  /**
   * @param job
   * @return
   */
  public static int readCurrentIndex(JobConf job)
  {
    String v = job.get(CURRENT_IDX_NAME, "0");
    return Integer.parseInt(v);
  }
}

// TODO: make this a static inner class
/**
 * 
 */
class CompositeSplit implements InputSplit
{
  private InputSplit baseSplit;

  private int        adapterIdx;

  /**
   * 
   */
  public CompositeSplit()
  {
  }

  /**
   * @param split
   * @param idx
   */
  public CompositeSplit(InputSplit split, int idx)
  {
    this.baseSplit = split;
    this.adapterIdx = idx;
  }

  /**
   * @return
   */
  public int getAdapterIdx()
  {
    return adapterIdx;
  }

  /**
   * @return
   */
  public InputSplit getSplit()
  {
    return baseSplit;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.mapred.InputSplit#getLength()
   */
  public long getLength() throws IOException
  {
    return baseSplit.getLength();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.mapred.InputSplit#getLocations()
   */
  public String[] getLocations() throws IOException
  {
    return baseSplit.getLocations();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.io.Writable#readFields(java.io.DataInput)
   */
  public void readFields(DataInput in) throws IOException
  {
    this.adapterIdx = in.readInt();
    String cName = in.readUTF();
    try
    {
      Class<?> c = Class.forName(cName).asSubclass(InputSplit.class);;
      this.baseSplit = (InputSplit) ReflectionUtils.newInstance(c, null);
      this.baseSplit.readFields(in);
    }
    catch (ClassNotFoundException ce)
    {
      throw new IOException(ce.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.io.Writable#write(java.io.DataOutput)
   */
  public void write(DataOutput out) throws IOException
  {
    out.writeInt(adapterIdx);
    // WARNING: getCanonicalName may not work for inner classes.
    out.writeUTF(baseSplit.getClass().getCanonicalName());
    baseSplit.write(out);
  }

}
