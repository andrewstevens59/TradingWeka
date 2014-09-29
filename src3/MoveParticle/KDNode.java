package MoveParticle;

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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import Main.Similarity;
import MoveParticle.Sample.NScore;

import net.sf.javaml.core.Dataset;

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
	
	// This stores the N most recent scores
	class NScore {
		float score;
		NScore prev_ptr = null;
		NScore next_ptr = null;
	}
	
	// This stores the most recent of the N scores
	private NScore head_score = null;
	// This stores the least recent of the N scores
	private NScore tail_score = null;
	// This stores the number of current scores
	private int score_num = 0;

	// This stores the left child
	public KDNode left_ptr = null;
	// This stores the right child
	public KDNode right_ptr = null;
	// This stores the parent ptr
	public KDNode parent_ptr = null;
	// This stores the corresponding output for this node
	public Classify classifier = null;
	// This indicates that a node has been set
	private boolean is_node_set = false;
	// This stores the correlation between two clusters
	private float node_corr;
	// This stores the set of root nodes
	private static ArrayList<HashSet<KDNode>> root_map = new ArrayList<HashSet<KDNode>>();
	// This stores the current class id
	private static int class_id;
	// This stores the set of leaf nodes
	private ArrayList<KDNode> leaf_buff = null;
	
	// This stores the set of s-links
	public SLink s_links = null;
	// This stores the s_link number
	private int s_link_num = 0;
	// This stores the maximum number of s-links
	public static int max_s_link_num = 10000;
	// This stores the classification vector for the classifier
	private double class_vect[];
	// This stores the fitness based on net error for the classifier
	private float fitness;

	// This stores the error gradient for this classifier
	public float error_grad;
	// This stores the class label for the kd-node
	public int class_label;
	
	// This defines the attributes selection number
	static private int DIM_RED_NUM = 2;
	
	
	public KDNode(Classify c) throws IOException {
		root_map.get(class_id).add(this);
		classifier = c;
	}
	
	// This sets the current class id
	public static void SetClassID(int id) {
		class_id = id;
	}
	
	// This returns the class vector
	public double[] ClassVect() {
		return class_vect;
	}
	
	// This stores the set of root nodes
	public static HashSet<KDNode> RootSet() {
		return root_map.get(class_id);
	}
	
	// This returns the fitness of the classifier
	public float Fitness() {
		return 1.0f / ((fitness / score_num) + 1);
	}
	
	// This updates the error term for this time step
	public void UpdateErrorDelta(double sample[], int output) {
		
		int out = Output(sample);
		
		NScore prev_ptr = head_score;
		head_score = new NScore();
		head_score.next_ptr = prev_ptr;
		head_score.score = Math.abs(out - output);
		
		if(prev_ptr != null) {
			prev_ptr.prev_ptr = head_score;
		}
		
		score_num++;
		if(tail_score == null) {
			tail_score = head_score;
		}
		
		fitness += head_score.score;
		
		if(score_num >= 20) {
			fitness -= tail_score.score;
			tail_score.prev_ptr.next_ptr = null;
			tail_score = tail_score.prev_ptr;
			score_num--;
		}
		
		ArrayList<Float> error_buff = new ArrayList<Float>();
		NScore ptr = head_score;
		while(ptr != null) {
			error_buff.add(ptr.score);
			ptr = ptr.next_ptr;
		}
		
		error_grad = LinearRegression.Regression(error_buff, 0, error_buff.size());
		
		if(left_ptr != null) {
			left_ptr.UpdateErrorDelta(sample, output);
		}
		
		if(right_ptr != null) {
			right_ptr.UpdateErrorDelta(sample, output);
		}
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
	
	// This prints the class vector
	public void PrintClassVector() {
		for(int i=0; i<class_vect.length; i++) {
			System.out.print(class_vect[i]+" ");
		}
		
		System.out.println("");
	}
	
	// This stores the majority vote for this node
	public int MajorityVote(double sample[]) {
		 
		if(leaf_buff == null) {
			leaf_buff = new ArrayList<KDNode>();
			LeafNodes(leaf_buff);
		}

		int class_count[] = new int[]{0, 0, 0, 0};
		for(int i=0; i<leaf_buff.size(); i++) {
			int label = leaf_buff.get(i).classifier.Output(sample);

			switch(label) {
				case -2: class_count[0]++;break;
				case -1: class_count[1]++;break;
				case 1: class_count[2]++;break;
				case 2: class_count[3]++;break;
			}
		}
		
		int max = 0;
		int label = 0;
		for(int i=0; i<4; i++) {
			
			if(class_count[i] > max) {
				max = class_count[i];
				label = i;
			}
		}
		
		return label;
	}
	
	// This returns the output of this node
	public int Output(double sample[]) {
		return MajorityVote(sample);
	}
	
	// This returns the node correlation 
	public float NodeCorr() {
		return node_corr;
	}
	
	// This creates the reduced data set
	private static void CreateClassifierVec(Dataset set, int dim_num,
			ArrayList<KDNode> class_buff, int vec_offset, int data_start, int data_end) {
			
		int set_size = data_end - data_start;
		Attribute atts[] = new Attribute[set_size];
		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(4);
		 fvClassVal.addElement("n2");
		 fvClassVal.addElement("n1");
		 fvClassVal.addElement("p1");
		 fvClassVal.addElement("p2");
		 Attribute ClassAttribute = new Attribute("theClass", fvClassVal);
	    
		FastVector fvWekaAttributes = new FastVector(set_size + 1);
	    for(int i=0; i<set_size; i++) {
	    	atts[i] = new Attribute("att"+i);
	    	fvWekaAttributes.addElement(atts[i]);
	    }
	    
	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    Instances  isTrainingSet = new Instances("Rel", fvWekaAttributes, class_buff.size()); 
	    isTrainingSet.setClassIndex(set_size);
		
	    for(int k=0; k<class_buff.size(); k++) {
	    	
	    	ArrayList<Integer> data_set = new ArrayList<Integer>();
			for(int j=data_start; j<data_end; j++) {
				
				int offset = 0;
				Set<Entry<Integer, Double>> s = set.instance(j).entrySet();
				double sample[] = new double[dim_num];
				for(Entry<Integer, Double> val : s) {
					sample[offset++] = val.getValue();
				}
				
				int output = class_buff.get(k).classifier.Output(sample);
				data_set.add(output);
			}
			
			if(set_size != data_set.size()) {
				System.out.println("dim mis");System.exit(0);
			}
						
			Instance iExample = new Instance(set_size + 1);
			for(int j=0; j<data_set.size(); j++) {
				iExample.setValue(j, data_set.get(j));
			}
			
			if(Math.random() < 0.5) {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(set_size), "n1");    
			} else {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(set_size), "p1"); 
			}
			 
			// add the instance
			isTrainingSet.add(iExample);
	    }
		
	    System.out.println(dim_num+"  *************");
	    int select_num = DIM_RED_NUM;
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
        	
        	if(length == 0) {
        		
        		ArrayList<Integer> data_set = new ArrayList<Integer>();
    			for(int j=data_start; j<data_end; j++) {
    				
    				int offset1 = 0;
    				Set<Entry<Integer, Double>> s = set.instance(j).entrySet();
    				double sample[] = new double[dim_num];
    				for(Entry<Integer, Double> val1 : s) {
    					System.out.print(val1.getValue()+" ");
    					sample[offset1++] = val1.getValue();
    				}
    				System.out.println("");
    				
    				int output = class_buff.get(i).classifier.Output(sample);
    				data_set.add(output);
    			}
    			
    			System.out.println("(((((((((((((((");
    			for(int j=0; j<data_set.size(); j++) {
    				System.out.println(data_set.get(j));
    			}
    			System.exit(0);
        	}

        	length = (float) Math.sqrt(length);
        	for(int j=0; j<select_num; j++) {
        		class_buff.get(i).class_vect[offset++] = val[j] / length;
        	}
        }
	}
	
	// This builds the tree
	public static ArrayList<KDNode> BuildHierarchyTree(Dataset d, int dim_num,
			ArrayList<Classify> buff) throws IOException {
		
		root_map.add(new HashSet<KDNode>());
		
		System.out.println(d.size()+"   ----------");
		ArrayList<KDNode> class_buff = new ArrayList<KDNode>();
		for(int i=0; i<buff.size(); i++) {
			class_buff.add(new KDNode(buff.get(i)));
		}
		
		for(int k=0; k<class_buff.size(); k++) {
			class_buff.get(k).class_vect = new double[DIM_RED_NUM * (d.size() / 15)];
		}
		
		int vec_offset = 0;
		int offset = 0;
		int inc = d.size() / 15;
		for(int i=0; i<15; i++) {
			CreateClassifierVec(d, dim_num, class_buff, vec_offset, offset, offset + inc);
			offset += inc;
			vec_offset += DIM_RED_NUM;
		}
		
		for(int i=0; i<class_buff.size(); i++) {
			Sample.NormVect(class_buff.get(i).class_vect);
        }

		while(root_map.get(class_id).size() > 5) {
			
			System.out.println("Root Size: "+root_map.get(class_id).size());
			ArrayList<KDNode> root_buff = new ArrayList<KDNode>();
			for(KDNode k : root_map.get(class_id)) {
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
			
			KDNode parent = new KDNode(null);
			parent.left_ptr = max_join.dim1;
			parent.right_ptr = max_join.dim2;
			parent.node_corr = max;
			
			parent.class_vect = new double[max_join.dim1.class_vect.length];
			for(int k=0; k<max_join.dim1.class_vect.length; k++) {
				parent.class_vect[k] = (max_join.dim1.class_vect[k] + max_join.dim2.class_vect[k]) / 2;
			}
			
			Sample.NormVect(parent.class_vect);
			
			parent.left_ptr.parent_ptr = parent;
			parent.right_ptr.parent_ptr = parent;
		}
		
		return class_buff;
	}
	
	// This projects a parent class vector to the local class vector
	public float ProjectClassVector(KDNode parent, float len) {
		
		float dot = 0;
		for(int i=0; i<class_vect.length; i++) {
			dot += class_vect[i] * parent.class_vect[i] * len;
		}
		
		return dot;
	}
	
	// This resets the KDNode tree following several decompressions
	public static void Reset() {
		
		ArrayList<KDNode> root_buff = new ArrayList<KDNode>();
		for(KDNode k : KDNode.RootSet()) {
			root_buff.add(k);
		}
		
		root_map.get(class_id).clear();
		
		for(int i=0; i<root_buff.size(); i++) {
			KDNode ptr = root_buff.get(i);
			
			KDNode prev_ptr = ptr;
			while(ptr != null) {
				ptr.is_node_set = false;
				prev_ptr = ptr;
				ptr = ptr.parent_ptr;
			}
			
			root_map.get(class_id).add(prev_ptr);
		}
		
	}

	// This embeds an s-link in the hierarchy
	public static void EmbedSLink(SLink s_link) {
		
		KDNode ptr = s_link.src;
		while(ptr != null) {
			
			if(s_link.dst.IsParent(ptr) == true) {
				// cannot merge in the same node
				//break;
			}
			
			if(ptr.s_link_num < max_s_link_num) {
				
				SLink s = new SLink();
				s.src = s_link.src;
				s.dst = s_link.dst;
				s.trav_prob = s_link.trav_prob;
				
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
	
	// This returns the current set of root nodes
	public static KDNode[] RootBuff() {

		 int offset = 0;
		 KDNode[] buff = new KDNode[root_map.get(class_id).size()];
		 for(KDNode k : root_map.get(class_id)) {
			 buff[offset++] = k;
		 }
		 
		 return buff;
	}
	
	// This stores the number of dimensions
	public static int DimNum() {
		return root_map.get(class_id).size();
	}

	// This removes the root node below a node
	private void RemoveRootNode() {
		
		root_map.get(class_id).remove(this);
		is_node_set = false;
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
	
	// This returns the highest comp node 
	public KDNode CompNode(HashMap<KDNode, Integer> node_space) {
		
		KDNode ptr = this;
		while(ptr != null) {
			
			Integer node = node_space.get(ptr);
			if(node != null) {
				return ptr;
			}
			
			ptr = ptr.parent_ptr;
		}

		return null;
	}
	
	// This returns the transition probabilities vect for a given sample space
	public float[] TransProb(SampleSpace s) {
		
		float vect[] = new float[s.node_space.size()];
		for(int i=0; i<s.node_space.size(); i++) {
			vect[i] = 0;
		}
		
		float sum = 0;
		SLink link = s_links;
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		while(link != null) {
			KDNode ptr = link.dst.CompNode(s.node_space);
			if(ptr != null) {
				buff.add(ptr);
				sum += link.trav_prob;
			}

			link = link.next_ptr;
		}

		link = s_links;
		for(int i=0; i<buff.size(); i++) {
			KDNode ptr = buff.get(i);
			Integer id = s.node_space.get(ptr);
			vect[id] += (link.trav_prob / sum) * s.node_weight.get(ptr);
		}
		
		if(buff.size() == 0) {
			return null;
		}
		
		Sample.NormVect(vect);
		
		return vect;
	}
	
	// This decompresses the node
	public void DecompressNode(int level_num, ArrayList<KDNode> child_buff) {
		
		if((left_ptr == null && right_ptr == null) || (level_num <= 0)) {
			is_node_set = true;
			root_map.get(class_id).add(this);
			child_buff.add(this);
			return;
		}
		
		RemoveRootNode();
		
		if(left_ptr.parent_ptr != this) {
			System.out.println("bo");System.exit(0);
		}
		
		if(right_ptr.parent_ptr != this) {
			System.out.println("bo");System.exit(0);
		}
		
		left_ptr.DecompressNode(level_num - 1, child_buff);
		right_ptr.DecompressNode(level_num - 1, child_buff);
	}
	
	// This returns a predicate indicating children for this node
	public boolean HasChildren() {
		return left_ptr != null && right_ptr != null;
	}
}
