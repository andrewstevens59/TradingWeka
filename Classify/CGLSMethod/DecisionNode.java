package CGLSMethod;

import java.io.IOException;
import java.util.ArrayList;

import java.util.Random;

import VectorMethod.VectorMethod;

import weka.classifiers.functions.SMO;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.weka.WekaClassifier;

import DecisionNetwork.DataSet;

public class DecisionNode {

	// This stores the left decision node
	private ArrayList<DecisionNode> child_buff;
	// This stores the current compressed node
	private KDNode input_node;
	// This stores the output value
	private boolean output_val;
	// This stores the decision rule table
	public int rule_table[][];
	// This stores the classification error
	public float class_error = 0;
	// This stores the variable id
	public int var_id = 0;
	
	// This stores the current output assignment id
	private static int current_assign_id = 0;
	
	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	// This stores the classifier
	private LDA lda_method = null;
	// This stores the classifier 
	private Classifier javamlsmo = null;
	// This stores the classifier
	private VectorMethod vm = null;
	
	public DecisionNode(KDNode input_node, ArrayList<DecisionNode> children) {
		this.input_node = input_node;
		child_buff = children;
	}
	
	// This returns the set of children 
	public ArrayList<DecisionNode> Children() {
		return child_buff;
	}
	
	// This returns the input node
	public KDNode InputNode() {
		return input_node;
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
		
		
		if(child_buff == null) {
			output_val = input_node.Output(sample);
			return output_val;
		}
		
		int row = 0;
		boolean input[] = new boolean[child_buff.size()];
		double set[] = new double[child_buff.size()];
		for(int i=0; i<child_buff.size(); i++) {
			boolean val = child_buff.get(i).Output(sample);
			
			input[i] = val;
			set[i] = val == true ? 1.0f : -1.0f;
			if(val == true) {
				row |= 1 << i;
			}
		}
		
		if(lda_method != null) {
			return (lda_method.predict(set) == 1) ? true : false; 
		}
		
		if(javamlsmo != null) {
			Boolean obj = (Boolean) javamlsmo.classify(new DenseInstance(set));
			return obj == TRUE_VAL;
		}
		
		if(vm != null) {
			return vm.Classify(input);
		}
		
		output_val = rule_table[row][0] > rule_table[row][1];
		return output_val;
	}

	// This assigns the utility score for the node for different rules 
	public void AssignBestRule(DataSet d, int sample_size, int depth) {
		
		if(child_buff == null) {
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
		
		Random r = new Random();
		Dataset data = new DefaultDataset();
		
		ArrayList<boolean []> data_set = new ArrayList<boolean []>();
		ArrayList<boolean []> output_set = new ArrayList<boolean []>();
		
		int[] group = new int[d.DataSet().size()];
		double[][] data1 = new double[d.DataSet().size()][child_buff.size()];
		
		for(int i=0; i<Math.min(sample_size, d.DataSet().size()); i++) {
			
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			DecisionNode.IncOutputAssignID();
			
			int row = 0;
			double set[] = new double[child_buff.size()];
			boolean in[] = new boolean[child_buff.size()];
			boolean out[] = new boolean[]{output};
			group[i] = (output == true) ? 1 : 2;
			
			for(int j=0; j<child_buff.size(); j++) {
				boolean val = child_buff.get(j).Output(sample);
				if(val == true) {
					row |= 1 << j;
				}
				
				data1[i][j] = val == true ? 1.0f : 0.0f;
				set[j] = val == true ? 1.0f : -1.0f;
				in[j] = val;
				
				if(output == true) {
					data.add(new DenseInstance(set, TRUE_VAL));
				} else {
					data.add(new DenseInstance(set, FALSE_VAL));
				}
			}
			
			data_set.add(in);
			output_set.add(out);

			if(output == true) {
				rule_table[row][0]++;
			} else {
				rule_table[row][1]++;
			}
		}

		/*try {
			lda_method = new LDA(data1, group, true);
			return;
		} catch(RuntimeException e) {	
		}*/
		
		ArrayList<boolean []> test_data_set = new ArrayList<boolean []>();
		ArrayList<boolean []> test_output_set = new ArrayList<boolean []>();
		
		for(int i=0; i<d.TestDataSet().size(); i++) {
			
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.TestDataSet().get(id);
			boolean output = d.TestOutputSet().get(id)[0];
			DecisionNode.IncOutputAssignID();
			
			boolean in[] = new boolean[child_buff.size()];
			boolean out[] = new boolean[]{output};
			
			for(int j=0; j<child_buff.size(); j++) {
				boolean val = child_buff.get(j).Output(sample);
				in[j] = val;
			}
			
			test_data_set.add(in);
			test_output_set.add(out);
		}
		
		DataSet d2 = new DataSet(data_set, output_set, test_data_set, test_output_set);
		vm = new VectorMethod();
		
		try {
			vm.FindClassError(d2, 0, 60);
		} catch (NumberFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        /*try {
	     
	        SMO smo = new SMO();
	        javamlsmo = new WekaClassifier(smo);
	        javamlsmo.buildClassifier(data);
	        
        } catch(net.sf.javaml.tools.weka.WekaException e) {
        	javamlsmo = null;
        }*/
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

		class_error = true_count / d.TestDataSet().size();
		return class_error;
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
}
