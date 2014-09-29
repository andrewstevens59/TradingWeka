package MoveParticle;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import net.sf.javaml.core.Dataset;

public class SampleGraph {

	// This stores a graph link
	class GraphLink {
		double trav_prob = 0;
		GraphNode node;
		GraphLink next_ptr;
	}
	
	// This stores the graph node
	class GraphNode {
		// This stores the node id
		int node_id;
		// This stores the kd-node
		KDNode kd_node;
		// This stores the set of forward graph links
		GraphLink forward_link = null;
		
		public GraphNode(int node_id, KDNode kd_node) {
			this.node_id = node_id;
			this.kd_node = kd_node;
		}
	}
	
	// This stores the parent graph
	public SampleGraph parent_ptr = null;
	// This stores the child buff
	public ArrayList<SampleGraph> child_buff = new ArrayList<SampleGraph>();
	// This stores the set of expanded children
	private HashSet<KDNode> expand_map = new HashSet<KDNode>();
	// This stores the set of expanded children
	private HashMap<SampleGraph, KDNode> graph_map = new HashMap<SampleGraph, KDNode>();
	
	// This stores the set of root maps
	private ArrayList<GraphNode> graph_set = new ArrayList<GraphNode>();
	// This defines the maximum allowed samples at any time
	private int MAX_SAMPLE_NUM = 500;
	
	// This stores the most recently generated set of samples
	private ArrayList<Sample> sample_buff = new ArrayList<Sample>();
	// This stores the sample space for this graph
	public SampleSpace space = new SampleSpace();
	
	// This stores the average class weight for all samples
	private float class_weight[];
	// This stores the average weight of the samples
	private float avg_weight;
	
	public SampleGraph() {

		Reset();
		
		int offset = 0;
		for(KDNode k : KDNode.RootSet()) {
			KDNodeDim dim = new KDNodeDim(k, offset++, 1.0f);
			space.kdnode_dim.add(dim);
			space.node_space.put(k, dim.dim);
			space.node_weight.put(k, dim.weight);
		}
		
		space.NormClassWeight();
	}
	
	// This constructs a set of samples in an expanded space
	public SampleGraph(SampleGraph g) {
		
		this.space.sample_history = g.space.sample_history >> 1;
		
		float max = -Float.MAX_VALUE;
		int max_dim = 0;
		KDNode max_node = null;
		for(int i=0; i<g.space.kdnode_dim.size(); i++) {
			KDNodeDim dim = g.space.kdnode_dim.get(i);
			if(expand_map.contains(dim.dim) == true) {
				continue;
			}
			
			if(g.class_weight[dim.dim] > max) {
				max = g.class_weight[i];
				max_node = dim.kdnode;
				max_dim = dim.dim;
			}
		}
		
		g.graph_map.put(this, max_node);
		g.expand_map.add(max_node);
		g.child_buff.add(this);
		this.parent_ptr = g;
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		max_node.DecompressNode(1, buff);

		int offset = 0;
		for(int i=0; i<g.space.kdnode_dim.size(); i++) {
			KDNodeDim dim = g.space.kdnode_dim.get(i);
			if(dim.kdnode != max_node) {
				KDNodeDim dim1 = new KDNodeDim(dim.kdnode, offset, dim.weight * g.class_weight[dim.dim]);
				space.kdnode_dim.add(dim1);
				offset++;
			}
		}

		for(int i=0; i<buff.size(); i++) {
			KDNodeDim dim1 = new KDNodeDim(buff.get(i), offset++, 1.0f);
			space.kdnode_dim.add(dim1);
		}
		
		space.NormClassWeight();
		
		for(int i=0; i<space.kdnode_dim.size(); i++) {
			KDNodeDim dim = space.kdnode_dim.get(i);
			space.node_space.put(dim.kdnode, dim.dim);
			space.node_weight.put(dim.kdnode, dim.weight);
		}

		for(int i=0; i<g.sample_buff.size(); i++) {
			Sample old = g.sample_buff.get(i);
			
			Sample s = new Sample(space);
			s.class_weight = new float[space.kdnode_dim.size()];
			
			offset = 0;
			for(int j=0; j<g.space.kdnode_dim.size(); j++) {
				KDNodeDim dim = g.space.kdnode_dim.get(j);
				if(dim.kdnode != max_node) {
					s.class_weight[offset++] = old.class_weight[dim.dim];
				}
			}
			
			for(int j=0; j<buff.size(); j++) {
				float len = old.class_weight[max_dim];
				KDNodeDim dim = space.kdnode_dim.get(offset++);
				s.class_weight[dim.dim] = dim.kdnode.ProjectClassVector(max_node, len);	
			}
			
			Sample.NormVect(s.class_weight);
			
			for(int j=0; j<space.kdnode_dim.size(); j++) {
				KDNodeDim dim = space.kdnode_dim.get(i);
				s.class_weight[dim.dim] *= dim.weight;
			}
			
			Sample.NormVect(s.class_weight);
			
			s.AssignMotionVect();
			s.CopySampleHistory(old);
			sample_buff.add(s);
		}
	}
	
