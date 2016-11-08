azimuth
=======

ASL Azimuth


WHAT IT DOES
------

This code implements the strategy for determining seismic data sensor azimuths 
based on the strategy in the paper "Relative azimuth inversion by way of damped
maximum correlation estimates" by Ringer, Edwards, Hutt, Shelly (2011, Elsevier 
Computers & Geosciences).

Specifically, this program takes in data samples from a seismometer with known 
reference orientation, using its measurements along East and North. These are
compared with a sample from the detector whose angle is to be determined, and
a Levenberg-Marquardt algorithm is used to estimate the correction angle over
a long-period seismic event.

This readme will eventually be augmented to include more specific information about the functionality of the program.

HOW IT WORKS (INSTRUCTIONS FOR USE)
------

This program takes in SEED files as input. For documentation on the SEED format, see https://ds.iris.edu/ds/nodes/dmc/data/formats/#seed.

The program begins by taking in 3 files, loaded in one at a time. From top to bottom of the user interface, these are the following: a sample from a detector with a known orientation taken in the north-south direction, a sample from the same detector taken in the east-west direction, and a sample from a detector whose orientation is to be determined.

To load data in a channel, press the load button under a plot window. Another window will appear. The first time this window appears, press the "Add Files" button to load in files; multiple files can be selected at once. Once files have been loaded, select "read files" to prepare data for the channels. This only needs to be done once. Once this is done, select the data to be sent to the program by the drop-down menu near the bottom of the window and click "OK".

By default the program is set to load in data at the 1Hz interval ("LH*" channels, meaning 1 sample/s from high-gain seismometers). Higher-frequency data can be used by clearing the "channel" filter in the file selection window. These samples will be downsampled to 1Hz when loaded into the program. It is preferable to use 1Hz data whenever available, as the downsampling leads to small errors in the angle result.

While the program requires data to have overlapping time ranges in order to match data for the azimuth calculation, it is not necessary for each file to be taken over exactly the same time range. If files do not match their time scale exactly, the program will generate all ranges of time common to all loaded data sets. The program will display the largest contiguous segment of time, but other shared ranges of time will be listed in the selection block below the data, if they exist.

Once data has been loaded for all 3 plots, the sliders below the plots will set an active range for calculation, marked by vertical blue bars. (Note that by default the calculation uses the entire plot.) When a range has been selected, pressing the "Zoom In" button will cause the plots to display only the data from that segment. If data has been zoomed in, pressing the "Zoom Out" button will reset the graph and the corresponding selection window.

If the "Zoom Out" button was pressed on accident, the previous zoom window can be restored by pressing the "Zoom In" button again without adjusting the sliders.

It is also possible to examine a range of data more closely with clicking and dragging the cursor. Clicking and dragging to the right on a plot will zoom in over the selected range, and clicking and dragging to the left will zoom back out. This does not change the actively selected range.

Below the plots and data selection / zoom controls is a field for placing a reference angle. Set this with the degree value of the known detector's offset from north.

Once data has been loaded and selected, pressing the Generate button near the very bottom of the window will begin calculation of the angle offset. The resulting angle (plus the given reference offset) and correlation values from the calculation will be displayed. The results of this window can be saved as an image. When finished with the result, click the button labeled "Done" to close the window.

KNOWN ISSUES
------

The program's command-line interface is currently unfinished. Running the code thus requires using the GUI. Display code and backend logic are in the process of being split apart but there is still work to be done before the command line is ready.

On the testing data provided, the rebuilt program has a difference of approximately 0.3 degrees on the correction angle compared to the previous version found on the USGS repository.

Similarly, using high-frequency data introduces small errors to the correction angle. This was determined by using sample ANMO data where the unknown sensor data was the high frequency samples (IU 00/BH1) taken from a detector over the same time range as its low frequency north and east data (IU 00/LH1 and LH2).

Fetching timescales is currently slow. It is done effectively "from scratch" every time a graph is added or removed, rather than having intermediate results stored as graphs are loaded in or removed which would speed up calculations.

During development, it was possible that after clearing a graph and loading a new one that the new graph would fail to plot, and the program would become stuck. While this has presumably already been fixed, in the event that this happens, the program can be reset by clearing all three plots and reloading data.

Attempting to build this code produces several warnings due to lack of parameterization of typing for interface components in the ChannelSelector class, which was imported from a separate repository and has not been modified. (Specifically, components are defined like `JComboBox` instead of `JComboBox<File>`).

Selecting the currently-displayed time series with the drop-down menu resets the plot, just like hitting the zoom out button. It may be preferable to keep track of the currently-selected graph and only update the plot when the selection has changed.

Some debugging text used for checking control flow is still output to terminal when the jar is launched from command line (i.e., through "java -jar Azimuth.jar").

One feature request was for the correction angle as shown in the azimuth calculation popup window to be changed to display the angle of the sensor from north, which has not currently been implemented.

If samples from time ranges that do not overlap are loaded, the program may not produce proper error handling to prevent the loading of that data. This has not been fully tested.

While all plots zoom in when dragging from the right, only the plot where clicking and dragging from the left was done will be zoomed out.