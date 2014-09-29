package DecisionNetwork;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
		int dim1;
		int dim2;
	}

	// This stores the left child
	private KDNode left_ptr = null;
	// This stores the right child
	private KDNode right_ptr = null;
	// This stores the parent ptr
	private KDNode parent_ptr = null;
	// This stores the split dim
	private int split_dim;
	// This stores the set of root nodes
	private static HashSet<KDNode> root_map = new HashSet<KDNode>();
	// This stores the preference weight
	private float pref_weight;
	// This stores the dimension set
	private ArrayList<Integer> dim_buff = new ArrayList<Integer>();
	
	// This stores the number of true attributes
	private int true_num = 0;
	// This stores the number of false attributes
	private int false_num = 0;
	// This stores the maximum dimension number
	static int MAX_DIM_NUM = 4;
	
	
	public KDNode(ArrayList<boolean []> data_set, ArrayList<boolean []> output_set, KDNode parent) throws IOException {
		
		if(data_set.size() < 50) {
			root_map.add(this);
			return;
		}
		
		
		int max = 0;
		split_dim = 0;
		
		ArrayList<Table> table_buff = new ArrayList<Table>();
		for(int j=0; j<data_set.get(0).length; j++) {
			
			for(int i=j+1; i<data_set.get(0).length; i++) {
			
				Table t = new Table();
				table_buff.add(t);
				for(int k=0; k<data_set.size(); k++) {
					int input1 = data_set.get(k)[j] == true ? 1 : 0;
					int input2 = data_set.get(k)[i] == true ? 1 : 0;
					t.count[input1][input2]++;
				}
			}
				
		}
		
		if(parent != null) {
			for(int i=0; i<parent.dim_buff.size(); i++) {
				dim_buff.add(parent.dim_buff.get(i));
			}
		}
		
		dim_buff.add(split_dim);
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
			root_map.add(this);
			return;
		}
		
		left_ptr = new KDNode(data_set1, output_set1, this);
		right_ptr = new KDNode(data_set2, output_set2, this);
		
		left_ptr.parent_ptr = this;
		right_ptr.parent_ptr = this;
	}
	
	// This returns the current set of root nodes
	public static KDNode[] RootBuff() {

		 int offset = 0;
		 KDNode[] buff = new KDNode[root_map.size()];
		 for(KDNode k : root_map) {
			 buff[offset++] = k;
		 }
		 
		 return buff;
	}
	
	// This stores the number of dimensions
	public static int DimNum() {
		return root_map.size();
	}

	// This removes the root node below a node
	private void RemoveRootNode() {
		
		root_map.remove(this);
		if(left_ptr == null && right_ptr == null) {
			return;
		}
		
		left_ptr.RemoveRootNode();
		right_ptr.RemoveRootNode();
	}
	
	// This compresses the node
	private void CompressNode() {
		
		int count = 0;
		KDNode ptr = this;
		while(ptr != null) {
			count++;
			ptr = ptr.parent_ptr;
		}
		
		if(count < 6) {
			return;
		}
		
		if(parent_ptr == null) {
			return;
		}
		
		root_map.add(parent_ptr);
		parent_ptr.left_ptr.RemoveRootNode();
		parent_ptr.right_ptr.RemoveRootNode();
	}
	
	// This decompresses the node
	private void DecompressNode() {
		
		if(left_ptr == null && right_ptr == null) {
			return;
		}
		
		root_map.remove(this);
		root_map.add(left_ptr);
		root_map.add(right_ptr);
	}
	
	// This updates the true false count for an attribute
	public void UpdateTruth(boolean input) {
		
		KDNode ptr = this;
		while(root_map.contains(this) == false) {
			ptr = ptr.parent_ptr;
		}
		
		if(input == true) {
			ptr.true_num++;
		} else {
			ptr.false_num++;
		}
	}
	
	// This classifies the sample 
	public boolean[] ClassifySample1(boolean sample[]) {
		
		if(root_map.contains(this) == true) {
			
			HashSet<Integer> map = new HashSet<Integer>();
			for(int i=0; i<dim_buff.size(); i++) {
				map.add(i);
			}
			
			true_num = 0;
			false_num = 0;
			
			boolean output[] = new boolean[root_map.size()];
			for(int i=0; i<dim_buff.size(); i++) {
				if(map.contains(i) == true) {
					output[i] = false;
					continue;
				}
				
				output[i] = sample[i];
			}
			
			return output;
		}
		
		if(sample[split_dim] == true) {
			return left_ptr.ClassifySample(sample);
		}
		
		return right_ptr.ClassifySample(sample);
	}
	
	// This returns the Boolean value for a sample
	public static boolean ClassifySample(KDNode k, boolean sample[]) {
		
		k.true_num = 0;
		k.false_num = 0;
		
		HashSet<Integer> map = new HashSet<Integer>();
		for(int i=0; i<k.dim_buff.size(); i++) {
			
			if(map.contains(k.dim_buff.get(i)) == true) {
				System.out.println("dim exists");System.exit(0);
			}
			
			map.add(k.dim_buff.get(i));
			if(sample[k.dim_buff.get(i)] == true) {
				k.true_num++;
			} else {
				k.false_num++;
			}
		}
		
		return k.true_num > k.false_num;
	}
	
	// This classifies the sample 
	public boolean[] ClassifySample(boolean sample[]) {
		
		int offset = 0;
		boolean output[] = new boolean[root_map.size()];
		for(KDNode k : root_map) {
			
			
			output[offset++] = ClassifySample(k, sample);
		}
		
		return output;
	}
	
	// This checks if something is a parent of
	private float IsParent(KDNode n) {
		
		float factor = 1.0f;
		KDNode ptr = this;
		while(ptr != null) {
			
			if(ptr == n) {
				return factor;
			}
			
			ptr = ptr.parent_ptr;
			factor *= 0.5f;
		}
		
		return 0;
	}
	
	// This maps the old feature weight to the new feature weight
	public static void MapFeatureWeight(KDNode old_buff[], KDNode new_buff[],
			float old_feat[], float new_feat[], float weight) {
		
		for(int i=0; i<new_buff.length; i++) {
			for(int j=0; j<old_buff.length; j++) {
				
				float factor1 = new_buff[i].IsParent(old_buff[j]);
				float factor2 = old_buff[j].IsParent(new_buff[i]);
				
				new_feat[i] += old_feat[j] * factor1 * weight;
				new_feat[i] += old_feat[j] * factor2 * weight;
			}
		}
	}
	
	// This finds the distance between two preference weights
	public static float PrefDistWeight(KDNode buff1[], KDNode buff2[],
			float feat1[], float feat2[]) {
		
		System.out.println("in");
		float dist = 0;
		for(int i=0; i<buff1.length; i++) {
			for(int j=0; j<buff2.length; j++) {
				
				float factor1 = buff1[i].IsParent(buff2[j]);
				float factor2 = buff2[j].IsParent(buff1[i]);
				
				dist += Math.abs(feat1[i] - feat2[j]) * factor1;
				dist += Math.abs(feat1[i] - feat2[j]) * factor2;
			}
		}
		
		System.out.println("out");
		return dist / (buff1.length * buff2.length);
	}
	
	// This updates the compressions correspondingly
	public static void UpdateCompression(float weight[]) {
		
		int offset = 0;
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		for(KDNode k : root_map) {
			buff.add(k);
			k.pref_weight = weight[offset++];
		}
		
		if(weight.length != root_map.size()) {
			System.out.println("root mis "+root_map.size());System.exit(0);
		}
		
		// biggest to smallest
		Collections.sort(buff, new Comparator<KDNode>() {
			 
	        public int compare(KDNode arg1, KDNode arg2) {
	        	
	        	if(arg1.pref_weight < arg2.pref_weight) {
	    			return 1;
	    		}

	    		if(arg1.pref_weight > arg2.pref_weight) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		buff.get(0).DecompressNode();
		buff.get(1).DecompressNode();
		
		// smallest to biggest
		Collections.sort(buff, new Comparator<KDNode>() {
			 
	        public int compare(KDNode arg1, KDNode arg2) {
	        	
	        	if(arg1.pref_weight < arg2.pref_weight) {
	    			return -1;
	    		}

	    		if(arg1.pref_weight > arg2.pref_weight) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<buff.size(); i++) {
			if(root_map.size() <= 16) {
				break;
			}
			
			KDNode ptr = buff.get(i);
			while(ptr != null) {
				if(root_map.contains(ptr) == true) {
					break;
				}
				ptr = ptr.parent_ptr;
			}
			
			if(ptr != null) {
				ptr.CompressNode();
			}
		}
		
		for(KDNode k : root_map) {
			System.out.print(k+" ");
		}
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
