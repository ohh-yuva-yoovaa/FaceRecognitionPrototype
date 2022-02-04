package faceRecognizerPackage;

import java.awt.Point;

// Special class for handling points with co-ordinates in 
// double format as the co-ordinates in the CbCr space
// along with strength of that point. 

public class DPoint
{
    public double x;
    public double y;
    public int strength;
    
    // Different constructors for different usages.

    public DPoint()
    {
        x = 0;
        y = 0;
        strength = 1;
    }
    
    public DPoint(double a, double b)
    {
        x = a;
        y = b;
        strength = 1;
    }    

    public DPoint(double a, double b, int c)
    {
        x = a;
        y = b;
        strength = c;
    }    

    public DPoint(Point p)
    {
        x = p.x;
        y = p.y;
        strength = 1;
    }    
 
    public DPoint(DPoint p)
    {
        x = p.x;
        y = p.y;
        strength = 1;
    }    
 
    // Fucntion to set the coordinates to Pixel coordinates.
    public void setPoint(Point p)
    {
        x = p.x;
        y = p.y;
    }    
    
    // Function to copy data from another DPoint object
    public void setPoint(DPoint p)
    {
        x = p.x;
        y = p.y;
    }    

    // Function to compute the Eucledian distance from a Point object
    public double distance(Point p)
    {
        return(Math.sqrt((p.x - x)*(p.x - x)+(p.y - y)*(p.y - y)));
    }

    // Function to compute the Eucledian distance from a DPoint object
    public double distance(DPoint p)
    {
        return(Math.sqrt((p.x - x)*(p.x - x)+(p.y - y)*(p.y - y)));
    }

    // To overide the toString function of object class
    // for displaying DPoint co-ordinates.
    public String toString()
    {
        return(x+" , "+y);
    }
    
    // Check if the DPoint co-ordinates are same as in another DPoint object
    public boolean equals(DPoint p)
    {
        return((this.x==p.x) && (this.y==p.y));
    }
    
    // Check if the DPoint co-ordinates are same as in another Point object    
    public boolean equals(Point p)
    {
        return(((int)this.x==p.x) && ((int)this.y==p.y));
    }

    // Set the strength of the point represented by this DPoint
    public void setStrength(int strength)
    {
        this.strength = strength;
    }
}
