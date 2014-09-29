package ParticleFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import ParticleFilter.KDNode.LinkSample;

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
	
	// This stores the set of root maps
	private ArrayList<GraphNode> graph_set = new ArrayList<GraphNode>();
	// This defines the maximum allowed samples at any time
	private int MAX_SAMPLE_NUM = 1100;
	
	// This stores the data set used for population growth
	private Dataset train_set;
	// This stores the current training instance
	private int train_inst = 0;
	
	// This stores the set of training samples
	private static ArrayList<double []> train_buff = new ArrayList<double []>();
	// This stores the set of training samples output
	private ArrayList<Integer> output_buff = new ArrayList<Integer>();
	// This stores the most recently generated set of samples
	private static ArrayList<Sample> sample_buff = new ArrayList<Sample>();
	
	public SampleGraph(Dataset training_set, int train_num) {
		
		train_set = training_set;
		train_inst = train_set.size() - train_num;
		Reset();
	}
	
	// This resets the sample graph
	public void Reset() {
		
		KDNode.Reset();
		LoadGraph();
		
		for(KDNode k : KDNode.RootSet()) {
			k.space.sample_buff.clear();
		}
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
				Sample s = new Sample(k.space);
				s.GenerateSample(KDNode.DimNum());
				s.AddToKNeighbours(MAX_SAMPLE_NUM / KDNode.DimNum());
			}
		}
		
	}
	
	// This updates the fitness of each sample
	private GraphNode UpdateFitness(boolean next_sample) {
		
		if(next_sample == true || train_buff.size() == 0) {
			int offset = 0;
			int output = (Integer)train_set.instance(train_inst).classValue();
			Set<Entry<Integer, Double>> s = train_set.instance(train_inst).entrySet();
			double sample[] = new double[Classify.DimNum()];
			
			for(Entry<Integer, Double> val : s) {
				sample[offset++] = val.getValue();
			}
			
			train_buff.add(sample);
			output_buff.add(output);
		}
		
		double sample[] = train_buff.get(train_buff.size() - 1);
		int output = output_buff.get(output_buff.size() - 1);
		
		if(next_sample == true) {
			train_inst++;
		}
		
		for(KDNode k : KDNode.RootSet()) {
			k.UpdateSampleWeights(sample, output);
		}
		
		float max = -Float.MAX_VALUE;
		GraphNode node = null;

		for(int i=0; i<graph_set.size(); i++) {
			KDNode k = graph_set.get(i).kd_node;
			float util = k.AssignFitness();
			//System.out.println(util+" (((");

			if(util > max) {
				max = util;
				node = graph_set.get(i);
			}
		}
		
		return node;
	}
	
	// This grows the set of samples using MCMC
	public float GrowSampleSet(boolean next_sample) {
		
		GraphNode node = UpdateFitness(next_sample);
		sample_buff.clear();
		
		float max = 0;
		Random r = new Random();
		for(int i=0; i<MAX_SAMPLE_NUM; i++) {
			
			Sample s = node.kd_node.SelectSample();
			if(node.forward_link == null || s == null) {
				// randomly select a new node
				node = graph_set.get(r.nextInt(graph_set.size()));
				i--;
				continue;
			}
			
			Sample ns = new Sample(node.kd_node.space);
			ns.GenerateSample(s, train_buff, output_buff);
			node.kd_node.space.sample_buff.add(ns);
			sample_buff.add(ns);
			
			max = Math.max(max, ns.weight);
			
			float min = Float.MAX_VALUE;
			GraphLink link = node.forward_link;
			while(link != null) {
				min = (float) Math.min(min, link.trav_prob * link.node.kd_node.fitness);
				link = link.next_ptr;
			}
			
			min = Math.abs(min) + 1;

			float sum = 0;
			link = node.forward_link;
			while(link != null) {
				sum += (float)(link.trav_prob * link.node.kd_node.fitness) + min;
				
				if((float)(link.trav_prob * link.node.kd_node.fitness) + min < 0) {
					System.out.println("neg val");System.exit(0);
				}
				link = link.next_ptr;
			}
			
			float val = (float) (Math.random() * sum);
			
			sum = 0;
			link = node.forward_link;
			while(link != null) {
				sum += (float)(link.trav_prob * link.node.kd_node.fitness) + min;

				if(sum >= val) {
					node = link.node;
					break;
				}
				
				link = link.next_ptr;
			}
			
			if(link == null) {
				System.out.println("error");System.exit(0);
			}
		}
		
		// add the back buffer to the forward buffer
		for(KDNode k : KDNode.RootSet()) {
			k.ResetKClosest();
		}
		
		for(int i=0; i<sample_buff.size(); i++) {
			sample_buff.get(i).AddToKNeighbours(MAX_SAMPLE_NUM / KDNode.DimNum());
		}
		
		for(KDNode k : KDNode.RootSet()) {
			k.AddKSamples(MAX_SAMPLE_NUM / KDNode.DimNum());
		}
		
		
		return max;
	}
	
	// This rearranges the samples in a new dimensional space through
	// decompression operation
	public void DecompressMaXDim() {
		
		KDNode node = MaxDim();
		
		HashMap<KDNode, Integer> root_map = new HashMap<KDNode, Integer>();
		ArrayList<KDNode> child_buff = new ArrayList<KDNode>();
		node.DecompressNode(2, child_buff);
		
		System.out.println("decomp: "+node+"  "+child_buff.size());
		
		for(int i=0; i<node.space.kdnode_dim.size(); i++) {
			KDNodeDim dim = node.space.kdnode_dim.get(i);
			root_map.put(dim.kdnode, dim.dim);
		}

		SampleSpace child_space = null;
		for(int i=0; i<child_buff.size(); i++) {
			KDNode k = child_buff.get(i);
			k.space = new SampleSpace();
			k.space.kdnode_dim = new ArrayList<KDNodeDim>(node.space.kdnode_dim);
			child_space = k.space;
			k.ResetKClosest();
			
			for(int j=0; j<child_buff.size(); j++) {
				KDNodeDim dim = new KDNodeDim(child_buff.get(j), j);
				
				k.space.kdnode_dim.add(dim);
				k.space.k_neighbour.add(dim);
			}
			
			for(int j=0; j<k.space.kdnode_dim.size(); j++) {
				k.space.kdnode_dim.get(j).dim = j;
			}
		}
		
		node.ProjectSamples(child_space, MAX_SAMPLE_NUM / KDNode.DimNum(), root_map, train_buff, output_buff);
		
		LoadGraph();
	}

	// This returns the dimension with the highest fitness
	private KDNode MaxDim() {
		float max = -Float.MAX_VALUE;
		KDNode node = null;
		for(KDNode k : KDNode.RootSet()) {
			
			if(k.left_ptr == null && k.right_ptr == null) {
				continue;
			}
			
			float util = k.fitness;
			if(util > max) {
				max = util;
				node = k;
			}
		}
		return node;
	}
	
	// This updates the training instances for the system dynamics
	public void UpdateTrainingInst() {
		
		double sample[] = train_buff.get(train_buff.size() - 1);
		int output = output_buff.get(output_buff.size() - 1);
		
		ClassifyDynamics.AddTrainingSample(sample, output);
	}
	
	// This returns the current training instance
	public int TrainInst() {
		return train_inst;
	}
	
	// This returns the majority class label
	public static int ClassLabel() {
		
		double sample[] = train_buff.get(train_buff.size() - 1);
		
		float min = Float.MAX_VALUE;
		for(int i=0; i<sample_buff.size(); i++) {
			min = Math.min(sample_buff.get(i).weight, min);
		}
			
		min = Math.abs(min) + 1;
		
		float class_count[] = new float[]{0, 0, 0, 0};
		for(int i=0; i<sample_buff.size(); i++) {
			sample_buff.get(i).norm_weight = sample_buff.get(i).weight + min;
			Sample s = sample_buff.get(i);
			
			int out = s.Classify(sample);
			
			switch(out) {
				case -2: class_count[0] += s.norm_weight; break;
				case -1: class_count[1] += s.norm_weight; break;
				case 1: class_count[2] += s.norm_weight; break;
				case 2: class_count[3] += s.norm_weight; break;
			}
		}
		
		float max = 0;
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
	public int NextDayOutput() {
		
		double sample[] = train_buff.get(train_buff.size() - 1);
		Integer label1 = ClassifyDynamics.ClassLabel(sample);
		int label = ClassLabel();
  
		
		System.out.println(label+"  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		return label1 != null ? label1 : label;
	}
	

}
