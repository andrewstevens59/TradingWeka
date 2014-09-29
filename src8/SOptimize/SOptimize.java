package SOptimize;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.StringTokenizer;

import math.jwave.Transform;
import math.jwave.exceptions.JWaveFailure;
import math.jwave.transforms.DiscreteFourierTransform;

public class SOptimize {

	/**
	 * @param args
	 * @throws JWaveFailure 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws JWaveFailure, IOException {
		
		/*Random r = new Random();
		Transform t = new Transform( new DiscreteFourierTransform( ) );


		double mean = 0;
	    // arrTime = { r1, c1, r2, c2, ... }
	    double[ ] arrTime = new double[128];
	    for(int i=0; i<arrTime.length; i++) {
	    	arrTime[i] = mean;
	    	mean += r.nextGaussian() * 100;
	    }

	    double[ ] arrFreq = t.forward( arrTime ); // 1-D DFT forward
	    
	    for(int i=10; i<arrTime.length; i++) {
	    	//arrTime[i] = 0;
	    }

	    double[ ] arrReco = t.reverse( arrFreq ); // 1-D DFT reverse
	    
	    for(double v : arrReco) {
	    	System.out.println(v);
	    }*/
	    
	    BufferedReader br = new BufferedReader(new FileReader("I:/PhD/Code/Bitcoin/data.txt"));
    	String line;

    	int line_num = 0;
    	while ((line = br.readLine()) != null) {
    		
    		DecimalFormat numberFormat = new DecimalFormat("0.00");
    		StringTokenizer tok = new StringTokenizer(line, " ");
    		
    		int count = 0;
    		while(tok.hasMoreTokens()) {
    			String t = tok.nextToken();
    			
    			if(line_num < 500000) {
	    			if(count == 2) {
	    				
	    				if(t.contains("\\%")) {
	    					System.out.print(numberFormat.format(100 - Double.parseDouble(t.substring(0, t.length() - 2)))+"\\% ");
	    				} else {
	    					System.out.print(numberFormat.format(100 - Double.parseDouble(t))+" ");
	    				}
	    			} else {
	    				System.out.print(t+" ");
	    			}
    			} else {
    				if(count == 2 || count == 7) {
	    				
	    				if(t.contains("\\%")) {
	    					System.out.print(numberFormat.format(100 - Double.parseDouble(t.substring(0, t.length() - 2)))+"\\% ");
	    				} else {
	    					System.out.print(numberFormat.format(100 - Double.parseDouble(t))+" ");
	    				}
	    			} else {
	    				System.out.print(t+" ");
	    			}
    			}
    		
	    	   
	    	    count++;
    		}
    		System.out.println("");
    		line_num++;
    	}
    	br.close();

	}

}
