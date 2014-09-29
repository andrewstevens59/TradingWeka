package VectorMethod;

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
	// This stores the preference weight
	private float pref_weight;
	// This stores the set of root nodes
	private static HashSet<KDNode> root_map = new HashSet<KDNode>();
	// This stores the node set for the default feature vector
	private static KDNode default_vec_node[];
	
	// This stores the number of true attributes
	// This stores the maximum dimension number
	static int MAX_DIM_NUM = 8;
	
	
	public KDNode(int input_dim) throws IOException {
		this.input_dim = input_dim;
		root_map.add(this);
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
	
	// This stores the majority vote for this node
	public boolean MajorityVote(boolean sample[]) {
		
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		LeafNodes(buff);
		
		int true_count = 0;
		int false_count = 0;
		for(int i=0; i<buff.size(); i++) {
			
			
			if(sample[buff.get(i).input_dim] == true) {
				true_count++;
			} else {
				false_count++;
			}
		}
		
		return true_count > false_count;
	}
	
	// This creates the tree
	public static void BuildTree(DataSet d) throws IOException {
		
		root_map.clear();
		for(int i=0; i<d.DimNum(); i++) {
			new KDNode(i);
		}
		
		/*while(root_map.size() > 220) {
			
			ArrayList<KDNode> root_buff = new ArrayList<KDNode>();
			for(KDNode k : root_map) {
				root_buff.add(k);
			}
			
			int max = 0;
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
					
					t.match = Math.max(t.count[0][0], t.count[1][1]);
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
			
			parent.left_ptr.parent_ptr = parent;
			parent.right_ptr.parent_ptr = parent;
		}*/
		
		int offset = 0;
		default_vec_node = new KDNode[root_map.size()];
		for(KDNode k : root_map) {
			default_vec_node[offset++] = k;
		}
	}
	
	// This stores the default vector
	public static float[] DefaultVec(float feat_vec[], KDNode node_buff[]) {
		
		float default_vec[] = new float[default_vec_node.length];
		
		for(int i=0; i<default_vec.length; i++) {
			default_vec[i] = 0;
		}
		
		MapFeatureWeight(node_buff, default_vec_node, feat_vec, default_vec, 1.0f);
		
		return default_vec;
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
				
				if(new_buff[i] == old_buff[j]) {
					new_feat[i] += old_feat[j] * weight;
					continue;
				}
				
				float factor1 = new_buff[i].IsParent(old_buff[j]);
				float factor2 = old_buff[j].IsParent(new_buff[i]);
				
				new_feat[i] += old_feat[j] * factor1 * weight;
				new_feat[i] += old_feat[j] * factor2 * weight;
			}
		}
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
