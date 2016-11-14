package asl.azimuth.test;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.File;
import java.util.List;

import org.junit.Test;

import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitter;

public class SeedSplitterInputTest {

  // This module is used to make sure file time ranges match
  // since this is an assumption relied upon by our code
  
  String fname = "00_LH1.512.SEED"; // file to test on
  File[] file;
  List<DataSet> dataSets;
  List<ContiguousBlock> contigBlocks;
  
  public void setUp(){
    file = new File[] {new File(fname)};
    SeedSplitter sp = new SeedSplitter(file);
    sp.doInBackground();
    
  }
  // we want to read in a SINGLE file and be sure its time ranges
  // match up between the dataset and contiguousblocks
  // so iterate, and check that they are the same (use TimeRange equals?)
  @Test
  public void test() {
    setUp();
    fail("Not yet implemented");
  }

}
