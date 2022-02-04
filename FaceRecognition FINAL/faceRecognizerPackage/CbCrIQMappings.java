package faceRecognizerPackage;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

// Class to plot the CbCr values for all the pixels in an image
public class CbCrIQMappings extends Frame
{
	Image image;
	int[] x = new int[999999];
	int[] y = new int[999999];
	int index;

	// Constructor.
	// name - path of the image file
	// mode - map CbCr or IQ ?
	public CbCrIQMappings(String name, int mode)
	{
		// Open the image
	    index=0;
		image = Toolkit.getDefaultToolkit().getImage(name);
		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);

		try
		{
			mediaTracker.waitForID(0);
		}

		catch (InterruptedException ie)
		{
			System.err.println(ie);
			System.exit(1);
		}

		// Obtain width and height of the image
		int w  = image.getWidth(null);
		int h  = image.getHeight(null);
		
		BufferedImage bufferedImage = new BufferedImage
						(w, h, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D graphics = bufferedImage.createGraphics();
		graphics.drawImage(image,0,0,w,h,null);

		// For every pixel
		for(int i = 0; i < h; i++)
			for(int j = 0; j < w; j++)
			{
				int Alpha,R,G,B,rgbValue;
				double Cb, Cr;
				double I, Q;
				rgbValue = bufferedImage.getRGB(j,i);
				
				// 1. Compute RGB values
				Alpha	= 	(rgbValue >>> 24) & 0xFF;
				R 		= 	(rgbValue >>> 16) & 0xFF;
				G 		= 	(rgbValue >>> 8) & 0xFF;
				B 		= 	(rgbValue >>> 0) & 0xFF;
				
				// 2. Compute CbCr values or IQ values
				//    depending on mode of invoking
				if(mode == 0)
				{
					Cb = 	(-0.168736f)*(float)R + 
							(-0.331264f)*(float)G + 
							(0.5f)*(float)B + 128.0f;
					Cr = 	(0.5f)*(float)R + 
							(-0.418688f)*(float)G + 
							(-0.081312f)*(float)B + 128.0f;

					x[index] = (int)Cb;
					y[index] = (int)Cr;
				}

				else if(mode == 1)
				{
					I = (0.6f)*(float)R + 
						(-0.28f)*(float)G + 
						(-0.32f)*(float)B + 128.0f;
					Q = (0.21f)*(float)R + 
						(-0.52f)*(float)G + 
						(0.31f)*(float)B + 128.0f;

					x[index] = (int)I;
					y[index] = (int)Q;
				}

				else
				{
					System.err.println("Incorrect mode...");
					System.exit(0);
				}

				index++;
				repaint();
			}

		setSize(260, 260);
		setTitle(name);
		show();
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e) 
			{
				System.exit(0);
			}
		}
		);
	}

	// Plot the points on an output frame.
	public void paint(Graphics graphics) 
	{
		graphics.setColor(Color.black);
		for(int i=0;i<index;i++)
            graphics.drawLine(x[i],y[i],x[i],y[i]);
	}

	public static void main(String args[]) throws IOException
	{
		new CbCrIQMappings(args[0], Integer.parseInt(args[1]));
	}
	
}