package VectorMethod;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


import java.util.Random;

import Main.DataSet;


public class DecisionNode {

	// This stores the left decision node
	private ArrayList<DecisionNode> child_buff;
	// This stores the input id
	private int input_id;
	// This stores the output value
	private boolean output_val;
	// This stores the decision rule table
	public int rule_table[][];
	// This is a predicate indicating a root node
	public boolean is_root = true;
	// This stores the classification error
	public float class_error = 0;
	
	// This stores the number of true values on the output
	private int true_num = 0;
	// This stores the number of false values on the output
	private int false_num = 0;
	
	// This stores the distribution of input feature weights
	private float feat_weight[];
	

	public DecisionNode(int input_id) {
		this.input_id = input_id;
	}
	
	public DecisionNode(int input_id, ArrayList<DecisionNode> children, int dim_num) {
		this.input_id = input_id;
		child_buff = children;
		
		feat_weight = new float[dim_num];
		for(int i=0; i<feat_weight.length; i++) {
			feat_weight[i] = 0;
		}
		
		if(child_buff == null) {
			feat_weight[input_id] = 1.0f;
			return;
		}
		
		for(int j=0; j<child_buff.size(); j++) {
			DecisionNode n = child_buff.get(j);
			if(Math.max(n.true_num, n.false_num) == 0) {
				System.out.println(n.child_buff);System.exit(0);
			}
			for(int i=0; i<feat_weight.length; i++) {
				feat_weight[i] += (n.feat_weight[i]) / child_buff.size();
			}
		}
		
		float sum = 0;
		for(int i=0; i<feat_weight.length; i++) {
			sum += feat_weight[i];
		}
		
		for(int i=0; i<feat_weight.length; i++) {
			feat_weight[i] /= sum;
		}
	}
	
	// This returns the majority vote on inputs
	public boolean MajorityVote(boolean sample[]) {
		
		float true_count = 0;
		float false_count = 0;
		for(int i=0; i<feat_weight.length; i++) {
			
			boolean val = sample[i];
			
			if(val == true) {
				true_count += feat_weight[i];
			} else {
				false_count += feat_weight[i];
			}
		}
		
		return true_count > false_count;
	}
	
	// This writes a decision node to file 
	public void WriteDecisionNode(DataOutputStream outStream) {
		
		try {
			
			if(child_buff != null) {
				outStream.writeInt(child_buff.size());
				for(int j=0; j<child_buff.size(); j++) {
					child_buff.get(j).WriteDecisionNode(outStream);
				}
			} else {
				outStream.writeInt(0);
			}
			
			outStream.writeInt(input_id);
			
			if(rule_table != null) {
				outStream.writeInt(rule_table.length);
				
				for(int i=0; i<rule_table.length; i++) {
					outStream.writeInt(rule_table[i][0]);
					outStream.writeInt(rule_table[i][1]);
				}
			} else {
				outStream.writeInt(0);
			}
			
			outStream.writeInt(feat_weight.length);
			for(int i=0; i<feat_weight.length; i++) {
				outStream.writeFloat(feat_weight[i]);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// This writes a decision node to file 
	public static DecisionNode ReadDecisionNode(DataInputStream inStream) throws IOException {
		
		int child_num = inStream.readInt();

		ArrayList<DecisionNode> child_set = null;
		if(child_num > 0) {
			child_set = new ArrayList<DecisionNode>();
		}
		
		for(int i=0; i<child_num; i++) {
			child_set.add(ReadDecisionNode(inStream));
		}
		
		int input_id = inStream.readInt();
		DecisionNode dn =  new DecisionNode(input_id);
		dn.child_buff = child_set;
		
		int table_length = inStream.readInt();
		dn.rule_table = new int[table_length][2];
		for(int i=0; i<table_length; i++) {
			dn.rule_table[i][0] = inStream.readInt();
			dn.rule_table[i][1] = inStream.readInt();
		}
		
		int feat_length = inStream.readInt();
		dn.feat_weight = new float[feat_length];

		for(int i=0; i<feat_length; i++) {
			dn.feat_weight[i] = inStream.readFloat();
		}
		
		return dn;
	}
	
	// This returns the truth value for the node
	public boolean Output(boolean sample[]) {
		
		/*if(assign_id == OutputAssignID()) {
			return output_val;
		}*/
		
		if(child_buff == null) {
			output_val = MajorityVote(sample);
			return output_val;
		}
		
		int row = 0;
		for(int i=0; i<child_buff.size(); i++) {
			boolean val = child_buff.get(i).MajorityVote(sample);
			if(val == true) {
				row |= 1 << i;
			}
		}
		
		if(rule_table[row][0] == rule_table[row][1]) {
			output_val = MajorityVote(sample);
		} else {
			output_val = rule_table[row][0] > rule_table[row][1];
		}
		
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
	public void AssignBestRule(DataSet d, int sample_size, int depth) {
		
		if(child_buff == null) {
			true_num = 1;
			false_num = 1;
			return;
		}
		
		if(depth == 0) {
			return;
		}
		
		if(rule_table != null) {
			return;
		}

		for(int i=0; i<child_buff.size(); i++) {
			child_buff.get(i).AssignBestRule(d, sample_size, depth - 1);
		}
		
		if(rule_table == null) {
			rule_table = new int [1 << child_buff.size()][2];
			for(int i=0; i<rule_table.length; i++) {
				rule_table[i][0] = 0;
				rule_table[i][1] = 0;
			}
		}

		true_num = 0;
		false_num = 0;
		for(int i=0; i<Math.min(sample_size, d.DataSet().size()); i++) {
			
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			
			int row = 0;
			for(int j=0; j<child_buff.size(); j++) {
				boolean val = child_buff.get(j).MajorityVote(sample);
				if(val == true) {
					row |= 1 << j;
				}
			}

			if(output == true) {
				rule_table[row][0]++;
				true_num++;
			} else {
				rule_table[row][1]++;
				false_num++;
			}
		}
		
		if(Math.max(true_num, false_num) == 0) {
			System.out.println("ko");System.exit(0);
		}
	}
	
	// This prints the rule table
	public void PrintRule() {
		
		System.out.println("**************");
		for(int i=0; i<rule_table.length; i++) {
			System.out.println(rule_table[i][0]+" "+rule_table[i][1]);
		}
		
		System.out.println("**************");
	}
	
	// This assigns the test error
	public float TestError(DataSet d) {
		
		float true_count = 0;
		for(int i=0; i<d.TestDataSet().size(); i++) {
			
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.TestDataSet().get(id);
			boolean output = d.TestOutputSet().get(id)[0];

			if(output == Output(sample)) {
				true_count++;
			} 
		}

		return true_count / d.TestDataSet().size();
	}
	
	// This assigns the class error 
	public void AssignClassError(DataSet d, int sample_size) {
		
		class_error = 0;
		for(int i=0; i<Math.min(sample_size, d.TestDataSet().size()); i++) {
			
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.TestDataSet().get(id);
			boolean output = d.TestOutputSet().get(id)[0];

			if(output == Output(sample)) {
				class_error++;
			} else {
				class_error--;
			}
		}

		class_error /= Math.min(sample_size, d.TestDataSet().size());
	}
	
	// This returns the feature vector
	public float [] StandardFeatVec() {
		return feat_weight;
	}
}
