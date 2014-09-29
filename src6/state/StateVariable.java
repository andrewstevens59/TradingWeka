package state;

import java.util.HashMap;

public class StateVariable {

	// This stores the mapping between a state and the state variable
	static HashMap<String, Double> state_var_map = new HashMap<String, Double>();
	// This is used to store the static assignment of variables
	static HashMap<Integer, Double> static_var_map = new HashMap<Integer, Double>();
	
	// This adds a static variable mapping
	public static void AddStaticVariable(int var_id) {
		static_var_map.put(var_id, (double) 0);
	}
	
	// This clears the variable set
	public static void Clear() {
		state_var_map.clear();
		static_var_map.clear();
	}
	
	// This assigns a state variable 
	public static void AssignStateVar(State state, int var_id, double val) {
		
		if(static_var_map.get(var_id) != null) {
			//System.out.println("Assign Static Var: "+var_id+" "+val+"  ************************");
			static_var_map.put(var_id, val);
			return;
		}
 
		String str = var_id + " " + state.toString();
		//System.out.println("Assign Var: "+str+" "+val+"  ************************");
		state_var_map.put(str, val);
		
		AssignVar prev_ptr = state.assign_ptr;
		state.assign_ptr = new AssignVar();
		state.assign_ptr.var_id = var_id;
		state.assign_ptr.val = val;
		state.assign_ptr.next_ptr = prev_ptr;
	}
	
	// This retrieves the value for a state variable
	public static double StateVar(State state, int var_id) {
		
		Double val = static_var_map.get(var_id);
		if(val != null) {
		//	System.out.println("Get Static Var: "+var_id+" "+val+"  ************************");
			return val;
		}

		int count = 0;
		String str = var_id + " " + state.toString();
	
		State s = state;
		//System.out.println("Get Var: "+str);
		val = state_var_map.get(str);

		while(val == null) {
			
			StateLink ptr = s.backward_link;
			while(ptr.next_ptr != null) {
				ptr = ptr.next_ptr;
			}
			
			s = ptr.s;
			str = var_id + " " + s.toString();
			val = state_var_map.get(str);
			count++;
		}
		
		if(count >= 10) {
			// caches the variable for later recall
			AssignStateVar(state, var_id, val);
		}
		
		return val;
	}

}
