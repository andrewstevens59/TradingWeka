package PriceSimulation;

import java.util.HashMap;

public class State {

	// This stores the time period
	int time_period;
	// This stores the price of the sample
	int price;
	// This stores the amount of US dollars invested in each trend
	float us_dollar[];
	// This stores the amount of bitcoins invested in each trend
	float bit_coin[];
	// This stores the set of forward links
	StateLink forward_link = null;
	
	// This stores the set of global states
	static HashMap<String, State> state_map = new HashMap<String, State>();
	
	// This returns the state for a given price 
	static State NextState(int price, int time) {
		
		String str = price + " " + time;
		State s = state_map.get(str);
		if(s != null) {
			return s;
		}
		
		s = new State();
		state_map.put(str, s);
		s.time_period = time;
		
		return s;
	}
	
	// This adds a new state
	public State AddState(int price, float gamma) {
		
		State s = State.NextState(price, time_period + 1);
		StateLink prev_ptr = forward_link;
		forward_link = new StateLink();
		forward_link.s = s;
		forward_link.trav_prob = gamma;
		forward_link.next_link = prev_ptr;
		
		return s;
	}
	
	// This is used to reset the state map
	public static void Clear() {
		state_map.clear();
	}
}
