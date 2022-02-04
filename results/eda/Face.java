// Code developed by Ranjani Balaji, Selvakumar Saravanavelu, Chaitanya Raj Malla, Bhavani Prasad Jasti

import faceRecognizerPackage.*;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.*;
import java.io.*;

import javax.imageio.*;
///import javax.imageio.stream.*;
//import java.util.*;

//import javax.imageio.stream.*;

//import java.util.*;
//import javax.media.util.*;

/*
This a program to access the individual video frames of a video
track by streaming the video data through a "pass-through" codec.
As the data passes through this codec, a callback is invoked for each
frame of video data.
*/

public class Face extends FrameAccess implements ControllerListener 
{

	// Object that does the overall processing of the video.
	// It contains the designed codec.
	Processor processor;
	
	// Variables dealing with the frame rate.
	double actualFrameRate, requestedFrameRate, frameRateFactor, frameSaveDecisionFactor ;
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
			System.err.println("Failed to create a processor from the given url "+ e);
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

		// Set the output content type to null as the output is not being used further.
		processor.setContentDescriptor(null);

		// Obtain the track controls available for the processor.
		TrackControl trackControl[] = processor.getTrackControls();

		if (trackControl == null)
		{
			System.err.println("Failed to obtain track controls for the processor...");
			return false;
		}

		// Search for the first available video format track control.
		TrackControl videoTrack = null;
		
		for (int i = 0; i < trackControl.length; i++)
		{
			if (trackControl[i].getFormat() instanceof VideoFormat) 
			{
				videoTrack = trackControl[i];
				actualFrameRate = ((VideoFormat)(videoTrack.getFormat())).getFrameRate();
				frameRateFactor = requestedFrameRate / actualFrameRate;
			
				// Inform user about the high frame rate factor.
				if(frameRateFactor > 1.0)
				{
				System.out.println(" The requested frame rate processing cannot be done...");
				frameRateFactor = 1.0;
				frameSaveDecisionFactor  = 0.0;
				}
				break;
			}
		}

		if (videoTrack == null) 
		{
			System.err.println("The input media does not contain a video track...");
			return false;
		}

		System.err.println("Video format 			: " + videoTrack.getFormat());
		System.err.println("Actual VideoFrame Rate 		: " + actualFrameRate);
		System.err.println("Processing Frame Rate		: " + actualFrameRate*frameRateFactor);

