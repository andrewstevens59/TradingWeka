package ParticleFilter;

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
	
	// This defines a linked sample
	class LinkSample {
		Sample s;
		LinkSample next_ptr;
		// This stores the dimension weight
		float dim_weight;
	}

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
	private static HashSet<KDNode> root_map = new HashSet<KDNode>();
	// This stores the set of leaf nodes
	private ArrayList<KDNode> leaf_buff = null;
	// This stores the dynamics classifier for this node
	private ClassifyDynamics class_dynm = null;
	
	// This stores the set of s-links
	public SLink s_links = null;
	// This stores the s_link number
	private int s_link_num = 0;
	// This stores the maximum number of s-links
	public static int max_s_link_num = 10000;
	// This stores the classification vector for the classifier
	private double class_vect[];
	
	// This stores the set of k-closest samples
	public LinkSample k_closest = null;
	// This stores the set of contained samples
	private HashSet<Sample> sample_map = new HashSet<Sample>();
	// This is the average fitness of the node based on k-closest neighbours
	public float fitness;
	// This stores the class label for the kd-node
	public int class_label;
	// This stores the sample space for the kdnode
	public SampleSpace space = new SampleSpace();
	
	
	public KDNode(Classify c, int class_id) throws IOException {
		root_map.add(this);
		classifier = c;
	}
	
	// This sets the classification dynamics
	public void SetClassDynm(ClassifyDynamics class_dynm) {
		this.class_dynm = class_dynm;
	}
	
	// This returns the classification dynamics
	public ClassifyDynamics GetClassDynm() {
		return this.class_dynm;
	}
	
	// This returns the class vector
	public double[] ClassVect() {
		return class_vect;
	}
	
	// This stores the set of root nodes
	public static HashSet<KDNode> RootSet() {
		return root_map;
	}
	
	// This stores the current number of stored samples
	public int SampleNum() {
		return sample_map.size();
	}
	
	// This assigns the average fitness value for the node
	public float AssignFitness() {
		
		fitness = 0;
		if(sample_map.size() == 0) {
			return fitness;
		}
		
		LinkSample ptr = k_closest;
		while(ptr != null) {
			fitness += ptr.s.weight;
			ptr = ptr.next_ptr;
		}
		
		fitness /= sample_map.size();
		return fitness;
	}
	
	// This updates the sample weights
	public void UpdateSampleWeights(double sample[], int output) {
		
		LinkSample ptr = k_closest;
		while(ptr != null) {
			ptr.s.UpdateSampleWeight(sample, output);
			ptr = ptr.next_ptr;
		}
	}
	
	// This updates the sample weights
	public void UpdateSampleWeights(ArrayList<double []> train_buff,
			ArrayList<Integer> output_buff) {
		
		LinkSample ptr = k_closest;
		while(ptr != null) {
			for(int i=Math.max(train_buff.size()-3, 0); i<train_buff.size(); i++) {
				ptr.s.UpdateSampleWeight(train_buff.get(i), output_buff.get(i));
			}
			
			ptr = ptr.next_ptr;
		}
	}
	
	// This adds the samples in the current space to the k-closest neighbour set
	public void AddKSamples(int max_num) {
		
		for(int i=0; i<space.k_neighbour.size(); i++) {
			KDNodeDim dim = space.k_neighbour.get(i);
			
			for(int j=0; j<space.sample_buff.size(); j++) {
				Sample s = space.sample_buff.get(j);
				dim.kdnode.AddSample(s, max_num, dim.dim);
			}
		}
	}
	
	// This adds and a sample and updates the K closest neighbour set
	public void AddSample(Sample s, int max_num, int dim) {
		
		if(sample_map.contains(s) == true) {
			// sample already exists
			return;
		}
		
		LinkSample ptr = k_closest;
		LinkSample prev_ptr = null;
		
		while(ptr != null) {
			if(ptr.dim_weight > s.class_weight[dim]) {
				break;
			}
			
			prev_ptr = ptr;
			ptr = ptr.next_ptr;
		}
		
		if(prev_ptr == null) {
			if(sample_map.size() >= max_num) {
				// no room left
				return;
			}
			
			prev_ptr = k_closest;
			k_closest = new LinkSample();
			k_closest.dim_weight = s.class_weight[dim];
			k_closest.s = s;
			k_closest.next_ptr = prev_ptr;
			sample_map.add(s);
			return;
		}
		
		LinkSample next_ptr = prev_ptr.next_ptr;
		prev_ptr.next_ptr = new LinkSample();
		prev_ptr.next_ptr.s = s;
		prev_ptr.next_ptr.dim_weight = s.class_weight[dim];
		prev_ptr.next_ptr.next_ptr = next_ptr;
		sample_map.add(s);
		
		if(sample_map.size() >= max_num) {
			// no room left - remove the lowest
			sample_map.remove(k_closest.s);
			k_closest = k_closest.next_ptr;
		}
	}
	
	// This projects a set of samples onto a subspace by creating additional dimensions
	public void ProjectSamples(SampleSpace child_space, int max_num,
			HashMap<KDNode, Integer> root_map, ArrayList<double []> train_buff,
			ArrayList<Integer> output_buff) {
		
		LinkSample ptr = k_closest;
		
		System.out.println(SampleNum()+" "+child_space.kdnode_dim.size());
		
		while(ptr != null) {
			Sample old = ptr.s;
			Sample s = new Sample(child_space);
			s.class_weight = new float[child_space.kdnode_dim.size()];
			s.weight = old.weight;
			
			for(int j=0; j<child_space.kdnode_dim.size(); j++) {
				KDNodeDim dim = child_space.kdnode_dim.get(j);
				Integer old_dim = root_map.get(dim.kdnode);
				
				if(old_dim != null) {
					s.class_weight[dim.dim] = old.class_weight[old_dim];
				}
			}
			
			s.GenerateSample(null, train_buff, output_buff);
			s.AddToKNeighbours(max_num);
			
			ptr = ptr.next_ptr;
		}

	}
	
	// This prints the set of k-neighbours
	public void PrintKNeighbours() {
		
		LinkSample ptr = k_closest;
		while(ptr != null) {
			System.out.print(ptr.dim_weight+" ");
			ptr = ptr.next_ptr;
		}
		
		System.out.println("");
	}
	
	// This randomly selects a sample with a probability proportional to the prior
	public Sample SelectSample() {
		
		if(sample_map.size() == 0) {
			return null;
		}
		
		float sum = 0;
		float min = Float.MAX_VALUE;
		LinkSample ptr = k_closest;
		while(ptr != null) {
			min = Math.min(min, ptr.s.weight);
			ptr = ptr.next_ptr;
		}
		
		min = Math.abs(min) + 1;
		ptr = k_closest;
		while(ptr != null) {
			sum += ptr.s.weight + min;
			if(ptr.s.weight + min < 0) {
				System.out.println("neg val");System.exit(0);
			}

			ptr = ptr.next_ptr;
		}
		
		float val = (float) (Math.random() * sum);

		sum = 0;
		ptr = k_closest;
		while(ptr != null) {
			sum += ptr.s.weight + min;

			if(sum >= val) {
				return ptr.s;
			}
			
			ptr = ptr.next_ptr;
		}
		System.out.println("error");System.exit(0);
		return null;
	}
	
	// This resets the set of k closest samples
	public void ResetKClosest() {
		k_closest = null;
		sample_map.clear();
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
			ArrayList<KDNode> class_buff, int vec_offset) {
			
		Attribute atts[] = new Attribute[set.size()];
		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(4);
		 fvClassVal.addElement("n2");
		 fvClassVal.addElement("n1");
		 fvClassVal.addElement("p1");
		 fvClassVal.addElement("p2");
		 Attribute ClassAttribute = new Attribute("theClass", fvClassVal);
	    
		FastVector fvWekaAttributes = new FastVector(set.size() + 1);
	    for(int i=0; i<set.size(); i++) {
	    	atts[i] = new Attribute("att"+i);
	    	fvWekaAttributes.addElement(atts[i]);
	    }
	    
	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    Instances  isTrainingSet = new Instances("Rel", fvWekaAttributes, class_buff.size()); 
	    isTrainingSet.setClassIndex(set.size());
		
	    for(int k=0; k<class_buff.size(); k++) {
	    	
	    	ArrayList<Integer> data_set = new ArrayList<Integer>();
			for(int j=0; j<set.size(); j++) {
				
					int offset = 0;
					Set<Entry<Integer, Double>> s = set.instance(j).entrySet();
					double sample[] = new double[dim_num];
					for(Entry<Integer, Double> val : s) {
						sample[offset++] = val.getValue();
					}
					
					int output = class_buff.get(k).classifier.Output(sample);
					data_set.add(output);
			}
			
			if(data_set.size() != set.size()) {
				System.out.println("dim mis");System.exit(0);
			}
						
			Instance iExample = new Instance(set.size() + 1);
			for(int j=0; j<set.size(); j++) {
				iExample.setValue(j, data_set.get(j));
			}
			
			if(Math.random() < 0.5) {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(set.size()), "n1");    
			} else {
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(set.size()), "p1"); 
			}
			 
			// add the instance
			isTrainingSet.add(iExample);
	    }
		
	    System.out.println(dim_num+"  *************");
	    int select_num = 16;
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
	public static void BuildHierarchyTree(Dataset d, int dim_num,
			ArrayList<KDNode> class_buff) throws IOException {
		
		for(int k=0; k<class_buff.size(); k++) {
			class_buff.get(k).class_vect = new double[20];
		}
		
		CreateClassifierVec(d, dim_num, class_buff, 0);
		
		while(root_map.size() > 4) {
			
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
			
			float length = 0;
			for(int i=0; i<parent.class_vect.length; i++) {
				length += parent.class_vect[i] * parent.class_vect[i];
			}
			
			length = (float) Math.sqrt(length);
			for(int i=0; i<parent.class_vect.length; i++) {
				parent.class_vect[i] /= length;
			}
			
			parent.left_ptr.parent_ptr = parent;
			parent.right_ptr.parent_ptr = parent;
		}
		
		int offset = 0;
		SampleSpace s = new SampleSpace();
		for(KDNode k : KDNode.RootSet()) {
			KDNodeDim dim = new KDNodeDim(k, offset++);
			s.k_neighbour.add(dim);
			s.kdnode_dim.add(dim);
		}
		
		for(KDNode k : KDNode.RootSet()) {
			k.space = s;
		}
	}
	
	// This resets the KDNode tree following several decompressions
	public static void Reset() {
		
		ArrayList<KDNode> root_buff = new ArrayList<KDNode>();
		for(KDNode k : KDNode.RootSet()) {
			root_buff.add(k);
		}
		
		root_map.clear();
		
		for(int i=0; i<root_buff.size(); i++) {
			KDNode ptr = root_buff.get(i);
			
			KDNode prev_ptr = ptr;
			while(ptr != null) {
				ptr.is_node_set = false;
				ptr.space.sample_buff.clear();
				prev_ptr = ptr;
				ptr = ptr.parent_ptr;
			}
			
			root_map.add(prev_ptr);
		}
		
	}

	// This embeds an s-link in the hierarchy
	public static void EmbedSLink(SLink s_link) {
		
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
	
	// This decompresses the node
	public void DecompressNode(int level_num, ArrayList<KDNode> child_buff) {
		
		if((left_ptr == null && right_ptr == null) || (level_num <= 0)) {
			is_node_set = true;
			root_map.add(this);
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
	
	// This finds the weighted class label for the kd-node
	public int ClassLabel(double sample[]) {
		
		if(k_closest == null) {
			class_label = Output(sample);
			return class_label;
		}
		
		float min = Float.MAX_VALUE;
		LinkSample ptr = k_closest;
		while(ptr != null) {
			min = Math.min(ptr.s.weight, min);
			ptr = ptr.next_ptr;
		}
		
		min = Math.abs(min) + 1;
		
		float class_count[] = new float[]{0, 0, 0, 0};
		ptr = k_closest;
		while(ptr != null) {
			
			ptr.s.norm_weight = (ptr.s.weight + min);
			Sample s = ptr.s;
			
			int out = s.Classify(sample);
			
			switch(out) {
				case -2: class_count[0] += s.norm_weight; break;
				case -1: class_count[1] += s.norm_weight; break;
				case 1: class_count[2] += s.norm_weight; break;
				case 2: class_count[3] += s.norm_weight; break;
			}
			
			ptr = ptr.next_ptr;
		}
		
		float max = 0;
		int label_set[] = new int[]{-2, -1, 1, 2};
		class_label = 0;
		for(int i=0; i<4; i++) {
			
			if(class_count[i] > max) {
				max = class_count[i];
				class_label = label_set[i];
			}
		}  
		
		return class_label;
	}
}