	// This removes a child graph from the set
	public void RemoveChildGraph(SampleGraph g) {
		
		ArrayList<SampleGraph> temp = new ArrayList<SampleGraph>();
		for(int i=0; i<child_buff.size(); i++) {
			if(child_buff.get(i) != g) {
				temp.add(child_buff.get(i));
			}
		}
		
		KDNode k = graph_map.get(g);
		graph_map.remove(g);
		expand_map.remove(k);
		child_buff = temp;
	}
	
	// This returns the set of available set of class weights
	public ArrayList<Float> ClassWeightDist() {
		
		System.out.println(space.kdnode_dim.size()+" (((( "+expand_map.size());
		ArrayList<Float> expand_buff = new ArrayList<Float>();
		for(int i=0; i<space.kdnode_dim.size(); i++) {
			KDNodeDim dim = space.kdnode_dim.get(i);
			if(expand_map.contains(dim.kdnode) == true) {
				continue;
			}
			
			expand_buff.add(class_weight[dim.dim]);
		}

		return expand_buff;
	}
	
	// This returns the average graph utility
	public float AvgUtil() {
		return avg_weight;
	}
	
	// This assigns the sample class weight
	public float AssignSampleClassWeight() {
		
		avg_weight = 0;
		for(int i=0; i<sample_buff.size(); i++) {
			avg_weight += sample_buff.get(i).Weight();
		}
		
		avg_weight /= sample_buff.size();
		class_weight = new float[space.kdnode_dim.size()];
		for(int i=0; i<class_weight.length; i++) {
			class_weight[i] = 0;
		}
		
		for(int i=0; i<sample_buff.size(); i++) {
			Sample s = sample_buff.get(i);
			for(int j=0; j<class_weight.length; j++) {
				class_weight[j] += s.class_weight[j] * s.weight;
			}
		}
		
		Sample.NormVect(class_weight);
		
		for(int i=0; i<class_weight.length; i++) {
			System.out.print(class_weight[i]+" ");
		}
		System.out.println(" ((((");
		
		avg_weight /= sample_buff.size();
		return avg_weight;
	}
	
	// This resets the sample graph
	public void Reset() {
		
		KDNode.Reset();
		LoadGraph();
	}
	
