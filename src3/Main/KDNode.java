package Main;

import java.io.File;



import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Normalize;



public class KDNode {

	// This stores the left child
	private KDNode left_ptr = null;
	// This stores the right child
	private KDNode right_ptr = null;
	// This stores the parent ptr
	private KDNode parent_ptr = null;
	// This stores the corresponding output for this node
	public Classifier classifier = null;
	// This stores the classifier id 
	private int class_id;
	// This indicates that a node has been set
	private boolean is_node_set = false;
	// This stores the correlation between two clusters
	private float node_corr;
	// This stores the set of root nodes
	private static HashSet<KDNode> root_map = new HashSet<KDNode>();
	// This stores the set of leaf nodes
	private ArrayList<KDNode> leaf_buff = null;
	
	// This stores the set of s-links
	public SLink s_links = null;
	// This stores the s_link number
	private int s_link_num = 0;
	// This stores the maximum number of s-links
	public static int max_s_link_num = 100;
	// This stores the classification vector for the classifier
	private double class_vect[];
	
	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	
	
	public KDNode(Classifier c, int class_id) throws IOException {
		root_map.add(this);
		classifier = c;
		this.class_id = class_id;
	}
	
	// This stores the set of root nodes
	public static HashSet<KDNode> RootSet() {
		return root_map;
	}
	
	// This writes a node to file
	private void WriteKDNode(DataOutputStream dos) throws IOException {
		
		dos.writeInt(class_vect.length);
		for(int i=0; i<class_vect.length; i++) {
			dos.writeDouble(class_vect[i]);
		}
		
		dos.writeBoolean(left_ptr != null);
		if(left_ptr != null) {
			left_ptr.WriteKDNode(dos);
		}
		
		dos.writeBoolean(right_ptr != null);
		if(right_ptr != null) {
			right_ptr.WriteKDNode(dos);
		}
		
		dos.writeFloat(node_corr);
		dos.writeInt(class_id);
	}
	
	// This writes the kd-tree
	public static void WriteKDTree() throws IOException {
		
		FileOutputStream fos = new FileOutputStream("kd_tree");
		DataOutputStream dos = new DataOutputStream(fos);
		
		dos.writeInt(root_map.size());
		for(KDNode k : root_map) {
			k.WriteKDNode(dos);
		}
		
		dos.close();
	}
	
	// This reads a kdnode into memory
	private void ReadKDNode(DataInputStream dos) throws IOException {
		
		int len = dos.readInt();
		class_vect = new double[len];
		for(int i=0; i<len; i++) {
			class_vect[i] = dos.readDouble();
		}
		
		boolean is_left = dos.readBoolean();
		
		if(is_left == true) {
			left_ptr = new KDNode(null, 0);
			root_map.remove(left_ptr);
			left_ptr.ReadKDNode(dos);
			left_ptr.parent_ptr = this;
		}
		
		boolean is_right = dos.readBoolean();
		if(is_right == true) {
			right_ptr = new KDNode(null, 0);
			root_map.remove(right_ptr);
			right_ptr.ReadKDNode(dos);
			right_ptr.parent_ptr = this;
		}
		
		node_corr = dos.readFloat();
		class_id = dos.readInt();
		
		classifier = new Classifier("Classify/class1_" + class_id);
	}
	
