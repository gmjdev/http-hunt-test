package com.gm.test.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public class ToolUsage {
  private String name;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime useStartTime;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime useEndTime;
  private long usagesDuration;
  private int weight;
  private int value;

  public ToolUsage(String name, int weight, int value) {
    super();
    this.name = name;
    this.weight = weight;
    this.value = value;
  }

  public ToolUsage() {}

  public ToolUsage(String name, LocalDateTime useStartTime, LocalDateTime useEndTime) {
    this.name = name;
    this.useStartTime = useStartTime;
    this.useEndTime = useEndTime;
  }

  public ToolUsage(String name, long usagesDuration) {
    super();
    this.name = name;
    this.usagesDuration = usagesDuration;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public long getUsagesDuration() {
    return usagesDuration;
  }

  public void setUsagesDuration(long usagesDuration) {
    this.usagesDuration = usagesDuration;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getUseStartTime() {
    return useStartTime;
  }

  public void setUseStartTime(LocalDateTime useStartTime) {
    this.useStartTime = useStartTime;
  }

  public LocalDateTime getUseEndTime() {
    return useEndTime;
  }

  public void setUseEndTime(LocalDateTime useEndTime) {
    this.useEndTime = useEndTime;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    result = prime * result + (useEndTime == null ? 0 : useEndTime.hashCode());
    result = prime * result + (useStartTime == null ? 0 : useStartTime.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ToolUsage)) {
      return false;
    }
    ToolUsage other = (ToolUsage) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (useEndTime == null) {
      if (other.useEndTime != null) {
        return false;
      }
    } else if (!useEndTime.equals(other.useEndTime)) {
      return false;
    }
    if (useStartTime == null) {
      if (other.useStartTime != null) {
        return false;
      }
    } else if (!useStartTime.equals(other.useStartTime)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "ToolUsage [name=" + name + ", useStartTime=" + useStartTime + ", useEndTime=" + useEndTime
        + ", usagesDuration=" + usagesDuration + ", weight=" + weight + ", value=" + value + "]";
  }

  public static ToolUsage valueOf(String name, LocalDateTime useStartTime, LocalDateTime useEndTime) {
    return new ToolUsage(name, useStartTime, useEndTime);
  }

  public static ToolUsage valueOf(String name, long usagesDuration) {
    return new ToolUsage(name, usagesDuration);
  }
}
