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
package com.ibm.jaql.lang.expr.io;

import com.ibm.jaql.lang.expr.core.Expr;
import com.ibm.jaql.lang.expr.function.DefaultBuiltInFunctionDescriptor;

/**
 * An expression used for reading data into jaql. The only different between
 * localRead and Read is that localRead is disabled for mapReduce rewirte.
 * 
 * @see ReadFn
 */
public class LocalReadFn extends AbstractReadExpr {
  public static class Descriptor extends DefaultBuiltInFunctionDescriptor.Par11 {
    public Descriptor() {
      super("localRead", LocalReadFn.class);
    }
  }

  /**
   * @param exprs
   */
  public LocalReadFn(Expr[] exprs) {
    super(exprs);
  }
}
