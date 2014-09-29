package CGLSMethod;

import java.io.File;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import weka.classifiers.functions.SMO;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;

import DecisionNetwork.DataSet;
import DecisionNetwork.Voter;

public class KDNode {

	// This stores the left child
	private KDNode left_ptr = null;
	// This stores the right child
	private KDNode right_ptr = null;
	// This stores the parent ptr
	private KDNode parent_ptr = null;
	// This stores the sample dimension
	private int input_dim;
	// This stores the corresponding output for this node
	public DecisionNode dec_node = null;
	// This stores the set of active edges
	private static boolean link_set_map[][];
	// This indicates that a node has been set
	private boolean is_node_set = false;
	// This stores the correlation between two clusters
	private float node_corr;
	// This stores the set of root nodes
	private static HashSet<KDNode> root_map = new HashSet<KDNode>();
	// This stores the set of leaf nodes
	private ArrayList<KDNode> leaf_buff = null;
	// This stores the outcome for a previously seen sample
	private HashMap<boolean [],Boolean> outcome_map = new HashMap<boolean [],Boolean>();
	// This stores the set of leaf decision nodes
	private static ArrayList<DecisionNode> decision_buff = new ArrayList<DecisionNode>();
	// This stores the best classification error
	public static float best_class_error = 0;
	
	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	// This stores the classifier 
	private Classifier javamlsmo = null;
	
	
	public KDNode(int input_dim) throws IOException {
		this.input_dim = input_dim;
		root_map.add(this);
	}
	
	// This stores the set of root nodes
	public static HashSet<KDNode> RootSet() {
		return root_map;
	}
	
	// This creates the set of leaf nodes
	public void LeafNodes(ArrayList<KDNode> buff) {
		
		if(left_ptr == null && right_ptr == null) {
			buff.add(this);
			return;
		}
		
		left_ptr.LeafNodes(buff);
		right_ptr.LeafNodes(buff);
	}
	
	// This creates the set of set leaf nodes
	public void SetNodes(ArrayList<KDNode> buff) {
		
		if(is_node_set == true) {
			buff.add(this);
			return;
		}
		
		if(left_ptr == null && right_ptr == null) {
			buff.add(this);
			return;
		}
		
		left_ptr.SetNodes(buff);
		right_ptr.SetNodes(buff);
	}
	
	// This stores the majority vote for this node
	public boolean MajorityVote(boolean sample[]) {
		
		Boolean output = outcome_map.get(sample);
		if(output != null) {
			return output;
		}
		 
		if(leaf_buff == null) {
			leaf_buff = new ArrayList<KDNode>();
			LeafNodes(leaf_buff);
		}
		
		int true_count = 0;
		int false_count = 0;
		
		DecisionNode node = null;
		float class_error = 0;
		
		for(int i=0; i<leaf_buff.size(); i++) {
			
			if(leaf_buff.get(i).dec_node != null) {
				if(leaf_buff.get(i).dec_node.class_error > class_error) {
					class_error = leaf_buff.get(i).dec_node.class_error;
					node = leaf_buff.get(i).dec_node;
					break;
				}
				/*if(leaf_buff.get(i).dec_node.Output(sample) == true) {
					true_count++;
				} else {
					false_count++;
				}*/
				continue;
			}
			
			
			if(sample[leaf_buff.get(i).input_dim] == true) {
				true_count++;
			} else {
				false_count++;
			}
		}
		
		boolean val = false;
		
		if(node != null) {
			val = node.Output(sample);
		} else {
			val = true_count > false_count;
		}
		
		outcome_map.put(sample, val);
		return val;
	}
	
	// This returns the output of this node
	public boolean Output(boolean sample[]) {
		
		return MajorityVote(sample);
		
		/*if(javamlsmo == null) {
			return MajorityVote(sample);
		}
		
		ArrayList<KDNode> leaf_buff = new ArrayList<KDNode>();
		LeafNodes(leaf_buff);
		double val[] = new double[leaf_buff.size()];
		
		for(int j=0; j<val.length; j++) {
			val[j] = sample[leaf_buff.get(j).input_dim] == true ? 1 : -1;
		}

		Boolean obj = (Boolean) javamlsmo.classify(new DenseInstance(val));
		return obj == TRUE_VAL;*/
	}
	
