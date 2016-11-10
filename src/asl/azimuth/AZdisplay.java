package asl.azimuth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import asl.seedsplitter.ContiguousBlock; // keep this, it’s simple
import asl.seedsplitter.DataSet; // part of the file import function
import asl.seedsplitter.SequenceRangeException;

// OK. So there's a lot that's changed here since I started
// working on this. The biggest change is NOT, I think that
// file loading now goes one-at-a-time (done to eliminate
// errors with interval mismatch with the previous version
// when trying to mix >1Hz sample rates).
// The biggest change is that nested arrays and lists are
// now almost exclusively MAPS. Instead of magic number
// indices 1-3 and having to rely on booleans or something
// to keep track of which files have been loaded, we can
// use the map's keys to see which files/plots are active
// and it means that keying into the different data structures
// can be kept more consistent and intuitive.



/**
 * Top level class for building the Azimuth GUI
 * 
 * @author  fshelly
 * @author  amkearns
 */

public class AZdisplay
{
    public static final int DEFAULT_WIDTH = 1150;
    public static final int DEFAULT_HEIGHT = 1000;
    public static AZprefs prefs;
    private MainFrame frame;

    /**
     * Constructor that gets called to start the GUI
     */
    public AZdisplay()
    {
        prefs = new AZprefs();
        WindowListener listener = new Terminator();
        frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(listener);
        frame.setVisible(true);
    } // ASPdisplay() constructor

    /**
     * Class called when the GUI exit button is clicked.
     * Allows GUI to save persistent state information for next launch.
     */
    private class Terminator extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            frame.SavePrefs();
        }
    } // class Terminator

} // class AZdisplay

/**
 * Top level frame for seed transfer GUI, everything else fits inside.
 */
