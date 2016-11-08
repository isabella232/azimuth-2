package asl.azimuth;

import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SequenceRangeException;
import freq.Cmplx;
import java.util.Arrays;

/**
 * Extends ContiguousBlock data class to include both the raw and filtered data for the block. Filtering is done via FilterHelper's methods.
 * 
 * @author fshelly
 */
public class FilterBlock extends ContiguousBlock
{
    public  int length = 0;
    private int []  m_intData;
    private int []  m_filterData;

    private String network = "";
    private String station = "";
    private String location = "";
    private String channel = "";


    /*
     * Construct a filter block from a ContiguousBlock (time interval data) and DataSet (meta data, time series)
     * @param cBlock A ContiguousBlock as taken from ChannelSelector
     * @param dSet A DataSet corresponding to the ContiguousBlock
     */
    public FilterBlock(ContiguousBlock cBlock, DataSet dSet) throws SequenceRangeException{
        // TODO: add exception in case someone decides to pass non-matching parameters?
        // contiguousBlock has the time/interval data we need, dataSet the data sequence and plot names
        super(cBlock.getStartTime(), cBlock.getEndTime(),
              FilterHelper.ONE_HZ_INTERVAL);

        network = dSet.getNetwork();
        station = dSet.getStation();
        location = dSet.getLocation();
        channel = dSet.getChannel();

        long itv = cBlock.getInterval();
        // data SHOULD already have consistent time ranges, but just in case
        // this is why we throw that exception
        try{
           int[] subset = dSet.getSeries(cBlock.getStartTime(),
                             cBlock.getEndTime());
           int[] temp = FilterHelper.decimate(subset, itv);

           if(temp == null) System.out.println("Debugging some weird null stuff...");
          
           // this prevents m_intData from becoming null when we exit the constructor
           // presumably this is because the filter helper methods are all static?
           m_intData = new int[temp.length];
           for(int i = 0; i < temp.length; i++){
              m_intData[i] = temp[i];
           }

           length = m_intData.length;
           double[] filterIn = Azimuth.intArrayToDoubleArray(m_intData);

           // now band-pass the data we're going to filter
           filterIn = lowPassFilter(filterIn, (int)(1000000 / itv));
           // convert it back to int why not
           
           // doing a deep copy to prevent this becoming null
           m_filterData = new int[filterIn.length];
           for(int i=0; i<filterIn.length; i++){
              m_filterData[i] = (int)filterIn[i];
           }
        } 
        catch(SequenceRangeException e){
           throw e;
        }


    }

/* THIS CONSTRUCTOR HAS BEEN COMMENTED OUT AS IT IS NO LONGER NECESSARY
    // Hopefully we can eventually get rid of this one
    public FilterBlock(long startTime, long endTime, long interval, int [] data){

        // we will filter the data down to this interval once we're done
        super(startTime, endTime, FilterHelper.ONE_HZ_INTERVAL);
        //m_intData = data;

        // Decimate source; apply low-pass filter
        int[] m_intData = FilterHelper.decimate(data, interval);

        // Create array of double for the (to be band-passed) data
        length = m_intData.length;
        double[] filterIn = Azimuth.intArrayToDoubleArray(m_intData);

        // doing that in case m_intData's length must be equal to the filter data's, now band-pass it
        filterIn = lowPassFilter(filterIn, (int)(1000000 / interval));

        m_filterData = Azimuth.doubleArrayToIntArray(filterIn);

    } // constructor

*/

    /**
     * Constructor which makes a new FilterBlock out of a subset of another one
     * @param superset  The FilterBlock that we want to create a subset of
     * @param iStart    Start index of where we want to grab data
     * @param size      The number of data points that we want to grab
     */
    public FilterBlock(FilterBlock superset, int iStart, int size){
        super(superset.getStartTime()+iStart*superset.getInterval(),
                superset.getStartTime()+(iStart+size)*superset.getInterval(), 
                superset.getInterval());
        length = size;

        location = superset.getLocation();
        channel = superset.getChannel();
        network = superset.getNetwork();
        station = superset.getStation();

        // Copy subset of integer data
        m_filterData = Arrays.copyOfRange(superset.getFilterData(), iStart, iStart+size);
        m_intData = Arrays.copyOfRange(superset.getIntData(), iStart, iStart+size);
        //System.arraycopy(superset.getIntData(), iStart, m_intData, 0, size);
        //System.arraycopy(superset.getFilterData(), iStart, m_filterData, 0, size);
        
    } // constructor for subset of another FilterBlock

