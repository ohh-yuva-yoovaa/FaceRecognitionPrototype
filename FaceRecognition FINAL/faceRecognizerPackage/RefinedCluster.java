package faceRecognizerPackage;

import java.util.*;

// Class to refine clusters and compute cluster parameters
public class RefinedCluster 
{
public ArrayList cluster;
public DPoint clusterMean;
public DPoint clusterMedoid;
public int totalStrength;
public int coveredArea;
public int extent;
public double coveredExtent;
public int minx;
public int miny;
public int maxx;
public int maxy;

public RefinedCluster(ArrayList cluster)
{
    this.cluster = cluster;
    getMean();
    getMedoid();
    removeOutliers();
    getMean();
    getMedoid();
    getExtent();
    getCoveredArea();
    coveredExtent = ((double)coveredArea)/((double)extent);
}

// Return the refined cluster
ArrayList getRefinedCluster()
{
    return(cluster);
}

void getMean()
{
    clusterMean = new DPoint();
    totalStrength = 0;
    for(int i = 0; i < cluster.size(); i++)
    {
        DPoint p = (DPoint)cluster.get(i);
        clusterMean.x += p.x * p.strength;
        clusterMean.y += p.y * p.strength;
        totalStrength += p.strength;
    }
    clusterMean.x = clusterMean.x/totalStrength;
    clusterMean.y = clusterMean.y/totalStrength;
}

void getMedoid()
{
    clusterMedoid = new DPoint();
    double shortestDistance = 999999999;
    for(int i = 0; i < cluster.size(); i++)
    {
        DPoint p = (DPoint)cluster.get(i);
        double distance = p.distance(clusterMean);
        if(distance < shortestDistance)
        {
            shortestDistance = distance;
            clusterMedoid.setPoint(p);
        }
    }
}

// Remove outliers from the cluster of points
void removeOutliers()
{
    double standardDeviation = 0;
    
    // Compute the standard deviation from the mean
    for(int i = 0; i < cluster.size(); i++)
    {
        DPoint p = (DPoint)cluster.get(i);
        double d = p.distance(clusterMedoid);
        standardDeviation += d*d;
    }
    
    standardDeviation = (double)standardDeviation/(cluster.size());
    standardDeviation = Math.sqrt(standardDeviation);
    
    // Remove outliers - points whose distance from cluster 
    // is greater than 1.5 times the standard deviation
    for(int i = 0; i < cluster.size();)
    {   
	    DPoint p = (DPoint)(cluster.get(i));
	    if(p.distance(clusterMedoid) > 1.5*standardDeviation)
	        cluster.remove(i);
	    else
	        i++;
    }
}

// Compute the bounding corners.
public void getExtent()
{
    minx = 999999999;
    miny = 999999999;
    maxx = -1;
    maxy = -1;
    
    for(int i = 0; i < cluster.size(); i++)
    {
        DPoint p = (DPoint)cluster.get(i);
        if(p.x > maxx)
            maxx = (int)p.x;

        if(p.y > maxy)
            maxy = (int)p.y;
        
        if(p.x < minx)
            minx = (int)p.x;

        if(p.y < miny)
            miny = (int)p.y;
    }
    extent = (maxx - minx)*(maxy - miny);
}

void getCoveredArea()
{
    coveredArea = cluster.size();
}
}