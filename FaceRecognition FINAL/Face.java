/* 
 * Program Face.java
 * Developed as a part of the project under Dr. Rob Byrd
 * Course CS476G
 * 
 * Code for the playing streamed or stored MPEG video, extract image frames at 
 * an intended frame rate, detect human face regions in the video, and extract 
 * partial features from the identified faces.
 * 
 * Developed by :
 * 
 * Chaitanya Raj Malla
 * Ranjani Balaji
 * Selvakumar Saravanavelu 
 * Bhavani Prasad Jasti
 * (Team 1)
 */

import faceRecognizerPackage.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.*;
import java.io.*;
import javax.imageio.*;
import java.util.*;

/*
 * This a program to access the individual video frames of a video
 * track by streaming the video data through a "pass-through" codec.
 * As the data passes through this codec, a callback is invoked for each
 * frame of video data.
 * 
 * When the frame buffer data for a frame is captured by the specialized 
 * face detector codec, the rest of the operations to detect and recognize 
 * faces are performed.
 */

public class Face extends FrameAccess implements ControllerListener 
{

	// Object that does the overall processing of the video.
	// It contains the designed codec.
	Processor processor;
	
	// Variables dealing with the frame rate.
	double actualFrameRate, requestedFrameRate; 
	double frameRateFactor, frameSaveDecisionFactor ;
	int frameNumber;
	
	// Output image directory.
	File outputImageDirectory;
	
	/*
	 * Objects to synchronize the state transitions of the processor.
	 * This synchronization is necessary if multiple thread instances
	 * are created for this (FrameAccess) class. This might be necessary 
	 * during the future stages of development.
	 * As of now, there is a single thread flow through the program and 
	 * hence synchronization is achieved without using any additional code.
	 */
	boolean stateTransitionAllowable = true;
	Object waitForSynchronization = new Object();

	// Output image directory.
	void setOutputImageDirectory(File outputDirectory)
	{
		this.outputImageDirectory = outputDirectory;
	}
	
	/*
	 * Given a media locator, create a processor and use that processor as 
	 * a player to playback the media. During the processor's Configured 
	 * state, a "pass-thru" codec, called SpecializedFrameAccessCodec is 
	 * set on the video track. This codec is used to get access to the
	 * individual video frames of the media.
	 */

