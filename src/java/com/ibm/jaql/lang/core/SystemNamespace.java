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
package com.ibm.jaql.lang.core;

import java.util.HashMap;
import java.util.Map;

import com.ibm.jaql.json.schema.SchemaFactory;
import com.ibm.jaql.lang.expr.agg.AnyAgg;
import com.ibm.jaql.lang.expr.agg.ArgMaxAgg;
import com.ibm.jaql.lang.expr.agg.ArgMinAgg;
import com.ibm.jaql.lang.expr.agg.ArrayAgg;
import com.ibm.jaql.lang.expr.agg.AvgAgg;
import com.ibm.jaql.lang.expr.agg.CombineExpr;
import com.ibm.jaql.lang.expr.agg.CountAgg;
import com.ibm.jaql.lang.expr.agg.CovStatsAgg;
import com.ibm.jaql.lang.expr.agg.ExpSmoothAgg;
import com.ibm.jaql.lang.expr.agg.IcebergCubeInMemory;
import com.ibm.jaql.lang.expr.agg.InferElementSchemaAgg;
import com.ibm.jaql.lang.expr.agg.JavaUdaCallFn;
import com.ibm.jaql.lang.expr.agg.JavaUdaFn;
import com.ibm.jaql.lang.expr.agg.MaxAgg;
import com.ibm.jaql.lang.expr.agg.MinAgg;
import com.ibm.jaql.lang.expr.agg.PickNAgg;
import com.ibm.jaql.lang.expr.agg.SingletonAgg;
import com.ibm.jaql.lang.expr.agg.SumAgg;
import com.ibm.jaql.lang.expr.agg.TopNAgg;
import com.ibm.jaql.lang.expr.agg.UdaCallFn;
import com.ibm.jaql.lang.expr.agg.UdaFn;
import com.ibm.jaql.lang.expr.agg.VectorSumAgg;
import com.ibm.jaql.lang.expr.array.AppendFn;
import com.ibm.jaql.lang.expr.array.ArrayToRecordFn;
import com.ibm.jaql.lang.expr.array.AsArrayFn;
import com.ibm.jaql.lang.expr.array.ColumnwiseFn;
import com.ibm.jaql.lang.expr.array.DeemptyFn;
import com.ibm.jaql.lang.expr.array.DistinctFn;
import com.ibm.jaql.lang.expr.array.EnumerateExpr;
import com.ibm.jaql.lang.expr.array.ExistsFn;
import com.ibm.jaql.lang.expr.array.Lag1Fn;
import com.ibm.jaql.lang.expr.array.MergeFn;
import com.ibm.jaql.lang.expr.array.NextElementFn;
import com.ibm.jaql.lang.expr.array.PairFn;
import com.ibm.jaql.lang.expr.array.PairwiseFn;
import com.ibm.jaql.lang.expr.array.PowersetFn;
import com.ibm.jaql.lang.expr.array.PrevAndNextElementFn;
import com.ibm.jaql.lang.expr.array.PrevElementFn;
import com.ibm.jaql.lang.expr.array.RemoveElementFn;
import com.ibm.jaql.lang.expr.array.ReplaceElementFn;
import com.ibm.jaql.lang.expr.array.ReverseFn;
import com.ibm.jaql.lang.expr.array.RowwiseFn;
import com.ibm.jaql.lang.expr.array.RunningCombineFn;
import com.ibm.jaql.lang.expr.array.ShiftFn;
import com.ibm.jaql.lang.expr.array.SliceFn;
import com.ibm.jaql.lang.expr.array.SlidingWindowBySizeFn;
import com.ibm.jaql.lang.expr.array.SlidingWindowFn;
import com.ibm.jaql.lang.expr.array.ToArrayFn;
import com.ibm.jaql.lang.expr.array.TumblingWindowBySizeFn;
import com.ibm.jaql.lang.expr.array.TumblingWindowFn;
import com.ibm.jaql.lang.expr.array.UnionFn;
import com.ibm.jaql.lang.expr.binary.Base64Fn;
import com.ibm.jaql.lang.expr.binary.HexFn;
import com.ibm.jaql.lang.expr.catalog.CatalogInsertFn;
import com.ibm.jaql.lang.expr.catalog.CatalogLookupFn;
import com.ibm.jaql.lang.expr.catalog.CatalogUpdateFn;
import com.ibm.jaql.lang.expr.catalog.UpdateCommentFn;
import com.ibm.jaql.lang.expr.core.CatchExpr;
import com.ibm.jaql.lang.expr.core.CompareFn;
import com.ibm.jaql.lang.expr.core.DaisyChainFn;
import com.ibm.jaql.lang.expr.core.DiamondTagFn;
import com.ibm.jaql.lang.expr.core.ExpectExceptionFn;
import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.core.GetHdfsPathFn;
import com.ibm.jaql.lang.expr.core.GetOptionsFn;
import com.ibm.jaql.lang.expr.core.GroupCombineFn;
import com.ibm.jaql.lang.expr.core.IndexExpr;
import com.ibm.jaql.lang.expr.core.JumpFn;
import com.ibm.jaql.lang.expr.core.ListVariablesFn;
import com.ibm.jaql.lang.expr.core.MergeContainersFn;
import com.ibm.jaql.lang.expr.core.PerPartitionFn;
import com.ibm.jaql.lang.expr.core.PerfFn;
import com.ibm.jaql.lang.expr.core.RangeExpr;
import com.ibm.jaql.lang.expr.core.RegisterExceptionHandler;
import com.ibm.jaql.lang.expr.core.RetagFn;
import com.ibm.jaql.lang.expr.core.SetOptionsFn;
import com.ibm.jaql.lang.expr.core.SkipUntilFn;
import com.ibm.jaql.lang.expr.core.StreamSwitchFn;
import com.ibm.jaql.lang.expr.core.TagFlattenFn;
import com.ibm.jaql.lang.expr.core.TagFn;
import com.ibm.jaql.lang.expr.core.TagSplitFn;
import com.ibm.jaql.lang.expr.core.TeeExpr;
import com.ibm.jaql.lang.expr.core.TimeoutExpr;
import com.ibm.jaql.lang.expr.core.UntilFn;
import com.ibm.jaql.lang.expr.date.DateFn;
import com.ibm.jaql.lang.expr.date.DateMillisFn;
import com.ibm.jaql.lang.expr.date.DatePartsFn;
import com.ibm.jaql.lang.expr.date.NowFn;
import com.ibm.jaql.lang.expr.db.JdbcExpr;
import com.ibm.jaql.lang.expr.del.JsonToDelFn;
import com.ibm.jaql.lang.expr.function.AddClassPathFn;
import com.ibm.jaql.lang.expr.function.AddRelativeClassPathFn;
import com.ibm.jaql.lang.expr.function.BuiltInFunction;
import com.ibm.jaql.lang.expr.function.BuiltInFunctionDescriptor;
import com.ibm.jaql.lang.expr.function.FenceFunction;
import com.ibm.jaql.lang.expr.function.FencePushFunction;
import com.ibm.jaql.lang.expr.function.JavaUdfExpr;
import com.ibm.jaql.lang.expr.hadoop.BuildModelFn;
import com.ibm.jaql.lang.expr.hadoop.ChainedMapFn;
import com.ibm.jaql.lang.expr.hadoop.LoadJobConfExpr;
import com.ibm.jaql.lang.expr.hadoop.MRAggregate;
import com.ibm.jaql.lang.expr.hadoop.MapReduceFn;
import com.ibm.jaql.lang.expr.hadoop.NativeMapReduceExpr;
import com.ibm.jaql.lang.expr.hadoop.ReadConfExpr;
import com.ibm.jaql.lang.expr.index.BuildJIndexFn;
import com.ibm.jaql.lang.expr.index.KeyLookupFn;
import com.ibm.jaql.lang.expr.index.KeyMergeFn;
import com.ibm.jaql.lang.expr.index.ProbeJIndexFn;
import com.ibm.jaql.lang.expr.index.ProbeLongListFn;
import com.ibm.jaql.lang.expr.index.SharedHashtableNFn;
import com.ibm.jaql.lang.expr.internal.ExprTreeExpr;
import com.ibm.jaql.lang.expr.internal.HashExpr;
import com.ibm.jaql.lang.expr.internal.LongHashExpr;
import com.ibm.jaql.lang.expr.io.ArrayReadExpr;
import com.ibm.jaql.lang.expr.io.DelFn;
import com.ibm.jaql.lang.expr.io.ExpandFDExpr;
import com.ibm.jaql.lang.expr.io.FileFn;
import com.ibm.jaql.lang.expr.io.FileSplitToRecordFn;
import com.ibm.jaql.lang.expr.io.HBaseReadExpr;
import com.ibm.jaql.lang.expr.io.HBaseWriteExpr;
import com.ibm.jaql.lang.expr.io.HadoopTempExpr;
import com.ibm.jaql.lang.expr.io.HdfsFn;
import com.ibm.jaql.lang.expr.io.HdfsShellExpr;
import com.ibm.jaql.lang.expr.io.HttpFn;
import com.ibm.jaql.lang.expr.io.HttpGetExpr;
import com.ibm.jaql.lang.expr.io.InputSplitsFn;
import com.ibm.jaql.lang.expr.io.JaqlTempFn;
import com.ibm.jaql.lang.expr.io.LinesFn;
import com.ibm.jaql.lang.expr.io.LocalReadFn;
import com.ibm.jaql.lang.expr.io.LocalWriteFn;
import com.ibm.jaql.lang.expr.io.MakeFileSplitFn;
import com.ibm.jaql.lang.expr.io.ReadAdapterRegistryExpr;
import com.ibm.jaql.lang.expr.io.ReadFn;
import com.ibm.jaql.lang.expr.io.ReadSplitFn;
import com.ibm.jaql.lang.expr.io.RegisterAdapterExpr;
import com.ibm.jaql.lang.expr.io.UnregisterAdapterExpr;
import com.ibm.jaql.lang.expr.io.WriteAdapterRegistryExpr;
import com.ibm.jaql.lang.expr.io.WriteFn;
import com.ibm.jaql.lang.expr.net.JaqlGetFn;
import com.ibm.jaql.lang.expr.nil.DenullFn;
import com.ibm.jaql.lang.expr.nil.EmptyOnNullFn;
import com.ibm.jaql.lang.expr.nil.FirstNonNullFn;
import com.ibm.jaql.lang.expr.nil.NullElementOnEmptyFn;
import com.ibm.jaql.lang.expr.nil.NullOnEmptyFn;
import com.ibm.jaql.lang.expr.nil.OnEmptyFn;
import com.ibm.jaql.lang.expr.number.AbsFn;
import com.ibm.jaql.lang.expr.number.DecfloatFn;
import com.ibm.jaql.lang.expr.number.DivFn;
import com.ibm.jaql.lang.expr.number.DoubleFn;
import com.ibm.jaql.lang.expr.number.ExpFn;
import com.ibm.jaql.lang.expr.number.LnFn;
import com.ibm.jaql.lang.expr.number.LongFn;
import com.ibm.jaql.lang.expr.number.ModFn;
import com.ibm.jaql.lang.expr.number.NumberFn;
import com.ibm.jaql.lang.expr.number.PowFn;
import com.ibm.jaql.lang.expr.number.ToNumberFn;
import com.ibm.jaql.lang.expr.pragma.ConstPragma;
import com.ibm.jaql.lang.expr.pragma.InlinePragma;
import com.ibm.jaql.lang.expr.pragma.UnrollLoopPragma;
import com.ibm.jaql.lang.expr.random.RandomDoubleFn;
import com.ibm.jaql.lang.expr.random.RandomLongFn;
import com.ibm.jaql.lang.expr.random.RegisterRNGExpr;
import com.ibm.jaql.lang.expr.random.Sample01RNGExpr;
import com.ibm.jaql.lang.expr.random.SampleRNGExpr;
import com.ibm.jaql.lang.expr.random.UuidFn;
import com.ibm.jaql.lang.expr.record.ArityFn;
import com.ibm.jaql.lang.expr.record.FieldsFn;
import com.ibm.jaql.lang.expr.record.NamesFn;
import com.ibm.jaql.lang.expr.record.RecordFn;
import com.ibm.jaql.lang.expr.record.RemapFn;
import com.ibm.jaql.lang.expr.record.RemoveFieldsFn;
import com.ibm.jaql.lang.expr.record.RenameFieldsFn;
import com.ibm.jaql.lang.expr.record.ReplaceFieldsFn;
import com.ibm.jaql.lang.expr.record.ValuesFn;
import com.ibm.jaql.lang.expr.regex.RegexExtractAllFn;
import com.ibm.jaql.lang.expr.regex.RegexExtractFn;
import com.ibm.jaql.lang.expr.regex.RegexFn;
import com.ibm.jaql.lang.expr.regex.RegexMatchFn;
import com.ibm.jaql.lang.expr.regex.RegexSpansFn;
import com.ibm.jaql.lang.expr.regex.RegexTestFn;
import com.ibm.jaql.lang.expr.schema.AssertFn;
import com.ibm.jaql.lang.expr.schema.CheckFn;
import com.ibm.jaql.lang.expr.schema.DataGuideFn;
import com.ibm.jaql.lang.expr.schema.ElementsOfFn;
import com.ibm.jaql.lang.expr.schema.FieldsOfFn;
import com.ibm.jaql.lang.expr.schema.IsNullableFn;
import com.ibm.jaql.lang.expr.schema.SchemaOfExpr;
import com.ibm.jaql.lang.expr.schema.SqlTypeCodeFn;
import com.ibm.jaql.lang.expr.schema.TypeOfExpr;
import com.ibm.jaql.lang.expr.span.SpanBeginFn;
import com.ibm.jaql.lang.expr.span.SpanContainsFn;
import com.ibm.jaql.lang.expr.span.SpanEndFn;
import com.ibm.jaql.lang.expr.span.SpanExtractFn;
import com.ibm.jaql.lang.expr.span.SpanFn;
import com.ibm.jaql.lang.expr.span.SpanOverlapsFn;
import com.ibm.jaql.lang.expr.span.SpanSelectFn;
import com.ibm.jaql.lang.expr.span.TokenizeFn;
import com.ibm.jaql.lang.expr.string.ConvertFn;
import com.ibm.jaql.lang.expr.string.EndsWithFn;
import com.ibm.jaql.lang.expr.string.JsonFn;
import com.ibm.jaql.lang.expr.string.SerializeFn;
import com.ibm.jaql.lang.expr.string.StartsWithFn;
import com.ibm.jaql.lang.expr.string.StrJoinFn;
import com.ibm.jaql.lang.expr.string.StrLenFn;
import com.ibm.jaql.lang.expr.string.StrPosFn;
import com.ibm.jaql.lang.expr.string.StrPosListFn;
import com.ibm.jaql.lang.expr.string.StrReplaceFn;
import com.ibm.jaql.lang.expr.string.StrSplitFn;
import com.ibm.jaql.lang.expr.string.StrSplitNFn;
import com.ibm.jaql.lang.expr.string.StrToLowerCaseFn;
import com.ibm.jaql.lang.expr.string.StrToUpperCaseFn;
import com.ibm.jaql.lang.expr.string.StrcatFn;
import com.ibm.jaql.lang.expr.string.SubstringFn;
import com.ibm.jaql.lang.expr.system.BatchFn;
import com.ibm.jaql.lang.expr.system.ExecFn;
import com.ibm.jaql.lang.expr.system.ExternalFnExpr;
import com.ibm.jaql.lang.expr.system.LsFn;
import com.ibm.jaql.lang.expr.system.RFn;
import com.ibm.jaql.lang.expr.xml.JsonToXmlFn;
import com.ibm.jaql.lang.expr.xml.TypedXmlToJsonFn;
import com.ibm.jaql.lang.expr.xml.XPathFn;
import com.ibm.jaql.lang.expr.xml.XmlToJsonFn;
import com.ibm.jaql.lang.expr.xml.XsltFn;


