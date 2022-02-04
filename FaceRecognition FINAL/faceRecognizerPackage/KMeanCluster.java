package faceRecognizerPackage;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

// Class to split an array of DPoints into K clusters
// Based on the K-Mean Clustering algorithm
public class KMeanCluster extends Frame
{
    public ArrayList[] cluster;
    public DPoint[] meanOfCluster;
    int k;
    int mode;
	
    // point - DPoint array of points to be clustered
    // givenK - Number of clusters that are to be obtained
    // showFrames - Display the identified clusters on an output frame ?
    public KMeanCluster(DPoint[] point,int givenK, boolean showFrames)
	{
        mode = 1;
        this.k = Math.min(givenK,point.length);
        DPoint[] oldMean = new DPoint[k];
        DPoint[] newMean = new DPoint[k];

        // Set the initial K Means to arbitrary points
        for(int i = 0; i < k ;  i++)
	    {
	        oldMean[i] = new DPoint(0,0);
	        oldMean[i].setPoint(point[i]);
	    }
   
        // Initialize a high error
	    double error = 100;
	    
	    // Until error is not below the minimum allowable error
	    while(error > 0.5)
	    {
	        // Instantiate lists to store dynamically varying clusters
	        ArrayList[] tempCluster = new ArrayList[k];
	        
	        for(int i = 0; i < k ; i++)
		        tempCluster[i] = new ArrayList();
	        
	        // For every point in the input array...
		    for(int i = 0; i < point.length; i++)
	        {
		        // Find the closest of the K Means
	            double shortestDistance = 999999999;
	            int nearestCluster = 0;
	            for(int j = 0; j < k; j++)
	            {
	                if(oldMean[j].distance(point[i]) < shortestDistance)
                    {
                    nearestCluster = j;
                    shortestDistance = oldMean[j].distance(point[i]);
                    }
	            }
	            // Add the point to the closest cluster
	            tempCluster[nearestCluster].add(point[i]);
	        }
		    
		    // Copy temporary cluster data into actual cluster list
		    cluster = tempCluster;
	        
		    // For each of the K Clusters...
		    // compute the new mean of that cluster
	        for(int i = 0; i < k; i++)
	        {
	            double meanX = 0, meanY = 0;
	            int totalStrength=0;
	            for(int j = 0; j < tempCluster[i].size(); j++)
	            {
	                DPoint p = (DPoint)tempCluster[i].get(j);
	                meanX += p.x * p.strength;
	                meanY += p.y * p.strength;
	                totalStrength += p.strength; 
	            }	                
	            
	            meanX = meanX / totalStrength;
	            meanY = meanY / totalStrength;
	            
	            newMean[i] = new DPoint(meanX,meanY);
	        }
	        
	        // Compute the error - distance of new mean from the old one.
		    error = 0;
		    for(int i = 0; i < k; i++)
		        error += newMean[i].distance(oldMean[i]);
		    
		    for(int i = 0; i < k; i++)
		    {
		        oldMean[i] = newMean[i];
		    }

		    meanOfCluster = oldMean;    
	    }
	    
	    // Refine the each of the cluster
	    for(int i = 0; i < cluster.length; i++)
	        cluster[i] = new RefinedCluster(cluster[i]).getRefinedCluster();
	    
	    // Display the output frame if asked for
	    if(showFrames)
	    {
		    setTitle(k+" Mean Clusters");
		    setSize(320,240);
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
	}
    
    // Plot the points on an output frame
    public void paint(Graphics graphics) 
	{
        for(int i = 0; i < k; i++)
        { 
            for(int j = 0; j < cluster[i].size(); j++)
            {                 
                int xcord, ycord;
                if(mode == 0)
                {
                    xcord = ((Point)cluster[i].get(j)).x;
                    ycord = ((Point)cluster[i].get(j)).y;
                }
                else
                {
                    xcord = (int)((DPoint)cluster[i].get(j)).x;
                    ycord = (int)((DPoint)cluster[i].get(j)).y;                    
                }

                if((i+1)%3==1)
                    graphics.setColor(Color.GREEN);
                else 
                    if((i+1)%3==2)
                        graphics.setColor(Color.BLUE);
                    else
                        graphics.setColor(Color.RED);
   
                graphics.fillOval(xcord ,ycord ,1,1);                
            }           
        }        
	}
    
    public ArrayList[] getClusters()
    {
        return(cluster);
    }
    
    // Main program to test the KMeanCluster class
    public static void main(String args[])
    {
	    DPoint[] points = new DPoint[]{	new DPoint(2,10),
	            						new DPoint(5,8),
	            						new DPoint(1,2),
	            						new DPoint(2,5),
	            						new DPoint(8,4),
	            						new DPoint(7,5),
	            						new DPoint(6,4),
	            						new DPoint(8,2),
	            						new DPoint(6,1),
	            						new DPoint(3,5),
	            						new DPoint(7,9),
	            						new DPoint(1,5),
	            						new DPoint(9,2),
	            					};
	    int k = 9;
	    KMeanCluster kmc = new KMeanCluster(points,k,true);
		ArrayList[] cluster = kmc.cluster;
        for(int i = 0; i < k; i++)
        {      
            System.err.println("Cluster : " + (i+1));
            for(int j = 0; j < cluster[i].size(); j++)
            {
                System.err.println(((DPoint)cluster[i].get(j)).x+
                        		" "+((DPoint)cluster[i].get(j)).y);
            }           
        }   
	}	
}