	// Function to Open the Media File at the given URL.
	public boolean open(MediaLocator mediaLocator) 
	{
		setTitle("Video");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e) 
			{
				System.exit(0);
			}
		}
		);
	
		try
		{
			processor = Manager.createProcessor(mediaLocator);
		}
		catch (Exception e)
		{
			System.err.println("Failed to create a processor from the" 
			        +"given url "+ e);
			return false;
		}

		// Add a listener to the processor to capture events.
		processor.addControllerListener(this);

		// Put the Processor in configured state.
		processor.configure();
		if (!waitForState(Processor.Configured))
		{
			System.err.println("Failed to configure the processor...");
			return false;
		}

		// Set the output content type to null as the output is 
		// not being used further.
		processor.setContentDescriptor(null);

		// Obtain the track controls available for the processor.
		TrackControl trackControl[] = processor.getTrackControls();

		if (trackControl == null)
		{
			System.err.println("Failed to obtain track controls for" 
			        +" the processor...");
			return false;
		}

		// Search for the first available video format track control.
		TrackControl videoTrack = null;
		
		for (int i = 0; i < trackControl.length; i++)
		{
			if (trackControl[i].getFormat() instanceof VideoFormat) 
			{
				videoTrack = trackControl[i];
				actualFrameRate = ((VideoFormat)
				        (videoTrack.getFormat())).getFrameRate();
				frameRateFactor = requestedFrameRate / actualFrameRate;
			
				// Inform user about the high frame rate factor.
				if(frameRateFactor > 1.0)
				{
				System.out.println(" The requested frame rate" +
				        " processing cannot be done...");
				frameRateFactor = 1.0;
				frameSaveDecisionFactor  = 0.0;
				}
				break;
			}
		}

		if (videoTrack == null) 
		{
			System.err.println("The input media does not contain" 
			        +" a video track...");
			return false;
		}

		System.err.println("Video format 			: " 
		        + videoTrack.getFormat());
		System.err.println("Actual VideoFrame Rate 	: " 
		        + actualFrameRate);
		System.err.println("Processing Frame Rate	: " 
		        + actualFrameRate*frameRateFactor);

		/*
		 * Instantiate and set the frame access codec to the data flow path.
		 * The codec could actually be an array of codecs each performing 
		 * one operation on an image frame before passing it on to the 
		 * next codec in the array. That is the reason why we declare the 
		 * codec as an array
		 */
		try 
		{
			Codec codec[] = { new FaceDetectorCodec
			        (frameRateFactor, outputImageDirectory)};
			videoTrack.setCodecChain(codec);
		}
		// If there wasn't a plugin that supported 
		// the format of the video track.
		
		catch (UnsupportedPlugInException e) 
		{
			System.err.println("Suitable plu-in not available...");
		}
		
		// Get the processor into prefetched state after realizing.
		processor.prefetch();
		if (!waitForState(Processor.Prefetched))
		{
			System.err.println("Failed to realize the processor...");
			return false;
		}

		/* 
		 * Display the visual & control component in the video track 
		 * (if there's one).
		 * Set the Frame layout of processor.
		 */
		setLayout(new BorderLayout());
		Component controlComponent;
		Component visualComponent;

		if ((visualComponent = processor.getVisualComponent()) 
		        != null) 
		{
			add("Center", visualComponent);
		}

		if ((controlComponent = processor.getControlPanelComponent())
		        != null) 
		{
			add("South", controlComponent);
		}

		// Start the processor.
		processor.start();
		
		// Make visible the frame components of the processor 
		// (if necessary).
		setVisible(true);
		
		return true;
	}

	// Function to Display the frame on the visual output device.
	public void addNotify() 
	{
		super.addNotify();
		// Adjust the frame borders to fit the components exactly.
		pack();
	}

	/*
	 * Block until the flow of execution during state transitions 
	 * of the Processor. Return false if the transition failed.
	 */

	boolean waitForState(int state)
	{
		synchronized (waitForSynchronization)
		{
			try 
			{
				while (processor.getState() != state && stateTransitionAllowable)
				waitForSynchronization.wait();
			} 
			catch (Exception e) 
			{
			}
		}
		return stateTransitionAllowable;
	}

	// Controller Listener – Capture events and act accordingly.
	public void controllerUpdate(ControllerEvent eventOccured) 
	{
		// Act for state completed events.
		if (	eventOccured instanceof ConfigureCompleteEvent 	||
		eventOccured instanceof RealizeCompleteEvent 	||
		eventOccured instanceof PrefetchCompleteEvent) 
		{
			synchronized (waitForSynchronization) 
			{
				stateTransitionAllowable = true;
				waitForSynchronization.notifyAll();
			}
		} 
		// Act for error event.
		else if (eventOccured instanceof ResourceUnavailableEvent) 
		{
			synchronized (waitForSynchronization) 
			{
				stateTransitionAllowable = false;
				waitForSynchronization.notifyAll();
			}
		} 
		// Act for end of media event.
		else if (eventOccured instanceof EndOfMediaEvent) 
		{
			processor.close();
			System.exit(0);
		}
	}

	// Main program
	public static void main(String [] args)
	{

		// Check for correct arguments
		if (args.length < 3)
		{
			UsageErrorNotify();
			System.exit(0);
		}

		String url = args[0];

		if (url.indexOf(":") < 0)
		{
			UsageErrorNotify();
			System.exit(0);
		}

		MediaLocator mediaLocator;

		if ((mediaLocator = new MediaLocator(url)) == null)
		{
			System.err.println("Cannot build media locator from: " + url);
			System.exit(0);
		}

		File outputDirectory = new File(args[2]);
/*		File edgeOutputDirectory = new File(args[2].concat("\\edge\\").toString());
*/		// Try creating the required output directory
		try
		{
			boolean directoryCreated;
//			directoryCreated2;
			directoryCreated = outputDirectory.mkdir();
//			directoryCreated2 = edgeOutputDirectory.mkdir();
//			if(!(directoryCreated1||directoryCreated2))
			if(!directoryCreated)
			{
				System.err.println("Could not create the output directory...");
				System.exit(0);
			}
		}
		catch(Exception e)
		{
			System.err.println(e);
			System.exit(0);
		}

		Face face = new Face();

		try
		{
			face.requestedFrameRate = Float.parseFloat(args[1]);
		}
		catch(NumberFormatException e)
		{
			UsageErrorNotify();
			System.exit(0);
		}

	face.setOutputImageDirectory(outputDirectory);
   	if (!face.open(mediaLocator))
			System.exit(0);
   	
   }

	static void UsageErrorNotify()
	{
		System.err.println("Usage: java Face <url> <frame_rate> <output_directory>");
	}

	// Defining the basic structure of the pass through codec 
	// used in the processor.
	public class BasicCodec implements Codec
	{
		// Actual input and output formats of the codec.
		Format inputFormat = null, outputFormat = null;

		// The basic codec function that could be used to do the
		// required processing.
		void accessFrame(Buffer frame, File outputImageDirectory){}

		// Allowed input formats for a codec.
		protected Format supportedInputFormats[] = new Format []
		{new VideoFormat(null)};

		// Allowed output formats for a codec.
		protected Format supportedOutputFormats[] = new Format []
		{new VideoFormat(null)};

		// Functions of the Interface Codec that have to be overridden.

		// Functions that are not called in the program.
		public String getName()
		{
			return "Basic Codec";
		}
		public void open() {}
		public void close() {}
		public void reset() {}
		public Object[] getControls()
		{
			return new Object[0];
		}
		public Object getControl(String type)
		{
			return null;
		}

		// Functions that are called in the program.
		public Format [] getSupportedInputFormats()
		{
			return supportedInputFormats;
		}

		public Format [] getSupportedOutputFormats(Format requiredFormat)
		{
			if (requiredFormat == null)
				return supportedOutputFormats;
			else
			{
				// If a required format is given, we use that format as the
				// output since we are not modifying the bit stream at all.
				Format output[] = new Format[1];
				output[0] = requiredFormat;
				return output;
			}
		}

		public Format setInputFormat(Format format)
		{
			inputFormat = format;
			return inputFormat;
		}

		public Format setOutputFormat(Format format)
		{
			outputFormat = format;
			return outputFormat;
		}

		public int process(Buffer inputBuffer, Buffer outputBuffer)
		{
			frameSaveDecisionFactor  += frameRateFactor;
			if(frameSaveDecisionFactor >= 1)
			{
				// This is the "Callback" to access individual frames.
				accessFrame(inputBuffer, outputImageDirectory);
				frameSaveDecisionFactor = 0.0;
			}

			// Copy the input data to the output buffer.
			Object data = inputBuffer.getData();
			inputBuffer.setData(outputBuffer.getData());
			outputBuffer.setData(data);

			// Copy the input attributes to the output
			outputBuffer.setFormat(inputBuffer.getFormat());
			outputBuffer.setLength(inputBuffer.getLength());
			outputBuffer.setOffset(inputBuffer.getOffset());

			return BUFFER_PROCESSED_OK;
		}
	}

	public class FaceDetectorCodec extends BasicCodec
	{
		// image parameter declarations
		int imageWidth;
		int imageHeight;
		int dataLength;
		BufferedImage outputImage,duplicateImage;
		int [][] duplicateImageData;
		byte[] byteData;

		// input format set to match BufferedImage format
		RGBFormat supportedRGBFormat = new RGBFormat(null,     // size
						Format.NOT_SPECIFIED,  // maxDataLength
						Format.byteArray,      // dataType
            		Format.NOT_SPECIFIED,  // frameRate
						24,                    // bitsPerPixel
						3, 2, 1,               // red/green/blue masks
						3,                     // pixelStride
						Format.NOT_SPECIFIED,  // lineStride
						Format.FALSE,          // flipped
						Format.NOT_SPECIFIED); // endian

		public Format setInputFormat(Format format)
		{
			if ((format != null) && (format instanceof RGBFormat) &&
			format.matches(supportedRGBFormat))
			{
			   Dimension size = ((RGBFormat)format).getSize();
			   imageWidth = size.width;
			   imageHeight = size.height;
			   dataLength = imageWidth * imageHeight * 3;
			   if ((dataLength > 0) &&
			   (( outputImage == null) 
				|| (outputImage.getWidth() != imageWidth) 
				|| (outputImage.getHeight() != imageHeight)))
			   {
			      // output buffered image is of the same 3-byte format with the essential image parameters.
			      outputImage = new BufferedImage(imageWidth, imageHeight , BufferedImage.TYPE_3BYTE_BGR);

			      // array of pixel byte data.
			      byteData = ((DataBufferByte) outputImage.getRaster().getDataBuffer()).getData();
			   }
			}
			return super.setInputFormat(format);
		 }
		
		// Construct edge detector codec as supporting RGB format types.
		public FaceDetectorCodec(double frameRateFactor, 
		        				File outputImageDirectory)
		{
			supportedInputFormats = new Format [] {supportedRGBFormat};
		}

		// Determine if a skin pixel given the Cb,Cr values.
		public boolean isSkinPixel(double Cb, double Cr)
		{
		    // Model the skin region into a union of 4 cirlces 
		    // with the centers at the following points in the 
		    // CbCr space
		    
		    DPoint[] skinRegionMeans = 
    					{
			            new DPoint(111.26815416191094 , 146.46413477543354),
			            new DPoint(115.15183281870878 , 141.19566386460122),
			            new DPoint(118.20208046937142 , 137.00662177387366),
			            new DPoint(120.75227272408203 , 133.63685072747424),
			            };
		    
		    DPoint thisPixel = new DPoint(Cb,Cr);
		    
		    // Check if the CbCr value is within the 
		    // defined skin region in the CbCr space
		    for(int i = 0; i < skinRegionMeans.length; i++)
		    {
		        if(thisPixel.distance(skinRegionMeans[i]) < 4)
		            return(true);
		    }
		    return(false);
		}
		
		// The specialized codec function that detects faces from frames 
		// and writes normalized face images data to the image files.
		
		void accessFrame(Buffer frameBuffer, File outputImageDirectory)
		{
			if((outputImage != null ) 
			  &&(frameBuffer.getData().getClass() 
			  == byte[].class))
			{
				byte [] frameByteData = (byte[])frameBuffer.getData();
				Graphics2D graphics = outputImage.createGraphics();
				System.arraycopy(frameByteData, 0, 
				        		byteData,0,
				        		Math.min(dataLength,frameByteData.length));
				duplicateImage = new BufferedImage
								(imageWidth, 
				        		imageHeight, 
				        		BufferedImage.TYPE_3BYTE_BGR);
				int black = 0;
				int white = 0xffffffff;
				ArrayList pointList = new ArrayList();

				// For every pixel in the image frame buffer

				for(int i = 0; i < imageWidth; i++)
					for(int j = 0; j < imageHeight ; j++)
					{
						int Alpha,R,G,B,GRAY,rgbValue;
						double Cb, Cr;
						double I, Q;
						int pixelCount = 0;

						// 1. Get the RGB color values.
						rgbValue = outputImage.getRGB(i,j);
				
						Alpha	= 	(rgbValue >>> 24) & 0xFF;
						R 		= 	(rgbValue >>> 16) & 0xFF;
						G 		= 	(rgbValue >>> 8) & 0xFF;
						B 		= 	(rgbValue >>> 0) & 0xFF;
                        
						// 2. Compute the equivalent GRAY level.
						GRAY	=	(R+G+B)/3;
						
						// 3. Threshold the gray level.
						if(GRAY < 111)
						{					
							// 5. Compute CbCr values 
							Cb = (-0.168736f)*(float)R 
									+ (-0.331264f)*(float)G 
									+ (0.5f)*(float)B 
									+ 128.0f;
							Cr = (0.5f)*(float)R 
									+ (-0.418688f)*(float)G 
									+ (-0.081312f)*(float)B 
									+ 128.0f;

							// Check if the those lie in the defined 
							// skin region andd add to list.
							if(isSkinPixel(Cb,Cr)) 
								pointList.add(new DPoint(i,j));
						}
						duplicateImage.setRGB(i,j,white);
					}
				
				// Create an array of points with the detected
				// skin pixel coordinates.
				DPoint[] point = new DPoint[pointList.size()];
				for(int m = 0; m < pointList.size(); m++)
				{
				    point[m] = (DPoint)(pointList.get(m));
				}

				KMeanCluster kmc;
				ArrayList[] skinClusters = null;
				
				// If atleast 50 skin pixels are detected...
				if(point.length > 50)
				{
				    // Create 3 clusters of the detected skin patches
				    kmc = new KMeanCluster(point,3,false);
				    skinClusters = kmc.getClusters();
				    
				    // Refine the clusters -
				    // 1. Remove outliers
				    // 2. Compute mean
				    // 3. Compute medoid
				    // 4. Extent, density and strength
					ArrayList refinedSkinCluster = new ArrayList();

					// For each of the 3 skin clusters
					for(int k = 0; k < 3; k++)
					{
					    DPoint[] points = new DPoint[skinClusters[k].size()];
					    for(int l = 0; l < skinClusters[k].size(); l++)
					    {
					        points[l] = new DPoint
					        			((DPoint)skinClusters[k].get(l));
					    }
					    
					    // 1. Cluster further into 9 clusters
					    kmc = new KMeanCluster(points,9,false);
					    ArrayList[] pl = kmc.getClusters();
					    
					    // 2. For each of the obtained 9 clusters
					    for(int l = 0; l < pl.length; l++)
					    {
					        // 2.1. Compute the density (share) of the cluster
					        double density = 
					            	(double)pl[l].size()/points.length;
					        
					        // 2.2. Check if the cluster has a minimum share
					        //      atleast one 20th of the bigger cluster.
					        //      If not clear this cluster.
					        if(density < 0.05 || pl[l].size() < 27)
					        {
					            pl[l] = null;
					        }
					    }
					    
					    ArrayList skinCluster = new ArrayList();
					    
					    // 3. Recombine the clusters that passed the above test
					    for(int l = 0; l < pl.length; l++)
					    {
					        if(pl[l] != null)
					        {
					            for(int m = 0; m < pl[l].size(); m++)
						            skinCluster.add((DPoint)pl[l].get(m));
					        }
					    }
					    
					    // 4. Refine and add to the main cluster if dense enough
					    if(skinCluster.size() > 27)
					    refinedSkinCluster.add
					    				(new RefinedCluster(skinCluster));
					}
					
					// Create an image with the pixels identified 
					// in the clusters plotted. 
					for(int i = 0; i < refinedSkinCluster.size(); i++)
					{
					    RefinedCluster rc = (RefinedCluster)
					    					refinedSkinCluster.get(i);
					    for(int j = 0; j < rc.cluster.size(); j++)
					    {
					        DPoint p = (DPoint)(rc.cluster.get(j));
					        int xcord = (int)p.x;
					        int ycord = (int)p.y;
					        
					        duplicateImage.setRGB(xcord,ycord,black);
					    }
					}

					// If all the 3 clusters are present after the 
					// above operations...
					if(refinedSkinCluster.size() == 3)
					{
						RefinedCluster rc1 = 
						    (RefinedCluster)refinedSkinCluster.get(0);
						RefinedCluster rc2 = 
						    (RefinedCluster)refinedSkinCluster.get(1);
						RefinedCluster rc3 = 
						    (RefinedCluster)refinedSkinCluster.get(2);
										
						// If two of the clusters are close enough...
						if(((DPoint)rc1.clusterMean).distance
						        ((DPoint)rc2.clusterMean) < 90)
						{
						    // Combine the clusters into one single cluster.
						    for(int l = 0; l < rc2.cluster.size(); l++)
						        rc1.cluster.add((DPoint)rc2.cluster.get(l));
						    rc1.getExtent();
						    refinedSkinCluster.remove(1);
						    // If this newly formed cluster is close enough with
						    // the third cluster... combine the two again.
							if(((DPoint)rc1.clusterMean).distance
							        ((DPoint)rc3.clusterMean) < 90)
							{
							    for(int l = 0; l < rc3.cluster.size(); l++)
							        rc1.cluster.add
							        	((DPoint)rc3.cluster.get(l));
							    rc1.getExtent();
							    refinedSkinCluster.remove(1);
							}
						}
						else
						// Else If the other two the clusters are close enough
						if(((DPoint)rc2.clusterMean).distance
						        ((DPoint)rc3.clusterMean) < 90)
						{
						    // Combine the clusters into one single cluster.
						    for(int l = 0; l < rc3.cluster.size(); l++)
						        rc2.cluster.add((DPoint)rc3.cluster.get(l));
						    rc2.getExtent();
						    refinedSkinCluster.remove(2);
						}
					}
					else
					// If only two clusters are left after the test
					if(refinedSkinCluster.size() == 2)
					{				
						RefinedCluster rc1 = 
						    (RefinedCluster)refinedSkinCluster.get(0);
						RefinedCluster rc2 = 
						    (RefinedCluster)refinedSkinCluster.get(1);
						
						// If two of the clusters are close enough...						
						if(((DPoint)rc1.clusterMean).distance
						        ((DPoint)rc2.clusterMean) < 90)
						{
						    // Combine the clusters into one single cluster.						    
						    for(int l = 0; l < rc2.cluster.size(); l++)
						        rc1.cluster.add((DPoint)rc2.cluster.get(l));
						    rc1.getExtent();
						    refinedSkinCluster.remove(1);
						}
					}
					
					// After combining the close clusters...
					// For each of the remaining clusters in the cluster list
					for(int i = 0; i < refinedSkinCluster.size(); i++)
					{
					    RefinedCluster rc = 
					        (RefinedCluster)refinedSkinCluster.get(i);
					    BufferedImage faceImage = new BufferedImage
			    						(imageWidth, 
			    						 imageHeight, 
			    						 BufferedImage.TYPE_3BYTE_BGR);
					    	    
//					    int totalGray = 0;
			    
						// 1. Obtain the normalized image for that cluster
					    for(int w = 0; w < imageWidth; w++)
					        for(int h = 0; h < imageHeight; h++)
						    {
						        int x = Math.min(rc.minx+10
			                		+w * (rc.maxx - rc.minx - 5)/imageWidth,
			                		imageWidth-1); 
						        int y = rc.miny + 
					        		h * (rc.maxy - rc.miny)/imageHeight;
						        
						        int rgbValue = outputImage.getRGB(x,y);
/*						        int Alpha	= 	(rgbValue >>> 24) & 0xFF;
								int R 		= 	(rgbValue >>> 16) & 0xFF;
								int G 		= 	(rgbValue >>> 8) & 0xFF;
								int B 		= 	(rgbValue >>> 0) & 0xFF;
		                        
								double Cb = (-0.168736f)*(float)R 
								+ (-0.331264f)*(float)G 
								+ (0.5f)*(float)B 
								+ 128.0f;
								double Cr = (0.5f)*(float)R 
								+ (-0.418688f)*(float)G 
								+ (-0.081312f)*(float)B 
								+ 128.0f;

								int GRAY	=	(R+G+B)/3;
								totalGray += GRAY;
*/								
							    faceImage.setRGB(w,h,rgbValue);
						    }
					    
//					    totalGray = totalGray/(imageWidth*imageHeight);
					    
/*					    ArrayList facePointList = new ArrayList();
					    for(int w = 0; w < imageWidth; w++)
					        for(int h = 0; h < imageHeight; h++)
						    {
						        int rgbValue = faceImage.getRGB(w,h);
						        int Alpha	= 	(rgbValue >>> 24) & 0xFF;
								int R 		= 	(rgbValue >>> 16) & 0xFF;
								int G 		= 	(rgbValue >>> 8) & 0xFF;
								int B 		= 	(rgbValue >>> 0) & 0xFF;
		                        
								int GRAY	=	(R+G+B)/3;
								
								if(GRAY < (double)totalGray/2)
								    faceImage.setRGB(w,h,black);
								else
								    faceImage.setRGB(w,h,white);
						    }
*/						
						// 2. Save the normalized image for that cluster
					    //	  as a JPEG image
					    try
						{
							long timeStamp = frameBuffer.getTimeStamp();
							File outputImageFile = new File
												(outputImageDirectory, 
												"Face_"+(frameNumber++)+"@"+ 
												timeStamp + ".jpg");
							ImageIO.write(faceImage,"jpg",outputImageFile);
						}

						catch(IOException e)
						{
							System.out.println(e);
						}					
						
						// Draw the bounding box around each of the 
						// identified clusters
						graphics.draw3DRect	(Math.min
						        (rc.minx+10,imageWidth-1),rc.miny,
						        Math.min((rc.maxx - rc.minx - 5),imageWidth),
						        (rc.maxx - rc.minx),false);
						
						// Write the modified image data back to 
						// the output buffer
						System.arraycopy(byteData, 0, 
						        		frameByteData,0,
						        		Math.min(dataLength,
						        		        frameByteData.length));
					}
				}				
			}
		}
	}
}