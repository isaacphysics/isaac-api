package uk.ac.cam.cl.dtg.segue.search;

import java.util.Objects;

/**
 * A DO to consolidate the common parameters across multiple search methods.
 */
public class BasicSearchParameters {
  private String indexBase;
  private String indexType;
  private Integer startIndex;
  private Integer limit;

  /**
   * A DO to consolidate the common parameters across multiple search methods.
   *
   * @param indexBase  - the base string for the name of the index
   * @param indexType  - the name of the type of document being searched for
   * @param startIndex - start index for results, e.g. 0 for the first set of results
   * @param limit      - the maximum number of results to return, -1 will attempt to return all results.
   */
  public BasicSearchParameters(final String indexBase, final String indexType, final Integer startIndex,
                               final Integer limit) {
    this.indexBase = indexBase;
    this.indexType = indexType;
    this.startIndex = startIndex;
    this.limit = limit;
  }

  public String getIndexBase() {
    return indexBase;
  }

  public void setIndexBase(final String indexBase) {
    this.indexBase = indexBase;
  }

  public String getIndexType() {
    return indexType;
  }

  public void setIndexType(final String indexType) {
    this.indexType = indexType;
  }

  public Integer getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(final Integer startIndex) {
    this.startIndex = startIndex;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(final Integer limit) {
    this.limit = limit;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    BasicSearchParameters otherBSP = (BasicSearchParameters) obj;
    return Objects.equals(indexBase, otherBSP.indexBase)
        && Objects.equals(indexType, otherBSP.indexType)
        && Objects.equals(startIndex, otherBSP.startIndex)
        && Objects.equals(limit, otherBSP.limit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indexBase, indexType, startIndex, limit);
  }
}