	// This loads the graph from the KDNode hierarchy
	private void LoadGraph() {
		
		int offset = 0;
		HashMap<String, GraphLink> link_map = new HashMap<String, GraphLink>();
		HashMap<KDNode, GraphNode> node_map = new HashMap<KDNode, GraphNode>();
		
		graph_set.clear();
		for(KDNode k : KDNode.RootSet()) {
			GraphNode n = new GraphNode(offset++, k);
			node_map.put(k, n);
			graph_set.add(n);
		}
		
		for(KDNode k : KDNode.RootSet()) {
			
			GraphNode n = node_map.get(k);
			 
			SLink link = k.s_links;
			if(link == null) {
				//System.out.println("no links");System.exit(0);
			}
			
			float sum = 0;
			while(link != null) {
				String str = k + " " + link.dst.CompNode();
				GraphLink graph_link = link_map.get(str);
				if(graph_link == null) {
					GraphLink prev_ptr = n.forward_link;
					n.forward_link = new GraphLink();
					n.forward_link.node = node_map.get(link.dst.CompNode());
					n.forward_link.next_ptr = prev_ptr;
					n.forward_link.trav_prob = 0;
					graph_link = n.forward_link;
					
					link_map.put(str, n.forward_link);
				}
				
				sum += link.trav_prob;
				graph_link.trav_prob += link.trav_prob;
				link = link.next_ptr;
			}
			
			GraphLink ptr = n.forward_link;
			while(ptr != null) {
				ptr.trav_prob /= sum;
				ptr = ptr.next_ptr;
			}
		}
	}
	
	// This initializes the graph with so many random samples
	public void InitSampleSet() {

		System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&");
		
		for(int i=0; i<MAX_SAMPLE_NUM; i++) {
			for(KDNode k : KDNode.RootSet()) {
				Sample s = new Sample(space);
				s.GenerateSample(KDNode.DimNum());
				sample_buff.add(s);
			}
		}
		
	}
	
	// This grows the set of samples using MCMC
	public void GrowSampleSet() {
		
		Random r = new Random();
		for(int i=0; i<MAX_SAMPLE_NUM; i++) {
			
			Sample s = sample_buff.get(r.nextInt(MAX_SAMPLE_NUM));
			Sample ns = new Sample(s.space);
			ns.GenerateSample(s.space.kdnode_dim.size());
			sample_buff.add(ns);
		}
	}
	
	// This is used to cull samples with a low utility
	public void ReduceSamples(double sample[], int output) {

		for(int i=0; i<sample_buff.size(); i++) {
			Sample s = sample_buff.get(i);
			s.UpdateSampleWeight(sample, output);
			s.norm_weight = s.Weight();
		}
		
		Collections.sort(sample_buff, new Comparator<Sample>() {
			 
	        public int compare(Sample arg1, Sample arg2) {
	        	
	        	if(arg1.norm_weight < arg2.norm_weight) {
	    			return 1;
	    		}

	    		if(arg1.norm_weight > arg2.norm_weight) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		while(sample_buff.size() > MAX_SAMPLE_NUM) {
			sample_buff.remove(sample_buff.size()-1);
		}
	}
	
	// This returns the majority class label
	public static int ClassLabel(HashSet<SampleGraph> graph_map, double sample[]) {
		
		float min = Float.MAX_VALUE;
		for(SampleGraph g : graph_map) {
			for(int i=0; i<g.sample_buff.size(); i++) {
				min = Math.min(g.sample_buff.get(i).weight, min);
			}
		}
			
		min = Math.abs(min) + 1;
		
		float class_count[] = new float[]{0, 0, 0, 0};
		for(SampleGraph g : graph_map) {
			

			for(int i=0; i<g.sample_buff.size(); i++) {
				g.sample_buff.get(i).norm_weight = g.sample_buff.get(i).weight + min;
				Sample s = g.sample_buff.get(i);
				
				int out = s.Classify(sample);
				
				switch(out) {
					case -2: class_count[0] += s.norm_weight; break;
					case -1: class_count[1] += s.norm_weight; break;
					case 1: class_count[2] += s.norm_weight; break;
					case 2: class_count[3] += s.norm_weight; break;
				}
			}
		}
		
		float max = -Float.MAX_VALUE;
		int label_set[] = new int[]{-2, -1, 1, 2};
		int label = 0;
		for(int i=0; i<4; i++) {
			
			if(class_count[i] > max) {
				max = class_count[i];
				label = label_set[i];
			}
		}
		
		return label;
	}
	
	// This is used to predict the next day class label
	public static int NextDayOutput(HashSet<SampleGraph> graph_map, double sample[]) {

		int label = ClassLabel(graph_map, sample);
  
		
		System.out.println(label+"  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		return label;
	}
	

}