/** The system namespace. Treated specially, always present. */
public final class SystemNamespace extends Module {
  public static final String NAME = "system";
  
	/** implementing class name to built in function */
  private final Map<Class<? extends Expr>, BuiltInFunctionDescriptor> implementationMap 
      = new HashMap<Class<? extends Expr>, BuiltInFunctionDescriptor>();
  
  
  // -- construction ------------------------------------------------------------------------------
  
  private static SystemNamespace theInstance;
  
  public static SystemNamespace getInstance()
  {
    if (theInstance == null)
    {
      theInstance = new SystemNamespace();
    }
    return theInstance;
  }

	private SystemNamespace()
	{
	  super(new Package(), NAME, null);
	  registerAll();
	  makeFinal();
	}
  
	
	// -- getters -----------------------------------------------------------------------------------
	
  // may return null
  public boolean isSystemExpr(Class<? extends Expr> c)
  {
    return implementationMap.containsKey(c);
  }
  
  
  // -- registration ------------------------------------------------------------------------------
	
  /** Adds a built-in function to the library. The argument is required to carry the 
   * {@link JaqlFn} annotation. The name of the function is extracted from this annotation. 
   * @param cls
   */
  private void register(BuiltInFunctionDescriptor descriptor)
  {
    // check args
    if (implementationMap.containsKey(descriptor.getImplementingClass()))
    {
      throw new IllegalArgumentException("implementing class " 
          + descriptor.getImplementingClass().getName() 
          + " registered using multiple descriptors: "
          + descriptor.getClass().getName()
          + ", " + implementationMap.get(descriptor.getImplementingClass()).getClass().getName());
    }
    if (variables.containsKey(descriptor.getName()))
    {
      throw new IllegalArgumentException("function name " + descriptor.getName() 
          + " registered twice");
    }
    
    // register
    BuiltInFunction f = new BuiltInFunction(descriptor);
    Var var = new Var(this, descriptor.getName(), SchemaFactory.schemaOf(f), f);
    variables.put(var.name(), var);
    // exportedVariables.add(var.name());
    implementationMap.put(descriptor.getImplementingClass(), descriptor);
  }
  
