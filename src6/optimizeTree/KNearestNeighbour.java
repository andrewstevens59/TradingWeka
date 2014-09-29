package optimizeTree;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import state.State;

public class KNearestNeighbour {

	// This stores a ptr to the left tree node
	KNearestNeighbour left_ptr = null;
	// This stores a ptr to the right tree node
	KNearestNeighbour right_ptr = null;
	// This stores the parent ptr tree node
	KNearestNeighbour parent_ptr = null;
	
	// This stores the split dimension
	int split_dim;
	// This stores the projected dimensions split for which 
	// all states are subdivided upon
	double split_val;
	
	// This stores the complete set of states
	ArrayList<State> state_set = new ArrayList<State>();
	
	// This subdivides the state set by three dimensions. First PCA is used to project
	// each state onto a 3 dimensional subspace and then subdivide each state.
	private boolean SplitStateSet() throws IOException {
		
		
		double max_var = 0;
		split_dim = 0;
		for(int i=0; i<state_set.get(0).v.length; i++) {
			
			double buff[] = new double[state_set.size()];
			for(int j=0; j<state_set.size(); j++) {
				buff[j] = state_set.get(j).v[i];
			}
			
			Statistics stat = new Statistics(buff);
			double var = stat.getVariance();
			if(var > max_var) {
				max_var = var;
				split_dim = i;
			}
		}
		
		if(max_var == 0) {
			System.out.println("zero var");System.exit(0);
		}

		ArrayList<Double> res_set = new ArrayList<Double>();
		for(int j=0; j<state_set.size(); j++) {
			res_set.add(state_set.get(j).v[split_dim]);
		}
		
		Collections.sort(res_set, new Comparator<Double>() {
			 
	        public int compare(Double arg1, Double arg2) {
	        	
	        	if(arg1 < arg2) {
	    			return -1;
	    		}

	    		if(arg1 > arg2) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });

		split_val = res_set.get(res_set.size() >> 1);
		
		if(split_val == res_set.get(0)) {
			for(int i=0; i<res_set.size(); i++) {
				if(res_set.get(i) != split_val) {
					split_val = (split_val + res_set.get(i)) / 2;
				}
			}
		}
		
		if(split_val == res_set.get(res_set.size() - 1)) {
			for(int i=res_set.size()-1; i>=0; i--) {
				if(res_set.get(i) != split_val) {
					split_val = (split_val + res_set.get(i)) / 2;
				}
			}
		}
		
		if(split_val == res_set.get(res_set.size() - 1) || split_val == res_set.get(0)) {
			System.out.println("no split");System.exit(0);
		}
		
		left_ptr = new KNearestNeighbour();
		right_ptr = new KNearestNeighbour();
		left_ptr.parent_ptr = this;
		right_ptr.parent_ptr = this;
	
		return true;
	}
	
	// This is used to split a state among two children
	private void SplitState(State s, boolean is_split) throws IOException {

		if(s.v[split_dim] >= split_val) {
			left_ptr.AddState(s, is_split);
			return;
		}
		
		right_ptr.AddState(s, is_split);
	}

	// This adds a state to the set of states
	public void AddState(State s, boolean is_split) throws IOException {

		if(left_ptr != null) {
			SplitState(s, is_split);
			return;
		}

		if(state_set.size() > 2 && is_split == true && SplitStateSet() == true) {

			for(int i=0; i<state_set.size(); i++) {
				SplitState(state_set.get(i), is_split);
			}
			
			SplitState(s, is_split);
			return;
		}
		
		state_set.add(s);
	}
	
	// This returns the set of K nearest neighbours to a given state
	public void KNearest(State s, ArrayList<State> buff, int num) {
		
		if(left_ptr == null && right_ptr == null) {
			for(int i=0; i<state_set.size(); i++) {
				buff.add(state_set.get(i));
			}
			
			return;
		}
		
		if(num > 0) {
			left_ptr.KNearest(s, buff, num - 1);
			right_ptr.KNearest(s, buff, num - 1);
			return;
		}
		
		if(s.v[split_dim] >= split_val) {
			left_ptr.KNearest(s, buff, num);
			return;
		}
		
		right_ptr.KNearest(s, buff, num);
	}
}
