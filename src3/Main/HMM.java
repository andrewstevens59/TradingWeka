package Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import VectorMethod.VectorMethod;


public class HMM {
	
	// This  one of the dynamic programming 
	class Node {
		// This stores the likelihood of the path
		public double prob;
		// This stores the graph node
		public Node max_node;
		// This stores the node id
		public int node_id;
		// This stores the node depth
		public int depth;
	}
	
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
		// This stores the set of backward graph links
		GraphLink backward_link = null;
		
		public GraphNode(int node_id, KDNode kd_node) {
			this.node_id = node_id;
			this.kd_node = kd_node;
		}
	}
	
	// This stores an output node used for classification
	class OutputNode {
		double prob;
		KDNode classifier;
	}
	
	// This stores the set of root maps
	private ArrayList<GraphNode> graph_set = new ArrayList<GraphNode>();
	// This stores the dynamic programming grid
	private Node dyn_grid[][];
	// This stores the top set of current ranked regimes
	private ArrayList<Node> regime_rank_buff = new ArrayList<Node>();
	// This is used to training the output of the classifier
	private VectorMethod vm = null;
	// This stores the set of output nodes
	private ArrayList<OutputNode> output_buff = new ArrayList<OutputNode>();
	
	// This stores the set of expansion nodes
	private PriorityQueue<Node> state_queue = new PriorityQueue<Node>(10, new Comparator<Node>() {
		 
        public int compare(Node arg1, Node arg2) {
        	
        	if(arg1.depth < arg2.depth) {
    			return 1;
    		}

    		if(arg1.depth > arg2.depth) {
    			return -1;
    		}
    		
    		if(arg1.prob < arg2.prob) {
    			return 1;
    		}

    		if(arg1.prob > arg2.prob) {
    			return -1;
    		}

    		return 0; 
        }
    });
	

	// This creates the set of transition probabilities
	private void CreateTransProb(DataSet d) {
		
		ArrayList<double []> member_prob_buff = new ArrayList<double []>();
		CalcObsProb(d.RegimeSet(), member_prob_buff);
		
		for(int k=0; k<member_prob_buff.size()-1; k++) {
			
			for(int i=0; i<graph_set.size(); i++) {
				
				if(graph_set.get(i).node_id != i) {
					System.out.println("errror");System.exit(0);
				}
				
				GraphLink link = graph_set.get(i).forward_link;
				while(link != null) {
					GraphNode n = graph_set.get(link.node.node_id);
					link.trav_prob += member_prob_buff.get(k)[i] * member_prob_buff.get(k+1)[n.node_id];
					link = link.next_ptr;
				}
			}
		}
		
		for(int i=0; i<graph_set.size(); i++) {
			
			double sum = 0;
			GraphLink link = graph_set.get(i).forward_link;
			while(link != null) {
				sum += link.trav_prob;
				link = link.next_ptr;
			}
			
			link = graph_set.get(i).forward_link;
			while(link != null) {
				link.trav_prob /= sum;
				GraphLink back_link = new GraphLink();
				
				GraphLink prev_ptr = link.node.backward_link;
				link.node.backward_link = back_link;
				back_link.next_ptr = prev_ptr;
				back_link.trav_prob = link.trav_prob;
				back_link.node = graph_set.get(i);
				
				link = link.next_ptr;
			}
		}
	}

	// This calculates the set of observation probabilities
	private void CalcObsProb(ArrayList<RegimeSet> regime_buff, 
			ArrayList<double[]> member_prob_buff) {
		
		double avg = 0;
		for(int i=0; i<regime_buff.size(); i++) {
			
			double max = 0;
			double obs_prob[] = new double[graph_set.size()];
			for(int j=0; j<graph_set.size(); j++) {
				KDNode kn = graph_set.get(j).kd_node;
				
				double true_count = 0;
				RegimeSet r = regime_buff.get(i);
				for(int k=0; k<r.data_set.size(); k++) {
					
					boolean sample[] = r.data_set.get(k);
					boolean output = r.output_set.get(k)[0];
					
					boolean val = kn.MajorityVote(sample);
					if(val == output) {
						true_count++;
					}
				}
				
				obs_prob[j] = true_count / r.data_set.size();
				max = Math.max(max, obs_prob[j]);
			}
			
			avg += max;
			
			member_prob_buff.add(obs_prob);
		}
		
		System.out.println(avg / regime_buff.size());
	}

	public HMM(DataSet d) {
		
		int offset = 0;
		HashSet<String> link_map = new HashSet<String>();
		HashMap<KDNode, GraphNode> node_map = new HashMap<KDNode, GraphNode>();
		
		for(KDNode k : KDNode.RootSet()) {
			GraphNode n = new GraphNode(offset++, k);
			node_map.put(k, n);
			graph_set.add(n);
		}
		
		for(KDNode k : KDNode.RootSet()) {
			
			GraphNode n = node_map.get(k);
			 
			SLink link = k.s_links;
			if(link == null) {
				System.out.println("no links");System.exit(0);
			}
			while(link != null) {
				String str = k + " " + link.dst.CompNode();
				if(link_map.contains(str) == false) {
					GraphLink prev_ptr = n.forward_link;
					n.forward_link = new GraphLink();
					n.forward_link.node = node_map.get(link.dst.CompNode());
					n.forward_link.next_ptr = prev_ptr;
					
					link_map.add(str);
				}
				link = link.next_ptr;
			}
		}
		
		CreateTransProb(d);
	}
	
	// This finds the optimal dynamic programming path
	private void OptimalPath(ArrayList<RegimeSet> regime_buff) {
		
		
		for(int j=0; j<graph_set.size(); j++) {
			regime_rank_buff.add(dyn_grid[regime_buff.size()-1][j]);
		}
		
		Collections.sort(regime_rank_buff, new Comparator<Node>() {
			 
	        public int compare(Node arg1, Node arg2) {
	        	
	        	if(arg1.prob < arg2.prob) {
	    			return 1;
	    		}

	    		if(arg1.prob > arg2.prob) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<6; i++) {
			state_queue.add(regime_rank_buff.get(i));
		}
		
		HashSet<Node> node_map = new HashSet<Node>();
		
		while(state_queue.size() > 0) {
			
			Node n = state_queue.remove();
			graph_set.get(n.node_id).kd_node.DecompressNode(1);
			node_map.add(n);
			
			if(node_map.size() > 30) {
				break;
			}
			
			if(n.max_node == null) {
				continue;
			}
			
			if(node_map.contains(n.max_node) == true) {
				continue;
			}
			
			state_queue.add(n.max_node);
		}
		
	}
	
	// This creates the set hmm model 
	public void CreateHMM(DataSet d) {
		
		ArrayList<RegimeSet> regime_buff = new ArrayList<RegimeSet>();
		
		int offset = 0;
		RegimeSet r = new RegimeSet();
		for(int i=d.TestDataSet().size()-800; i<d.TestDataSet().size(); i++) {
			r.data_set.add(d.TestDataSet().get(i));
			r.output_set.add(d.TestOutputSet().get(i));
			
			if(++offset >= 100) {
				regime_buff.add(r);
				r = new RegimeSet();
				offset = 0;
			}
		}
		
		if(offset > 0) {
			regime_buff.add(r);
		}
		
		ArrayList<double []> member_prob_buff = new ArrayList<double []>();
		CalcObsProb(regime_buff, member_prob_buff);
		
		HashSet<KDNode> map = new HashSet<KDNode>();
		for(int i=0; i<member_prob_buff.size(); i++) {
			
			double max = 0;
			KDNode max_node = null;
			double set[] = member_prob_buff.get(i);
			for(int j=0; j<graph_set.size(); j++) {
				if(set[j] > max && map.contains(graph_set.get(j).kd_node) == false) {
					max = set[j];
					max_node = graph_set.get(j).kd_node;
				}
			}
			
			map.add(max_node);
			max_node.DecompressNode(2);
		}
		
		dyn_grid = new Node[regime_buff.size()][graph_set.size()];
		for(int i=0; i<graph_set.size(); i++) {
			dyn_grid[0][i] = new Node();
			dyn_grid[0][i].prob = member_prob_buff.get(0)[i];
			dyn_grid[0][i].node_id = i;
			dyn_grid[0][i].depth = 0;
		}
		
		System.out.println("(((((((((((((");
		for(int i=0; i<regime_buff.size(); i++) {
			
			double max = 0;
			for(int j=0; j<graph_set.size(); j++) {
				max = Math.max(member_prob_buff.get(i)[j], max);
			}
			
			System.out.println(max);
		}
		
		System.out.println("(((((((((((((");
		
		for(int i=1; i<regime_buff.size(); i++) {
			
			if(member_prob_buff.get(i).length != graph_set.size()) {
				System.out.println("size miss");System.exit(0);
			}
			
			for(int j=0; j<graph_set.size(); j++) {
				dyn_grid[i][j] = new Node();
				dyn_grid[i][j].node_id = j;
				dyn_grid[i][j].depth = i;
				
				double max = 0;
				int max_node = - 1;
				GraphLink link = graph_set.get(j).backward_link;
				if(link == null) {
					System.out.println("no links");System.exit(0);
				}
				
				while(link != null) {
					double util = dyn_grid[i-1][link.node.node_id].prob * link.trav_prob;
					if(util >= max) {
						max = util;
						max_node = link.node.node_id;
					}

					link = link.next_ptr;
				}
				
				if(max_node < 0) {
					System.out.println("no links");System.exit(0);
					dyn_grid[i][j].prob = 0;
					dyn_grid[i][j].max_node = null;
					continue;
				}
				
				max = member_prob_buff.get(i)[j];
				dyn_grid[i][j].prob = max;
				dyn_grid[i][j].max_node = dyn_grid[i-1][max_node];
			}
		}
		
		for(int j=0; j<graph_set.size(); j++) {
			OutputNode n = new OutputNode();
			n.prob = dyn_grid[regime_buff.size()-1][j].prob;
			n.classifier = graph_set.get(j).kd_node;
			output_buff.add(n);
		}
		
		Collections.sort(output_buff, new Comparator<OutputNode>() {
			 
	        public int compare(OutputNode arg1, OutputNode arg2) {
	        	
	        	if(arg1.prob < arg2.prob) {
	    			return 1;
	    		}

	    		if(arg1.prob > arg2.prob) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<regime_buff.size(); i++) {
			
			for(int j=0; j<graph_set.size(); j++) {
				System.out.print(dyn_grid[i][j].prob+" ");
			}
			System.out.println("");
		}
		
		//OptimalPath(regime_buff);
	}
	
	private void TrainClassifier(DataSet d, int top_n, int data_index) throws NumberFormatException, IOException {
		
		vm = new VectorMethod();
		
		ArrayList<boolean []> data_set = new ArrayList<boolean []>();
		ArrayList<boolean []> outpt_set = new ArrayList<boolean []>();
		
		for(int i=data_index; i<data_index + 100; i++) {
			
			boolean sample[] = d.TestDataSet().get(i);
			boolean input[] = new boolean[top_n];
			
			for(int j=0; j<Math.min(output_buff.size(), top_n); j++) {
				input[j] = output_buff.get(j).classifier.MajorityVote(sample);
			}
			
			data_set.add(input);
			outpt_set.add(d.TestOutputSet().get(i));
		}
		
		DataSet d2 = new DataSet(data_set, outpt_set, data_set, outpt_set);
		vm.FindClassError(d2, 0, 10);
	}
	
	// This trains the output of the classifier on the top N regimes
	public double TrainClassifier(DataSet d) throws NumberFormatException, IOException {
		
		
		double true_count = 0;
		for(int i=d.TestDataSet().size()-100; i<d.TestDataSet().size(); i++) {
			
			boolean sample[] = d.TestDataSet().get(i);
			boolean output = d.TestOutputSet().get(i)[0];
			
			//TrainClassifier(d, 20, i-100);
			
			boolean val = Output(sample);
			System.out.println(val+" "+output);
			if(val == output) {
				true_count++;
			}
		}
		
		for(int i=0; i<output_buff.size(); i++) {
			System.out.print(output_buff.get(i).prob+" ");
		}
		System.out.println("");
		return true_count / 100;
	}
	
	// This classifies a sample
	public boolean Output1(boolean sample[]) {
		return vm.Classify(sample);
	}
	
	// This classifies a sample
	public boolean Output(boolean sample[]) {
		
		double true_count = 0;
		double false_count = 0;
		
		for(int i=0; i<Math.min(output_buff.size(), 5); i++) {
			
			boolean val = output_buff.get(i).classifier.MajorityVote(sample);
			if(val == true) {
				true_count += output_buff.get(i).prob;
			} else {
				false_count += output_buff.get(i).prob;
			}
		}
		
		return true_count > false_count;
	}

}