  private void registerAll()
  {
    // TODO: add "import extension" that loads the functions in some jar (and loads types?)
    // schema
    register(new TypeOfExpr.Descriptor());
    register(new SchemaOfExpr.Descriptor());
    register(new IsNullableFn.Descriptor());
    register(new ElementsOfFn.Descriptor());
    register(new FieldsOfFn.Descriptor());
    register(new SqlTypeCodeFn.Descriptor());
    register(new CheckFn.Descriptor());
    register(new AssertFn.Descriptor());
    //    
    register(new CompareFn.Descriptor());
    register(new ListVariablesFn.Descriptor());
    register(new ExistsFn.Descriptor());
    register(new Lag1Fn.Descriptor());
    register(new PowersetFn.Descriptor());
    //lib.put("loadXml", LoadXmlExpr.Descriptor());
    //lib.put("deepCompare", DeepCompareExpr.Descriptor());
    register(new NowFn.Descriptor());
    register(new DateFn.Descriptor());
    register(new DateMillisFn.Descriptor());
    register(new DatePartsFn.Descriptor());
    register(new HexFn.Descriptor());
    register(new Base64Fn.Descriptor());
    register(new CountAgg.Descriptor());
    register(new SumAgg.Descriptor());
    register(new MinAgg.Descriptor());
    register(new MaxAgg.Descriptor());
    register(new AvgAgg.Descriptor());
    register(new ArrayAgg.Descriptor());
    register(new SingletonAgg.Descriptor());
    register(new AnyAgg.Descriptor());
    register(new PickNAgg.Descriptor());
    register(new CombineExpr.Descriptor());
    register(new ArgMaxAgg.Descriptor());
    register(new ArgMinAgg.Descriptor());
    register(new TopNAgg.Descriptor());
    register(new CovStatsAgg.Descriptor()); // experimental
    register(new VectorSumAgg.Descriptor()); // experimental
    register(new InferElementSchemaAgg.Descriptor()); // experimental
    register(new ExpSmoothAgg.Descriptor());
    register(new GroupCombineFn.Descriptor()); // experimental
    register(new IcebergCubeInMemory.Descriptor()); // experimental
    register(new JumpFn.Descriptor());
    register(new StreamSwitchFn.Descriptor()); // experimental / internal use
    register(new UntilFn.Descriptor()); // experimental
    register(new SkipUntilFn.Descriptor()); // experimental
    register(new DiamondTagFn.Descriptor()); // internal use
    register(new RetagFn.Descriptor()); // internal use
    register(new TagFlattenFn.Descriptor()); // internal use
    register(new TagFn.Descriptor()); // internal use
    register(new TagSplitFn.Descriptor()); // internal use
    register(new TeeExpr.Descriptor());
    register(new PerPartitionFn.Descriptor());
    register(new PerfFn.Descriptor());
    register(new ExpectExceptionFn.Descriptor());
    register(new ShiftFn.Descriptor());
    register(new PrevElementFn.Descriptor());
    register(new NextElementFn.Descriptor());
    register(new PrevAndNextElementFn.Descriptor());
    register(new TumblingWindowFn.Descriptor()); // experimental
    register(new TumblingWindowBySizeFn.Descriptor()); // experimental
    register(new SlidingWindowFn.Descriptor()); // experimental
    register(new SlidingWindowBySizeFn.Descriptor()); // experimental
    register(new RunningCombineFn.Descriptor()); // experimental
    register(new ModFn.Descriptor());
    register(new DivFn.Descriptor());
    register(new AbsFn.Descriptor());
    register(new LongFn.Descriptor());
    register(new NumberFn.Descriptor());
    register(new DoubleFn.Descriptor());
    register(new DecfloatFn.Descriptor());
    register(new ToNumberFn.Descriptor());
    register(new SetOptionsFn.Descriptor());
    register(new GetOptionsFn.Descriptor());
    register(new MapReduceFn.Descriptor());
    register(new NativeMapReduceExpr.Descriptor());
    register(new MRAggregate.Descriptor());
    register(new UdaFn.Descriptor());
    register(new UdaCallFn.Descriptor());
    register(new JavaUdaFn.Descriptor());
    register(new JavaUdaCallFn.Descriptor());
    //    register(new MRCogroup.Descriptor());
    // register(new DefaultExpr.Descriptor());
    register(new JdbcExpr.Descriptor());
    register(new SpanFn.Descriptor());
    register(new SpanOverlapsFn.Descriptor());
    register(new SpanContainsFn.Descriptor());
    register(new SpanBeginFn.Descriptor());
    register(new SpanEndFn.Descriptor());
    register(new SpanExtractFn.Descriptor());
    register(new SpanSelectFn.Descriptor());
    register(new RegexFn.Descriptor());
    register(new RegexTestFn.Descriptor());
    register(new RegexMatchFn.Descriptor());
    register(new RegexSpansFn.Descriptor());
    register(new RegexExtractFn.Descriptor());
    register(new RegexExtractAllFn.Descriptor());
    register(new TokenizeFn.Descriptor());
    register(new XmlToJsonFn.Descriptor());
    register(new TypedXmlToJsonFn.Descriptor());
    register(new XPathFn.Descriptor());
    register(new XsltFn.Descriptor());
    register(new JsonToXmlFn.Descriptor());
    register(new JsonToDelFn.Descriptor());
    //register(new IsnullExpr.Descriptor());
    register(new DenullFn.Descriptor());
    register(new DeemptyFn.Descriptor());
    register(new StartsWithFn.Descriptor());
    register(new EndsWithFn.Descriptor());
    register(new SubstringFn.Descriptor());
    register(new StrLenFn.Descriptor());
    register(new StrPosFn.Descriptor());
    register(new StrPosListFn.Descriptor());
    register(new StrReplaceFn.Descriptor());
    register(new BatchFn.Descriptor());
    register(new SerializeFn.Descriptor());
    register(new StrcatFn.Descriptor());
    register(new StrToLowerCaseFn.Descriptor());
    register(new StrToUpperCaseFn.Descriptor()); 
    register(new StrSplitNFn.Descriptor());
    register(new StrSplitFn.Descriptor());
    register(new StrJoinFn.Descriptor());
    register(new ConvertFn.Descriptor());
    register(new JsonFn.Descriptor());
    register(new RecordFn.Descriptor());
    register(new ArityFn.Descriptor());
    register(new PairwiseFn.Descriptor());
    register(new NullElementOnEmptyFn.Descriptor());
    register(new NullOnEmptyFn.Descriptor());
    register(new JaqlGetFn.Descriptor());
    register(new RemoveFieldsFn.Descriptor());
    register(new FieldsFn.Descriptor());
    // register(new IsdefinedExpr.Descriptor());
    register(new NamesFn.Descriptor());
    register(new ValuesFn.Descriptor());
    register(new ArrayToRecordFn.Descriptor());
    register(new RemapFn.Descriptor());
    register(new ReplaceFieldsFn.Descriptor());
    register(new RenameFieldsFn.Descriptor());
    register(new AppendFn.Descriptor());
    register(new ColumnwiseFn.Descriptor());
    register(new RowwiseFn.Descriptor());
    register(new SliceFn.Descriptor());
    register(new IndexExpr.Descriptor());
    register(new ExecFn.Descriptor());
    register(new RFn.Descriptor());
    register(new ReplaceElementFn.Descriptor());
    register(new RemoveElementFn.Descriptor());
    register(new InlinePragma.Descriptor());
    register(new ConstPragma.Descriptor());
    register(new UnrollLoopPragma.Descriptor());
    register(new AsArrayFn.Descriptor());
    register(new ToArrayFn.Descriptor());
    register(new EnumerateExpr.Descriptor());
    register(new RangeExpr.Descriptor());
    // register(new CombinerExpr.Descriptor());
    register(new MergeFn.Descriptor());
    register(new UnionFn.Descriptor());
    register(new MergeContainersFn.Descriptor());
    register(new ReverseFn.Descriptor());
    register(new OnEmptyFn.Descriptor());
    register(new FirstNonNullFn.Descriptor());
    register(new EmptyOnNullFn.Descriptor());
    register(new PairFn.Descriptor());
    register(new ExpFn.Descriptor());
    register(new LnFn.Descriptor());
    register(new PowFn.Descriptor());
    register(new RandomLongFn.Descriptor());
    register(new RandomDoubleFn.Descriptor());
    register(new UuidFn.Descriptor());
    register(new DistinctFn.Descriptor());
    register(new ReadFn.Descriptor());
    register(new WriteFn.Descriptor());
    register(new LocalWriteFn.Descriptor());
    register(new LocalReadFn.Descriptor());
    register(new InputSplitsFn.Descriptor()); // TODO: experimental
    register(new ReadSplitFn.Descriptor()); // TODO: experimental
    register(new MakeFileSplitFn.Descriptor()); // TODO: experimental
    register(new FileSplitToRecordFn.Descriptor()); // TODO: experimental
    register(new HdfsFn.Descriptor());
    register(new DelFn.Descriptor());
    register(new LinesFn.Descriptor());
    register(new FileFn.Descriptor());
    register(new HttpFn.Descriptor());
    register(new JaqlTempFn.Descriptor());
    register(new CatalogInsertFn.Descriptor());
    register(new CatalogLookupFn.Descriptor());
    register(new CatalogUpdateFn.Descriptor());
    register(new UpdateCommentFn.Descriptor());
  // TODO: delete: register(new HdfsWriteExpr.Descriptor());
  // TODO: delete: register(new HdfsReadExpr.Descriptor());
    register(new HadoopTempExpr.Descriptor());
    register(new HBaseWriteExpr.Descriptor());
    register(new HBaseReadExpr.Descriptor());
    register(new ArrayReadExpr.Descriptor());
    register(new HttpGetExpr.Descriptor());
    // store registration expressions
    register(new RegisterAdapterExpr.Descriptor());
    register(new UnregisterAdapterExpr.Descriptor());
    register(new WriteAdapterRegistryExpr.Descriptor());
    register(new ReadAdapterRegistryExpr.Descriptor());
    // rand expressions
    register(new RegisterRNGExpr.Descriptor());
    register(new SampleRNGExpr.Descriptor());
    register(new Sample01RNGExpr.Descriptor());
    register(new ReadConfExpr.Descriptor());
    register(new LoadJobConfExpr.Descriptor());
    // lower level shell access
    register(new LsFn.Descriptor());
    register(new HdfsShellExpr.Descriptor());
    register(new SharedHashtableNFn.Descriptor()); // TODO: experimental
    register(new KeyLookupFn.Descriptor()); // TODO: experimental
    register(new KeyMergeFn.Descriptor()); // TODO: experimental
    register(new ProbeLongListFn.Descriptor()); // TODO: experimental
    register(new DaisyChainFn.Descriptor()); // TODO: experimental
    //register(new BuildLuceneFn.Descriptor()); // TODO: experimental
    //register(new ProbeLuceneFn.Descriptor()); // TODO: experimental
    register(new BuildJIndexFn.Descriptor());
    register(new ProbeJIndexFn.Descriptor());
    register(new BuildModelFn.Descriptor()); // TODO: experimental
    register(new ChainedMapFn.Descriptor()); // TODO: experimental
    // internal
    register(new ExprTreeExpr.Descriptor());
    register(new HashExpr.Descriptor());
    register(new LongHashExpr.Descriptor());
    register(new DataGuideFn.Descriptor());
    register(new JavaUdfExpr.Descriptor());
    register(new AddClassPathFn.Descriptor());
    register(new AddRelativeClassPathFn.Descriptor());
    register(new CatchExpr.Descriptor());
    register(new TimeoutExpr.Descriptor());
    register(new RegisterExceptionHandler.Descriptor());
    register(new FenceFunction.Descriptor());
    register(new FencePushFunction.Descriptor());
    register(new ExternalFnExpr.Descriptor());
    register(new GetHdfsPathFn.Descriptor());
    register(new ExpandFDExpr.Descriptor());
  } 

}
