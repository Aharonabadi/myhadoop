/**
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

package org.apache.hadoop.mapreduce.task;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configuration.IntegerRanges;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.StringUtils;

/**
 * A read-only view of the job that is provided to the tasks while they
 * are running.
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class JobContextImpl implements JobContext {

  protected final org.apache.hadoop.mapred.JobConf conf;
  private JobID jobId;
  /**
   * The UserGroupInformation object that has a reference to the current user
   */
  protected UserGroupInformation ugi;
  protected final Credentials credentials;
  
  public JobContextImpl(Configuration conf, JobID jobId) {
    if (conf instanceof JobConf) {
      this.conf = (JobConf)conf;
    } else {
      this.conf = new JobConf(conf);
    }
    this.jobId = jobId;
    this.credentials = this.conf.getCredentials();
    try {
      this.ugi = UserGroupInformation.getCurrentUser();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the configuration for the job.
   * @return the shared configuration object
   */
  public Configuration getConfiguration() {
    return conf;
  }

  /**
   * Get the unique ID for the job.
   * @return the object with the job id
   */
  public JobID getJobID() {
    return jobId;
  }
  
  /**
   * Set the JobID.
   */
  public void setJobID(JobID jobId) {
    this.jobId = jobId;
  }
  
  /**
   * Get configured the number of reduce tasks for this job. Defaults to 
   * <code>1</code>.
   * @return the number of reduce tasks for this job.
   */
  public int getNumReduceTasks() {
    return conf.getNumReduceTasks();
  }
  
  /**
   * Get the current working directory for the default file system.
   * 
   * @return the directory name.
   */
  public Path getWorkingDirectory() throws IOException {
    return conf.getWorkingDirectory();
  }

  /**
   * Get the key class for the job output data.
   * @return the key class for the job output data.
   */
  public Class<?> getOutputKeyClass() {
    return conf.getOutputKeyClass();
  }
  
  /**
   * Get the value class for job outputs.
   * @return the value class for job outputs.
   */
  public Class<?> getOutputValueClass() {
    return conf.getOutputValueClass();
  }

  /**
   * Get the key class for the map output data. If it is not set, use the
   * (final) output key class. This allows the map output key class to be
   * different than the final output key class.
   * @return the map output key class.
   */
  public Class<?> getMapOutputKeyClass() {
    return conf.getMapOutputKeyClass();
  }

  /**
   * Get the value class for the map output data. If it is not set, use the
   * (final) output value class This allows the map output value class to be
   * different than the final output value class.
   *  
   * @return the map output value class.
   */
  public Class<?> getMapOutputValueClass() {
    return conf.getMapOutputValueClass();
  }

  /**
   * Get the user-specified job name. This is only used to identify the 
   * job to the user.
   * 
   * @return the job's name, defaulting to "".
   */
  public String getJobName() {
    return conf.getJobName();
  }

  /**
   * Get the {@link InputFormat} class for the job.
   * 
   * @return the {@link InputFormat} class for the job.
   */
  @SuppressWarnings("unchecked")
  public Class<? extends InputFormat<?,?>> getInputFormatClass() 
     throws ClassNotFoundException {
    return (Class<? extends InputFormat<?,?>>) 
      conf.getClass(INPUT_FORMAT_CLASS_ATTR, TextInputFormat.class);
  }

  /**
   * Get the {@link Mapper} class for the job.
   * 
   * @return the {@link Mapper} class for the job.
   */
  @SuppressWarnings("unchecked")
  public Class<? extends Mapper<?,?,?,?>> getMapperClass() 
     throws ClassNotFoundException {
    return (Class<? extends Mapper<?,?,?,?>>) 
      conf.getClass(MAP_CLASS_ATTR, Mapper.class);
  }

  /**
   * Get the combiner class for the job.
   * 
   * @return the combiner class for the job.
   */
  @SuppressWarnings("unchecked")
  public Class<? extends Reducer<?,?,?,?>> getCombinerClass() 
     throws ClassNotFoundException {
    return (Class<? extends Reducer<?,?,?,?>>) 
      conf.getClass(COMBINE_CLASS_ATTR, null);
  }

  /**
   * Get the {@link Reducer} class for the job.
   * 
   * @return the {@link Reducer} class for the job.
   */
  @SuppressWarnings("unchecked")
  public Class<? extends Reducer<?,?,?,?>> getReducerClass() 
     throws ClassNotFoundException {
    return (Class<? extends Reducer<?,?,?,?>>) 
      conf.getClass(REDUCE_CLASS_ATTR, Reducer.class);
  }

  /**
   * Get the {@link OutputFormat} class for the job.
   * 
   * @return the {@link OutputFormat} class for the job.
   */
  @SuppressWarnings("unchecked")
  public Class<? extends OutputFormat<?,?>> getOutputFormatClass() 
     throws ClassNotFoundException {
    return (Class<? extends OutputFormat<?,?>>) 
      conf.getClass(OUTPUT_FORMAT_CLASS_ATTR, TextOutputFormat.class);
  }

  /**
   * Get the {@link Partitioner} class for the job.
   * 
   * @return the {@link Partitioner} class for the job.
   */
  @SuppressWarnings("unchecked")
  public Class<? extends Partitioner<?,?>> getPartitionerClass() 
     throws ClassNotFoundException {
    return (Class<? extends Partitioner<?,?>>) 
      conf.getClass(PARTITIONER_CLASS_ATTR, HashPartitioner.class);
  }

  /**
   * Get the {@link RawComparator} comparator used to compare keys.
   * 
   * @return the {@link RawComparator} comparator used to compare keys.
   */
  public RawComparator<?> getSortComparator() {
    return conf.getOutputKeyComparator();
  }

  /**
   * Get the pathname of the job's jar.
   * @return the pathname
   */
  public String getJar() {
    return conf.getJar();
  }

  /**
   * Get the user defined {@link RawComparator} comparator for
   * grouping keys of inputs to the combiner.
   *
   * @return comparator set by the user for grouping values.
   * @see Job#setCombinerKeyGroupingComparatorClass(Class) for details.
   */
  public RawComparator<?> getCombinerKeyGroupingComparator() {
    return conf.getCombinerKeyGroupingComparator();
  }

  /** 
   * Get the user defined {@link RawComparator} comparator for 
   * grouping keys of inputs to the reduce.
   * 
   * @return comparator set by the user for grouping values.
   * @see Job#setGroupingComparatorClass(Class) for details.  
   */
  public RawComparator<?> getGroupingComparator() {
    return conf.getOutputValueGroupingComparator();
  }
  
  /**
   * Get whether job-setup and job-cleanup is needed for the job 
   * 
   * @return boolean 
   */
  public boolean getJobSetupCleanupNeeded() {
    return conf.getBoolean(MRJobConfig.SETUP_CLEANUP_NEEDED, true);
  }
  
  /**
   * Get whether task-cleanup is needed for the job 
   * 
   * @return boolean 
   */
  public boolean getTaskCleanupNeeded() {
    return conf.getBoolean(MRJobConfig.TASK_CLEANUP_NEEDED, true);
  }

  /**
   * This method checks to see if symlinks are to be create for the 
   * localized cache files in the current working directory 
   * @return true if symlinks are to be created- else return false
   */
  public boolean getSymlink() {
    return DistributedCache.getSymlink(conf);
  }
  
  /**
   * Get the archive entries in classpath as an array of Path
   */
  public Path[] getArchiveClassPaths() {
    return getArchiveClassPaths(conf);
  }

  /**
   * Get the archive entries in classpath as an array of Path.
   * Used by internal DistributedCache code.
   *
   * @param conf Configuration that contains the classpath setting.
   * @return An array of Path consisting of archive entries in classpath.
   */
  public static Path[] getArchiveClassPaths(Configuration conf) {
    ArrayList<String> list = (ArrayList<String>)conf.getStringCollection(
        MRJobConfig.CLASSPATH_ARCHIVES);
    if (list.size() == 0) {
      return null;
    }
    Path[] paths = new Path[list.size()];
    for (int i = 0; i < list.size(); i++) {
      paths[i] = new Path(list.get(i));
    }
    return paths;
  }

  /**
   * Get cache archives set in the Configuration
   * @return A URI array of the caches set in the Configuration
   * @throws IOException
   */
  public URI[] getCacheArchives() throws IOException {
    return getCacheArchives(conf);
  }

  /**
   * Get cache archives set in the Configuration. Used by
   * internal DistributedCache and JobContextImpl code.
   *
   * @param conf The configuration which contains the archives.
   * @return A URI array of the caches set in the Configuration.
   */
  public static URI[] getCacheArchives(Configuration conf) {
    return StringUtils.stringToURI(conf.getStrings(MRJobConfig.CACHE_ARCHIVES));
  }

  /**
   * Get cache files set in the Configuration
   * @return A URI array of the files set in the Configuration
   * @throws IOException
   */

  public URI[] getCacheFiles() throws IOException {
    return getCacheFiles(conf);
  }

  /**
   * Get cache files set in the Configuration. Used by internal
   * DistributedCache and MapReduce code.
   *
   * @param conf The configuration which contains the files.
   * @return A URI array of the files set in the Configuration.
   */
  public static URI[] getCacheFiles(Configuration conf) {
    return StringUtils.stringToURI(conf.getStrings(MRJobConfig.CACHE_FILES));
  }

  /**
   * Return the path array of the localized caches
   * @return A path array of localized caches
   * @throws IOException
   */
  public Path[] getLocalCacheArchives()
    throws IOException {
    return getLocalCacheArchives(conf);
  }

  /**
   * Return the path array of the localized caches.
   *
   * @param conf Configuration that contains the localized archives.
   * @return A path array of localized caches.
   */
  public static Path[] getLocalCacheArchives(Configuration conf) {
    return StringUtils.stringToPath(conf.getStrings(MRJobConfig.CACHE_LOCALARCHIVES));
  }

  /**
   * Return the path array of the localized files
   * @return A path array of localized files
   * @throws IOException
   */
  public Path[] getLocalCacheFiles()
    throws IOException {
    return getLocalCacheFiles(conf);
  }

  /**
   * Return the path array of the localized files.
   *
   * @param conf Configuration that contains the localized files.
   * @return A path array of localized files.
   */
  public static Path[] getLocalCacheFiles(Configuration conf) {
    return StringUtils.stringToPath(conf.getStrings(MRJobConfig.CACHE_LOCALFILES));
  }

  /**
   * Parse a list of strings into longs.
   * @param strs the list of strings to parse
   * @return a list of longs that were parsed. same length as strs.
   */
  private static long[] parseTimestamps(String[] strs) {
    if (strs == null) {
      return null;
    }
    long[] result = new long[strs.length];
    for(int i=0; i < strs.length; ++i) {
      result[i] = Long.parseLong(strs[i]);
    }
    return result;
  }

  /**
   * Get the timestamps of the archives. Used by internal
   * DistributedCache and MapReduce code.
   *
   * @param conf The configuration which stored the timestamps.
   * @return a long array of timestamps.
   */
  public static long[] getArchiveTimestamps(Configuration conf) {
    return parseTimestamps(conf.getStrings(MRJobConfig.CACHE_ARCHIVES_TIMESTAMPS));
  }

  /**
   * Get the timestamps of the files. Used by internal
   * DistributedCache and MapReduce code.
   *
   * @param conf The configuration which stored the timestamps.
   * @return a long array of timestamps.
   */
  public static long[] getFileTimestamps(Configuration conf) {
    return parseTimestamps(conf.getStrings(MRJobConfig.CACHE_FILE_TIMESTAMPS));
  }

  /**
   * Get the file entries in classpath as an array of Path
   */
  public Path[] getFileClassPaths() {
    return getFileClassPaths(conf);
  }

  /**
   * Get the file entries in classpath as an array of Path.
   * Used by internal DistributedCache code.
   *
   * @param conf Configuration that contains the classpath setting.
   * @return Array of Path consisting of file entries in the classpath.
   */
  public static Path[] getFileClassPaths(Configuration conf) {
    ArrayList<String> list =
        (ArrayList<String>) conf.getStringCollection(MRJobConfig.CLASSPATH_FILES);
    if (list.size() == 0) {
      return null;
    }
    Path[] paths = new Path[list.size()];
    for (int i = 0; i < list.size(); i++) {
      paths[i] = new Path(list.get(i));
    }
    return paths;
  }

  /**
   * Parse a list of longs into strings.
   * @param timestamps the list of longs to parse
   * @return a list of string that were parsed. same length as timestamps.
   */
  private static String[] toTimestampStrs(long[] timestamps) {
    if (timestamps == null) {
      return null;
    }
    String[] result = new String[timestamps.length];
    for(int i=0; i < timestamps.length; ++i) {
      result[i] = Long.toString(timestamps[i]);
    }
    return result;
  }

  /**
   * Get the timestamps of the archives.  Used by internal
   * DistributedCache and MapReduce code.
   * @return a string array of timestamps 
   */
  public String[] getArchiveTimestamps() {
    return toTimestampStrs(getArchiveTimestamps(conf));
  }

  /**
   * Get the timestamps of the files.  Used by internal
   * DistributedCache and MapReduce code.
   * @return a string array of timestamps 
   */
  public String[] getFileTimestamps() {
    return toTimestampStrs(getFileTimestamps(conf));
  }

  /** 
   * Get the configured number of maximum attempts that will be made to run a
   * map task, as specified by the <code>mapred.map.max.attempts</code>
   * property. If this property is not already set, the default is 4 attempts.
   *  
   * @return the max number of attempts per map task.
   */
  public int getMaxMapAttempts() {
    return conf.getMaxMapAttempts();
  }

  /** 
   * Get the configured number of maximum attempts  that will be made to run a
   * reduce task, as specified by the <code>mapred.reduce.max.attempts</code>
   * property. If this property is not already set, the default is 4 attempts.
   * 
   * @return the max number of attempts per reduce task.
   */
  public int getMaxReduceAttempts() {
    return conf.getMaxReduceAttempts();
  }

  /**
   * Get whether the task profiling is enabled.
   * @return true if some tasks will be profiled
   */
  public boolean getProfileEnabled() {
    return conf.getProfileEnabled();
  }

  /**
   * Get the profiler configuration arguments.
   *
   * The default value for this property is
   * "-agentlib:hprof=cpu=samples,heap=sites,force=n,thread=y,verbose=n,file=%s"
   * 
   * @return the parameters to pass to the task child to configure profiling
   */
  public String getProfileParams() {
    return conf.getProfileParams();
  }

  /**
   * Get the range of maps or reduces to profile.
   * @param isMap is the task a map?
   * @return the task ranges
   */
  public IntegerRanges getProfileTaskRange(boolean isMap) {
    return conf.getProfileTaskRange(isMap);
  }

  /**
   * Get the reported username for this job.
   * 
   * @return the username
   */
  public String getUser() {
    return conf.getUser();
  }

  public Credentials getCredentials() {
    return credentials;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(
        "JobContextImpl{");
    sb.append("jobId=").append(jobId);
    sb.append('}');
    return sb.toString();
  }
}
