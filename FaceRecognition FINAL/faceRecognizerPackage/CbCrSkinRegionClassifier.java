package faceRecognizerPackage;

import java.awt.*;
import java.util.*;
import java.awt.image.*;
import java.io.*;

// Class to model skin region in the CbCr space 
// given a set of image files with skin patches.
public class CbCrSkinRegionClassifier
{
	DPoint[] point;

	public CbCrSkinRegionClassifier(DPoint[] point)
	{
	    this.point = point;
		
	    // Cluster the pixels in the Cb Cr space into four clusters
		KMeanCluster kmc  = new KMeanCluster(point,4,true); 
		
		// Display the means in a format that can be directly used in
		// Face.java
		for(int i = 0; i < kmc.meanOfCluster.length; i++)
		    System.err.println("new DPoint("+kmc.meanOfCluster[i]+");");
	}

	public static int exists(DPoint p,  ArrayList pointList)
	{
	    int location = -1;
	    for(int i = 0; i < pointList.size(); i++)
	    {
	        DPoint point = (DPoint)pointList.get(i);
	        if(point.equals(p))
	            	location = i;
	    }
	    return(location);
	}

	public static void main(String args[]) throws IOException
	{
	    // List of train images for skin regions
	    String[] fileNames = new String[]
	                                    {
	            						"c:\\JustFaces\\1.jpg",
	            						"c:\\JustFaces\\2.jpg",
	            						"c:\\JustFaces\\3.jpg",
	            						"c:\\JustFaces\\4.jpg",
	            						"c:\\JustFaces\\5.jpg",
	            						"c:\\JustFaces\\6.jpg",
	            						"c:\\JustFaces\\7.jpg",
	            						"c:\\JustFaces\\8.jpg",
	            						"c:\\JustFaces\\9.jpg",
	            						"c:\\JustFaces\\10.jpg",
	            						"c:\\JustFaces\\11.jpg",
	            						"c:\\JustFaces\\12.jpg",
	            						"c:\\JustFaces\\13.jpg",
	            						"c:\\JustFaces\\14.jpg",
	            						"c:\\JustFaces\\15.jpg",
	            						"c:\\JustFaces\\16.jpg",
	            						"c:\\JustFaces\\17.jpg",
	            						"c:\\JustFaces\\18.jpg",
	    								};
	    ArrayList pointList = new ArrayList();
	    long totalNoOfSamples = 0;
	    
	    // For every train file
	    for(int fileIndex = 0; fileIndex < fileNames.length; fileIndex++)
	    {
	        // Open the image
	        Image image = Toolkit.getDefaultToolkit().
	        						getImage(fileNames[fileIndex]);
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
	
			int w  = image.getWidth(null);
			int h  = image.getHeight(null);
		
			BufferedImage bufferedImage = new BufferedImage
									(w, h, BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D graphics = bufferedImage.createGraphics();
			graphics.drawImage(image,0,0,w,h,null);
	
			// For each pixel in the image...
			for(int i = 0; i < h; i++)
				for(int j = 0; j < w; j++)
				{
					int Alpha,R,G,B,rgbValue;
					double Cb, Cr;
					double I, Q;
					rgbValue = bufferedImage.getRGB(j,i);
					
					Alpha	= 	(rgbValue >>> 24) & 0xFF;
					R 		= 	(rgbValue >>> 16) & 0xFF;
					G 		= 	(rgbValue >>> 8) & 0xFF;
					B 		= 	(rgbValue >>> 0) & 0xFF;
				
					// Obtain the CbCr values
					Cb = 	(-0.168736f)*(float)R + 
							(-0.331264f)*(float)G + 
							(0.5f)*(float)B + 128.0f;
					Cr = 	(0.5f)*(float)R + 
							(-0.418688f)*(float)G + 
							(-0.081312f)*(float)B + 128.0f;

					// If the color is not white or black 
					if(!(Cb == 128 && Cr == 128))
					{
					    totalNoOfSamples++;
					    // Check if the (Cb,Cr) point is present in the list
					    int location = exists(new DPoint(Cb,Cr),pointList);
					    if(location == -1)
					    {
					        // If not present add to list
					        pointList.add(new DPoint(Cb,Cr,1));
					    }
					    else
					    {
					        // If present...
					        DPoint p = (DPoint)pointList.get(location);
					        // Increase the strength... 
					        p.strength++;
					        pointList.set(location,p);
					    }
					}
				}
	    }

	    // Eliminate outliers basing on the strength...
	    double outLierError = 0.0005*totalNoOfSamples; 

	    for(int m = 0; m < pointList.size();)
	    {   
		    DPoint p = (DPoint)(pointList.get(m));
		    if(p.strength < outLierError)
		        pointList.remove(m);
		    else
		        m++;
	    }

	    DPoint[] point = new DPoint[pointList.size()];

	    for(int m = 0; m < pointList.size(); m++)
	    {   
	        point[m] = (DPoint)(pointList.get(m));
	    }
		new CbCrSkinRegionClassifier(point);
	}
}