package asl.azimuth;

import freq.Cmplx;


/**
 * Used to perform filtering and decimation on high-frequency data
 * Assumes a target frequency of 1Hz, true for azimuth stuff
 * @author amkearns
 * @author fshelly (low pass filter/apply)
 */
public class FilterHelper{

  // one Hz interval expressed in microseconds, often used outside the class
  public static final long ONE_HZ_INTERVAL = 1000000L;

  /**
   * Initial driver for the decimation utility
   * which takes a timeseries of unknown rate and
   * runs downsampling to convert it to a target
   * frequency of a 1Hz interval.
   * @param data The timeseries to be decimated
   * @param src The source frequency as interval between samples (microseconds)
   * @return A timeseries decimated to the correct frequency
   */
  public static double[]  decimate(double[] data, long src){
     
     long tgt = ONE_HZ_INTERVAL; // target frequency
     // a sample lower than 1Hz frq has longer time between samples
     // since it's an inverse relationship and all
     if(src >= tgt){
       return data;
       // TODO: throw exception if data is TOO low frequency?
     }

     // find what the change in size is going to be
     long gcd = euclidGCD(src, tgt);
     // conversion up- and down-factors
     // (upsample by target, downsample by source)
     // cast is valid because any discrete interval
     // from 1Hz and up is already expressable
     // as an int
     int upf = (int)(src/gcd);
     int dnf = (int)(tgt/gcd);

     // one valid sample rate for data is 2.5Hz
     // that is a ratio of 5/2, which won't
     // downsample neatly in some cases
     // so we would first upsample,
     // filter out any noise terms,
     // then downsample
     double[] upped = upsample(data,upf);
     upped = lowPassFilter(upped,src*upf);
     double[] down = downsample(upped,dnf);

     return down;

  }

  /**
   * Does decimation on int arrays by conversion to double
   * and using the same code as above
   * @param data Timeseries (int array)
   * @param src  Source frequency as interval between samples (microseconds)
   * @return Decimated timeseries (int array)
   */
  public static int[] decimate(int[] data, long src){

    double[] result = decimate(Azimuth.intArrayToDoubleArray(data),src);
    return Azimuth.doubleArrayToIntArray(result);

  }

  /**
   * Implements Euclid's algorithm for finding GCD
   * used to find common divisors to give us upsample
   * and downsample rates by dividing the timeseries intervals
   * by this value
   * @param src Initially, one of two frequencies to calculate
   * @param tgt Initially, one of two frequencies to calculate
   * @return The GCD of the two frequencies
   */
  public static long euclidGCD(long src,long tgt){
  
    // take remainders until we hit 0
    // which means the divisor is the gcd
    long rem = src % tgt;
    if(rem == 0){
      return tgt;
    }
    
    return euclidGCD(tgt, rem);
  }

  /**
   * Upsamples data by a multiple of passed factor, placing zeros
   * between each data point. Result is data.length*factor cells in size.
   * Requires use of a low-pass filter to remove discontinuities.
   * @param data The timeseries to be upsampled
   * @param factor The factor to increase the size by
   * @return The upsampled series
   */
  public static double[] upsample(double[] data, int factor){

    double[] upsamp = new double[data.length*factor];
   
    for(int i=0; i<data.length; i++){
      upsamp[i*factor] = data[i];
    }

    return upsamp;
  }

  /**
   * Downsamples data by a multiple of passed factor. Result is
   * data.length/factor cells in size
   * Requires previous use of a low-pass filter to avoid aliasing
   * @param data The timeseries to be downsampled
   * @param factor The factor to decrease the size by
   * @return The downsampled series
   */
  public static double[] downsample(double[] data, int factor){

    double[] downsamp = new double[data.length/factor];
    for(int i=0; i < downsamp.length; i++){
      downsamp[i] = data[i*factor]; 
    }

    return downsamp;
  }

