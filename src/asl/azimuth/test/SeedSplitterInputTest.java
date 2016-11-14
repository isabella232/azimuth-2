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
  // TODO:
  // What we WANT this to do is to read in DataSets and ContiguousBlocks
  // from the result of ChannelSelector or other input generator
  // then we test to see if their time scales match by index
  // which is a vital assumption in how FilterBlocks are currently constructed
  // HOWEVER, we can't do that without a decent non-GUI tool for doing this,
  // but right now the main access points to that data are code like
  // the ChannelSelector class, which has it strongly coupled with the interface
  // making writing tests like this difficult -- if not impossible -- without
  // extensive re-writes of the code
  
  String fname = "samples/00_LH1.512.SEED"; // file to test on
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
