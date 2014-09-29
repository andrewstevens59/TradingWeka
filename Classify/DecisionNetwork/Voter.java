package DecisionNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import Investor.Investor;

public class Voter {

	// This stores the left node
	private Voter left_ptr;
	// This stores the right node
	private Voter right_ptr;
	// This stores the parent of the voter
	private Voter parent_ptr = null;
	// This stores the associated decision node
	public DecisionNode node;
	// This stores the boolean output val
	public boolean output_val;
	// This stores the utility of the classifier
	public float util = 0;
	// This stores the age of the classifier
	public int age = 1;
	
	// This stores the classification error
	public int true_count;
	// This stores the net classification error for this node
	private int net_true_count;
	// This stores the classification error
	public float class_error;
	// This stores the assignment id so node output only assigned once
	private int assign_id = -1;
	// This is a predicate that indicates the whether the node as been decompressed
	public boolean is_decomp = false;
	
	// This stores the set of investors 
	private Set<Investor> investor_set = new HashSet<Investor>();

	// This stores the current set of root nodes
	private static HashMap<Voter, Boolean> root_map = new HashMap<Voter, Boolean>();
	// This stores the set of classification errors
	private static ArrayList<Voter> class_error_buff;
	
	public Voter(DecisionNode node) {
		this.node = node;
		AddRootNode();
	}
	
	// This resets the voter for the next set of dimensions
	public static void ResetVoter() {
		root_map.clear();
		
		if(class_error_buff != null) {
			class_error_buff.clear();
		}
	}
	
	// This adds an investor to the set of investors
	public void AddInvestor(Investor i) {
		investor_set.add(i);
	}
	
	// This removes an investor from the set of investors
	public void RemoveInvestor(Investor i) {
		investor_set.remove(i);
	}
	
	// This returns the number of investors
	public int InvestorNum() {
		return investor_set.size();
	}
	
	// This returns the feature weight preference
	public float[] FeatureWeight() {
		return node.feat_weight;
	}
	
	// This returns the number of root nodes
	public static int RootNodeNum() {
		return root_map.size();
	}
	
	// This returns the root node
	public static Set<Voter> RootNode() {
		return root_map.keySet();
	}
	
	// This returns the utility of the classifier
	public float Utility() {
		return util / age;
	}
	
	// This adds a root node to the set
	public void AddRootNode() {
		root_map.put(this, false);
	}

	
	// This removes this node from the LRU queue
	private void RemoveNodeFromLRU() {
		
		root_map.remove(this);
	}
	
	// This stores the set of classification errors
	public static void CreateClassifierError(DataSet d, int sample_num) {
		
		System.out.println("in");
		for(Voter ptr : RootNode()) {
			ptr.true_count = 0;
		}

		Random r = new Random();
		for(int i=0; i<sample_num; i++) {
			int id = r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			DecisionNode.IncOutputAssignID();

			for(Voter ptr : RootNode()) {
				boolean val = ptr.Output(sample);
				if(val== output) {
					ptr.true_count++;
				}
			}
		}
		
		int avg = 0;
		for(Voter ptr : RootNode()) {
			avg += ptr.true_count;
		}

		avg /= RootNodeNum();
		int net_error = 0;
		class_error_buff = new ArrayList<Voter>();
		for(Voter ptr : RootNode()) {
			net_error += Math.min(avg << 2, ptr.true_count) + 1;
			ptr.net_true_count = net_error;
			class_error_buff.add(ptr);
		}
	}
	
	// This returns the net number of truly classified samples
	public static int NetTruth() {
		return class_error_buff.get(class_error_buff.size()-1).net_true_count;
	}
	
	// This returns a random node weighted by the classification error
	public static Voter NextRandomNode() {
		
		Random r = new Random();
		
		int last = class_error_buff.get(class_error_buff.size()-1).net_true_count;
		int val = r.nextInt(last);
		
		for(int i=0; i<class_error_buff.size(); i++) {
			if(val < class_error_buff.get(i).net_true_count) {
				return class_error_buff.get(i);
			}
		}
		
		return class_error_buff.get(class_error_buff.size()-1);	
	}
	
