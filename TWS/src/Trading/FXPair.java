package Trading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
import com.ib.client.TagValue;

import net.sf.javaml.core.DenseInstance;

import MoveParticle.Classify;
import MoveParticle.SampleGraphSet;
import MoveParticle.Train;

public class FXPair {

	// This stores a sample of the market over a period of time
	class MarketSample {
		// This stores the number of bid samples
		int bid_num = 0;
		// This stores the number of ask samples
		int ask_num = 0;
		// This stores the bid volume
		float bid_vol = 0;
		// This stores the ask volume
		float ask_vol = 0;
		// This stores the low exchange rate
		float bid_low = Float.MAX_VALUE;
		// This stores the high exchange rate
		float ask_high = -Float.MAX_VALUE;
		// This stores the exchange rate
		double exch_rate;
		// This stores the output from the price jump
		int output;
		// This stores the training sample
		double sample[];
		// This stores the next ptr
		MarketSample next_ptr;
		// This stores the prev ptr
		MarketSample prev_ptr;
	}
	
	// This stores the set of order ids
	private ArrayList<Integer> order_id_buff = new ArrayList<Integer>();
	
	// This stores the head market sample
	private MarketSample head_ptr = null;
	// This stores the prev market sample
	private MarketSample tail_ptr = null;
	// This stores the current market state
	private MarketSample sample = new MarketSample();
	// This stores the time the last sample was taken
	private long prev_time = System.currentTimeMillis();
	// This stores the number of samples
	private int sample_num = 0;
	// This is used for prediction
	private SampleGraphSet graph;
	// This stores the cash value of each currency in the account
	private static HashMap<String, Float> currency_val_map = new HashMap<String, Float>();
	// This stores the first currency
	private String currency1;
	// This stores the second currency
	private String currency2;
	// This stores the connection client
	private EClientSocket client;
	// This stores the current fxpair
	private static int order_id = 6;
	
	public FXPair(SampleGraphSet g, String currency1, String currency2, EClientSocket client) {
		graph = g;
		this.currency1 = currency1;
		this.currency2 = currency2;
		this.client = client;
		
		currency_val_map.put(currency1, 10.0f);
		currency_val_map.put(currency2, 10.0f);
	}
	
	// This sets the current order id
	public static synchronized void SetOrderID(int order_id1) {
		order_id = order_id1;
	}
	
	// This assigns the training sample based on sample history
	private synchronized void AssignSample() {
		int offset = 0;
		Train t = new Train();
		MarketSample s[] = new MarketSample[4];
		MarketSample ptr = head_ptr;
		while(ptr != null) {
			s[offset++] = ptr;
			ptr = ptr.next_ptr;
		}
		
		System.out.println(offset+"  ----------------");
		if(offset < 4) {
			return;
		}
		
		float bid_sum = 0;
		float ask_sum = 0;
		for(int k=0; k<t.ba_vol_diff.length; k++) {
			bid_sum += s[k].bid_vol;
			ask_sum += s[k].ask_vol;
			
			t.ba_vol_diff[k] = (bid_sum - ask_sum) / (bid_sum + ask_sum);
		}
		
		t.exch_rate = (float) s[0].exch_rate;
		float min_rate = Float.MAX_VALUE;
		float max_rate = -Float.MAX_VALUE;
		for(int k=0; k<4; k++) {
			min_rate = Math.min(min_rate, s[k].bid_low);
			max_rate = Math.max(max_rate, s[k].ask_high);
			t.gap_rat[k] = (t.exch_rate - min_rate) / (max_rate - min_rate);
			t.volatility_gap[k] = max_rate - min_rate;
		}
		
		ArrayList<Float> val_buff = new ArrayList<Float>();
		for(int i=0; i<t.ba_vol_diff.length; i++) {
			val_buff.add(t.ba_vol_diff[i]);
		}
		
		for(int i=0; i<Math.min(2, t.gap_rat.length); i++) {
			val_buff.add(t.gap_rat[i]);
		}
		
		for(int i=0; i<Math.min(1, t.volatility_gap.length); i++) {
			val_buff.add(t.volatility_gap[i]);
		}
		
		head_ptr.sample = new double[val_buff.size()];
		for(int i=0; i<val_buff.size(); i++) {
			head_ptr.sample[i] = val_buff.get(i);
		}
	}
	
