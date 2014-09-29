package DecisionNetwork;

import java.util.ArrayList;
import java.util.Random;

public class DecisionNode {

	// This stores the left decision node
	private ArrayList<Voter> child_buff;
	// This stores the input id
	private int input_id;
	// This stores the output value
	private boolean output_val;
	// This stores the depth of the node
	public int node_depth = 0;
	// This stores the decision rule table
	public int rule_table[][];
	// This is a predicate indicating a root node
	public boolean is_root = true;
	// This stores the prediction weight
	public float predict_weight;
	// This stores the assignment id so node output only assigned once
	private int assign_id = -1;
	
	// This stores the confidence with a rule
	public float rule_prob = 0;
	
	// This stores the current output assignment id
	private static int current_assign_id = 0;
	
	// This stores the training data
	private static DataSet d;
	// This stores the the number of training samples
	private static int train_num;
	// This stores the distribution of input feature weights
	public float feat_weight[];
	// This stores the set of root nodes for the input set
	public KDNode input_buff[];
	
	public DecisionNode(int input_id, ArrayList<Voter> children) {
		this.input_id = input_id;
		child_buff = children;
		this.input_buff = KDNode.RootBuff();
		
		feat_weight = new float[KDNode.DimNum()];
		for(int i=0; i<feat_weight.length; i++) {
			feat_weight[i] = 0;
		}
		
		if(child_buff == null) {
			feat_weight[input_id] = 1.0f;
			return;
		}
		
		for(int j=0; j<child_buff.size(); j++) {
			KDNode.MapFeatureWeight(child_buff.get(j).node.input_buff, input_buff, 
					child_buff.get(j).node.feat_weight, feat_weight, 1.0f / child_buff.size());
		}
		
		float sum = 0;
		for(int i=0; i<feat_weight.length; i++) {
			sum += feat_weight[i];
		}
		
		for(int i=0; i<feat_weight.length; i++) {
			feat_weight[i] /= sum;
		}
	}
	
	// This sets the data set and training samples
	public static void SetTraining(DataSet d1, int train_num1) {
		d = d1;
		train_num = train_num1;
	}
	
	// This returns the current assignment id
	public static int OutputAssignID() {
		return current_assign_id;
	}
	
	// This increments the current assignment id
	public static void IncOutputAssignID() {
		current_assign_id++;
	}
	
	// This returns the truth value for the node
	public boolean Output(boolean sample[]) {
		
		if(assign_id == OutputAssignID()) {
			return output_val;
		}
		
		assign_id = OutputAssignID();
		if(child_buff == null) {
			output_val = KDNode.ClassifySample(input_buff[input_id], sample);
			return output_val;
		}
		
		int row = 0;
		for(int i=0; i<child_buff.size(); i++) {
			
			if(child_buff.get(i).is_decomp == true) {
				ArrayList<Voter> buff = new ArrayList<Voter>();
				child_buff.get(i).BuildNetwork(buff, 2);
				
				if(buff.size() > 4) {
					System.out.println("boo");System.exit(0);
				}
				
				Voter v = new Voter(new DecisionNode(0, buff));
				
				IncOutputAssignID();
				v.node.AssignBestRule(d, train_num, 4);
				child_buff.set(i, v);
			}
			
			boolean val = child_buff.get(i).Output(sample);
			if(val == true) {
				row |= 1 << i;
			}
		}

		rule_prob = (float)Math.max(rule_table[row][0], rule_table[row][1]) 
				/ (Math.min(rule_table[row][0], rule_table[row][1]) + 1);
		output_val = rule_table[row][0] > rule_table[row][1];
		return output_val;
	}
	
	// This creates a table 
	public void CreateTable(int val, int output_num) {
		
		if(rule_table == null || rule_table.length != output_num) {
			rule_table = new int[output_num][2];
		}
		
		for(int i=0; i<output_num; i++) {
			if((val & 0x01) == 0x01) {
				rule_table[i][0] = 1;
				rule_table[i][1] = 0;
			} else {
				rule_table[i][0] = 0;
				rule_table[i][1] = 1;
			}
			
			val >>= 1;
		}
	}
	
	// THis returns the classification error
	public float ClassificationError(boolean sample[]) {
		
		if(child_buff == null) {
			return 0;
		}
		
		int row = 0;
		for(int i=0; i<child_buff.size(); i++) {
			boolean val = child_buff.get(i).Output(sample);
			if(val == true) {
				row |= 1 << i;
			}
		}
		
		if(Math.max(rule_table[row][0], rule_table[row][1]) == 0) {
			// this row has not been assigned
			return 1.0f;
		}

		return (float)Math.min(rule_table[row][0], rule_table[row][1]) 
				/ Math.max(rule_table[row][0], rule_table[row][1]);
	}
	
	// This checks if the rule table has been assigned
	public boolean IsTableEmpty() {
		
		
		if(rule_table == null) {
			return true;
		}
		
		int sum = 0;
		for(int i=0; i<rule_table.length; i++) {
			sum += rule_table[i][0] + rule_table[i][1];
		}
		
		return sum == 0;
	}

	// This assigns the utility score for the node for different rules 
	public float AssignBestRule(DataSet d, int sample_size, int depth) {
		
		if(child_buff == null) {
			return 0;
		}
		
		if(depth == 0) {
			return 0;
		}
		
		if(rule_table != null) {
			return 0;
		}
		
		if(assign_id == OutputAssignID()) {
			return 0;
		}
		
		assign_id = OutputAssignID();

		for(int i=0; i<child_buff.size(); i++) {

			ArrayList<Voter> buff = new  ArrayList<Voter>();
			child_buff.get(i).LeafNode(buff);
			for(int j=0; j<buff.size(); j++) {
				buff.get(j).node.AssignBestRule(d, sample_size, depth - 1);
			}
		}
		
		if(rule_table == null) {
			rule_table = new int [1 << child_buff.size()][2];
			for(int i=0; i<rule_table.length; i++) {
				rule_table[i][0] = 0;
				rule_table[i][1] = 0;
			}
		}
		
		Random r = new Random();
		for(int i=0; i<Math.min(sample_size, d.DataSet().size()); i++) {
			
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			DecisionNode.IncOutputAssignID();
			
			int row = 0;
			for(int j=0; j<child_buff.size(); j++) {
				boolean val = child_buff.get(j).Output(sample);
				if(val == true) {
					row |= 1 << j;
				}
			}

			if(output == true) {
				rule_table[row][0]++;
			} else {
				rule_table[row][1]++;
			}

		}
		
		float predict = 0;
		for(int i=0; i<rule_table.length; i++) {
			predict += ((float)Math.max(rule_table[i][0], rule_table[i][1]) + 1) / ((float)Math.min(rule_table[i][0], rule_table[i][1]) + 1);
		}
		
		return predict / rule_table.length;
	}
}
