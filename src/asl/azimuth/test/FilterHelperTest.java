package asl.azimuth.test;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import asl.azimuth.FilterHelper;

public class FilterHelperTest {

  @Test
  public void testTimeMatch() {
    long interval = FilterHelper.ONE_HZ_INTERVAL/2; // samples twice as often
    double[] data = {1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0};
    double[] decm = FilterHelper.decimate(data, interval);
    assertEquals(decm.length, 5);
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
    
    assertEquals(FilterHelper.euclidGCD(val1,val2),1000);
    
  }

}
