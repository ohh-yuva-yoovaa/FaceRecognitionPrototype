package faceRecognizerPackage;

// Code modified by Ranjani Balaji, Selvakumar Saravanavelu, Chaitanya Raj Malla, Bhavani Kumar Jasti
// Code from Java Forum downloaded on Sept 8, 2004
/*
* @(#)FrameAccess.java 1.5 01/03/13
*
* Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
*
* Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
* modify and redistribute this software in source and binary code form,
* provided that i) this copyright notice and license appear on all copies of
* the software; and ii) Licensee does not utilize the software in a manner
* which is disparaging to Sun.
*
* This software is provided "AS IS," without a warranty of any kind. ALL
* EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
* IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
* NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
* LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
* OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
* LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
* INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
* CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
* OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGES.
*
* This software is not designed or intended for use in on-line control of
* aircraft, air traffic, aircraft navigation or aircraft communications; or in
* the design, construction, operation or maintenance of any nuclear
* facility. Licensee represents and warrants that it will not use or
* redistribute the Software for such purposes.
*/

import java.awt.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.Color.*;
import java.awt.image.*;
import java.util.*;
import java.awt.image.*;
import javax.media.util.*;

/**
* Sample program to access individual video frames by using a
* "pass-thru" codec. The codec is inserted into the data flow
* path. As data pass through this codec, a callback is invoked
* for each frame of video data.
*/
public class FrameAccess extends Frame implements ControllerListener 
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
			Codec codec[] = { new SpecializedFrameAccessCodec(frameRateFactor, outputImageDirectory)};
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
		if (eventOccured instanceof ConfigureCompleteEvent 	||
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
			boolean directoryCreated1,directoryCreated2;
			directoryCreated1 = outputDirectory.mkdir();
			//			directoryCreated2 = edgeOutputDirectory.mkdir();
			//			if(!(directoryCreated1||directoryCreated2))
			if(!directoryCreated1)
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

		FrameAccess frameAccess = new FrameAccess();

		try
		{
			frameAccess.requestedFrameRate = Float.parseFloat(args[1]);
		}
		catch(NumberFormatException e)
		{
			UsageErrorNotify();
			System.exit(0);
		}

		frameAccess.setOutputImageDirectory(outputDirectory);
		if (!frameAccess.open(mediaLocator))
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
	public class SpecializedFrameAccessCodec extends BasicCodec
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
		
		// Construct specialized codec as supporting RGB format types.
		public SpecializedFrameAccessCodec(double frameRateFactor, File outputImageDirectory)
		{
			supportedInputFormats = new Format [] {supportedRGBFormat};
		}

		// The specialized code function that writes image data to the image files.
		void accessFrame(Buffer frameBuffer, File outputImageDirectory)
		{
			if((outputImage != null ) && (frameBuffer.getData().getClass() == byte[].class))
			{

				byte [] frameByteData = (byte[])frameBuffer.getData();
				System.arraycopy(frameByteData, 0, byteData,0,Math.min(dataLength,frameByteData.length));

				// If gray scale output images are required
/*
				for(int i = 0; i < imageHeight; i++)
					for(int j = 0; j < imageWidth; j++)
					{
						int Alpha,R,G,B,rgbValue;
						double Cb, Cr;
						double I, Q;
						rgbValue = outputImage.getRGB(j,i);
				
						Alpha	= 	(rgbValue >>> 24) & 0xFF;
						R 		= 	(rgbValue >>> 16) & 0xFF;
						G 		= 	(rgbValue >>> 8) & 0xFF;
						B 		= 	(rgbValue >>> 0) & 0xFF;
                        
						R		=	(R+G+B)/3;
						rgbValue=	0; 
						rgbValue=	(rgbValue)* 256 + Alpha; 
						rgbValue=	(rgbValue)* 256 + R; 
						rgbValue=	(rgbValue)* 256 + R; 
						rgbValue=	(rgbValue)* 256 + R; 

						outputImage.setRGB(j,i,rgbValue);
					}
*/
				try
				{
					long timeStamp = frameBuffer.getTimeStamp();
					//	Finally the JPEG image is written to the directory specified by the user.
					File outputImageFile = new File(outputImageDirectory, "Frame_"+(frameNumber++)+"@"+ timeStamp + ".jpg");
					ImageIO.write(outputImage,"jpg",outputImageFile);
				}

				catch(IOException e)
				{
					System.out.println(e);
				}
			}
		}
	}

}
