package Bitcoin;
import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
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
	
	// This defines a state transition
	class Transit {
		double price;
		OBState s;
		PriceState ps;
	}

	// This stores the set of order book states
	private HashMap<String, OBState> ob_map = new HashMap<String, OBState>();
	// This stores the number of transitions between two states
	private HashMap<String, Integer> trans_map = new HashMap<String, Integer>();
	// This stores the set of state transitions
	private ArrayList<Transit> transit_buff = new ArrayList<Transit>();
	// This stores the set of observed states
	private HashMap<String, PriceState> price_map = new HashMap<String, PriceState>();
	
	// This parses the full order book and condenses it
	private void ReduceOrderBook() throws IOException, JSONException {
		
		PrintWriter out = new PrintWriter("order.txt");

		
		int line_num = 0;
		BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\callum\\Desktop\\info_mtgox_depth_history.json"));
		String line;
		while ((line = br.readLine()) != null) {
			
		//	System.out.println(line);
			
			try {
				JSONArray MyList = new JSONObject(line).getJSONArray("asks");
				
				int offset = 0;
				OBState ob_state = new OBState();
				double val[] = new double[ob_state.dist.length];
				double price[] = new double[ob_state.dist.length];
						
				for(int i=ob_state.dist.length >> 1; i<ob_state.dist.length; i++) {
					val[i] = ((JSONObject)MyList.get(offset)).getDouble("amount");
					price[i] = ((JSONObject)MyList.get(offset++)).getDouble("price");
				}
				
				MyList = new JSONObject(line).getJSONArray("bids");
				offset = MyList.length() - (ob_state.dist.length >> 1);
				for(int i=0; i<ob_state.dist.length >> 1; i++) {
					val[i] = ((JSONObject)MyList.get(offset)).getDouble("amount");
					price[i] = ((JSONObject)MyList.get(offset++)).getDouble("price");
				}
				
				String key = new String();
				for(int i=0; i<ob_state.dist.length; i++) {
					ob_state.dist[i] = (int) (Math.log10(val[i] + 1.0f));
					key += ob_state.dist[i] + " ";
				}
				
				ob_map.put(key, ob_state);
				
				key += (price[ob_state.dist.length >> 1] + price[ob_state.dist.length >> 1 + 1]) / 2;
				
				out.println(key);
				out.flush();
				
				System.out.println(key +"      "+ob_map.size()+"  "+line_num);
				System.out.print("\n");
				line_num++;
			} catch(JSONException e) { 
			}
			
		}
		
		br.close();
		out.close();
	}
	
	// This parses the reduce order book and loads it into memory
	private void ParseOrderBook() throws NumberFormatException, IOException {
		

		int line_num = 0;
		ArrayList<Float> prices = new ArrayList<Float>();
		BufferedReader br = new BufferedReader(new FileReader("order.txt"));
		
		String line;
		while ((line = br.readLine()) != null) {

			String key = new String();
			OBState ob_state = new OBState();
			StringTokenizer strtok = new StringTokenizer(line, " ");
			for(int i=0; i<ob_state.dist.length; i++) {
				ob_state.dist[i] = Integer.parseInt(strtok.nextToken());
				key += ob_state.dist[i] +" ";
			}

			double price = Double.parseDouble(strtok.nextToken());
			OBState temp = ob_map.get(key);
			if(temp == null) {
				ob_map.put(key, ob_state);
				temp = ob_state;
			}
			
			prices.add((float) price * 15);
			
			PriceState ps = new PriceState();
			ps.FindPriceState(prices);
			PriceState temp1 = price_map.get(ps.toString());
			System.out.println(ps.toString());
			if(temp1 == null) {
				price_map.put(ps.toString(), ps);
				temp1 = ps;
			}
			
			Transit s = new Transit();
			s.price = price;
			s.s = temp;
			s.ps = ps;
			transit_buff.add(s);
			
			System.out.println(key +"      "+ob_map.size()+" "+price_map.size()+"  "+line_num);
			System.out.print("\n");
			line_num++;

		}
		
		br.close();
	}
	
	// This creates the transition probability matrix for hidden states
	private void CreateHiddenTransMat() {
		
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();
		for(int i=0; i<transit_buff.size()-1; i++) {
			
			String str = transit_buff.get(i).s + " " + transit_buff.get(i+1).s;
			Integer count = trans_map.get(str);
			if(count == null) {
				trans_map.put(str, 1);
			} else {
				trans_map.put(str, count + 1);
			}
		}
		
		for(int i=0; i<transit_buff.size()-1; i++) {
			String str = transit_buff.get(i).s + " " + transit_buff.get(i+1).s;
			if(map.get(str) == null) {
				continue;
			}
			
			map.put(str, false);
			
			OBLink link = new OBLink();
			link.trav_prob = trans_map.get(str);
			OBState s = transit_buff.get(i).s;
			link.dst = transit_buff.get(i+1).s;
			
			OBLink prev_ptr = s.forward_link;
			s.forward_link = link;
			link.next_ptr = prev_ptr;
		}
	}
	
	// This creates the transition probability matrix for observed states
	private void CreateObservedTransMat() {
		
		trans_map.clear();
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();
		for(int i=0; i<transit_buff.size(); i++) {
			
			String str = transit_buff.get(i).s + " " + transit_buff.get(i).ps;
			Integer count = trans_map.get(str);
			if(count == null) {
				trans_map.put(str, 1);
			} else {
				trans_map.put(str, count + 1);
			}
		}
		
		for(int i=0; i<transit_buff.size(); i++) {
			
			String str = transit_buff.get(i).s + " " + transit_buff.get(i).ps;
			if(map.get(str) == null) {
				continue;
			}
			
			map.put(str, false);
			
			OBLink link = new OBLink();
			link.trav_prob = trans_map.get(str);
			PriceState s = transit_buff.get(i).ps;
			link.dst = transit_buff.get(i).s;
			
			OBLink prev_ptr = s.forward_link;
			s.forward_link = link;
			link.next_ptr = prev_ptr;
		}
	}
	
	// This stores the number of hidden states
	public int HiddeStateNum() {
		return ob_map.size();
	}
	
	
	public OrderBook() throws IOException, JSONException {
		ParseOrderBook();
		CreateHiddenTransMat();
		CreateObservedTransMat();
	}

}