	// This compresses these two decision nodes 
	public void CompressNodes(Voter neighbour) {
		
		if(this == neighbour) {
			System.out.println("same1");System.exit(0);
		}
		
		if(neighbour.parent_ptr != null) {
			// removes the node from the current location in the hierarchy
			Voter parent = neighbour.parent_ptr;
			neighbour.parent_ptr = null;
			neighbour.RemoveNodeFromLRU();
			
			Voter other = null;
			if(parent.left_ptr == neighbour) {
				other = parent.right_ptr;
				parent.right_ptr = null;
			} else {
				other = parent.left_ptr;
				parent.left_ptr = null;
			}
			
			other.parent_ptr = parent.parent_ptr;
			
			if(other.parent_ptr == other) {
				System.out.println("same");System.exit(0);
			}
			
			if(other.parent_ptr == null) {
				other.AddRootNode();
				
				if(other.left_ptr == null ^ other.right_ptr == null) {
					System.out.println("bo");System.exit(0);
				}
			} else {
				other.RemoveNodeFromLRU();
				
				if(other.parent_ptr.left_ptr == parent) {
					other.parent_ptr.left_ptr = other;
				} else if(other.parent_ptr.right_ptr == parent) {
					other.parent_ptr.right_ptr = other;
				} else {
					System.out.println("error "+other.parent_ptr.left_ptr+" "+other.parent_ptr.right_ptr);System.exit(0);
				}
				
				if(other.parent_ptr.left_ptr == null || other.parent_ptr.right_ptr == null) {
					System.out.println("bo");System.exit(0);
				}
			}
			
			parent.RemoveNodeFromLRU();
		} 
		
		this.RemoveNodeFromLRU();
		neighbour.RemoveNodeFromLRU();

		Voter root = new Voter(null);
		Voter prev_parent = this.parent_ptr;
		
		if(prev_parent != null) {
			if(prev_parent.left_ptr == this) {
				prev_parent.left_ptr = root;
			} else {
				prev_parent.right_ptr = root;
			}
		}
		
		root.parent_ptr = prev_parent;
		
		if(root.parent_ptr == root) {
			System.out.println("same");System.exit(0);
		}
		
		if(root.parent_ptr != null) {
			root.RemoveNodeFromLRU();
		}
		
		root.left_ptr = this;
		root.right_ptr = neighbour;
		root.left_ptr.parent_ptr = root;
		root.right_ptr.parent_ptr = root;
		
		if(root.left_ptr.parent_ptr == root.left_ptr || root.right_ptr.parent_ptr == root.right_ptr) {
			System.out.println("same");System.exit(0);
		}
		
		if(root.left_ptr == null || root.right_ptr == null) {
			System.out.println("bo");System.exit(0);
		}
	}
	
	// This builds the conditional bayesian network 
	public void BuildNetwork(ArrayList<Voter> buff, int num) {
		
		buff.add(new Voter(new DecisionNode(0, buff)));
		
		if(is_decomp == false) {
			buff.add(this);
			return;
		}
		
		if(num == 0) {
			
			ArrayList<Voter> temp = new ArrayList<Voter>();
			left_ptr.BuildNetwork(temp, 2);
			right_ptr.BuildNetwork(temp, 2);
			
			if(temp.size() > 4) {
				System.out.println("boo");System.exit(0);
			}
			
			buff.add(new Voter(new DecisionNode(0, temp)));
		}
		
		left_ptr.BuildNetwork(buff, num - 1);
		right_ptr.BuildNetwork(buff, num - 1);
	}
	
	// This returns the outcome of the voting process
	public void Output(boolean sample[], ArrayList<Boolean> outcome,
			ArrayList<Float> prob_buff) {
		
		if(left_ptr == null && right_ptr == null) {
			outcome.add(node.Output(sample));
			prob_buff.add(node.rule_prob);
			return;
		}
		
		left_ptr.Output(sample, outcome, prob_buff);
		right_ptr.Output(sample, outcome, prob_buff);
	}
	
	// This returns the outcome of the voting process
	public boolean Output(boolean sample[]) {
		
		if(assign_id == DecisionNode.OutputAssignID()) {
			return output_val;
		}
		
		assign_id = DecisionNode.OutputAssignID();
		ArrayList<Boolean> buff = new ArrayList<Boolean>();
		ArrayList<Float> prob_buff = new ArrayList<Float>();
		Output(sample, buff, prob_buff);
		
		float true_count = 0;
		float false_count = 0;
		
		for(int i=0; i<buff.size(); i++) {
			
			if(buff.get(i) == true) {
				true_count += 1;
			} else {
				false_count += 1;
			}
		}
		
		if(buff.size() > 1) {
			class_error += (float)Math.min(true_count, false_count) / Math.max(true_count, false_count);
		} else {
			class_error += node.ClassificationError(sample);
		}
		
		output_val = true_count > false_count;
		return output_val;
	}
	
	// This returns the set of leaf nodes
	public void LeafNode(ArrayList<Voter> buff) {
		if(left_ptr == null && right_ptr == null) {
			buff.add(this);
			return;
		}
		
		left_ptr.LeafNode(buff);
		right_ptr.LeafNode(buff);
	}
	
	// This updates the utility of the classifier
	public void UpdateUtility(boolean sample[], boolean output) {
		boolean val = Output(sample);
		//util *= 0.95f;
		
		if(val == output) {
			util++;
		} else {
			util--;
		}
		
		age++;
	}
	
	// This decompresses the node and adds the children to the root
	public boolean DecompressNode() {
		
		if(left_ptr == null && right_ptr == null) {
			return false;
		}
		
		if(node == null) {
			is_decomp = true;
			RemoveNodeFromLRU();
		} 
		
		left_ptr = null;
		right_ptr = null;
		parent_ptr = null;
		
		
		left_ptr.AddRootNode();
		right_ptr.AddRootNode();
		
		return true;
	}
	
	// This decompresses the parent of the current node
	public boolean DecompressParent() {
		
		Voter ptr = this;
		while(ptr != null) {
			if(root_map.get(ptr) != null) {
				boolean val = DecompressNode();
				return val;
			}
			
			ptr = ptr.parent_ptr;
		}
		
		System.out.println("none");System.exit(0);
		return false;
	}
	
	// This kills the lowest utility investor
	public static void KillClassifier() {
		
		ArrayList<Voter> buff = new ArrayList<Voter>();
		buff.addAll(RootNode());
		
		Collections.sort(buff, new Comparator<Voter>() {
			 
	        public int compare(Voter arg1, Voter arg2) {
	        	
	        	if(arg1.Utility() < arg2.Utility()) {
	    			return -1;
	    		}

	    		if(arg1.Utility() > arg2.Utility()) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		root_map.remove(buff.get(0));
	}

}
