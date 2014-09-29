package Bitcoin;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


// This loads the order book states into memory and finds transition probabilities
public class OrderBook {

	
	
	// This parses the full order book and condenses it
	private void ReduceOrderBook() throws IOException, JSONException {
		
		BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\finzyholly\\Desktop\\info_mtgox_depth_history.json"));
		String line;
		
		FileOutputStream fos = new FileOutputStream("order.txt");
		DataOutputStream dos = new DataOutputStream(fos);
		
		
		while ((line = br.readLine()) != null) {
			
			System.out.println(line);
			
			/*try {
				JSONArray MyList = new JSONObject(line).getJSONArray("asks");
				
				int offset = 0;
				int len = Math.min(50, MyList.length());
				double val[] = new double[len];
				double price[] = new double[len];
						
				dos.writeInt(len);
				for(int i=0; i<len; i++) {
					val[i] = ((JSONObject)MyList.get(offset)).getDouble("amount");
					price[i] = ((JSONObject)MyList.get(offset++)).getDouble("price");
					dos.writeDouble(val[i]);
					dos.writeDouble(price[i]);
					
				}
				
				MyList = new JSONObject(line).getJSONArray("bids");
				
				offset = MyList.length() - 1;
				len = Math.min(50, MyList.length());
				val = new double[len];
				price = new double[len];
						
				dos.writeInt(len);
				for(int i=0; i<len; i++) {
					val[i] = ((JSONObject)MyList.get(offset)).getDouble("amount");
					price[i] = ((JSONObject)MyList.get(offset)).getDouble("price");
					offset--;
					dos.writeDouble(val[i]);
					dos.writeDouble(price[i]);
				}

			} catch(JSONException e) { 
			}*/
			
		}
		
		br.close();
		dos.close();
	}
	
	// This parses the reduce order book and loads it into memory
	private void ParseOrderBook() throws NumberFormatException, IOException {
		FileInputStream fos = new FileInputStream("order.txt");
		DataInputStream dos = new DataInputStream(fos);
		
		while(dos.available() > 0) {
			int ask_len = dos.readInt();
			double ask_vol[] = new double[ask_len];
			double ask_price[] = new double[ask_len];
			for(int i=0; i<ask_len; i++) {
				ask_vol[i] = dos.readDouble();
				ask_price[i] = dos.readDouble();
			}
			
			int bid_len = dos.readInt();
			double bid_vol[] = new double[bid_len];
			double bid_price[] = new double[bid_len];
			for(int i=0; i<bid_len; i++) {
				bid_vol[i] = dos.readDouble();
				bid_price[i] = dos.readDouble();
			}
		}
		
		dos.close();

	}

	
	public OrderBook() throws IOException, JSONException {
		
		ReduceOrderBook();
		ParseOrderBook();
	}

}
