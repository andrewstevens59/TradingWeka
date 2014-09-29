package BitcoinExchange;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class TimeSeries {
	
	// This stores a price point
	class Price {
		float price;
		long time;
	}
	
	// This stores the set of training sets
	private ArrayList<Price> price_buff = new ArrayList<Price>();
	// This stores the unix time
	private long unix_time = 1372401005;
	
	// This stores the start time
	private long start_time = Long.MAX_VALUE;
	// This stores the end time 
	private long end_time = 0;
	
	public TimeSeries() {
		
	}

	public TimeSeries(String symbol, int start_set, int end_set) throws IOException {
		
		for(int i=start_set; i<end_set; i++) {
			FileInputStream fos = new FileInputStream("DataSets/"+symbol+"_set"+i);
			DataInputStream dos = new DataInputStream(fos);
			
			while(dos.available() > 0) {
				long time = dos.readLong();
				float price = dos.readFloat();
				
				end_time = Math.max(end_time, time);
				start_time = Math.min(start_time, time);
				
				Price p = new Price();
				p.time = time;
				p.price = price;
				price_buff.add(p);
			}
			
			dos.close();
		}
		
		System.out.println(price_buff.size());
	}
	
	// This returns the start time
	public long StartTime() {
		return start_time;
	}
	
	// This returns the start time
	public long EndTime() {
		return end_time;
	}
	
	// This returns the price
	public float Price(long time) {
		
		for(int i=price_buff.size()-1; i>=0; i--) {
			
			Price p = price_buff.get(i);
			if(p.time >= time) {
				return p.price;
			}
		}
		
		return price_buff.get(0).price;
	}
	
	// This returns the price gradient between two time periods
	public float PriceGrad(long start, long end) {
		
		ArrayList<Float> buff = new ArrayList<Float>();
		
		for(int i=price_buff.size()-1; i>=0; i--) {
			
			Price p = price_buff.get(i);
			if(p.time >= start && p.time < end) {
				buff.add(p.price);
			}
		}
		
		return LinearRegression.Regression(buff, 0, buff.size());
	}
	
	public void RetrieveTimeSeries(String symbol) throws IOException {
		
		unix_time = 1372043769 ;
		for(int i=2; i<150; i++) {


			long end = unix_time;// - (i * 200000);
			long start = unix_time - ((i+1) * 200000);
	
			System.out.println(start+"     "+ unix_time+"   ***************");
		
			URL url = new URL("http://api.bitcoincharts.com/v1/trades.csv?symbol="+symbol+"&end="+end);
		
			URLConnection conn = url.openConnection();
			DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
			BufferedReader d = new BufferedReader(new InputStreamReader(in));
	
			String str;
			ArrayList<Float> buff = new ArrayList<Float>();
			ArrayList<Long> time_buff = new ArrayList<Long>();
			while((str = d.readLine()) != null) {
				StringTokenizer strtok = new StringTokenizer(str, ",");
				String tok1 = strtok.nextToken();
				long time = Long.valueOf(tok1);
				String tok2 = strtok.nextToken();
				buff.add(Float.valueOf(tok2));
				time_buff.add(time);
				
				unix_time = time;
			}
			
			in.close();
			d.close();
			
			System.out.println(buff.size());
			if(buff.size() < 10) {
				System.exit(0);
			}
			
			System.out.println("http://api.bitcoincharts.com/v1/trades.csv?symbol="+symbol+"&end="+end);
			System.out.println("DataSet/"+symbol+"_set"+i+"      "+unix_time+"    "+buff.size());
			FileOutputStream fos = new FileOutputStream("DataSets/"+symbol+"_set"+i);
			DataOutputStream dos = new DataOutputStream(fos);
			
			for(int j=0; j<buff.size(); j++) {
				dos.writeLong(time_buff.get(j));
				dos.writeFloat(buff.get(j));
			}
			
			dos.close();
		}
		
	}

}
