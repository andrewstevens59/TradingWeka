package Investor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import DecisionNetwork.KDNode;
import DecisionNetwork.Voter;

public class Investor {
	
	// This defines a possible classifier to be selected
	class Classifier {
		Voter v;
		float weight;
	}

	// This stores the feature set preference
	private float pref_weight[];
	// This stores the current classifier for the investor
	private Voter curr_voter;
	// This stores the current utility of the investor
	private float util = 1;
	// This stores the life span of the investor
	private int age = 0;
	// This stores the system clock gap
	private int system_clock = 0;
	// This stores the set of root nodes for the input set
	private KDNode input_buff[];
	
	// This stores the global set of investors
	private static Set<Investor> investor_set = new HashSet<Investor>();
	// This stores the random selector factor
	private static float RANDOM_FACT = 0.35f;
	
	public Investor(int dim_num, Voter v, int system_clock) {
		
		float sum = 0;
		curr_voter = v;
		investor_set.add(this);
		v.AddInvestor(this);
		
		this.input_buff = KDNode.RootBuff();

		pref_weight = new float[dim_num];
		this.system_clock = system_clock;
		
		if(investor_set.size() < 1000) {
			for(int i=0; i<dim_num; i++) {
				pref_weight[i] = (float) Math.random();
				sum += pref_weight[i];
			}
		} else {
			
			Random r = new Random();
			Investor inv = Investor.ChooseInvestor();
			
			for(int i=0; i<dim_num; i++) {
				pref_weight[i] = inv.pref_weight[i];
				pref_weight[i] += (Math.random() - 0.5) * 0.01f;
				pref_weight[i] = Math.max(0, pref_weight[i]);
				sum += pref_weight[i];
			}
			
			//this.util = inv.util;
			//this.age = inv.age;
			this.system_clock = inv.system_clock;
		}
		
		for(int i=0; i<dim_num; i++) {
			pref_weight[i] /= sum;
		}
	}
	
	// This resets the investor for the next set of dimensions
	public static void ResetInvestor() {
		investor_set.clear();
	}
	
	// This returns the total number of investors
	static public int InvestorNum() {
		return investor_set.size();
	}
	
	// This returns the set of investors
	static public Set<Investor> InvestorSet() {
		return investor_set;
	}

	// This updates the utility of the investor
	public void UpdateUtil(boolean sample[], boolean output) {
		boolean val = curr_voter.Output(sample);
		
		if(val == output) {
			util++;
		} else {
			util--;
		}
		
		age++;
	}
	
	// This returns the utility of the investor
	public float Util() {
		return util / age;
	}
	
	
	// This chooses a random utility with a bias given to fitter investors
	public static Investor ChooseInvestor() {
		
		ArrayList<Investor> buff = new ArrayList<Investor>();
		for(Investor i : investor_set) {
			buff.add(i);
		}
		
		Collections.sort(buff, new Comparator<Investor>() {
			 
	        public int compare(Investor arg1, Investor arg2) {
	        	
	        	if(arg1.Util() < arg2.Util()) {
	    			return 1;
	    		}

	    		if(arg1.Util() > arg2.Util()) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		int id = -(int) (Math.log(1-Math.random()) * buff.size() * RANDOM_FACT);
		id = Math.min(buff.size() - 1, id);
		return buff.get(id);
	}
	
	// This chooses the classifier for a given investor
	public Voter ChooseClassifier(Set<Voter> set) {
		
		ArrayList<Classifier> buff = new ArrayList<Classifier>();
		
		for(Voter v : set) {
			Classifier c = new Classifier();
			c.v = v;
			
			float dot = KDNode.PrefDistWeight(input_buff, v.node.input_buff,
					v.node.feat_weight, pref_weight);
			
			c.weight = (v.InvestorNum() + 1) / dot;
			buff.add(c);
		}
		
		Collections.sort(buff, new Comparator<Classifier>() {
			 
	        public int compare(Classifier arg1, Classifier arg2) {
	        	
	        	if(arg1.weight < arg2.weight) {
	    			return 1;
	    		}

	    		if(arg1.weight > arg2.weight) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		int id = -(int) (Math.log(1-Math.random()) * buff.size() * RANDOM_FACT);
		id = Math.min(buff.size() - 1, id);
		return buff.get(id).v;
	}
	
	// This updates the classifier for this investor
	public void UpdateInvestorClassifier(Set<Voter> set) {
		
		float val = -(float) (Math.log(1-Math.random()));
		if(val < system_clock) {
			//return;
		}
		
		ArrayList<Classifier> buff = new ArrayList<Classifier>();

		for(Voter v : set) {
			Classifier c = new Classifier();
			c.v = v;
			
			
			float dot = KDNode.PrefDistWeight(input_buff, v.node.input_buff,
					v.node.feat_weight, pref_weight);
			
			c.weight = v.Utility() / dot;
			buff.add(c);
		}
		
		Collections.sort(buff, new Comparator<Classifier>() {
			 
	        public int compare(Classifier arg1, Classifier arg2) {
	        	
	        	if(arg1.weight < arg2.weight) {
	    			return 1;
	    		}

	    		if(arg1.weight > arg2.weight) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		int id = -(int) (Math.log(1-Math.random()) * buff.size() * RANDOM_FACT);
		id = Math.min(buff.size() - 1, id);
		curr_voter.RemoveInvestor(this);
		
		buff.get(id).v.AddInvestor(this);
		curr_voter = buff.get(id).v;
	}
	
	// This kills the lowest utility investor
	public static void KillInvestor() {
		
		ArrayList<Investor> buff = new ArrayList<Investor>();
		buff.addAll(investor_set);
		
		Collections.sort(buff, new Comparator<Investor>() {
			 
	        public int compare(Investor arg1, Investor arg2) {
	        	
	        	if(arg1.util < arg2.util) {
	    			return -1;
	    		}

	    		if(arg1.util > arg2.util) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<buff.size(); i++) {
			if(buff.get(i).age > 10) {
				investor_set.remove(buff.get(0));
				break;
			}
		}
	}
}
