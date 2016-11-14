package asl.azimuth.test;

import static org.junit.Assert.*;

import org.junit.Test;

import asl.azimuth.FilterHelper;
import java.util.Arrays;

public class FilterHelperTest {

  @Test
  public void testTimeMatch() {
    long interval = FilterHelper.ONE_HZ_INTERVAL/2; // samples twice as often
    double[] data = {-5,-4,-3,-2,-1,0,1,2,3,4};
    
    double[] decm = FilterHelper.decimate(data, interval);
    
    
    assertEquals(decm.length, 5); // did we shrink it down by two?
    // need the filtered data to be increasing like the data
    // trying to equals on doubles is awkward at best, so instead of
    // trying to say that the result is something specific, make sure
    // the resulting data still has the same pattern as the input: increasing
    for(int i=0; i<(decm.length-1); i++){
      assertTrue(decm[i] < decm[i+1]);
    }
    
  }
  
  @Test
  public void testUnchange(){
    long interval = FilterHelper.ONE_HZ_INTERVAL;
    double[] data = {1.0,2.0,3.0,4.0};
    
    double[] decm = FilterHelper.decimate(data, interval);
    
    assertEquals(decm, data);
  }
  
  @Test
  public void testGCD(){
    long val1 = 4000L;
    long val2 = 5000L;
    
    long gcd = FilterHelper.euclidGCD(val1, val2);
    
    assertEquals(gcd,1000);
    
  }

}
