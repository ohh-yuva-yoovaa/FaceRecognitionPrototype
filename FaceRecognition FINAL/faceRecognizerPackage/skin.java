import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;


/*
 * Class to extract the lip feature from the image
 * uses other utility classes to find the ends of the 
 * lip region. Need to be integrated with the face 
 * recognizer package. This is a stand alone module 
 * used at the testing phase.
 */
public class skin extends Frame
{
	Image image;
	int x,y;
	int[] oneDPix,moneDPix,imagePix;
    int flag = 0;
    int mouthMinCb;
	int mouthMaxCb;
	int mouthMinCr;
	int mouthMaxCr;
	float factorMinRange;
	float factorMaxRange;
	int Alpha,R,G,B,rgbValue;
	public skin(String name) throws IOException
	{
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

		int w  = image.getWidth(null);
		int h  = image.getHeight(null);
		
		System.err.println(w);
		System.err.println(h);
		BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);	
		Graphics2D graphics = bufferedImage.createGraphics();
		graphics.drawImage(image,0,0,w,h,null);
		oneDPix = new int[w * h];
		moneDPix = new int[w * h];
		imagePix = new int[w * h];

		PixelGrabber pgObj = new PixelGrabber(
		                  image,0,0,w,h,oneDPix,0,w);
		try {
	        pgObj.grabPixels();
	      }
	      catch (InterruptedException e) {
	        e.printStackTrace();
	      }

	    int p = 0;
	    int avgGray = 0;
	    for(int i = 0; i < h; i++)
		{
			for(int j = 0; j < w; j++)
			{
				p = i * w + j;
				int pixel = oneDPix[p];
				Alpha	= 	(pixel >>> 24) & 0xFF;
				R 		= 	(pixel >>> 16) & 0xFF;
				G 		= 	(pixel >>> 8) & 0xFF;
				B 		= 	(pixel >>> 0) & 0xFF;
				avgGray += (R+G+B)/3;

			}
		}
	    avgGray = avgGray/(w*h);
		System.out.println("ava:"+avgGray);
		int compAvgGray = 0;
		for(int i = 0; i < h; i++)
		{
			for(int j = 0; j < w; j++)
			{
				p = i * w + j;
				compAvgGray = 0;
				int pixel = oneDPix[p];
				imagePix[p] = pixel;
				//System.out.println("p:"+pixel);
				
				double Cb, Cr;
				//pgObj.setPixels()
				//rgbValue = bufferedImage.getRGB(j,i);

				Alpha	= 	(pixel >>> 24) & 0xFF;
				R 		= 	(pixel >>> 16) & 0xFF;
				G 		= 	(pixel >>> 8) & 0xFF;
				B 		= 	(pixel >>> 0) & 0xFF;

				compAvgGray = (R+G+B)/3;

				Cr = (-0.168736f)*(float)R + (-0.331264f)*(float)G + (0.5f)*(float)B + 128.0f;
				Cb = (0.5f)*(float)R + (-0.418688f)*(float)G + (-0.081312f)*(float)B + 128.0f;
				x = (int)Cb;
				y = (int)Cr;
				//System.out.println(x+":"+y);
				// works good for mark and joe..Americans
				mouthMinCb = 134;
				mouthMaxCb = 140;//-mark
				mouthMinCr = 118;//-mark
				mouthMaxCr = 122;
				factorMinRange = 1.5f;
				factorMaxRange = 2.5f;
		/*		//Indians...
				mouthMinCb = 143;
				mouthMaxCb = 170;
				mouthMinCr = 112;
				mouthMaxCr = 119;
				factorMinRange = 1.2f;
				factorMaxRange = 2.5f;*/
				ArrayList pointList = new ArrayList();
			/*	if(compAvgGray < avgGray/3)
				{
					flag = 1;
				}*/
				if((Cb>=mouthMinCb && Cb<=mouthMaxCb)
						&& (Cr>=mouthMinCr && Cr<=mouthMaxCr))
				{
					int min = Math.min(R,G);
					int v1  = Math.min(R,B);
					if(v1<min)min=v1;
					
					int max = Math.max(R,G);
					int v2  = Math.max(R,B);
					if(v2>max)max=v2;
					float factor;
					
					if(min==0)
						factor = 1.0f;
					else
						factor = (float)(max/min);
					
					if((factor>=factorMinRange && factor<=factorMaxRange))
											flag = 1;
					
						
				}
				if(flag==1)
				{
					Alpha = R = G = B = 0;
				}
				else 
					Alpha = R = G = B = 255;
				pixel = ((Alpha << 24) | (R << 16)  |
							(G << 8)  |
							(B << 0) );
							

				moneDPix[p] = pixel;
				
				flag = 0;
			}
		}
		
		int hw = w/3;
		int hh = h/3;
		ArrayList dpList = new ArrayList();
		int count = 0;
		for(int i = hh; i < h; i++)
		{
			for(int j = hw; j < w; j++)
			{
				p = i * w + j;
				int pixel = moneDPix[p];
				int Alpha,R,G,B,rgbValue;
				Alpha	= 	(pixel >>> 24) & 0xFF;
				R 		= 	(pixel >>> 16) & 0xFF;
				G 		= 	(pixel >>> 8) & 0xFF;
				B 		= 	(pixel >>> 0) & 0xFF;
				if(Alpha==0 && R==0 && G==0 && B==0)
				{
					dpList.add(new DPoint(j,i));
					count++;
				}
			}
		}
		
		DPoint[] dp = new DPoint[dpList.size()];
		
		for(int i = 0; i < dpList.size(); i++)
			dp[i] = (DPoint)dpList.get(i);
		
		
		for(int k=0;k<count;k++)
		{
			for(int m=0;m<count;m++)
			{
				if(m!=k)
				{
					double distance = dp[k].distance(dp[m]);
					if(distance<9)
					{
						dp[k].strength += 1;
					}
				}
			}
		}
		int temp = 0;
		int maxStrength  = 0;
		for(int k=0;k<count;k++)
		{
			if(dp[k].strength > maxStrength)
			{
				maxStrength = dp[k].strength;
			}
		}
		
		ArrayList boundary = new ArrayList();
		for(int k=0;k<count;k++)
		{
			if(dp[k].strength> (int)((float)maxStrength/(2.5)))
			{
				int arrayIndex = (int)(dp[k].y * w + dp[k].x);
				Alpha = R = G = B = 255;
				int pixel = ((Alpha << 24) | (R << 16)  |
							(G << 8)  |
							(B << 0) );
				imagePix[arrayIndex] = pixel;
				boundary.add(dp[k]);
		
			}
		}

		RefinedCluster rc = new RefinedCluster(boundary);
		
		System.out.println(rc.minx+":"+rc.miny+","+rc.maxx+":"+rc.maxy);
		
		Image modImg = createImage( new MemoryImageSource( w,h,imagePix,0,w)); 
		w = modImg.getWidth(this);
		h = modImg.getHeight(this); 
		System.err.println(w); 
		System.err.println(h); 
		BufferedImage bufferImage = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR); 
		graphics = bufferImage.createGraphics(); 
		graphics.drawImage(modImg,0,0,w,h,null); 
		File f = new File("c:\\","mark_out.jpg"); 
		ImageIO.write(bufferImage,"jpg",f); 
	}




	public static void main(String args[]) throws IOException
	{
		String name = "mark.jpg";
		new skin(name);
	}
	
}