	// This reads the kd-tree into memory
	public static void ReadKDTree() throws IOException {
		
		FileInputStream fos = new FileInputStream("kd_tree");
		DataInputStream dos = new DataInputStream(fos);
		
		int tree_num = dos.readInt();
		for(int i=0; i<tree_num; i++) {
			(new KDNode(null, 0)).ReadKDNode(dos);
		}
		
		dos.close();
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
	
	// This returns the leaf nodes
	public int LeafNodeNum() {
		
		if(left_ptr == null && right_ptr == null) {
			return 1;
		}
		
		return left_ptr.LeafNodeNum() + right_ptr.LeafNodeNum();
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
	
	// This prints the class vector
	public void PrintClassVector() {
		for(int i=0; i<class_vect.length; i++) {
			System.out.print(class_vect[i]+" ");
		}
		
		System.out.println("");
	}
	
	// This stores the majority vote for this node
	public boolean MajorityVote(boolean sample[]) {
		 
		if(leaf_buff == null) {
			leaf_buff = new ArrayList<KDNode>();
			LeafNodes(leaf_buff);
		}

		double sum = 0;
		ArrayList<Double> sim_buff = new ArrayList<Double>();
		for(int i=0; i<leaf_buff.size(); i++) {

			double sim = Similarity.calculateSimilarity(class_vect, leaf_buff.get(i).class_vect, 0);
			sim = 1.0f / sim;
			sim_buff.add(sim);
			
			if(sim < 0) {
				System.out.println("sim errror");System.exit(0);
			}
			sum += sim;
		}
		
		float true_count = 0;
		float false_count = 0;

		for(int i=0; i<leaf_buff.size(); i++) {

				double sim = Similarity.calculateSimilarity(class_vect, leaf_buff.get(i).class_vect, 0);
				sim = 1.0f / sim;
				sim /= sum;
				if(leaf_buff.get(i).classifier.Output(sample) == true) {
					true_count += 1;
				} else {
					false_count += 1;
				}
				continue;
		}

		return true_count > false_count;
	}
	
	// This returns the output of this node
	public boolean Output(boolean sample[]) {
		
		return MajorityVote(sample);
	}
	
	// This returns the node correlation 
	public float NodeCorr() {
		return node_corr;
	}
	
	// This creates the reduced data set
	private static void CreateClassifierVec(RegimeSet set, 
			ArrayList<KDNode> class_buff, int vec_offset) {
		
		
		int true_num[] = new int[set.data_set.size()];
		int false_num[] = new int[set.data_set.size()];
		for(int j=0; j<set.data_set.size(); j++) {
			true_num[j] = 0;
			false_num[j] = 0;
		}
		
		for(int j=0; j<set.data_set.size(); j++) {
			
			for(int k=0; k<class_buff.size(); k++) {
				
				boolean sample[] = set.data_set.get(j);
				boolean output = class_buff.get(k).classifier.Output(sample);

				if(output == true) {
					true_num[j]++;
				} else {
					false_num[j]++;
				}
			}
		}
		
		int dim_num = 0;
		for(int j=0; j<set.data_set.size(); j++) {
			if(Math.min(true_num[j], false_num[j]) > 0) {
				dim_num++;
			}
		}
		
		if(dim_num == 0)System.exit(0);
			
		Attribute atts[] = new Attribute[dim_num];
		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(2);
		 fvClassVal.addElement("positive");
		 fvClassVal.addElement("negative");
		 Attribute ClassAttribute = new Attribute("theClass", fvClassVal);
	    
		FastVector fvWekaAttributes = new FastVector(dim_num + 1);
	    for(int i=0; i<dim_num; i++) {
	    	fvClassVal = new FastVector(2);
			fvClassVal.addElement("positive");
			fvClassVal.addElement("negative");
			 
	    	atts[i] = new Attribute("att"+i, fvClassVal);
	    	fvWekaAttributes.addElement(atts[i]);
	    }
	    
	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    Instances  isTrainingSet = new Instances("Rel", fvWekaAttributes, class_buff.size()); 
	    isTrainingSet.setClassIndex(dim_num);
		
	    for(int k=0; k<class_buff.size(); k++) {
	    	
	    	ArrayList<Boolean> data_set = new ArrayList<Boolean>();
			for(int j=0; j<set.data_set.size(); j++) {
				
				if(Math.min(true_num[j], false_num[j]) > 0) {
					boolean sample[] = set.data_set.get(j);
					boolean output = class_buff.get(k).classifier.Output(sample);
					data_set.add(output);
				}
			}
			
			if(data_set.size() != dim_num) {
				System.out.println("dim mis");System.exit(0);
			}
						
			Instance iExample = new Instance(dim_num + 1);
			for(int j=0; j<dim_num; j++) {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(j), data_set.get(j) == true ? "positive" : "negative"); 
			}
			
			if(Math.random() < 0.5) {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(dim_num), "negative");    
			} else {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(dim_num), "positive"); 
			}
			 
			// add the instance
			isTrainingSet.add(iExample);
	    }
		
	    System.out.println(dim_num+"  *************");
	    int select_num = 3;
		PrincipalComponents pca = new PrincipalComponents();
		//pca.setVarianceCovered(0.1);
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(select_num);
        AttributeSelection selection = new AttributeSelection();
        selection.setEvaluator(pca);
        
