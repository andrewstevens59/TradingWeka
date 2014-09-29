package PriceSimulation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;


public class PriceHistory {
	
	// This stores the set of price histories
	private ArrayList<Float> price_buff = new ArrayList<Float>();

	public PriceHistory() throws IOException {
		
		 BufferedReader br = new BufferedReader(new FileReader("bcfeed_mtgoxUSD_1min.csv"));
		    try {
		        String line = br.readLine();

		        while (line != null) {
		        	StringTokenizer tok = new StringTokenizer(line, ",");
		        	tok.nextToken();
		        	
		        	String str = tok.nextToken();
		        	if(str != null) {
			        	float val = Float.parseFloat(str);
			        	if(val < 200 && val > 5) {
			        		price_buff.add(val);
			        	}
		        	}
		        	
		            line = br.readLine();
		        }
		    } finally {
		        br.close();
		    }
	}
	
	// This creates the price distribution 
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {
		
		PriceHistory p = new PriceHistory();
		
	}

}
