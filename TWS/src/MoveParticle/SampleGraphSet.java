package MoveParticle;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;


import net.sf.javaml.core.Dataset;

public class SampleGraphSet {
	
	// This stores the class weight distribution and the corresponding graph
	class ClassWeightNode {
		float weight;
		SampleGraph graph;
		
		public ClassWeightNode(float weight, SampleGraph graph) {
			this.weight = weight;
			this.graph = graph;
		}
	}

	// This defines the maximum number of graphs
	private int MAX_GRAPH_NUM = 4;
	// This stores the set of sample graphs
	private HashSet<SampleGraph> graph_map = new HashSet<SampleGraph>();
	// This stores the root graph
	private SampleGraph root_graph;
	
	public SampleGraphSet() {
		SampleGraph g = new SampleGraph();
		graph_map.add(g);
		g.InitSampleSet();
		root_graph = g;
	}
	
	// This finds the set of graph weights
	private ArrayList<ClassWeightNode> GraphWeights(boolean is_add_graph) {
		
		ArrayList<ClassWeightNode> set = new ArrayList<ClassWeightNode>();
		for(SampleGraph g : graph_map) {
			
			if(g.space.sample_history < 4) {
				continue;
			}
			
			if(is_add_graph == true) {
				ArrayList<Float> buff = g.ClassWeightDist();
				for(int j=0; j<buff.size(); j++) {
					set.add(new ClassWeightNode(buff.get(j), g));
				}
			} else {
				set.add(new ClassWeightNode(g.AvgUtil(), g));
			}
		}
		
		Collections.sort(set, new Comparator<ClassWeightNode>() {
			 
	        public int compare(ClassWeightNode arg1, ClassWeightNode arg2) {
	        	
	        	if(arg1.weight < arg2.weight) {
	    			return -1;
	    		}

	    		if(arg1.weight > arg2.weight) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		return set;
	}
	
	// This removes all the child graphs
	private void RemoveGraph(SampleGraph g) {
		
		graph_map.remove(g);
		for(int i=0; i<g.child_buff.size(); i++) {
			RemoveGraph(g.child_buff.get(i));
		}
	}
	
	// This counts the number of children
	private int CountChildren(SampleGraph g) {
		
		int sum = g.child_buff.size();
		for(int i=0; i<g.child_buff.size(); i++) {
			
			if(graph_map.contains(g.child_buff.get(i)) == false) {
				System.out.println("child not found");System.exit(0);
			}
			sum += CountChildren(g.child_buff.get(i));
		}
		
		return sum;
	}
	
	// This updates the graph set
	public void UpdateGraphSet(Dataset training_set, int train_num) {
		
		for(SampleGraph g : graph_map) {
			g.AssignSampleClassWeight();
		}
		
		ArrayList<ClassWeightNode> set = GraphWeights(false);
		
		System.out.println(set.size());
		
		if(graph_map.size() >= MAX_GRAPH_NUM) {
			// remove a graph
			int id = 0;
			if(set.get(id).graph == root_graph) {
				id++;
			}
			
			if(id < set.size()) {
				SampleGraph g = set.get(id).graph;
				
				System.out.println(graph_map.size());
				RemoveGraph(g);
				
				if(g.parent_ptr != null) {
					g.parent_ptr.RemoveChildGraph(g);
				}
				
				System.out.println("klo "+id+" "+graph_map.size());
			}
		}
		
		// add a graph
		set = GraphWeights(true);
		graph_map.add(new SampleGraph(set.get(set.size()-1).graph));

		if(CountChildren(root_graph) != graph_map.size()-1) {
			System.out.println("child miss");System.exit(0);
		}
	}
	
	// This is used to predict the next day class label
	public int NextDayOutput(double sample[]) {
		
		return SampleGraph.NextDayOutput(graph_map, sample);
	}

	// This grows the set of samples using MCMC
	public void GrowSampleSet() {
		
		for(SampleGraph g : graph_map) {
			g.GrowSampleSet();
		}
	}
	
	// This is used to cull samples with a low utility
	public void ReduceSamples(double sample[], int output) {
		
		for(SampleGraph g : graph_map) {
			g.ReduceSamples(sample, output);
		}
	}

}
