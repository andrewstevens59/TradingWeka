package KDTree;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import weka.classifiers.functions.SMO;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;

import DecisionNetwork.Voter;

public class KDNode {
	
	class Table {
		int count[][] = new int[][]{{0, 0}, {0, 0}};
		int match;
		int dim;
	}

	// This stores the left child
	private KDNode left_ptr = null;
	// This stores the right child
	private KDNode right_ptr = null;
	// This stores the split dim
	private int split_dim;
	// This stores the set of ranked split dimensions
	private ArrayList<Table> table_buff;
	// This stores the classifier 
	private Classifier javamlsmo = null;
	

	// This stores the boolean table
	private int rule_table[][];
	
	// This stores the maximum dimension number
	static int MAX_DIM_NUM = 4;
	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	
	// This constructs the classifier
	private void BuildClassifier(ArrayList<boolean []> data_set, ArrayList<boolean []> output_set) throws IOException {
		
		table_buff = new ArrayList<Table>();
		for(int j=0; j<data_set.get(0).length; j++) {
			
			Table t = new Table();
			table_buff.add(t);

			for(int i=0; i<data_set.size(); i++) {
				int output = output_set.get(i)[0] == true ? 1 : 0;
				int input = data_set.get(i)[j] == true ? 1 : 0;
				t.count[input][output]++;
			}
			
			t.match = Math.max(t.count[0][0], t.count[0][1]) * Math.max(t.count[1][0], t.count[1][1]);
			t.dim = j;
		}
		
		Collections.sort(table_buff, new Comparator<Table>() {
			 
	        public int compare(Table arg1, Table arg2) {
	        	
	        	if(arg1.match < arg2.match) {
	    			return 1;
	    		}

	    		if(arg1.match > arg2.match) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		rule_table = new int[1 << Math.min(table_buff.size(), MAX_DIM_NUM)][2];
		for(int i=0; i<rule_table.length; i++) {
			rule_table[i][0] = 0;
			rule_table[i][1] = 0;
		}
		
		for(int i=0; i<data_set.size(); i++) {
			
			int val = 0;
			boolean sample[] = data_set.get(i);
			for(int j=0; j<Math.min(table_buff.size(), MAX_DIM_NUM); j++) {
				val |= (sample[table_buff.get(j).dim] == true ? 1 : 0) << j;
			}
			
			if(output_set.get(i)[0] == true) {
				rule_table[val][0]++;
			} else {
				rule_table[val][1]++;
			}
		}
		
		Dataset data = new DefaultDataset();
		for(int j=0; j<data_set.size(); j++) {
			
			boolean sample[] = data_set.get(j);
			boolean output = output_set.get(j)[0];
			
			double val[] = new double[sample.length];
			for(int i=0; i<sample.length; i++) {
				val[i] = sample[i] == true ? 1.0f : 0.0f;
			}
			
			if(output == true) {
				data.add(new DenseInstance(val, TRUE_VAL));
			} else {
				data.add(new DenseInstance(val, FALSE_VAL));
			}
		}
		
        try {
	     
	        SMO smo = new SMO();
	        javamlsmo = new WekaClassifier(smo);
	        javamlsmo.buildClassifier(data);
	        
        } catch(net.sf.javaml.tools.weka.WekaException e) {
        	javamlsmo = null;
        }
		
	}
	
	public KDNode(ArrayList<boolean []> data_set, ArrayList<boolean []> output_set) throws IOException {
		
		if(data_set.size() < 1000) {
			BuildClassifier(data_set, output_set);
			return;
		}
		
		
		int max = 0;
		split_dim = 0;
		
		for(int j=0; j<data_set.get(0).length; j++) {
			
			Table t = new Table();

			for(int i=0; i<data_set.size(); i++) {
				int output = output_set.get(i)[0] == true ? 1 : 0;
				int input = data_set.get(i)[j] == true ? 1 : 0;
				t.count[input][output]++;
			}
			
			t.match = Math.max(t.count[0][0], t.count[0][1]) * Math.max(t.count[1][0], t.count[1][1]);
			if(t.match > max) {
				max = t.match;
				split_dim = j;
			}
		}
		
		ArrayList<boolean []> data_set1 = new ArrayList<boolean []>();
		ArrayList<boolean []> output_set1 = new ArrayList<boolean []>();
		
		ArrayList<boolean []> data_set2 = new ArrayList<boolean []>();
		ArrayList<boolean []> output_set2 = new ArrayList<boolean []>();
		
		for(int i=0; i<data_set.size(); i++) {
			if(data_set.get(i)[split_dim] == true) {
				data_set1.add(data_set.get(i));
				output_set1.add(output_set.get(i));
			} else {
				data_set2.add(data_set.get(i));
				output_set2.add(output_set.get(i));
			}
		}
		
		if(Math.min(data_set1.size(), data_set2.size()) < 10) {
			BuildClassifier(data_set, output_set);
			return;
		}
		
		left_ptr = new KDNode(data_set1, output_set1);
		right_ptr = new KDNode(data_set2, output_set2);
	}

	// This returns the label for a sample
	public boolean Output(boolean sample[]) {
		
		if(left_ptr == null && right_ptr == null) {
			
			if(javamlsmo != null) {
				
				double val[] = new double[Math.min(table_buff.size(), MAX_DIM_NUM)];
				for(int j=0; j<val.length; j++) {
					val[j] = sample[table_buff.get(j).dim] == true ? 1 : 0;
				}

				Boolean obj = (Boolean) javamlsmo.classify(new DenseInstance(val));

				System.out.println((TRUE_VAL == obj)+" ***");
				return obj == TRUE_VAL;
			}
			
			int val = 0;
			for(int j=0; j<Math.min(table_buff.size(), MAX_DIM_NUM); j++) {
				val |= (sample[table_buff.get(j).dim] == true ? 1 : 0) << j;
			}
			
		//	System.out.println(rule_table[val][0]+" "+rule_table[val][1]+" ***");
			return rule_table[val][0] > rule_table[val][1];
		}
		
		if(sample[split_dim] == true) {
			return left_ptr.Output(sample);
		} 
			
		return right_ptr.Output(sample);
	}
}
