package org.camunda.optimize.upgrade.to;


public class SourceWrapper {
  private String index;

  public SourceWrapper(String sourceIndex) {
    this.index = sourceIndex;
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }
}
