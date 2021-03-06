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

package org.apache.hadoop.yarn.server.nodemanager.containermanager.scheduler;

import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.ResourceUtilization;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.Container;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.monitor.ContainersMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the {@link ResourceUtilizationTracker} that equates
 * resource utilization with the total resource allocated to the container.
 */
public class AllocationBasedResourceUtilizationTracker implements
    ResourceUtilizationTracker {

  private static final Logger LOG =
      LoggerFactory.getLogger(AllocationBasedResourceUtilizationTracker.class);

  private static final long LEFT_SHIFT_MB_IN_BYTES = 20;
  private static final int RIGHT_SHIFT_BYTES_IN_MB = 20;

  private ResourceUtilization containersAllocation;
  private ContainerScheduler scheduler;

  AllocationBasedResourceUtilizationTracker(ContainerScheduler scheduler) {
    this.containersAllocation = ResourceUtilization.newInstance(0, 0, 0.0f);
    this.scheduler = scheduler;
  }

  /**
   * Get the accumulation of totally allocated resources to a container.
   * @return ResourceUtilization Resource Utilization.
   */
  @Override
  public ResourceUtilization getCurrentUtilization() {
    return this.containersAllocation;
  }

  /**
   * Add Container's resources to the accumulated Utilization.
   * @param container Container.
   */
  @Override
  public void addContainerResources(Container container) {
    ContainersMonitor.increaseResourceUtilization(
        getContainersMonitor(), this.containersAllocation,
        container.getResource());
  }

  /**
   * Subtract Container's resources to the accumulated Utilization.
   * @param container Container.
   */
  @Override
  public void subtractContainerResource(Container container) {
    ContainersMonitor.decreaseResourceUtilization(
        getContainersMonitor(), this.containersAllocation,
        container.getResource());
  }

  /**
   * Check if NM has resources available currently to run the container.
   * @param container Container.
   * @return True, if NM has resources available currently to run the container.
   */
  @Override
  public boolean hasResourcesAvailable(Container container) {
    return hasResourcesAvailable(container.getResource());
  }

  /**
   * Converts memory in megabytes to bytes by bitwise left-shifting 20 times.
   * @param memMB the memory in megabytes
   * @return the memory in bytes
   */
  private static long convertMBToBytes(final long memMB) {
    return memMB << LEFT_SHIFT_MB_IN_BYTES;
  }

  /**
   * Converts memory in bytes to megabytes by bitwise right-shifting 20 times.
   * @param bytes the memory in bytes
   * @return the memory in megabytes
   */
  private static long convertBytesToMB(final long bytes) {
    return bytes >> RIGHT_SHIFT_BYTES_IN_MB;
  }

  @Override
  public boolean hasResourcesAvailable(Resource resource) {
    long pMemBytes = convertMBToBytes(resource.getMemorySize());
    final long vmemBytes = (long)
        (getContainersMonitor().getVmemRatio() * pMemBytes);
    return hasResourcesAvailable(
        pMemBytes, vmemBytes, resource.getVirtualCores());
  }

  private boolean hasResourcesAvailable(long pMemBytes, long vMemBytes,
      int cpuVcores) {
    // Check physical memory.
    if (LOG.isDebugEnabled()) {
      LOG.debug("pMemCheck [current={} + asked={} > allowed={}]",
          this.containersAllocation.getPhysicalMemory(),
          convertBytesToMB(pMemBytes),
          convertBytesToMB(
              getContainersMonitor().getPmemAllocatedForContainers()));
    }
    if (this.containersAllocation.getPhysicalMemory() +
        convertBytesToMB(pMemBytes) > convertBytesToMB(
            getContainersMonitor().getPmemAllocatedForContainers())) {
      return false;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("before vMemCheck" +
              "[isEnabled={}, current={} + asked={} > allowed={}]",
          getContainersMonitor().isVmemCheckEnabled(),
          this.containersAllocation.getVirtualMemory(),
          convertBytesToMB(vMemBytes),
          convertBytesToMB(
              getContainersMonitor().getVmemAllocatedForContainers()));
    }
    // Check virtual memory.
    if (getContainersMonitor().isVmemCheckEnabled() &&
        this.containersAllocation.getVirtualMemory() +
            convertBytesToMB(vMemBytes) >
            convertBytesToMB(getContainersMonitor()
                .getVmemAllocatedForContainers())) {
      return false;
    }

    LOG.debug("before cpuCheck [asked={} > allowed={}]",
        this.containersAllocation.getCPU(),
        getContainersMonitor().getVCoresAllocatedForContainers());
    // Check CPU.
    if (this.containersAllocation.getCPU() + cpuVcores >
        getContainersMonitor().getVCoresAllocatedForContainers()) {
      return false;
    }
    return true;
  }

  public ContainersMonitor getContainersMonitor() {
    return this.scheduler.getContainersMonitor();
  }
}