		/*
		 * Instantiate and set the frame access codec to the data flow path.
		 * The codec could actually be an array of codecs each performing one 
		 * operation on an image frame before passing it on to the next codec
		 * in the array. That is the reason why we declare the codec as an array
		 */
		try 
		{
			Codec codec[] = { new EdgeDetectorCodec(frameRateFactor, outputImageDirectory)};
			videoTrack.setCodecChain(codec);
		}
		// If there wasn't a plugin that supported the format of the video track.
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
		 * Display the visual & control component in the video track (if there's one).
		 * Set the Frame layout of processor.
		 */
		setLayout(new BorderLayout());
		Component controlComponent;
		Component visualComponent;

		if ((visualComponent = processor.getVisualComponent()) != null) 
		{
			add("Center", visualComponent);
		}

		if ((controlComponent = processor.getControlPanelComponent()) != null) 
		{
			add("South", controlComponent);
		}

		// Start the processor.
		processor.start();
		
		// Make visible the frame components of the processor (if necessary).
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
		File edgeOutputDirectory = new File(args[2].concat("\\edge\\").toString());
		File skinOutputDirectory = new File(args[2].concat("\\skin\\").toString());
		File smallOutputDirectory = new File(args[2].concat("\\small\\").toString());
		// Try creating the required output directory
		try
		{
			boolean directoryCreated1,directoryCreated2,directoryCreated3,directoryCreated4;
			directoryCreated1 = outputDirectory.mkdir();
			directoryCreated2 = edgeOutputDirectory.mkdir();
			directoryCreated3 = skinOutputDirectory.mkdir();
			directoryCreated4 = smallOutputDirectory.mkdir();
			if((!directoryCreated1)||(!directoryCreated2)||(!directoryCreated3)||(!directoryCreated4))
			//if(!directoryCreated1)
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
		System.err.println("Usage: java FrameAccess <url> <frame_rate> <output_directory>");
	}

	// Defining the basic structure of the pass through codec used in the processor.
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

	public class EdgeDetectorCodec extends BasicCodec
	{
		// image parameter declarations
		int imageWidth;
		int imageHeight;
		int dataLength;
		BufferedImage outputImage;
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
			   (( outputImage == null) || (outputImage.getWidth() != imageWidth) || (outputImage.getHeight() != imageHeight)))
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
		public EdgeDetectorCodec(double frameRateFactor, File outputImageDirectory)
		{
			supportedInputFormats = new Format [] {supportedRGBFormat};
		}

		// The specialized code function that detects edges from frames and writes edge detected data to the image files.
		void accessFrame(Buffer frameBuffer, File outputImageDirectory)
		{
			int[] oneDPix,moneDPix,picDPix,smlPix;
			if((outputImage != null ) && (frameBuffer.getData().getClass() == byte[].class))
			{
				byte [] frameByteData = (byte[])frameBuffer.getData();
				System.arraycopy(frameByteData, 0, byteData,0,Math.min(dataLength,frameByteData.length));


				int		gradWindRad		=	2;
				int		minLength		=	5;
				float	nmxRank			=	0.5f;
				float	nmxConf			=	0.5f;
				int		nmxType			=	BgEdgeDetect.FC_ELLIPSE; // Arc
				float	hystHighRank	=	0.93f;
				float	hystHighConf	=	0.96f;
				int		hystHighType	=	BgEdgeDetect.FC_SQUARE_BOX; // Box
				float	hystLowRank		=	0.99f;
				float	hystLowConf		=	0.91f;
				int		hystLowType		=	BgEdgeDetect.FC_ELLIPSE; // Arc

				ComponentColorModel colorModel = null;
				ComponentSampleModel componentSampleModel = null;
				int width  = outputImage.getWidth();
				int height = outputImage.getHeight();

				colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
							 new int[] {1}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
				componentSampleModel =	new ComponentSampleModel(DataBuffer.TYPE_BYTE,
										width, height, 1, width, new int[] {0});
				int numComponents = outputImage.getColorModel().getNumComponents();
				BufferedImage edgeOutputImage=outputImage;
				if(numComponents > 1)
				{
					// Values from Building Imaging Applications with Java Technology, L. Rodrigues, p.379
					// Values from Bogdan Georgescu's BgImage C++ code and prompt/globalFnc.cpp
				    float[][] grayBandCombine = {{0.299f, 0.587f, 0.114f}};
				    BandCombineOp bandCombineOp = new BandCombineOp(grayBandCombine, null);
				    Raster source = edgeOutputImage.getRaster();

				    DataBuffer dataBuf = new DataBufferByte(width * height);
				    WritableRaster writableRaster = Raster.createWritableRaster(componentSampleModel, dataBuf, new Point(0, 0));
				    try
					{
					bandCombineOp.filter(source, writableRaster);
					edgeOutputImage = new BufferedImage(colorModel, writableRaster, false, null);
				    }
				    catch(IllegalArgumentException e)
					{
					System.err.println("EdgeDetection:" + e);
					System.exit(2);
				    }
				}

				DataBufferByte dbb = (DataBufferByte)(edgeOutputImage.getRaster().getDataBuffer());
				byte[] data = dbb.getData();

				BgImage bgImage = new BgImage();
				bgImage.SetImage(data, width, height, false);
				BgEdgeDetect bgEdgeDetect = new BgEdgeDetect(gradWindRad);
				BgEdgeList edgeList = new BgEdgeList();
				bgEdgeDetect.DoEdgeDetect(bgImage, edgeList,
							  nmxRank, nmxConf, hystHighRank, hystHighConf,
							  hystLowRank, hystLowConf, minLength,
							  nmxType, hystHighType, hystLowType);
				
				// Load edge pixels into BgImage data as black lines on white background
				bgImage.fillImage(255);
				edgeList.SetBinImage(bgImage, (char)0);

				// Extract BgImage data and build greyscale BufferedImage
				byte[] byteData = new byte[width * height];
				int[] edgeData = new int[width * height];
				bgImage.GetImage(byteData);
				dbb = new DataBufferByte(byteData, width);
				WritableRaster writableRaster = Raster.createWritableRaster(componentSampleModel, dbb, new Point(0, 0));
				BufferedImage writableRasterImage = new BufferedImage(colorModel, writableRaster, true, null);

				Image image = Toolkit.getDefaultToolkit().createImage(outputImage.getSource());
				Graphics2D graphics = outputImage.createGraphics();
				graphics.drawImage(image,0,0,imageWidth,imageHeight,null);
				oneDPix = new int[imageWidth * imageHeight];
				moneDPix = new int[imageWidth * imageHeight];
				picDPix = new int[imageWidth * imageHeight];
				
				PixelGrabber pgObj = new PixelGrabber(
				                  image,0,0,imageWidth,imageHeight,oneDPix,0,imageWidth);
				try {
			        pgObj.grabPixels();
			      }
			      catch (InterruptedException e) {
			        e.printStackTrace();
			      }

			      UtilityClass util = new UtilityClass();
			      util.imageHeight = imageHeight;
			      util.imageWidth  = imageWidth;
				  moneDPix = util.skinPixelInfo(oneDPix);

				  util.findTopPixel();
				  util.findBottomPixel();
				  util.findLeftPixel();
				  util.findRightPixel();
				  System.out.println("Top    :"+util.topRow+","+util.topCol);
				  System.out.println("Bottom :"+util.botRow+","+util.botCol);
				  System.out.println("Left   :"+util.leftRow+","+util.leftCol);
				  System.out.println("Right  :"+util.rightRow+","+util.rightCol);
				  
				  int xcor1 = 0;
				  int ycor1 = 0;
				  int xcor2 = 0;
				  int ycor2 = util.rightCol-util.leftCol;
				  int xcor3 = util.botRow-util.topRow;
				  int ycor3 = 0;
				  int xcor4 = util.botRow-util.topRow;
				  int ycor4 = util.rightCol-util.leftCol;
				  //float wscale = imageWidth / ycor4;  
				  //float hscale = imageHeight / xcor4;
				Image modImg = createImage( new MemoryImageSource( imageWidth,imageHeight,moneDPix,0,imageWidth)); 
				BufferedImage bufferImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR); 
				graphics = bufferImage.createGraphics(); 
				graphics.drawImage(modImg,0,0,imageWidth,imageHeight,null); 
				
				//Smaller boundary region 
				int w = util.rightCol - util.leftCol;
				int h = util.botRow - util.topRow;
				System.err.println("width:"+w);
				System.err.println("height:"+h);
				smlPix = new int[imageWidth * imageHeight];
				smlPix = oneDPix;
				Image smlImg = createImage( new MemoryImageSource( imageWidth,imageHeight,smlPix,0,imageWidth));
				BufferedImage smlBufferImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR); 
				graphics = smlBufferImage.createGraphics(); 
				graphics.drawImage(smlImg,0,0,imageWidth,imageHeight,null);
				if(!((w<=0)||(h<=0)))
				{
					smlPix = new int[w * h];
					int p1 = 0;
					for(int i = util.topRow; i < util.botRow; i++)
					{	for(int j = util.leftCol; j <  util.rightCol; j++)	
						{
							int p = i * w + j;
							p1 = (i-util.topRow) * w + (j - util.leftCol);
							smlPix[p1] = oneDPix[p];
							oneDPix[p] = 0;
						}
					}
/*					smlImg = createImage( new MemoryImageSource( w,h,smlPix,0,w));
					smlBufferImage = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR); 
					graphics = smlBufferImage.createGraphics(); 
					graphics.drawImage(smlImg,0,0,w,h,null);
*/					
					smlImg = createImage( new MemoryImageSource( imageWidth,imageHeight,oneDPix,0,imageWidth));
					smlBufferImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR); 
					graphics = smlBufferImage.createGraphics(); 
					graphics.drawImage(smlImg,0,0,imageWidth,imageHeight,null);
				}
				
				
				
				try
				{
					long timeStamp = frameBuffer.getTimeStamp();
					//	Finally the JPEG image is written to the directory specified by the user.
					File outputImageFile = new File(outputImageDirectory, "Frame_"+(frameNumber)+"@"+ timeStamp + ".jpg");
					ImageIO.write(outputImage,"jpg",outputImageFile);
					File edgeOutputImageFile = new File(outputImageDirectory.getPath().concat("\\edge").toString(), "Frame_"+(frameNumber)+"@"+ timeStamp + ".jpg");
					ImageIO.write(writableRasterImage,"jpg",edgeOutputImageFile);
					File skinOutputImageFile = new File(outputImageDirectory.getPath().concat("\\skin").toString(), "Frame_"+(frameNumber)+"@"+ timeStamp + ".jpg");
					ImageIO.write(bufferImage,"jpg",skinOutputImageFile);
					File smallOutputImageFile = new File(outputImageDirectory.getPath().concat("\\small").toString(), "Frame_"+(frameNumber++)+"@"+ timeStamp + ".jpg");
					ImageIO.write(smlBufferImage,"jpg",smallOutputImageFile);
				}

				catch(IOException e)
				{
					System.out.println(e);
				}
			}
		}
	}
}