class MainFrame extends JFrame implements ActionListener, FocusListener,
      ChangeListener, PropertyChangeListener
{
    private static final long serialVersionUID = 1L;
    private ArrayList<EHChartPanel> plotPanels = null;
    public static final int MINIMUM_WIDTH = 700;
    public static final int MINIMUM_HEIGHT = 1000;

    /**
     * Constructor for MainFrame.  Top level Gui creation routine
     */
    public MainFrame()
    {
        setIconImage(Resources.getAsImageIcon(
                         "resources/icons/chart.png", 128, 128).getImage());

        prefs = new AZprefs();
        setTitle("Azimuth Instrument Determination");
        setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT));
        setPreferredSize(new Dimension(prefs.GetMainWidth(), 
                                       prefs.GetMainHeight()));
        setBounds(prefs.GetMainOriginX(), prefs.GetMainOriginY(), 
                prefs.GetMainWidth(), prefs.GetMainHeight());

        seedFileDir = prefs.GetLocalDir();

        // ======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        // ======== ViewJPanel ========

        // display the three graph windows, associated file loaders,
        // and the additional controls
        
        northSegmentPlot = new SegmentPlotter("North Ref. (H1)",
                "North", mRefNetwork, "LH1", mRefLocation);
        northViewJPanel = northSegmentPlot.createTimePanel();
        northViewJPanel.setMinimumSize(new Dimension(600, 160));
        northViewJPanel.setPreferredSize(new Dimension(prefs.GetMainWidth(),
                    prefs.GetMainHeight()));
        northViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        northViewBufferJPanel = new JPanel();
        northViewBufferJPanel.setLayout(new BorderLayout());
        northViewBufferJPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
        northViewBufferJPanel.add(northViewJPanel, BorderLayout.CENTER);
        
        // add button that will be used to load in north sensor data
        // similar buttons are instantiated for the east and reference data sets

        JPanel northSubPanel = new JPanel(); 
                     // subpanel for north load and cancel buttons 

        // button for selecting a north data set to load
        northButton = new JButton("Load North", 
                   Resources.getAsImageIcon("resources/icons/add.png", 20, 20));
        northButton.addActionListener(this);
        northButton.setMaximumSize(northButton.getPreferredSize());
        northSubPanel.add(northButton, BorderLayout.WEST); 
                    // place below the graph window

        // to clear the north data set
        northCancel = new JButton("Clear", 
                Resources.getAsImageIcon("resources/icons/remove.png", 20, 20));
        northCancel.addActionListener(this);
        northCancel.setMaximumSize(northCancel.getPreferredSize());
        northCancel.setEnabled(false);
        northSubPanel.add(northCancel, BorderLayout.EAST);

        // add the two buttons below the graph
        northViewBufferJPanel.add(northSubPanel, BorderLayout.SOUTH);

        add(northViewBufferJPanel);

        eastSegmentPlot = new SegmentPlotter("East Ref. (H2)",
                "EAST", mRefNetwork, "LH2", mRefLocation);
        eastViewJPanel = eastSegmentPlot.createTimePanel();
        eastViewJPanel.setMinimumSize(new Dimension(600, 160));
        eastViewJPanel.setPreferredSize(new Dimension(prefs.GetMainWidth(),
                    prefs.GetMainHeight()));
        eastViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        eastViewBufferJPanel = new JPanel();
        eastViewBufferJPanel.setLayout(new BorderLayout());
        eastViewBufferJPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
        eastViewBufferJPanel.add(eastViewJPanel, BorderLayout.CENTER);
        
        JPanel eastSubPanel = new JPanel(); 
                     // same as the one for north (see above)
 
        eastButton = new JButton("Load East", 
                   Resources.getAsImageIcon("resources/icons/add.png", 20, 20));
        eastButton.addActionListener(this);
        eastButton.setMaximumSize(eastButton.getPreferredSize());
        eastSubPanel.add(eastButton, BorderLayout.WEST);

        eastCancel = new JButton("Clear", 
                Resources.getAsImageIcon("resources/icons/remove.png", 20, 20));
        eastCancel.addActionListener(this);
        eastCancel.setMaximumSize(eastCancel.getPreferredSize());
        eastCancel.setEnabled(false);
        eastSubPanel.add(eastCancel, BorderLayout.EAST);

        eastViewBufferJPanel.add(eastSubPanel, BorderLayout.SOUTH);

        add(eastViewBufferJPanel);

        referenceSegmentPlot = new SegmentPlotter("Unknown (H1 or H2)",
                "REF", mRefNetwork, "LHZ", mRefLocation);
        referenceViewJPanel = referenceSegmentPlot.createTimePanel();
        referenceViewJPanel.setMinimumSize(new Dimension(600, 160));
        referenceViewJPanel.setPreferredSize(new Dimension(prefs.GetMainWidth(),
                    prefs.GetMainHeight()));
        referenceViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        referenceViewBufferJPanel = new JPanel();
        referenceViewBufferJPanel.setLayout(new BorderLayout());
        referenceViewBufferJPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
        referenceViewBufferJPanel.add(referenceViewJPanel, BorderLayout.CENTER);

        JPanel refSubPanel = new JPanel();

        refButton = new JButton("Load Unknown", 
                   Resources.getAsImageIcon("resources/icons/add.png", 20, 20));
        refButton.addActionListener(this);
        refButton.setMaximumSize(refButton.getPreferredSize());
        refSubPanel.add(refButton, BorderLayout.WEST);

        refCancel = new JButton("Clear", 
                Resources.getAsImageIcon("resources/icons/remove.png", 20, 20));
        refCancel.addActionListener(this);
        refCancel.setMaximumSize(refCancel.getPreferredSize());
        refCancel.setEnabled(false);
        refSubPanel.add(refCancel, BorderLayout.WEST);

        referenceViewBufferJPanel.add(refSubPanel, BorderLayout.SOUTH);

        add(referenceViewBufferJPanel);

        plotPanels = new ArrayList<EHChartPanel>(3);
        plotPanels.add((EHChartPanel)northViewJPanel);
        plotPanels.add((EHChartPanel)eastViewJPanel);
        plotPanels.add((EHChartPanel)referenceViewJPanel);
        ((EHChartPanel)northViewJPanel).setAssociates(plotPanels);
        ((EHChartPanel)eastViewJPanel).setAssociates(plotPanels);
        ((EHChartPanel)referenceViewJPanel).setAssociates(plotPanels);

        JPanel panelSlider = new JPanel();
        panelSlider.setLayout(new BoxLayout(panelSlider, BoxLayout.X_AXIS));
        panelSlider.setBorder(new EmptyBorder(20, 10, 5, 10));
        leftSlider = new JSlider(0, 1000, iLeftSliderValue);
        leftSlider.setInverted(true);
        leftSlider.addChangeListener(this);
        panelSlider.add(leftSlider);
        rightSlider = new JSlider(0, 1000, iRightSliderValue);
        rightSlider.addChangeListener(this);
        panelSlider.add(rightSlider);
        add(panelSlider);

        JPanel panelTime = new JPanel();
        panelTime.setLayout(new BoxLayout(panelTime, BoxLayout.X_AXIS));
        panelTime.setBorder(new EmptyBorder(5, 10, 10, 10));
        zoomInButton = new JButton("Zoom In ", 
               Resources.getAsImageIcon("resources/icons/zoom-in.png", 20, 20));
        zoomInButton.addActionListener(this);
        zoomInButton.setMaximumSize(zoomInButton.getPreferredSize());
        panelTime.add(zoomInButton);
        panelTime.add(Box.createHorizontalGlue());
        startTime = new JLabel();
        startTime.setAlignmentX(LEFT_ALIGNMENT);
        startTime.setText("Segment start time");
        endTime = new JLabel();
        endTime.setAlignmentX(RIGHT_ALIGNMENT);
        endTime.setText("Segment end time");
        panelTime.add(startTime);
        panelTime.add(Box.createHorizontalStrut(20));
        panelTime.add(endTime);
        panelTime.add(Box.createHorizontalGlue());
        zoomOutButton = new JButton("Zoom Out", 
              Resources.getAsImageIcon("resources/icons/zoom-out.png", 20, 20));
        zoomOutButton.addActionListener(this);
        zoomOutButton.setMaximumSize(zoomOutButton.getPreferredSize());
        panelTime.add(zoomOutButton);
        add(panelTime);

        segmentCombo = new JComboBox<TimeRange>();
        segmentCombo.setEditable(false);
        segmentCombo.setBorder(new EmptyBorder(10, 10, 10, 10));
        segmentCombo.addActionListener(this);
        add(segmentCombo);

        JPanel panelProgress = new JPanel();
        panelProgress.setLayout(new BorderLayout());
        panelProgress.setBorder(new EmptyBorder(20, 10, 20, 10));
        inverterProgress = new JProgressBar(SwingConstants.HORIZONTAL);
        panelProgress.add(inverterProgress, BorderLayout.NORTH);
        add(panelProgress);

        JPanel panelReference = new JPanel();
        panelReference.setLayout(new BoxLayout(
                                        panelReference, BoxLayout.X_AXIS));
        panelReference.setBorder(new EmptyBorder(0, 10, 0, 10));
        panelReference.setPreferredSize(
                                    new Dimension(prefs.GetMainWidth(), 50));
        panelReference.setMaximumSize(
                                 new Dimension(prefs.GetMainWidth()+500, 50));    
        refAngleField = new JFormattedTextField();
        refAngleField.setValue("0.0");
        refAngleField.setColumns(8);
        refAngleField.setName("refAngleField");
        JLabel labelSeconds = new JLabel("Reference Angle: ",  JLabel.TRAILING);
        labelSeconds.setLabelFor(refAngleField);
        panelReference.add(labelSeconds);
        panelReference.add(refAngleField);
        refAngleField.addFocusListener(this);
        add(panelReference);

        JPanel panelButton = new JPanel();
        panelButton.setLayout(new BoxLayout(panelButton, BoxLayout.X_AXIS));
        panelButton.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelButton.setPreferredSize(new Dimension(prefs.GetMainWidth(), 60));
        panelButton.setMaximumSize(new Dimension(prefs.GetMainWidth()+500, 60));
        

        generateButton = new JButton("Generate", 
                 Resources.getAsImageIcon("resources/icons/chart.png", 20, 20));
        generateButton.addActionListener(this);
        //generateButton.setEnabled(false);
        panelButton.add(generateButton);
        panelButton.add(Box.createHorizontalStrut(20));
        cancelButton = new JButton("Cancel", 
                Resources.getAsImageIcon("resources/icons/cancel.png", 20, 20));
        cancelButton.addActionListener(this);
        //cancelButton.setEnabled(true);
        panelButton.add(cancelButton);

        panelButton.add(Box.createHorizontalGlue());
        quitButton = new JButton("Quit", 
                  Resources.getAsImageIcon("resources/icons/exit.png", 20, 20));
        quitButton.addActionListener(this);
        panelButton.add(quitButton);
        add(panelButton);

        // Status field
        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setText("Initializing...");
        add(statusField);

        updateGui();

        // This should never be deleted. Rather, the user will can
        // re-select files if they wish, otherwise the previous
        // selection and data is retained.

        // NOTE: moved channelSelector to this package
        // formerly part of Joel Edwareds asl-java-tools repo


       final int CHANNEL_COUNT = 1;
       // instantiate a new channelSelector 
       // (used to read in the SEED files whose data will be plotted)
       channelSelector = new ChannelSelector(this, prefs.GetMainOriginX(), 
                           prefs.GetMainOriginY(), seedFileDir, CHANNEL_COUNT);
       // populate its icons
       channelSelector.setWindowIcon(Resources.getAsImageIcon(
                             "resources/icons/chart.png", 128, 128).getImage());
       channelSelector.setAddFilesButtonIcon(Resources.getAsImageIcon(
                             "resources/icons/add.png", 20, 20));
       channelSelector.setRemoveSelectedButtonIcon(Resources.getAsImageIcon(
                             "resources/icons/remove.png", 20, 20));
       channelSelector.setReadFilesButtonIcon(Resources.getAsImageIcon(
                             "resources/icons/load.png", 20, 20));
       channelSelector.setCancelReadButtonIcon(Resources.getAsImageIcon(
                             "resources/icons/delete.png", 20, 20));
       channelSelector.setOkButtonIcon(Resources.getAsImageIcon(
                             "resources/icons/start.png", 20, 20));
       channelSelector.setCancelButtonIcon(Resources.getAsImageIcon(
                             "resources/icons/stop.png", 20, 20));

       timerGraph = new GraphUpdate();
       t = new Timer(50, timerGraph);
    } // constructor MainFrame()


   /*
    * Used to reset the data when the plots have been cleared
    */
   private void reinitializePersistents(){
      if(!northSet && !eastSet && !refSet){ // safety check
         iMarginSlider=100; // default values
         iLeftSliderValue=1000;
         iRightSliderValue=1000;
         leftSliderDate=null;
         rightSliderDate=null;
         contBlocks.clear();
         // reset the time scales
         selectBlock = new HashMap<Character, FilterBlock>();

         segmentCombo.removeAllItems();
      }
   }


    // TODO: move most of this code into a backend function instead 
    // (i.e., into Azimuth.java?)
    /*
     * Called to replace one of the plots at a time based on passed character,
     * 'n' for the north plot, 'e' for the east plot, 'r' (or any other 
     * character) for the ref plot.
     * See the actionPerformed function for where this is called
     * Loading in one-at-a-time allows us to avoid IntervalMismatch errors 
     * which we want to suppress since we will be decimating any data
     * above 1 Hz anyway!
     */
    private void FileProcess(char plot){
       
       ArrayList<DataSet>  dataSets = null;

       ArrayList<ContiguousBlock> fileBlocks = null;
       instantiateChSel(plot);
       channelSelector.setVisible(true);

       // much of what is still here  is taken straight from the original 
       // all-three-at-once file process method

       // First we have to take care of how that window was entered or exited

       // Handle user hit the Cancel button
       if (channelSelector.GetCancel()){
           statusField.setText("File selection canceled, select again...");
       }
       else{

           seedFileDir = channelSelector.GetDefaultDirectory();        
       }

       if (!channelSelector.GetProcessed()){  // Handle user hit the OK button
          statusField.setText(
             "Failed to process azimuth data files, select again...");
       }
       else if (channelSelector.GetBlocks() == null){
          statusField.setText(
             "Failed to find a large enough contiguous block, select again...");      
       }
       else{
           // since we only plot one set at a time, 
           // dataSets should only have one entry (index 0)
           fileBlocks = channelSelector.GetBlocks();
           dataSets = channelSelector.GetDataSets().get(0);
           // these two lists should be the same size, too
       }

       if(null == fileBlocks || null == dataSets){
          return; // we have no data to process, and so can't do anything
       }

       // let's process the data now that we have it
       // (and then replace any current plot with a new one)
       
       SegmentPlotter segPlot;    // choose which object set we'll be updating
       // identify which channel to update based on the passed parameter
       // and mark it as currently unset (to be filled with new data)
       // TODO: separate it into its own function?
       switch (plot) {
          case 'n':
             northSet = false;
             segPlot = northSegmentPlot;
             break;
          case 'e':
             eastSet = false;
             segPlot = eastSegmentPlot;
             break;
          default:
             refSet = false;
             segPlot = referenceSegmentPlot;
       }

       // reinitialize the time series ONLY if all 3 graphs are cleared
       // to make sure that all 3 time series are aligned
       reinitializePersistents();

       factory = null; // this may also not be necessary 
                       // but resets the azimuth solver
                       // to be reinstantiated when the 
                       // "generate" button is hit again
       result = null;  // will also get reset once the "generate" button is hit 
                       // (solution angle)
       

       long interval = FilterHelper.ONE_HZ_INTERVAL; 
                       // we'll be decimating all data to this rate

       // Only use segments that have enough data
       // Iteration here assumes order in lists of 
       // fileBlocks and corresponding dataSets
       // are coupled, so the contiguous block at 
       // fileBlocks.get(i) matches dataSet at dataSets.get(i)
       // Changing either data structure to something 
       // where the order is not guaranteed (i.e., a set)
       // will break this assumption, so be careful 
       // about doing something like that!
       // Although if you wanted to do that, you could 
       // define a contiguous block's equality function
       // so two blocks are equal if the timeranges match  
       // and then do the foreach over the dataset.
       // Then you get data from a set if the two blocks
       // are equal. But this is easier (if the assumption holds).

       // this used to be a list of ContiguousBlocks called 
       // 'blocks' but why bother creating any such
       // intermediate data structure when we can instantiate 
       // the filter blocks over that time range now
       // and then map that to the corresponding character
       List<FilterBlock> blocks = new ArrayList<FilterBlock>();
       Set<TimeRange> rangesInImport = new HashSet<TimeRange>();

       // this removes any blocks that don't meet our 
       // requirement for how long our plot should be
       for (int i = 0; i < fileBlocks.size(); i++){ 
          // initially was a for-each ContiguousBlock in fileBlocks

          // this seems to be a valid assumption on 
          // how data is returned by channel selector
          // at least when being run a single SEED file at a time
          ContiguousBlock block = fileBlocks.get(i);
          DataSet data = dataSets.get(i);

          // make sure that the data, once decimated, is 
          // still long enough for us to work with
          long dataPoints = (block.getEndTime() - block.getStartTime()) 
                    / interval;

          if (dataPoints < MIN_DATAPOINTS){
             continue;
          }
          try{
             FilterBlock temp = new FilterBlock(block,data);
             // System.out.println(Arrays.toString(temp.getFilterData()));
             blocks.add(temp);
             
             rangesInImport.add(new TimeRange(
                          block.getStartTime(), block.getEndTime()));
          }
          catch(SequenceRangeException e){
             // TODO: if this error shows up, rewrite above indexing to find 
             // the dataSet that matches the contblock
             // (i.e., find the dataSet doesn't throw this, remove from list,
             // and create a filter block from those two elements)
             // because if this DOES happen, then our loop index assumption 
             // is invalid
             statusField.setText("Error in matching imported data time ranges,"
                                      + "  try again...");
             return;
          }
       }

       if (blocks.size() == 0)
       {
          statusField.setText("Failed to find a large enough contiguous block,"
                                     + " select again...");
          return;
       }

       statusField.setText("Found "+rangesInImport.size()+" time block(s).");

       contBlocks.put(plot, blocks); 
               // recall that contBlocks stores ALL the valid data for each set
       rangesPerPlot.put(plot, rangesInImport);       

       setPlotActive(plot);

       int totalDataPoints = 0; // TODO: get rid of this, find counts per plot
                                // when constructing the range plot map

       // TODO: move this to time range plot map construction
       // where totalDataPoints will be more reflective of actual data
       countsPerPlot = totalDataPoints / prefs.GetMainWidth();
       if (countsPerPlot < 1){
           countsPerPlot = 1;
       }

       statusField.setText("Azimuth data files contained " + blocks.size() 
            + " seperate time segments. Largest is (" 
            + startTime.toString()+","+endTime.toString()+")");

    }

    /*
     * Used to update the title of a plot with
     * data taken from a filter block of that plot's data
     * Since the list of blocks is persistent it can be called
     * at any time, though it only needs to be when data is loaded
     */
    private void updatePlotTitle(char plot, SegmentPlotter segPlt){
       // update a plot when we get new data for it 
       FilterBlock active;

       // since we won't call this until plot time
       // selectBlock should already be defined 
       switch (plot) {
          case 'n':
             active = selectBlock.get('n');
             mNorthStation = active.getStation();
             mNorthNetwork = active.getNetwork();
             mNorthChannel = active.getChannel();
             mNorthLocation = active.getLocation();
             segPlt.SetTitle(mNorthStation, mNorthNetwork,
                    mNorthChannel, mNorthLocation);
             break;
          case 'e':
             active = selectBlock.get('e');
             mEastStation = active.getStation();
             mEastNetwork = active.getNetwork();
             mEastChannel = active.getChannel();
             mEastLocation = active.getLocation();
             segPlt.SetTitle(mEastStation, mEastNetwork,
                    mEastChannel, mEastLocation);
             break;
          default:
             active = selectBlock.get('r');
             mRefStation = active.getStation();
             mRefNetwork = active.getNetwork();
             mRefChannel = active.getChannel();
             mRefLocation = active.getLocation();
             segPlt.SetTitle(mRefStation, mRefNetwork,
                    mRefChannel, mRefLocation);
       }
    }

    /*
     * Once valid data is loaded, set a plot as having that data
     */
    private void setPlotActive(char plot){
       switch (plot){
          case 'n':
             northSet = true;
             northCancel.setEnabled(true);
             break;
          case 'e':
             eastSet = true;
             eastCancel.setEnabled(true);
             break;
          default:
             refSet = true;
             refCancel.setEnabled(true);
       }

    }

    /*
     * Collects the cartesian product of each set's valid time ranges
     * and eventually puts them in a single set to be used to populate the graph
     * view windows, finding the intersecting set of each product result
     */
    private void setComboRanges(){

       // the trick is that cartesian product A x B x C = A x (B x C)
       // so, given 3 sets we can calculate the ranges of the first two
       // and then get the product of that with the third
       // this saves us some combinatorial effort since we can merge
       // the first two sets and then do the same thing with the third and that
       // intermediate result; it's effectively the same operation for two sets
       // as for 3

       ranges = new HashSet<TimeRange>();
       // list of each plot's valid timeranges, indexable
       List<Set<TimeRange>> listRanges = new 
                          ArrayList<Set<TimeRange>>(rangesPerPlot.values());

       // take combinations of the first two entries in the list, 
       // merge into a single set,
       // then take the combinations of THAT entry with the next, recursively
       // due to shifting operations on delete, 
       // it's faster to start from the array's end
       while(listRanges.size() > 1){
           Set<TimeRange> mergedRanges = new HashSet<TimeRange>();
           Set<TimeRange> set1 = listRanges.remove(listRanges.size()-1); 
                              // now size >= 1
           Set<TimeRange> set2 = listRanges.remove(listRanges.size()-1); 
                              // cannot be < 0 given while condition
           for(TimeRange t1 : set1){
              for(TimeRange t2 : set2){
                 try{
                    /* used for debugging control flow
                    System.out.println("COMPARING T1: "
                               +t1.getStart()+","+t1.getEnd());
                    System.out.println("COMPARING T2: "
                               +t2.getStart()+","+t2.getEnd());
                    TimeRange result = new TimeRange(t1,t2);
                    System.out.println("RESULT:       "
                               +result.getStart()+","+result.getEnd());
                    */
                    mergedRanges.add(result);
                 }
                 catch(ArithmeticException e){
                     // happens in an empty range, not a worry, just keep going
                     // System.out.println();
                     continue;
                 }
              } // loop on set2
           } // loop on set1
           listRanges.add(mergedRanges);
       }

       // if this is the first plot being loaded, we're good anyway
       ranges.addAll(listRanges.get(0)); 
                                // should be only entry in listRanges now

       System.out.println("How many valid ranges? "+ranges.size());

       // TODO: add exception handling if sets have no time ranges in common?
       // (i.e., result turns out empty)

       // TODO: remove ranges if they lack sufficient data points?

       // ironic to have to build yet another array 
       // to get the longest entry in the set
       // but then we only use the ranges set to make sure 
       // the combo box contains no duplicates
       // as long as nothing is done to change trArr, this will work
       // if you need to make a change, change the ranges set instead
       TimeRange[] trArr = ranges.toArray(new TimeRange[0]);
       Arrays.sort(trArr, Collections.reverseOrder());
                                 // largest range should be FIRST now
       
       buildTimeRangePlotMap(); // sets up plotBlockMap
       // do this BEFORE setting the combo box, 
       // because setting the combo box triggers plotting
       // and plotting requires the plot map to be constructed -- 
       // that's where it gets data from!

       segmentCombo.removeAllItems();  
                          // repopulate selections with new set of ranges!
       for(TimeRange t : trArr){
           segmentCombo.addItem(t);
       }
       

       segmentCombo.setSelectedItem(0); // longest range

       // Now we have our ranges. Now we'll want to get the data over each.


    }

    /*
     * Construct a map that lets us find sub-sequences of the read-in data
     * so that when we switch from one range of time to another we can quickly
     * find the relevant filter block and replot, rather than look back up if
     * the time range of interest changes, which could be slow 
     */
    private void buildTimeRangePlotMap(){
       for(TimeRange t : ranges){
          // map each valid time range to a set of plottable data blocks
          Map<Character,FilterBlock> plottableBlocks = 
                               new HashMap<Character,FilterBlock>();

          for(char c : contBlocks.keySet()){
             // iterating on keys so we can keep maps consistent below:
             List<FilterBlock> lookup = contBlocks.get(c);
             for(FilterBlock block : lookup){
                // try to find the block that includes the range of interest
                // we store this in a map because it's potentially 
                // slow to generate if the data has a lot of distinct 
                // blocks in it
                try{
                  FilterBlock adding = 
                              new FilterBlock(block, t.getStart(),t.getEnd());
                  plottableBlocks.put(c, adding);
                  
                  /* was for debugging control flow
                  System.out.println("Found a plottable data block for "+c);
                  System.out.println("("+t.toString()+")");
                  */
                }
                catch(SequenceRangeException e){
                  /* more debugging statements
                  System.out.println("Not a valid block for "+c);
                  System.out.println("Range is "+t.getStart()+","+t.getEnd());
                  System.out.println("Block is "
                             +block.getStartTime()+","+block.getEndTime());
                  */
                  // this just means we didn't find the block we're looking for
                  // this will happen if we have multiple contiguous blocks
                  // associated with any given data set
                  continue; // so we don't do anything right now
                }
             } // loop over the read-in filter block data
          } // loop over the 3 plots
          // now we have subsequences for all loaded data, 
          // map them to this time series
          plotBlockMap.put(t, plottableBlocks);
       } // loop over time ranges

    }


    /*
     * Called as part of FileProcess, to load the corresponding 
     * seed file when the user clicks on the corresponding button
     * Since we only load one seed file at a time, 
     * we set channelSelector to have a channelCount of 1
     */
    private void instantiateChSel(char plot){

       // don't create a new channel selector here; 
       // that would clear out the input file list
       // as well as the data files we've already read in
       // this makes it easier to load in a single file at a time
       
       // now make it clear which channel we are about to load from here
       switch (plot) {
          case 'n':
             channelSelector.setChannelLabel(0, "1/North Reference: ");
             break;
          case 'e':
             channelSelector.setChannelLabel(0, "2/East Reference: ");
             break;
          default:
             channelSelector.setChannelLabel(0, "H1 or H2 of Unknown Sensor: "); 
       }

    }

    /**
     * Displays the data found under the current active time range.
     * Assumes we have already done time-range gathering
     * (Zoomed-in data taken from a subset of the data plotted here)
     */
    private void displayPlotSegments()
    {

        Map<Character, SegmentPlotter> segPlots = collectSegPlots();

        // oh java, if only the getSelectedItem() method 
        // didn't have return type Object
        // anyway, we want the time range of the active combo box selection
        TimeRange tr = segmentCombo.getItemAt(segmentCombo.getSelectedIndex());
        leftSliderDate = tr.getStartAsDate();
        rightSliderDate = tr.getEndAsDate();
        
        System.out.println("Got a current time range!");
        System.out.println(""+tr.getStart()+","+tr.getEnd());

        // selectBlock will contain the data shown in a plot at any time
        // deep copy so that our timeseries map's blocks won't be
        // overwritten from zooming later
        for(Character c : plotBlockMap.get(tr).keySet()){
           selectBlock.put(c,new FilterBlock(plotBlockMap.get(tr).get(c)));

           /* more debugging here
           if(selectBlock.get(c).getFilterData() == null){
              // System.out.println("Dang, there goes the filtered data...");
           }
           */
        }

        // previously we had to get a valid filter block from the select block
        // set, but now that we use the map structure, we can just iterate on it
        // to get the data that we need to plot 
        // (and we already have the time range)

        statusField.setText("Plotting Seismic channel data...");

        // statusField.setText(FilterHelperTest.test());
        
        for(Character c : selectBlock.keySet()){
           
           /* these are redundant
           // update the title
           segPlots.get(c).setVisible(false);
           segPlots.get(c).resetTimeData();
           */

           updatePlotTitle(c,segPlots.get(c));
        }

        setFinalData(); // make sure the solver has data to work with

        // we'll use displayZoom to actually put the graphics up there
        // technically we're zoomed in, just at a factor of 1:1 ;)
        // TODO: maybe give displayZoom a better name as it is the 
        // primary display driver now
        displayZoom();
        
        // System.out.println("All graphs plotted!");

    } // DisplayPlotSegments()


    /*
     * Sets the data to be passed into the Azimuth solver
     */
    private void setFinalData(){

       for(char c : selectBlock.keySet()){
          int[] data = selectBlock.get(c).getIntData();
          finalData.put(c,Arrays.copyOf(data,data.length));
       }

    }


    /**
     * Provide a function to get all 3 plots in a single collection (map)
     */
    private Map<Character, SegmentPlotter> collectSegPlots(){
       Map<Character,SegmentPlotter> segPlots = 
                             new HashMap<Character,SegmentPlotter>();
       if(northSet){
          segPlots.put('n', northSegmentPlot);
       }
       if(eastSet){
          segPlots.put('e', eastSegmentPlot);
       }
       if(refSet){
          segPlots.put('r', referenceSegmentPlot);
       }
       return segPlots;
    }


    /**
     * This function is used for adjusting the selection bars (the blue sliders)
     * and send that data to the azimuth-solver via the finalData/finalBlock
     * structures
     */
    private void displaySelectSegment()
    {

        
        Map<Character,SegmentPlotter> segPlots = collectSegPlots();

        int iSeg0Size=0;
        int iSeg1Size=0;
        int iSeg2Size=0;

        for (Character c : segPlots.keySet())
        {
            // TODO: may need to change references to viewBlock 
            // to guarantee consistent axes
            // (So far this doesn't seem to be true though)
            int len = selectBlock.get(c).getFilterData().length;
            // I know there's this length parameter in a FilterBlock, 
            // but I don't like touching it since it's public
            iSeg0Size = selectBlock.get(c).length * 
                                   (1000-iLeftSliderValue) / 1000;
            iSeg2Size = selectBlock.get(c).length * 
                                   (1000-iRightSliderValue) / 1000;
            iSeg1Size = selectBlock.get(c).length - (iSeg0Size + iSeg2Size);
            //System.out.println("" + iSeg1Size);

            int segFinal[] = new int[iSeg1Size];
            // deep copy to prevent errant references from biting us
            System.arraycopy(selectBlock.get(c).getIntData(), 
                                       iSeg0Size, segFinal, 0, iSeg1Size);
            finalData.put(c,segFinal);
            finalBlock.put(c, new FilterBlock(selectBlock.get(c), 
                                       iSeg0Size, iSeg1Size));
        } // loop through each plot file  

        for (Character c : segPlots.keySet())
        {
            if(finalData.get(c) == null){ 
               continue;
            }

            int len = finalBlock.get(c).getFilterData().length;
            //System.out.println("" + len);
            segPlots.get(c).setStartEndMarks(
                    leftSliderDate, finalBlock.get(c).getFilterData()[0],
                    rightSliderDate, finalBlock.get(c).getFilterData()[len-1]);
        }

        startTime.setText(leftSliderDate.toString());
        endTime.setText(rightSliderDate.toString());
    } // DisplaySelectSegment()

    /**
     * Draws the plot and does additional bookkeeping
     */
    private void displayZoom()
    {
        
        // need to get the plots
        Map<Character, SegmentPlotter> segPlots = collectSegPlots();
        int len = 1;

        for (Character c : segPlots.keySet())
        {
           System.out.println(c);
           // these values should be consistent for all 3 plots
           len = selectBlock.get(c).getFilterData().length;
           leftSliderDate = new Date(selectBlock.get(c).getStartTime()/1000);
           rightSliderDate = new Date(selectBlock.get(c).getEndTime()/1000);
           
           int zoomPerPlot = len / prefs.GetMainWidth();
           if(zoomPerPlot < 1){
              zoomPerPlot = 1;
           }
           // now do the plot adjustment

            segPlots.get(c).SetVisible(false);
            segPlots.get(c).resetTimeData();
            segPlots.get(c).AddNewData(
                   selectBlock.get(c).getFilterData(), dRate, leftSliderDate, 
                   zoomPerPlot, 0);
            segPlots.get(c).SetVisible(true);
        } // loop through each plot file  

        iMarginSlider = (1000 * MIN_DATAPOINTS + 999) 
            / len;
        if (iMarginSlider > 1000)
            iMarginSlider = 1000;
        iLeftSliderValue = 1000;
        iRightSliderValue = 1000;

        for (Character c : selectBlock.keySet())
        {
            // cloning is probably not necessary here
            segPlots.get(c).setStartEndMarks(
                    (Date)leftSliderDate.clone(), 
                    selectBlock.get(c).getFilterData()[0],
                    (Date)rightSliderDate.clone(), 
                    selectBlock.get(c).getFilterData()[len-1]);
        }

        startTime.setText(leftSliderDate.toString());
        endTime.setText(rightSliderDate.toString());
        leftSlider.setValue(iLeftSliderValue);
        rightSlider.setValue(iRightSliderValue);
    } // DisplayZoom()

    /**
     * Saves persistent state information so that it can be retrieved the
     * next time the program is run.
     */
    public void SavePrefs()
    {
        prefs.SetLocalDir(seedFileDir);

        // Remember any changes to preferences
        prefs.SetMainOriginX(this.getX());
        prefs.SetMainOriginY(this.getY());
        prefs.SetMainHeight(this.getHeight());
        prefs.SetMainWidth(this.getWidth());

        prefs.SavePrefs();
        System.exit(0);
    } // SavePrefs()

    /**
     * Timer routine that allows the cursors to accurately reflect 
     * their current position.  
     * Needed because we have a minimum amount of data that a user can select.  
     * If the user tries to push the cursor beyond that point, 
     * this timer pushes it back.
     */
    class GraphUpdate implements ActionListener
    {
        public void actionPerformed(ActionEvent event)
        {
            if (leftSlider.getValue() != iLeftSliderValue)
            {
                if (!leftSlider.getValueIsAdjusting())
                    leftSlider.setValue(iLeftSliderValue);
            }
            if (rightSlider.getValue() != iRightSliderValue)
            {
                if (!rightSlider.getValueIsAdjusting())
                    rightSlider.setValue(iRightSliderValue);
            }

            t.stop();

            displaySelectSegment();
        } // ActionPerformed

    } // class GraphUpdate

    /**
     * Implements listener for slider events for data selection cursor control
     * @param e	ChangeEvent representing the slider motion detected
     */
    public void stateChanged(ChangeEvent e)
    {

        // don't allow the sliders to change without
        // all of the data being loaded in already
        if(!northSet || !eastSet || !refSet){
           // just reset for now
           iMarginSlider = 100;
           iLeftSliderValue = 1000;
           leftSlider.setValue(iLeftSliderValue);
           iRightSliderValue = 1000;
           rightSlider.setValue(iRightSliderValue);
           return;
        }

        JSlider source = (JSlider)e.getSource();
        int iMargin = iMarginSlider;
       
        for(FilterBlock viewBlock : selectBlock.values()){
           // all active plots SHOULD have the same time range...

           long iDelta = 
                     (viewBlock.getEndTime() - viewBlock.getStartTime()) / 1000;
           if (source == leftSlider)
           {
              iLeftSliderValue = leftSlider.getValue();
              if (iLeftSliderValue < iMargin)
              {
                 iLeftSliderValue = iMargin;
              }
              leftSliderDate = new Date(viewBlock.getStartTime()/1000
                      + (1000-iLeftSliderValue)*(iDelta/1000));
              startTime.setText(leftSliderDate.toString());
              if (iLeftSliderValue + iRightSliderValue < 1000 + iMargin)
              {
                 iRightSliderValue = 1000 + iMargin - iLeftSliderValue;
                 rightSliderDate = new Date(viewBlock.getEndTime()/1000
                       - (1000-iRightSliderValue)*(iDelta/1000));
                 endTime.setText(rightSliderDate.toString());
                 rightSlider.setValue(iRightSliderValue);
              }
              t.start();
           } // leftSlider
           else if (source == rightSlider)
           {
              iRightSliderValue = rightSlider.getValue();
              if (iRightSliderValue < iMargin)
              {
                 iRightSliderValue = iMargin;
              }
              rightSliderDate = new Date(viewBlock.getEndTime()/1000
                      - (1000-iRightSliderValue)*(iDelta/1000));
              endTime.setText(rightSliderDate.toString());
              if (iLeftSliderValue + iRightSliderValue < 1000 + iMargin)
              {
                 iLeftSliderValue = 1000 + iMargin - iRightSliderValue;
                 leftSliderDate = new Date(viewBlock.getStartTime()/1000
                      + (1000-iLeftSliderValue)*(iDelta/1000));
                 startTime.setText(leftSliderDate.toString());
                 leftSlider.setValue(iLeftSliderValue);
              }
              t.start();
           } // rightSlider
           break; // only need one
       }//for character
    } // stateChanged()


    /*
     * Clears out data when a plot has been cleared
     *
     */
    private void removeGraph(Character c){
    
       switch(c){
          case 'n':
            northSet = false;
            northCancel.setEnabled(false);
            updateGui();
            northSegmentPlot.resetTimeData();
            break;
          case 'e':
            eastSet = false;
            eastCancel.setEnabled(false);
            updateGui();
            eastSegmentPlot.resetTimeData();
            break;
          default:
            refSet = false;
            refCancel.setEnabled(false);
            updateGui();
            referenceSegmentPlot.resetTimeData();
       }

       reinitializePersistents();
       rangesPerPlot.remove(c);
       contBlocks.remove(c);
       selectBlock.remove(c);
       setComboRanges();
       buildTimeRangePlotMap();
    }


    /**
     * Implements ActionListener for this class. 
     * Performs all button push actions. 
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == quitButton)
        {
            SavePrefs(); // also exits
        }
        else if (source == zoomInButton)
        {
            statusField.setText("Zooming in on selected data region...");
            
            // finalBlock changes whenever the blue bars are adjusted
            // this just reflects that change in the plot            
            for(Character c : finalBlock.keySet()){
               int len = finalBlock.get(c).getFilterData().length;
               selectBlock.put(c, new FilterBlock(finalBlock.get(c), 0, len));
            }
            
            displayZoom();
            statusField.setText("Done zooming in");
            //zoomOutButton.setEnabled(true);
            zoomed = true;
            updateGui();
        }
        else if (source == zoomOutButton)
        {
            statusField.setText("Zooming out to full data display...");
            // even if this can't be done without all 3 plots set
            // I'd rather maintain this convention
            Map<Character, SegmentPlotter> segPlots = collectSegPlots();
            for (SegmentPlotter plot : segPlots.values()){
                plot.resetTimeData();
            }

            displayPlotSegments(); // go back to the zoomed-out plot

            for (SegmentPlotter plot : segPlots.values()){
                // z: see if redundant
                plot.SetVisible(true);
            }
            statusField.setText("Zoomed out");
            zoomed = false;
            updateGui();
        }
        else if (source == northButton)
        {
           // leave the data there alone if we can help it
           // only reset the plot if we use the cancel button
           // (i.e., if source == northCancel)
           updateGui();
           FileProcess('n');
           setComboRanges();
           updateGui();
           // TODO: since these are the same for 
           // each source, set them up as an 
           // external function?
        }
        else if (source == northCancel)
        {
           removeGraph('n');
        }
        else if (source == eastButton)
        {
           updateGui();
           FileProcess('e');
           setComboRanges();
           updateGui();
        }
        else if (source == eastCancel)
        {
           removeGraph('e');
        }
        else if (source == refButton)
        {
           updateGui();
           FileProcess('r');
           setComboRanges();
           updateGui();
        }
        else if (source == refCancel)
        {
           removeGraph('r');
        }
        else if (source == segmentCombo)
        {
            // if we select a new segment to plot, now we have 
            // a nice function to quickly get that data
            statusField.setText("Resetting time range...");
            // of course the segmentCombo changes when we clear data, too
            if(segmentCombo.getItemCount() > 0 && 
                                  segmentCombo.getSelectedIndex() > -1){
               zoomed = false;
               displayPlotSegments();
               for(SegmentPlotter plot : collectSegPlots().values()){
                  plot.SetVisible(true);
               }
               statusField.setText("Selected range plotted.");
               updateGui();
            }

        } // Segment list item was selected
        else if (source == generateButton)
        {
            if (selectBlock.size() == 3) // do all 3 plots have data?
            {
                // Convert data from int to doubles
                double [] north = Azimuth.intArrayToDoubleArray(
                                                     finalData.get('n'));
                double [] east = Azimuth.intArrayToDoubleArray(
                                                     finalData.get('e'));
                double [] reference = Azimuth.intArrayToDoubleArray(
                                                     finalData.get('r'));

                //generateButton.setEnabled(false);
                //cancelButton.setEnabled(true);
                generating = true;
                updateGui();

                // Now determine the Azimuth offset
                statusField.setText("Generating Azimuth offset angle...");
                factory = new AzimuthLocator(north, east, reference);   
                result = null; // probably not necessary?
                factory.addPropertyChangeListener(this);
                factory.execute();

            } // we have data to use
        } // Generate button pushed
        else if (source == cancelButton)
        {
            if (generating) {
                factory.cancel(true);
                statusField.setText("Canceled Azimuth offset angle generation");
                factory = null;
                //generateButton.setEnabled(true);
                //cancelButton.setEnabled(false);
                generating = false;
                updateGui();
                inverterProgress.setValue(0);
            }
        }
    } // actionPerformed()

    /**
     * Implements Gain part of FocusListener for this class.  
     * Needed to remember a text field before user edits it.
     */
    public void focusGained(FocusEvent e)
    {
        JFormattedTextField field = (JFormattedTextField) e.getComponent();

        // Save the current field value in case the user botches up the edit.
        // This allows us to restore the prior value upon field exit
        saveFocusString = field.getText();  
    }

    /**
     * Implements Lost part of FocusListener for this class.  
     * Verifies the validity of an edited field.
     */
    public void focusLost(FocusEvent e)
    {
        Object source = e.getSource();
        if (source == refAngleField)
        {
            try
            {
                if (Double.parseDouble(refAngleField.getText()) < -360.0)
                {
                    refAngleField.setText("-360.0");
                    statusField.setText(
                          "Reset reference angle to minimum value of -360.0\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                if (Double.parseDouble(refAngleField.getText()) > 360.0)
                {
                    refAngleField.setText(Double.toString(360.0));
                    statusField.setText(
                           "Reset reference angle to maximum value of 360.0\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                double angle = Double.parseDouble(refAngleField.getText());
                refAngleField.setText(Double.toString(angle));
            } 
            catch (NumberFormatException e1){
                statusField.setText(
                        "Invalid reference angle '" + refAngleField.getText()
                        + "' in reference field, restoring former value\n");
                refAngleField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }
        } // if refAngleField


    } // focusLost()

    /**
     * Implements callbacks for SeedSplitter and AzimuthLocator worker events.
     * This is where the popup plots for the final results gets initiated.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        Date firstTheta;
        double interval_sec;
        if ("progress".equals(evt.getPropertyName()))
        {
            inverterProgress.setValue((Integer)evt.getNewValue());

            if (factory.isDone())
            {
                generating = false;
                updateGui();
                try
                {
                    if (factory.getSuccess())
                    {
                        result = factory.get();
                        interval_sec = InvertAzimuth.OVERLAP / dRate;
                        firstTheta = new Date(leftSliderDate.getTime() +
                               (long)(1000*InvertAzimuth.WINDOWLENGTH/2/dRate));
                        double refAngle = 
                                   Double.parseDouble(refAngleField.getText());
                        String status = String.format(
                             "Reference %.3f + angle offset %.3f = %.3f",
                             refAngle, 
                             AzAngleDisplay.Normalize360(result.getBestTheta()),
                             AzAngleDisplay.Normalize360(refAngle + 
                                                     result.getBestTheta()));
                        statusField.setText(status);
                        azAngleDisplay = new AzAngleDisplay(this,
                                mRefStation, mRefNetwork, mRefChannel, 
                                mRefLocation,
                                mNorthStation, mNorthNetwork, mNorthChannel, 
                                mNorthLocation,
                                mEastStation, mEastNetwork, mEastChannel, 
                                mEastLocation,
                                refAngle, result.getBestTheta(), 
                                result.getThetas(), 
                                result.getCorrelations(), 
                                result.getStandardDeviation(),
                                result.getMeanOfBestCorrelations(), 
                                firstTheta, interval_sec);
                        azAngleDisplay.setVisible(true);            
                        //azAngleDisplay.correctSize();
                        //generateButton.setEnabled(true);
                        //cancelButton.setEnabled(false);
                    }
                    else
                    {
                        statusField.setText("Convergence failed!");
                        inverterProgress.setValue(0);
                        //generateButton.setEnabled(true);
                        //cancelButton.setEnabled(false);
                    }
                } 
                catch (InterruptedException e)
                {
                    statusField.setText(
                                    "Interrupt Exception, Convergence failed!");
                    e.printStackTrace();
                    //generateButton.setEnabled(true);
                    //cancelButton.setEnabled(false);
                } catch (ExecutionException e)
                {
                    statusField.setText(
                                    "Execution Exception, Convergence failed!");
                    e.printStackTrace();
                    //generateButton.setEnabled(true);
                    //cancelButton.setEnabled(false);
                }
            } // factory reports it is done
        } // "progress" event

    } // propertyChange()

    /**
     * Makes sure that each button is enabled or greyed out 
     * depending on the current data state.
     */
    private void updateGui() {
        if (generating) {
            generateButton.setEnabled(false);
            cancelButton.setEnabled(true);
            zoomOutButton.setEnabled(false);
            zoomInButton.setEnabled(false);

            northButton.setEnabled(false);
            eastButton.setEnabled(false);
            refButton.setEnabled(false);

            northCancel.setEnabled(false);
            eastCancel.setEnabled(false);
            refCancel.setEnabled(false);
        } else {
            cancelButton.setEnabled(false);

            northButton.setEnabled(true);
            eastButton.setEnabled(true);
            refButton.setEnabled(true);
            if(northSet){
               northCancel.setEnabled(true);
            }
            if(eastSet){
               eastCancel.setEnabled(true);
            }
            if(refSet){
               refCancel.setEnabled(true);
            }
            // can't do generation without 3 sets!
            if (northSet && eastSet && refSet) {
                generateButton.setEnabled(true);
                zoomInButton.setEnabled(true);
                if (zoomed) {
                    zoomOutButton.setEnabled(true);
                } else {
                    zoomOutButton.setEnabled(false);
                }
            } else {
                generateButton.setEnabled(false);
                zoomInButton.setEnabled(false);
                zoomOutButton.setEnabled(false);
            }
        }
    }

    private AZprefs prefs;

    // Persistence variables
    private String              seedFileDir;

    private String              mRefStation = "XXXX";
    private String              mNorthStation = "";
    private String              mEastStation = "";
    private String              mRefNetwork="XX";
    private String              mNorthNetwork = "";
    private String              mEastNetwork = "";
    private String              mRefChannel = "LH?";
    private String              mNorthChannel = "";
    private String              mEastChannel = "";
    private String              mRefLocation = "00";
    private String              mNorthLocation = "";
    private String              mEastLocation = "";

    private JPanel              northViewBufferJPanel;
    private JPanel              northViewJPanel;
    private SegmentPlotter      northSegmentPlot;
    private JPanel              eastViewBufferJPanel;
    private JPanel              eastViewJPanel;
    private SegmentPlotter      eastSegmentPlot;
    private JPanel              referenceViewBufferJPanel;
    private JPanel              referenceViewJPanel;
    private SegmentPlotter      referenceSegmentPlot;
    private JTextField          statusField;

    private GraphUpdate         timerGraph = null;
    private Timer               t;
    private AzimuthLocator      factory = null;
    private AzimuthResult       result = null;

    private JSlider                leftSlider;
    private JSlider                rightSlider;
    private JLabel                 startTime;
    private JLabel                 endTime;
    private JComboBox<TimeRange>   segmentCombo; 
                                   // selection box for plottable data segments
    private int                    iMarginSlider=100;
    private int                    iLeftSliderValue=1000;
    private int                    iRightSliderValue=1000;
    private Date                   leftSliderDate=null;
    private Date                   rightSliderDate=null;
    private JProgressBar           inverterProgress;

    private JFormattedTextField refAngleField;
    private String              saveFocusString;

    private JButton             northButton;
    private JButton             eastButton;
    private JButton             refButton;

    private JButton             northCancel;
    private JButton             eastCancel;
    private JButton             refCancel;

    private JButton             generateButton;
    private JButton             cancelButton;
    private JButton             quitButton;
    private JButton             zoomInButton;
    private JButton             zoomOutButton;

    private ChannelSelector     channelSelector;
    private AzAngleDisplay      azAngleDisplay;

    // these are used to map data to plots
    // TODO: replace bare characters with these
    private final char NORTH = 'n';
    private final char EAST  = 'e';
    private final char REF   = 'r';

    // constant value used to represent data rate of a time series
    // after decimation
    private final double dRate = 1000000.0 / FilterHelper.ONE_HZ_INTERVAL;

    // used to populate combobox with time ranges available to each set
    // (prevents duplicate entries in the combobox)
    private Set<TimeRange> ranges = new HashSet<TimeRange>();

    // used to keep track of time ranges when data is added or cleared
    private Map<Character, Set<TimeRange>> rangesPerPlot = 
                       new HashMap<Character, Set<TimeRange>>();
    
    // The main data structure for our plotting, 
    // maps each graph to a list of c blocks
    // from which we find the subset of timeseries to actually plot on the graph
    private Map<Character, List<FilterBlock>>  contBlocks = 
                                     new HashMap<Character,List<FilterBlock>>();

    // This is a fun one. Used to map the options in
    // the time-range combo box to data sets
    // There's a set of data for each time range, 
    // one filter block per plot (N,E,R);
    // We do this because it is faster to store than try to 
    // find in-range blocks on the fly
    private Map<TimeRange,Map<Character, FilterBlock>> plotBlockMap = 
                        new HashMap<TimeRange,Map<Character, FilterBlock>>();

    // used to store results of the zoom and slider operations for re-plotting
    Map<Character,FilterBlock>  selectBlock = 
                                  new HashMap<Character,FilterBlock>();
    // used to store the data that gets passed right to the solver
    Map<Character,FilterBlock>  finalBlock = 
                                  new HashMap<Character,FilterBlock>();
    // TODO: is this one still used?
    Map<Character,FilterBlock>  zoomBlock = 
                                  new HashMap<Character,FilterBlock>();
    // the timeseries the solver actually used, taken from finalBlock
    Map<Character, int[]>       finalData = new HashMap<Character, int[]>();

    // basically, the resolution of the data series' plots
    int countsPerPlot;

    private boolean generating = false;
    
    // used to check if each of the three graphs has been set yet
    private boolean northSet   = false;
    private boolean eastSet    = false;
    private boolean refSet     = false;

    private boolean zoomed     = false;

    public static final int MIN_DATAPOINTS = 
                          InvertAzimuth.WINDOWLENGTH + InvertAzimuth.OVERLAP*4;
} // class MainFrame