	// This builds a classifier for a given node
	private void BuildClassifier(DataSet d) {
		
		ArrayList<KDNode> leaf_buff = new ArrayList<KDNode>();
		LeafNodes(leaf_buff);
		
		if(leaf_buff.size() == 1) {
			return;
		}
		
		Dataset data = new DefaultDataset();
		for(int j=0; j<d.DataSet().size(); j++) {
			
			boolean sample[] = d.DataSet().get(j);
			boolean output = d.OutputSet().get(j)[0];
			
			double val[] = new double[leaf_buff.size()];
			for(int i=0; i<leaf_buff.size(); i++) {
				val[i] = sample[i] == true ? 1.0f : -1.0f;
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
	
	// This creates the classifier for a given node
	private static void BuildClassifier(DataSet d, ArrayList<KDNode> child_buff,
			DecisionNode parent, ArrayList<DecisionNode> parent_buff) {
		
		ArrayList<DecisionNode> buff = new ArrayList<DecisionNode>();
		for(int i=0; i<child_buff.size(); i++) {
			buff.add(new DecisionNode(child_buff.get(i), null));
		}
		
		if(parent != null) {
			parent_buff.add(parent);
		}
		
		for(int i=parent_buff.size()-1; i>=Math.max(parent_buff.size()-4, 0); i--) {
			buff.add(parent_buff.get(i));
		}
		
		System.out.println(child_buff.size()+" "+buff.size()+" ***");
		parent = new DecisionNode(null, buff);
		parent.AssignBestRule(d, d.DataSet().size(), 10);
		parent.TestError(d);
		best_class_error = Math.max(best_class_error, parent.class_error);
		
		
		int count = 0;
		for(int i=0; i<child_buff.size(); i++) {
			child_buff.get(i).DecompressNode(3);
			
			ArrayList<KDNode> buff1 = new ArrayList<KDNode>();
			child_buff.get(i).SetNodes(buff1);
			
			if(buff1.size() < 2) {
				count++;
				continue;
			}
			
			if(buff1.size() > 1) {
				ArrayList<DecisionNode> buff2 = new ArrayList<DecisionNode>(parent_buff);
				BuildClassifier(d, buff1, parent, buff2);
			}
		}
		
		if(count > 0) {
			decision_buff.add(parent);
		}
	}
	
	// This returns the node correlation 
	public float NodeCorr() {
		return node_corr;
	}
	
	// This builds the tree
	public static void BuildHierarchyTree(DataSet d) throws IOException {
		
		while(root_map.size() > 11) {
			
			System.out.println("Root Size: "+root_map.size());
			ArrayList<KDNode> root_buff = new ArrayList<KDNode>();
			for(KDNode k : root_map) {
				root_buff.add(k);
			}
			
			float max = -99999999;
			Table max_join = null;
			for(int j=0; j<root_buff.size(); j++) {
				
				for(int i=j+1; i<root_buff.size(); i++) {
				
					Table t = new Table();
					t.dim1 = root_buff.get(j);
					t.dim2 = root_buff.get(i);
					
					for(int k=0; k<d.DataSet().size(); k++) {
						boolean sample[] = d.DataSet().get(k);
						int input1 = t.dim1.MajorityVote(sample) == true ? 1 : 0;
						int input2 = t.dim2.MajorityVote(sample) == true ? 1 : 0;
						t.count[input1][input2]++;
					}
					
					t.match = (t.count[0][0] + t.count[1][1]) / 2;
					t.match -= (t.count[0][1] + t.count[1][0]) / 2;
					if(t.match > max) {
						max = t.match;
						max_join = t;
					}
				}	
			}
			
			max_join.dim1.RemoveRootNode();
			max_join.dim2.RemoveRootNode();
			
			KDNode parent = new KDNode(0);
			parent.left_ptr = max_join.dim1;
			parent.right_ptr = max_join.dim2;
			parent.node_corr = max;
			
			parent.left_ptr.parent_ptr = parent;
			parent.right_ptr.parent_ptr = parent;
		}
		
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		for(KDNode k : root_map) {
			//k.BuildClassifier(d);
			buff.add(k);
		}
		
		BuildClassifier(d, buff, null, new ArrayList<DecisionNode>());
	}
	
	// This creates the tree
	public static ArrayList<DecisionNode> BuildTree(DataSet d) throws IOException {
		
		root_map.clear();
		best_class_error = 0;
		decision_buff.clear();
		for(int i=0; i<d.DimNum(); i++) {
			new KDNode(i);
		}
		
		link_set_map = new boolean[d.DimNum()][d.DimNum()];
		for(int i=0; i<d.DimNum(); i++) {
			for(int j=0; j<d.DimNum(); j++) {
				link_set_map[i][j] = true;
			}
		}
		
		BuildHierarchyTree(d);
		
		return decision_buff;
	}
	
	// This creates the tree
	public static ArrayList<DecisionNode> BuildTree(DataSet d, ArrayList<DecisionNode> node_buff) throws IOException {
		
		root_map.clear();
		for(int i=0; i<node_buff.size(); i++) {
			KDNode k = new KDNode(i);
			k.dec_node = node_buff.get(i);
		}
		
		link_set_map = new boolean[node_buff.size()][node_buff.size()];
		for(int i=0; i<node_buff.size(); i++) {
			for(int j=0; j<node_buff.size(); j++) {
				link_set_map[i][j] = true;
			}
		}
		
		decision_buff.clear();
		BuildHierarchyTree(d);
		
		return decision_buff;
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
	private void DecompressNode(int level_num) {
		
		if((left_ptr == null && right_ptr == null) || (level_num == 0)) {
			is_node_set = true;
			return;
		}
		
		is_node_set = false;
		root_map.remove(this);
		root_map.add(left_ptr);
		root_map.add(right_ptr);
		
		left_ptr.DecompressNode(level_num - 1);
		right_ptr.DecompressNode(level_num - 1);
	}
	
	// This returns a predicate indicating children for this node
	public boolean HasChildren() {
		return left_ptr != null && right_ptr != null;
	}
}
