import static org.junit.Assert.*;

import org.junit.Test;

import asl.azimuth.TimeRange;

/**
 * 
 */

/**
 * @author akearns
 *
 */
public class TimeRangeTester {

  long st1 = 1234L;
  long st2 = 2345L;
  long st3 = 3455L;
  long st4 = 3456L;
  TimeRange tr1 = new TimeRange(st1,st2);
  TimeRange tr2 = new TimeRange(st2,st3);
  TimeRange tr3 = new TimeRange(st2,st4);
  TimeRange tr4 = new TimeRange(st1,st3);
  
  @Test
  public void testOrdering(){
    assertTrue((tr1.compareTo(tr2) > 0));
  }
  
  @Test
  public void testMerge(){
    TimeRange testTime = new TimeRange(tr4,tr3);
    assertEquals(testTime,tr2);  
  }

}