        Normalize normalizer = new Normalize();
        try {
                normalizer.setInputFormat(isTrainingSet);
                isTrainingSet = Filter.useFilter(isTrainingSet, normalizer);
                
                selection.setSearch(ranker);
                selection.SelectAttributes(isTrainingSet);
                isTrainingSet = selection.reduceDimensionality(isTrainingSet);

        } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
        }
        
        for(int i=0; i<class_buff.size(); i++) {
        	Instance inst = isTrainingSet.instance(i);
        	double val[] = inst.toDoubleArray();
        	int offset = vec_offset;
        	
        	float length = 0;
        	for(int j=0; j<select_num; j++) {
        		length += val[j] * val[j];
        	}
        	
        	length = (float) Math.sqrt(length);
        	for(int j=0; j<select_num; j++) {
        		class_buff.get(i).class_vect[offset++] = val[j] / length;
        	}
        }
	}
	
	// This builds the tree
	public static void BuildHierarchyTree(DataSet d, ArrayList<KDNode> class_buff) throws IOException {
		
		for(int k=0; k<class_buff.size(); k++) {
			class_buff.get(k).class_vect = new double[d.RegimeSet().size() * 3];
		}
		
		for(int i=0; i<d.RegimeSet().size(); i++) {
			CreateClassifierVec(d.RegimeSet().get(i), class_buff, i * 3);
		}
		
		while(root_map.size() > 1000002) {
			
			System.out.println("Root Size: "+root_map.size());
			ArrayList<KDNode> root_buff = new ArrayList<KDNode>();
			for(KDNode k : root_map) {
				root_buff.add(k);
				
				float length = 0;
				for(int i=0; i<k.class_vect.length; i++) {
					length += k.class_vect[i] * k.class_vect[i];
				}
				
				length = (float) Math.sqrt(length);
				for(int i=0; i<k.class_vect.length; i++) {
					k.class_vect[i] /= length;
				}
			}
			
			float max = -99999999;
			Table max_join = new Table();
			for(int j=0; j<root_buff.size(); j++) {
				
				for(int i=j+1; i<root_buff.size(); i++) {

					KDNode dim1 = root_buff.get(j);
					KDNode dim2 = root_buff.get(i);
					
					float dot_product = 0;
					for(int k=0; k<dim1.class_vect.length; k++) {
						dot_product += dim1.class_vect[k] * dim2.class_vect[k];
					}
					
					dot_product /= (dim1.LeafNodeNum() + dim2.LeafNodeNum());
					
					if(dot_product > max) {
						max = dot_product;
						max_join.dim1 = dim1;
						max_join.dim2 = dim2;
					}
				}	
			}
			
			max_join.dim1.RemoveRootNode();
			max_join.dim2.RemoveRootNode();
			
			KDNode parent = new KDNode(null, 0);
			parent.left_ptr = max_join.dim1;
			parent.right_ptr = max_join.dim2;
			parent.node_corr = max;
			
			parent.class_vect = new double[max_join.dim1.class_vect.length];
			for(int k=0; k<max_join.dim1.class_vect.length; k++) {
				parent.class_vect[k] = (max_join.dim1.class_vect[k] + max_join.dim2.class_vect[k]) / 2;
			}
			
			parent.left_ptr.parent_ptr = parent;
			parent.right_ptr.parent_ptr = parent;
		}
		
	}
	
	// This builds the tree
	public static void BuildHierarchyTree(DataSet d) throws IOException {
		
		while(root_map.size() > 12) {
			
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
					
					for(int k=0; k<d.TestDataSet().size(); k++) {
						boolean sample[] = d.TestDataSet().get(k);
						int input1 = t.dim1.MajorityVote(sample) == true ? 1 : 0;
						int input2 = t.dim2.MajorityVote(sample) == true ? 1 : 0;
						t.count[input1][input2]++;
					}
					
					t.match = (t.count[0][0] + t.count[1][1]) / 2;
					t.match -= (t.count[0][1] + t.count[1][0]) / 2;
					t.match /= (t.dim1.LeafNodeNum() + t.dim2.LeafNodeNum());
					if(t.match > max) {
						max = t.match;
						max_join = t;
					}
				}	
			}
			
			max_join.dim1.RemoveRootNode();
			max_join.dim2.RemoveRootNode();
			
			KDNode parent = new KDNode(null, 0);
			parent.left_ptr = max_join.dim1;
			parent.right_ptr = max_join.dim2;
			parent.node_corr = max;
			
			parent.left_ptr.parent_ptr = parent;
			parent.right_ptr.parent_ptr = parent;
		}
	}
	
	
	// This embeds an s-link in the hierarchy
	private static void EmbedSLink(SLink s_link) {
		
		KDNode ptr = s_link.src;
		while(ptr != null) {
			
			if(s_link.dst.IsParent(ptr) == true) {
				// cannot merge in the same node
				break;
			}
			
			if(ptr.s_link_num < max_s_link_num) {
				
				SLink s = new SLink();
				s.src = s_link.src;
				s.dst = s_link.dst;
				
				SLink prev_ptr = ptr.s_links;
				ptr.s_links = s;
				s.next_ptr = prev_ptr;
				ptr.s_link_num++;
			}
			
			ptr = ptr.parent_ptr;
		}
	}
	
	// This checks if a node has a particular parent
	public boolean IsParent(KDNode parent) {
		
		KDNode ptr = this;
		while(ptr != null) {
			if(ptr == parent) {
				return true;
			}
			ptr = ptr.parent_ptr;
		}
		
		return false;
	}
	
	// This generates the set of s-links
	public static void CreateSLinks(DataSet d, int max_s_link) {
		
		max_s_link_num = max_s_link;
		ArrayList<KDNode> leaf_buff = new ArrayList<KDNode>();
		for(KDNode k : root_map) {
			k.LeafNodes(leaf_buff);
		}
		
		float trav_prob[][] = new float[leaf_buff.size()][leaf_buff.size()];
		for(int i=0; i<leaf_buff.size(); i++) {
			for(int j=0; j<leaf_buff.size(); j++) {
				trav_prob[i][j] = 0;
			}
		}
		
		ArrayList<float []> member_prob_buff = new ArrayList<float []>();
		for(int i=0; i<d.RegimeSet().size(); i++) {
			
			float max = 0;
			float obs_prob[] = new float[leaf_buff.size()];
			for(int j=0; j<leaf_buff.size(); j++) {
				Classifier c = leaf_buff.get(j).classifier;
				
				float true_count = 0;
				RegimeSet r = d.RegimeSet().get(i);
				for(int k=0; k<r.data_set.size(); k++) {
					
					boolean sample[] = r.data_set.get(k);
					boolean output = r.output_set.get(k)[0];
					
					boolean val = c.Output(sample);
					if(val == output) {
						true_count++;
					}
				}
				
				obs_prob[j] = true_count / r.data_set.size();
				max = Math.max(max, obs_prob[j]);
			}
			
			member_prob_buff.add(obs_prob);
		}
		
		CreateSLinks(leaf_buff, trav_prob, member_prob_buff);
	}

	// This generates the set of s-links
	private static void CreateSLinks(ArrayList<KDNode> leaf_buff,
			float[][] trav_prob, ArrayList<float[]> member_prob_buff) {
		
		for(int k=0; k<member_prob_buff.size()-1; k++) {
			
			for(int i=0; i<leaf_buff.size(); i++) {
				for(int j=0; j<leaf_buff.size(); j++) {
					trav_prob[i][j] += member_prob_buff.get(k)[i] * member_prob_buff.get(k+1)[j];
				}
			}
		}
		
		for(int i=0; i<leaf_buff.size(); i++) {
			
			float sum = 0;
			for(int j=0; j<leaf_buff.size(); j++) {
				sum += trav_prob[i][j];
			}
			
			for(int j=0; j<leaf_buff.size(); j++) {
				trav_prob[i][j] /= sum;
			}
		}
		
		ArrayList<SLink> s_link_buff = new ArrayList<SLink>();
		for(int i=0; i<leaf_buff.size(); i++) {
			for(int j=0; j<leaf_buff.size(); j++) {
				SLink s_link = new SLink();
				s_link.src = leaf_buff.get(i);
				s_link.dst = leaf_buff.get(j);
				s_link.trav_prob = trav_prob[i][j];
				s_link_buff.add(s_link);
			}
		}
		
		Collections.sort(s_link_buff, new Comparator<SLink>() {
			 
	        public int compare(SLink arg1, SLink arg2) {
	        	
	        	if(arg1.trav_prob < arg2.trav_prob) {
	    			return 1;
	    		}

	    		if(arg1.trav_prob > arg2.trav_prob) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });

		for(int i=0; i<s_link_buff.size(); i++) {
			EmbedSLink(s_link_buff.get(i));
		}
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
	
	// This returns the highest comp node 
	public KDNode CompNode() {
		
		KDNode ptr = this;
		while(ptr.parent_ptr != null) {
			
			if(ptr.is_node_set == true) {
				return ptr;
			}
			
			ptr = ptr.parent_ptr;
		}
		
		return ptr;
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
	public void DecompressNode(int level_num) {
		
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