	// This adds a new sample to the set
	private synchronized void AddSample() {
		
		if(sample.ask_num < 4 || sample.bid_num < 4) {
			return;
		}
		
		long elaps_time = System.currentTimeMillis() - prev_time;
		if(elaps_time < 1000 * 30) {
			return;
		}
		
		sample.ask_vol /= sample.ask_num;
		sample.bid_vol /= sample.bid_num;
		if(head_ptr != null && head_ptr.sample != null) {
			double diff = sample.exch_rate - head_ptr.exch_rate;
			
			if(diff > 1e-4) {
				sample.output = Classify.BUY_VAL;
			} else if(diff < -1e-4) {
				sample.output = Classify.SELL_VAL;
			} else if(diff > 0) { 
				sample.output = Classify.STAY_POS;
			} else {
				sample.output = Classify.STAY_NEG;
			}
			
			graph.GrowSampleSet();
			graph.ReduceSamples(head_ptr.sample, sample.output);
		}

		MarketSample prev_ptr = head_ptr;
		head_ptr = sample;
		head_ptr.next_ptr = prev_ptr;
		
		if(prev_ptr != null) {
			prev_ptr.prev_ptr = head_ptr;
		}
		
		sample_num++;
		if(tail_ptr == null) {
			tail_ptr = head_ptr;
		}
		
		if(sample_num > 4) {
			tail_ptr.prev_ptr.next_ptr = null;
			tail_ptr = tail_ptr.prev_ptr;
			sample_num--;
		}
		
		prev_time = System.currentTimeMillis();
		sample = new MarketSample();
		
		AssignSample();
		
		System.out.println("New Sample  ------------------ "+head_ptr.sample);
		
		CreateOrder();
	}
	
	// This adds market data for this fx pair
	public synchronized void AddTickPrice(boolean is_ask, double price) {
		
		
		if(is_ask == true) {
			sample.ask_high = (float) Math.max(sample.ask_high, price);
		} else {
			sample.bid_low = (float) Math.min(sample.bid_low, price);
		}
		
		sample.exch_rate = price;
		
		AddSample();
	}
	
	// This adds market data for this fx pair
	public synchronized void AddTickVolume(boolean is_ask, int volume) {
		if(is_ask == true) {
			sample.ask_vol += volume;
			sample.ask_num++;
		} else {
			sample.bid_vol += volume;
			sample.bid_num++;
		}
		
		long elaps_time = System.currentTimeMillis() - prev_time;
		elaps_time /= 1000;
		System.out.println(sample.ask_num+" "+sample.bid_num+" "+elaps_time);
		AddSample();
	}
	
	// This updates the amount of each currency and net liquidation
	public static synchronized void UpdateCurrencyValue(String key, String value, String currency) {
		
		if(currency == null || value == null || key == null || value.length() == 0) {
			return;
		}
		
		if(key.equals("CashBalance") == false) {
			return;
		}
		
		
		try {
			Float val = Float.valueOf(value);
			currency_val_map.put(currency, val);
			System.out.println(currency+"   "+val+" (999999999999999999");
		} catch(NumberFormatException e) {
		}
	}
	
	// This creates an order based on the predicted outcome in the future
	public synchronized void CreateOrder() {
		
		if(head_ptr.sample == null) {
			return;
		}
		
		int output = graph.NextDayOutput(head_ptr.sample);
		
		System.out.println("ORDER PLACED ------------------ "+output);
		
		if(Math.abs(output) < 2) {
			return;
		}
		
		float val1 = currency_val_map.get(currency2);
		float val2 = currency_val_map.get(currency1);
		
		System.out.println(currency2+" "+val1);
		System.out.println(currency1+" "+val2);
		
		if(output > 1 && val1 <= 0) {
			return;
		}
		
		if(output < -1 && val2 <= 0) {
			return;
		}
		
		
		System.out.println("Order In Motion:   ");
		for(int i=Math.max(0, order_id_buff.size()-4); i<order_id_buff.size(); i++) {
			client.cancelOrder(order_id_buff.get(i));
		}
		
		Contract conn = new Contract();
		Order order = new Order();
		
		conn.m_symbol = currency2;
		conn.m_currency = currency1;
		conn.m_secType = "CASH";
		conn.m_exchange = "IDEALPRO";
		conn.m_localSymbol = "";
		
		order.m_action = Classify.BUY_VAL == output ? "BUY" : "SELL";
		order.m_totalQuantity = 60000;//(int) (Classify.BUY_VAL == output ? val1 : val2) + 1;
		order.m_orderType = "MKT";
		order.m_lmtPrice = 40;
		order.m_auxPrice = 0.0f;
		order.m_whatIf = false;
		
		
		/*order.m_action = "BUY";
		order.m_totalQuantity = 10000;
		order.m_orderType = "MKT";
		order.m_lmtPrice = 40;
		order.m_auxPrice = 0.0f;
		order.m_whatIf = false;*/
		
		order_id_buff.add(order_id);
		client.placeOrder(order_id, conn, order);
		order_id++;
	}

}
