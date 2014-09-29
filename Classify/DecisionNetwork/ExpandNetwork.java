package DecisionNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;


public class ExpandNetwork {
	
	// This defines a correlation link
	class CorrLink {
		// This stores the src
		Voter src;
		// This stores the dst
		Voter dst;
		// This stores the correlation
		int corr;
	}
	
	// This stores the current number of training samples 
	private int sample_num = 2000;
	// This stores the correlation between each root node
	private int corr_num[][];
	// This stores the set of nodes
	private ArrayList<Voter> node_buff = new ArrayList<Voter>();
	// This stores the top predictors
	private ArrayList<Voter> top_voter = new ArrayList<Voter>();
	
	// This defines the number of k neighbours
	static int K_NEIGH_NUM = 5;
	// This defines the maximum number of root nodes 
	static int MAX_ROOT_NUM = 20;
	// This defines the number of new classifiers to create
	static int NEW_CLASS_NUM = 20;
	
	// This compresses the current set of vertices
	private void CompressNodes(ArrayList<CorrLink> link_buff, int root_num) {
		
		root_num -= MAX_ROOT_NUM;
		if(root_num <= 0) {
			return;
		}
		
		// biggest to smallest
		Collections.sort(link_buff, new Comparator<CorrLink>() {
			 
	        public int compare(CorrLink arg1, CorrLink arg2) {
	        	
	        	if(arg1.corr < arg2.corr) {
	    			return 1;
	    		}

	    		if(arg1.corr > arg2.corr) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		// This stores the top n links
		ArrayList<CorrLink> top_n_link = new ArrayList<CorrLink>();
		for(int i=0; i<root_num; i++) {
			top_n_link.add(link_buff.get(i));
		}

		// smallest to biggest
		Collections.sort(top_n_link, new Comparator<CorrLink>() {
			 
	        public int compare(CorrLink arg1, CorrLink arg2) {
	        	
	        	if(arg1.corr < arg2.corr) {
	    			return -1;
	    		}

	    		if(arg1.corr > arg2.corr) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<top_n_link.size(); i++) {
			
			if(top_n_link.get(i).corr > (sample_num  * 0.8f)) {
				top_n_link.get(i).src.CompressNodes(top_n_link.get(i).dst);
			}
		}
	}
	
	// This creates the correlation matrix
	private void FindCorrMat(DataSet d) {
		
		ArrayList<Voter> buff = new ArrayList<Voter>();
		for(Voter ptr : Voter.RootNode()) {
			buff.add(ptr);
		}
		
		corr_num = new int[buff.size()][buff.size()];
		for(int i=0; i<buff.size(); i++) {
			for(int j=0; j<buff.size(); j++) {
				corr_num[i][j] = 0;
			}
		}
		
		Random r = new Random();
		for(int j=0; j<sample_num; j++) {
			int id = r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			DecisionNode.IncOutputAssignID();
			
			for(Voter ptr : Voter.RootNode()) {
				ptr.Output(sample);
			}
			 
			for(int i=0; i<buff.size(); i++) {
				for(int k=i+1; k<buff.size(); k++) {
					if(buff.get(i).output_val == buff.get(k).output_val) {
						corr_num[i][k]++;
					}
				}
			}
		}
		
		ArrayList<CorrLink> link_buff = new ArrayList<CorrLink>();
		for(int i=0; i<buff.size(); i++) {
			for(int k=i+1; k<buff.size(); k++) {
				
				CorrLink link = new CorrLink();
				link.src = buff.get(i);
				link.dst = buff.get(k);
				link.corr = corr_num[i][k];
				link_buff.add(link);
			}
		}
		System.out.println("Before "+Voter.RootNodeNum());
		CompressNodes(link_buff, buff.size());
		System.out.println("After "+Voter.RootNodeNum());
	}
	
	// This decompresses the root vertex set a number of times
	private void DecompressNetwork(DataSet d, int decomp_num) {
		
		Random r = new Random();
		for(int i=0; i<node_buff.size(); i++) {
			node_buff.get(i).true_count = 0;
		}
		
		for(int i=0; i<4000; i++) {
			int id = r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			
			for(int j=0; j<node_buff.size(); j++) {
				boolean val = node_buff.get(j).Output(sample);
				if(val == output) {
					node_buff.get(j).true_count++;
				}
			}
		}
		
		
		// biggest to smallest
		Collections.sort(node_buff, new Comparator<Voter>() {
			 
	        public int compare(Voter arg1, Voter arg2) {
	        	
	        	if(arg1.true_count < arg2.true_count) {
	    			return 1;
	    		}

	    		if(arg1.true_count > arg2.true_count) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		int count = 0;
		int offset = 0;
		while(offset < decomp_num && count < node_buff.size()) {
			if(node_buff.get(count++).DecompressParent() == true) {
				offset++;
			}
		}
		
		System.out.println("decomp num "+offset+" "+node_buff.size()+" "+Voter.RootNodeNum());
	}
	
	// This randomly selects the best neighbours
	public void SelectRandomNeighbours(DataSet d, int cycle_num) {
		
		for(int k=0; k<cycle_num; k++) {
			
			ArrayList<Voter> child_buff = new ArrayList<Voter>();
			
			for(int i=0; i<NEW_CLASS_NUM; i++) {
				HashMap<Voter, Boolean> map = new HashMap<Voter, Boolean>();
				
				while(child_buff.size() < K_NEIGH_NUM) {
					
					Voter v = Voter.NextRandomNode();
					System.out.println(v);
					if(v == null) {
						System.out.println("booo");System.exit(0);
					}
					if(map.get(v) == null) {
						child_buff.add(v);
						map.put(v, false);
					}
				}
				
				node_buff.add(new Voter(new DecisionNode(0, child_buff)));
				
			}
			
			System.out.println("fin");
			DecisionNode.IncOutputAssignID();
			
			for(Voter ptr : Voter.RootNode()) {
				ArrayList<Voter> buff = new ArrayList<Voter>();
				ptr.LeafNode(buff);

				for(int i=0; i<buff.size(); i++) {
					buff.get(i).node.AssignBestRule(d, sample_num, 6);
				}
			}
			
			System.out.println("out");

			Voter.CreateClassifierError(d, 10000);
			System.out.println(Voter.NetTruth()+" ****");
			FindCorrMat(d);
			System.out.println("done");
			
			//DecompressNetwork(d, 20);
			System.out.println("Decomp Num: "+Voter.RootNodeNum()+"   "+node_buff.size());
		}
	}
	
	// This builds the initial network 
	private Voter BuildNetwork(int start, int end) {
		
		if(end - start <= 1) {
			Voter v = new Voter(new DecisionNode(start, null));
			node_buff.add(v);
			return v;
		}
		
		int middle = (start + end) >> 1;
		int middle1 = (start + middle) >> 1;
		int middle2 = (middle + end) >> 1;
		
		Voter l1 = BuildNetwork(start, middle1);
		Voter l2 = BuildNetwork(middle1, middle);
		
		Voter r1 = BuildNetwork(middle, middle2);
		Voter r2 = BuildNetwork(middle2, end);
		
		ArrayList<Voter> child_buff = new ArrayList<Voter>();
		child_buff.add(l1);
		child_buff.add(l2);
		
		child_buff.add(r1);
		child_buff.add(r2);
		
		Voter v = new Voter(new DecisionNode(0, child_buff));
		node_buff.add(v);
		
		return v;
	}

	public ExpandNetwork(DataSet d, int cycle_num, int decomp_num) {
		
		DecisionNode.SetTraining(d, 1000);
		for(int i=0; i<d.DimNum(); i++) {
			node_buff.add(new Voter(new DecisionNode(i, null)));
		}
		
		DecisionNode.IncOutputAssignID();
		Voter v1 = BuildNetwork(0, d.DimNum());
		v1.node.AssignBestRule(d, sample_num, 10);
		
		Voter.CreateClassifierError(d, sample_num);
		
		SelectRandomNeighbours(d, cycle_num);
		
		ArrayList<Voter> root_node = new ArrayList<Voter>();
		for(Voter ptr : Voter.RootNode()) {
			root_node.add(ptr);
		}
		
		// greedy approach last
		for(int k=0; k<2; k++) {
			
			System.out.println(k+"    Num");
			ArrayList<Voter> child_buff = new ArrayList<Voter>();
			Voter.CreateClassifierError(d, 10000);
			
			// biggest to smallest
			Collections.sort(root_node, new Comparator<Voter>() {
				 
		        public int compare(Voter arg1, Voter arg2) {
		        	
		        	if(arg1.true_count < arg2.true_count) {
		    			return 1;
		    		}

		    		if(arg1.true_count > arg2.true_count) {
		    			return -1;
		    		}

		    		return 0; 
		        }
		    });
			
			for(int i=0; i<5; i++) {
				child_buff.add(root_node.get(i));
			}
			
			Voter v = new Voter(new DecisionNode(0, child_buff));
			node_buff.add(v);
			root_node.add(v);

			DecisionNode.IncOutputAssignID();
			
			ArrayList<Voter> buff = new ArrayList<Voter>();
			v.LeafNode(buff);
			for(int i=0; i<buff.size(); i++) {
				buff.get(i).node.AssignBestRule(d, sample_num, 10);
			}
		}
		
		for(Voter ptr : Voter.RootNode()) {
			ptr.true_count = 0;
		}
		
		Random r = new Random();
		for(int i=0; i<10000; i++) {
			int id = r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			boolean output = d.OutputSet().get(id)[0];
			DecisionNode.IncOutputAssignID();
			
			for(Voter ptr : Voter.RootNode()) {
				ptr.Output(sample);
			}
			
			for(Voter ptr : Voter.RootNode()) {
				if(ptr.output_val == output) {
					ptr.true_count++;
				}
			}
		}
		
		for(Voter ptr : Voter.RootNode()) {
			top_voter.add(ptr);
		}
		
		Collections.sort(top_voter, new Comparator<Voter>() {
			 
	        public int compare(Voter arg1, Voter arg2) {
	        	
	        	if(arg1.true_count < arg2.true_count) {
	    			return 1;
	    		}

	    		if(arg1.true_count > arg2.true_count) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });

		//DecompressNetwork(d, 1, 50);
		//	sample_num <<= 2;
	}
	
	// This returns the classification for a sample
	public boolean Classification(boolean sample[]) {
		
		int true_count = 0;
		int false_count = 0;
		DecisionNode.IncOutputAssignID();

		boolean val = top_voter.get(0).Output(sample);
		if(val == true) {
			true_count++;
		} else {
			false_count++;
		}
		
		return true_count > false_count;
	}

}
