package SOptimize;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

public class TradingSignals {
	
	// This stores the resulting trading series
	private ArrayList<Double> series_buff = new ArrayList<Double>();
	// This stores the training buffer
	private ArrayList<Train> train_buff = new ArrayList<Train>();
	// This stores the level sets
	private HashMap<Integer, ArrayList<Integer>> level_map = new HashMap<Integer, ArrayList<Integer>>();
	// This stores the association between a time point and a price level
	private HashMap<Integer, Integer> price_map = new HashMap<Integer, Integer>();
	// This stores the set of rules
	private ArrayList<Rule> rule_set = new ArrayList<Rule>();
	// This stores the series map offset
	private HashMap<Integer, Integer> time_map = new HashMap<Integer, Integer>();
	// This stores the current offset
	private int curr_level_offset = 0;
	// This stores the current level 
	private int curr_level = -10000;
	
	// This stores the minimum price
	private int min_price = Integer.MAX_VALUE;
	// This stores the maximum price
	private int max_price = -Integer.MAX_VALUE;
	
	private static int PRICE_WIDTH = 10;


	TradingSignals() {
		
	}
	
	// This creates the level set
	private void CreateLevelSet() {
		
		int offset = 0;
		for(double v : series_buff) {
			
			int price = (int) (v * 10000);
			int next_level = price - (price % PRICE_WIDTH);
			offset++;
			
			if(Math.abs(next_level - curr_level) < PRICE_WIDTH) {
				continue;
			}
			
			curr_level = next_level;
			time_map.put(curr_level_offset, offset - 1);
			
			ArrayList<Integer> series = level_map.get(curr_level);
			if(series == null) {
				series = new ArrayList<Integer>();
				level_map.put(curr_level, series);
			}

			price_map.put(curr_level_offset, curr_level);
			series.add(curr_level_offset++);
			level_map.put(curr_level, series);
			
			min_price = Math.min(curr_level, min_price);
			max_price = Math.max(curr_level, max_price);
		}
	}
	
	// This checks the boundary
	private boolean Boundary(Integer offset, int price, int width) {
		
		offset = time_map.get(offset);
		while(offset < series_buff.size()) {
			
			double v = series_buff.get(offset);
			int next_price = (int) (v * 10000);
			next_price = next_price - (next_price % PRICE_WIDTH);

			if(Math.abs(next_price - price) >= (PRICE_WIDTH * width)) {
				return next_price > price;
			}
			
			offset++;
		}
		
		return false;
	}
	
	// This process a given level set for a barrier width
	private void ProcessRuleSet(ArrayList<Integer> set, int width, int price) {
		
		Rule r = new Rule();
		r.fxset = this;
		r.price = price;
		r.width = width;
		
		for(Integer v : set) {
			
			int curr_series = v + 1;
			Integer next_price = null;
			 
			while(true) {
				next_price = price_map.get(curr_series);
				if(next_price == null) {
					break;
				}
				
				if(Math.abs(next_price - price) >= (PRICE_WIDTH * width)) {
					break;
				}
				
				curr_series++;
			}
			
			if(next_price == null) {
				continue;
			}
			
			boolean is_up = Boundary(v, price, width);
			if(is_up != (next_price > price)) {
				System.out.println("errror  "+price+"   "+next_price+"   "+is_up+"   "+v+" "+price_map.get(v + 1));System.exit(0);
			}
			
			int time_diff = time_map.get(curr_series) - time_map.get(v);
			
			Train t = new Train();
			t.window = width;
			t.price = price;
			t.barrier_side = 0;
			t.avg_freq = 0;
			
			if(next_price > price) {
				r.up_num++;
				t.barrier_side = 1;
			} else {
				r.down_num++;
				t.barrier_side = -1;
			}
			
			if(Math.abs(time_diff) > 30) {
				t.barrier_side = 0;
				continue;
				
			}
			
			train_buff.add(t);
		}
		
		if((r.up_num + r.down_num) == 0) {
			return;
		}
		
		rule_set.add(r);
	}
	
	// This process the rule set for each level set
	private void ProcessRuleSet(boolean is_up, int width) {
		
		int offset = 0;
		int prev_rule_size = rule_set.size();
		int curr_price = is_up ? min_price : max_price;
		while(Math.abs(rule_set.size() - prev_rule_size) < 1000) {
			
			if(level_map.containsKey(curr_price)) {
				ProcessRuleSet(level_map.get(curr_price), width, curr_price);
				
				if(++offset >= level_map.size()) {
					break;
				}
			}
			
			if(is_up == true) {
				curr_price += PRICE_WIDTH;
				if(curr_price > 0) {
					break;
				}
			} else {
				curr_price -= PRICE_WIDTH;
				if(curr_price < 0) {
					break;
				}
			}
		}
	}
	
	// This writes the rule set out to file
	private void WriteRuleSet() throws IOException {
		
		DataOutputStream os = new DataOutputStream(new FileOutputStream("rule_set"));
		
		for(Rule r : rule_set) {
			r.WriteRule(os);
			System.out.println(r.up_num+" "+r.down_num+" "+r.price+" "+r.width);
		}
		
		os.close();
	}
	
	public void ProcessSeries(double tseries[][], double weight[]) throws IOException {
		
		
		for(int i=0; i<tseries[0].length; i++) {
			
			double sum = 0;
			for(int j=0; j<weight.length; j++) {
				sum += weight[j] * tseries[j][i];
			}
			
			if(Math.abs(sum) > 0) {
				series_buff.add(sum);
			}
		}
	
		CreateLevelSet();
		
		for(int i=20; i<40; i+=5) {
			ProcessRuleSet(false, i + 1);
			ProcessRuleSet(true, i + 1);
		}
		
		Collections.sort(rule_set);
		
		Classifier c = new Classifier();
		try {
			c.BuildClassifier(train_buff);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
