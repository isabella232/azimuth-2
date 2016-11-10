package asl.azimuth;

import java.util.Date;

/**
 * Helper class to find and extract ranges of time from data
 * @author amkearns
 */
public class TimeRange implements Comparable<TimeRange>{

  // just an interval between two times (as longs)
  private long startTime;
  private long endTime;

  /**
   * Create a new TimeRange by assigning a start and end time
   * @param start Start time
   * @param end   End time
   */
  public TimeRange(long start, long end){
    startTime = start;
    endTime = end;
  }

  /**
   * @return The TimeRange's start time
   */
  public long getStart(){
    return startTime;
  }

  /**
   * @return The TimeRange's end time
   */
  public long getEnd(){
    return endTime;
  }

  /**
   * @return The length of time between start and end
   */
  public long length(){
    return endTime - startTime;
  }

  /**
   * Used mainly for when displaying the boundaries of the range as text
   * @return The start time as a java Date
   */
  public Date getStartAsDate(){
    return new Date(startTime);
  }

  /**
   * Used mainly for when displaying the boundaries of the range as text
   * @return The end time as a java Date
   */
  public Date getEndAsDate(){
    return new Date(endTime);
  }

  /**
   * Used mainly to display ranges in the combo box of the main display
   * @return A string of the start and end times as Dates, dash-delimited
   */
  public String toString(){
    return getStartAsDate() + " - " + getEndAsDate();
  }

  /**
   * Equals method used for populating sets with distinct TimeRanges
   * @return True if-only-if both timeranges have same start and end date
   */
  public boolean equals(Object o){
    if(o instanceof TimeRange){
       TimeRange tr2 = (TimeRange)o;
       return this.startTime == tr2.getStart() && this.endTime == tr2.getEnd();
    }
    return false;

  }

  /**
   * Returns a hash code, the product of the start and end times
   * @return A hash code for this object.
   */
  public int hashCode() {

    int result = (int)(startTime * endTime);
    return result;

  }

  /**
   * Implements comparable interface by finding the longer of two segments.
   * Shorter segments come first in the list.
   * Two segments with the SAME length are sorted by start time,
   * such that the earlier time is listed first.
   * Two segments with same duration, start, and end are equal.
   * @return +/- 1 or 0 according to comparable and strategy described above
   */
  public int compareTo(TimeRange trc){

    if(this.equals(trc)){
      return 0;
    }

    if(this.length() == trc.length()){
      return Long.signum(this.getStart() - trc.getStart());
    }

    return Long.signum(this.length() - trc.length());

  }


  /**
   * Creates a TimeRange from the overlap between two time ranges.
   * Throws an exception if the overlap is zero
   * @param tr1 One of the TimeRanges
   * @param tr2 The other TimeRange
   */
  public TimeRange(TimeRange tr1, TimeRange tr2) throws ArithmeticException{
    if(tr1.getStart() > tr2.getEnd() || tr2.getStart() > tr1.getEnd()){
      // triggered if one block starts after the other ends
      // (i.e., means there is no overlap)
      throw new ArithmeticException();
    }

    // we want to get the common time range, which is the later start
    // and the earlier end

    startTime = tr1.getStart();
    if (tr2.getStart() > startTime){
      // if the other time starts after, we use that
      startTime = tr2.getStart();
    }

    endTime = tr1.getEnd();
    if(tr2.getEnd() < endTime){
      // if the other time ends before, we use that
      endTime = tr2.getEnd();
    }
  }
}