  /**
   * Implements low pass band filter
   * @param timeseries  The data to be filtered
   * @param sps         Samples per second
   * @return            The filtered data
   */
  private static double[] lowPassFilter(double[] timeseries, long sps)
  {
    float[] timeseriesFilter = new float[timeseries.length];
    double[] timeseriesdouble = new double[timeseries.length];
    double fl = 0; // allow all low-frequency data through
    double fh = ONE_HZ_INTERVAL/2.0; // nyquist rate half of target frequency
       // note that this 1/2F where F is 1Hz frequency
    // we want the inverse of the target frequency

    for (int ind = 0; ind < timeseries.length; ind++)
    {
      timeseriesFilter[ind] = (float) timeseries[ind];
    }

    Cmplx[] timeseriesC = Cmplx.fft(timeseriesFilter);

    timeseriesC = apply((double) sps, timeseriesC, fl, fh);

    timeseriesFilter = Cmplx.fftInverse(timeseriesC, timeseries.length);

    for (int ind = 0; ind < timeseries.length; ind++)
    {
      timeseriesdouble[ind] = (double) timeseriesFilter[ind];
    }

    return timeseriesdouble;
  }

  /**
   * Implements bandpass filter for lowpassfilter()
   * @param dt  Time step
   * @param cx  Complex number form of time series
   * @param fl  Low corner frequency
   * @param fh  High corner frequency
   * @return  Complex form of filtered time series
   */
  public static Cmplx[] apply(double dt, Cmplx[] cx, double fl, double fh)
  {

    int npts = cx.length;
    // double fl = 0.01;
    // double fh = 2.0;
    int npole = 2;
    int numPoles = npole;
    int twopass = 2;
    double TWOPI = Math.PI * 2;
    double PI = Math.PI;

    Cmplx c0 = new Cmplx(0., 0.);
    Cmplx c1 = new Cmplx(1., 0.);

    Cmplx[] sph = new Cmplx[numPoles];
    Cmplx[] spl = new Cmplx[numPoles];

    Cmplx cjw, cph, cpl;
    int nop, nepp, np;
    double wch, wcl, ak, ai, ar, w, dw;
    int i, j;

    if (npole % 2 != 0)
    {
      System.out.println("WARNING - Number of poles not a multiple of 2!");
    }

    nop = npole - 2 * (npole / 2);
    nepp = npole / 2;
    wch = TWOPI * fh;
    wcl = TWOPI * fl;

    np = -1;
    if (nop > 0)
    {
      np = np + 1;
      sph[np] = new Cmplx(1., 0.);
    }
    if (nepp > 0)
    {
      for (i = 0; i < nepp; i++)
      {
        ak = 2. * Math
           .sin((2. * (double) i + 1.0) * PI / (2. * (double) npole));
        ar = ak * wch / 2.;
        ai = wch * Math.sqrt(4. - ak * ak) / 2.;
        np = np + 1;
        sph[np] = new Cmplx(-ar, -ai);
        np = np + 1;
        sph[np] = new Cmplx(-ar, ai);
      }
    }
    np = -1;
    if (nop > 0)
    {
      np = np + 1;
      spl[np] = new Cmplx(1., 0.);
    }
    if (nepp > 0)
    {
      for (i = 0; i < nepp; i++)
      {
        ak = 2. * Math
           .sin((2. * (double) i + 1.0) * PI / (2. * (double) npole));
        ar = ak * wcl / 2.;
        ai = wcl * Math.sqrt(4. - ak * ak) / 2.;
        np = np + 1;
        spl[np] = new Cmplx(-ar, -ai);
        np = np + 1;
        spl[np] = new Cmplx(-ar, ai);
      }
    }

    cx[0] = c0;
    dw = TWOPI / ((double) npts * dt);
    w = 0.;
    for (i = 1; i < npts / 2 + 1; i++)
    {
      w = w + dw;
      cjw = new Cmplx(0., -w);
      cph = c1;
      cpl = c1;
      for (j = 0; j < npole; j++)
      {
        cph = Cmplx.div(Cmplx.mul(cph, sph[j]), Cmplx.add(sph[j], cjw));
        cpl = Cmplx.div(Cmplx.mul(cpl, cjw), Cmplx.add(spl[j], cjw));
      }
      cx[i] = Cmplx.mul(cx[i], (Cmplx.mul(cph, cpl)).conjg());
      if (twopass == 2)
      {
        cx[i] = Cmplx.mul(cx[i], Cmplx.mul(cph, cpl));
      }
      cx[npts - i] = (cx[i]).conjg();
    }

    return (cx);

  }

}