    /*
     * Constructor to create a deep copy of a FilterBlock
     * @param superset The FilterBlock we want to create an exact copy of
     */
    public FilterBlock(FilterBlock superset){
       // calls the constructor above this one
       this(superset,0,superset.getIntData().length);

    }

    /**
     * Constructor which makes a new FilterBlock out of a subset of another one
     * using time intervals (rather than indices of specific data points)
     * @param superset  The FilterBlock that we want to create a subset of
     * @param startTime    Time offset to begin reading data from
     * @param endTime      Time offset to end reading data from
     * @throws SequenceRangeException if the times are outside the data's boundaries
     */
    // TODO: throw sequence range exception if the times are bad?
    public FilterBlock(FilterBlock superset, long startTime, long endTime) 
                                             throws SequenceRangeException{

       super(startTime,endTime, 
                superset.getInterval());

       location = superset.getLocation();
       channel = superset.getChannel();
       network = superset.getNetwork();
       station = superset.getStation();

       long ivl = superset.getInterval();
       long sTime = superset.getStartTime();
       long eTime = superset.getEndTime();

       if(startTime < sTime || eTime < endTime){
          // does the block start after our offset
          // or end before it?
          throw new SequenceRangeException();
       }

       int count = (int) ((endTime - startTime) / ivl);
       int index = (int) (((startTime - sTime) + (ivl / 2)) / ivl); // is this right?
       // Copy subset of integer data
       m_filterData = Arrays.copyOfRange(superset.getFilterData(), index, count);
       m_intData = Arrays.copyOfRange(superset.getIntData(), index, count);
       length = m_intData.length;
    }

    /**
     * Get the unfiltered values for the time series
     * @return array of unfiltered time series values
     */
    public int [] getIntData()
    {
        if(m_intData == null){
	   System.out.println("Well, it's null now!!");
        }
        return m_intData;
    }

    /**
     * Get the filtered values for the time series
     * @return array of filtered time series values
     */
    public int [] getFilterData()
    {
        return m_filterData;
    }

    /**
     * Get the network name for this block
     * @return Network name
     */
    public String getNetwork(){
       return network;
    }

    /**
     * Get the station name for this block
     * @return Station name
     */
    public String getStation(){
       return station;
    }

    /**
     * Get the location name for this block
     * @return Location name
     */
    public String getLocation(){
       return location;
    }


    /**
     * Get the channel name for this block
     * @return Channel name
     */
    public String getChannel(){
       return channel;
    }

    /**
     * Implements low pass band filter
     * @param timeseries  The data to be filtered
     * @param sps         Samples per second  
     * @return            The filtered data
     */
    private double[] lowPassFilter(double[] timeseries, int sps)
    {
        float[] timeseriesFilter = new float[timeseries.length];
        double[] timeseriesdouble = new double[timeseries.length];
        double fl = 1 / 500.0;
        double fh = 1 / 100.0;

        for (int ind = 0; ind < timeseries.length; ind++)
        {
            timeseriesFilter[ind] = (float) timeseries[ind];
        }

        Cmplx[] timeseriesC = Cmplx.fft(timeseriesFilter);

        timeseriesC = FilterHelper.apply((double) sps, timeseriesC, fl, fh);

        timeseriesFilter = Cmplx.fftInverse(timeseriesC, timeseries.length);

        for (int ind = 0; ind < timeseries.length; ind++)
        {
            timeseriesdouble[ind] = (double) timeseriesFilter[ind];
        }

        return timeseriesdouble;
    }

    /**
     * Reports whether this Sequence contains data within the specified time
     * range.
     * 
     * @param startTime
     *            Start time for the test range.
     * @param endTime
     *            End time for the test range.
     * @return A boolean value: true if this time range is available, otherwise
     *         false.
     */
    public boolean containsRange(long startTime, long endTime) {
      boolean result = false;
      if ((startTime >= this.getStartTime()) && (endTime <= this.getEndTime())) {
        result = true;
      }
        return result;
    }

} // class FilterBlock
