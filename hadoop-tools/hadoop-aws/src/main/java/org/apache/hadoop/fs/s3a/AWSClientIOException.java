/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.s3a;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkBaseException;
import org.apache.hadoop.util.Preconditions;

import java.io.IOException;

/**
 * IOException equivalent of an {@link AmazonClientException}.
 */
public class AWSClientIOException extends IOException {

  private final String operation;

  public AWSClientIOException(String operation,
      SdkBaseException cause) {
    super(cause);
    Preconditions.checkArgument(operation != null, "Null 'operation' argument");
    Preconditions.checkArgument(cause != null, "Null 'cause' argument");
    this.operation = operation;
  }

  public AmazonClientException getCause() {
    return (AmazonClientException) super.getCause();
  }

  @Override
  public String getMessage() {
    return operation + ": " + getCause().getMessage();
  }

}
