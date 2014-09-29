package DecisionNetwork;

import java.util.ArrayList;
import java.util.Random;

public class SimulatedAnnealing {
	
	// This stores the set of nodes in the network
	private ArrayList<DecisionNode> node_buff = new ArrayList<DecisionNode>();
	private Voter final_node;
	
	// This builds the initial network 
	private Voter BuildNetwork(int start, int end) {
		
		Random r = new Random();
		
		if(end - start <= 1) {
			DecisionNode d = new DecisionNode(start, null); 
			d.CreateTable(r.nextInt(16), 4);
			node_buff.add(d);
			return new Voter(d);
		}
		
		int middle = (start + end) >> 1;
		
		Voter l1 = BuildNetwork(start, middle);
		
		Voter r1 = BuildNetwork(middle, end);
		
		ArrayList<Voter> child_buff = new ArrayList<Voter>();
		child_buff.add(l1);
		child_buff.add(r1);
		
		DecisionNode d = new DecisionNode(start, child_buff); 
		d.CreateTable(r.nextInt(16), 4);
		node_buff.add(d);
		return new Voter(d);
	}
	
	// This finds the truth value
	private int TruthNet(DataSet d, int sample_num, Voter v) {
		
		int true_count = 0;
		Random r = new Random();
		for(int i=0; i<sample_num; i++) {
			int id = r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			
			boolean val = v.Output(sample);
			if(val == output) {
				true_count++;
			}
		}
		
		return true_count;
	}

	public SimulatedAnnealing(DataSet d) {
		
		Voter v = BuildNetwork(0, d.DimNum());
		Random r = new Random();
		int best_val = TruthNet(d, 1000, v);
		
		final_node = v;
		
		for(int j=0; j<100000; j++) {
		
			int select = r.nextInt(node_buff.size());
			int prev[][] = node_buff.get(select).rule_table;
			node_buff.get(select).CreateTable(r.nextInt(16), 4);
			
			int true_count = TruthNet(d, 2000, v);
			
			
			if(true_count < best_val) {
				node_buff.get(select).rule_table = prev;
			} else {
				best_val = true_count;
			}
			
			System.out.print(true_count+ " ");
			
			if((j % 40) == 0) {
				System.out.println("");
			}
		}
	}
	
	// This returns the classification 
	public boolean Classification(boolean sample[]) {
		
		return final_node.Output(sample);
	}

}
