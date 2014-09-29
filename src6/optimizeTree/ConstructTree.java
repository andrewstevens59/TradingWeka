package optimizeTree;


import gurobi.GRBException;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import state.AssignVar;
import state.StateLink;
import state.State;
import state.StateVariable;
import stateMachine.CodeException;
import stateMachine.ProcessStateMachine;
import stateMachine.RandomField;
import tsp.TSPSolver;

public class ConstructTree {
	
	
	// This stores the optimal value
	static float opt_val;
	// This states the set of optimal state links
	ArrayList<State> state_buff;
	// This stores the current set of states in the optimum policy tree
	ArrayList<State> optimum_state_buff = new ArrayList<State>();
	// This is used to create the state machine
	RandomField state_mach;
	// This stores the current time 
	int curr_time = 0;
	// This is used to find the K nearest neighbours
	KNearestNeighbour kn = new KNearestNeighbour();
	// This stores the optimal solution time
	static public long opt_sol_time;
	
	
	// This stores the set of expansion nodes
	PriorityQueue<State> state_queue = new PriorityQueue<State>(10, new Comparator<State>() {
		 
        public int compare(State arg1, State arg2) {
        	
        	if(arg1.choose_util < arg2.choose_util) {
    			return 1;
    		}

    		if(arg1.choose_util > arg2.choose_util) {
    			return -1;
    		}

    		return 0; 
        }
    });
	
	public ConstructTree(char buff[]) throws IOException, CodeException {
		
		state_mach = new RandomField();
		System.out.println(state_mach.RootState());
		//state_queue.add(state_mach.RootState());
	}
	
	// This is used to generate states for the state space
	public float GenerateStateSpaceMyopic(int state_num, boolean is_add_cg, int problem) throws IOException, CodeException {
		
		PolicyNode.SetMaxSLinkNum(20);
		
		/*for(int i=0; i<10; i++) {
			state_mach = new RandomField();
			state_mach.GenerateRandomGrid(15);
			state_mach.WriteProblem("C:/Users/callum/Desktop/OrienteeringProblems/random_graph_1000_nodes_16_eges_problem_"+i+".txt");
		}
		
		System.exit(0);*/
		
		problem = 6;
		state_mach = new RandomField();
		/*state_mach.GenerateRandomGraph(1000, 5);
		state_mach.WriteProblem("OrienteeringProblems/random_graph_1000_nodes_60_eges_problem_"+problem+".txt");
		
		state_mach = new RandomField();
		state_mach.ReadProblem("C:/Users/callum/Desktop/OrienteeringProblems/random_graph_1000_nodes_16_eges_problem_"+problem+".txt");*/
		
		state_mach.LoadTSPBenchMark("fl3795.tsp");
		

		System.out.println(state_num+" "+state_mach.LinkBuff().size());
		/*Collections.sort(state_mach.LinkBuff(), new Comparator<StateLink>() {
			 
	        public int compare(StateLink arg1, StateLink arg2) {
	        	
	        	if(arg1.dist < arg2.dist) {
	    			return -1;
	    		}

	    		if(arg1.dist > arg2.dist) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		

		for(int i=0; i<state_mach.LinkBuff().size(); i++) {
			
			if(state_mach.LinkBuff().get(i).src != state_mach.LinkBuff().get(i).s) {
				state_mach.LinkBuff().get(i).src.node.MergeNode(state_mach.LinkBuff().get(i).s.node);
			}
			
			if(PolicyNode.RootNodeNum() < 10) {
				break;
			}
		}

		Collections.sort(state_mach.LinkBuff(), new Comparator<StateLink>() {
			 
	        public int compare(StateLink arg1, StateLink arg2) {
	        	
	        	if(arg1.dist > arg2.dist) {
	    			return 1;
	    		}

	    		if(arg1.dist < arg2.dist) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<state_mach.LinkBuff().size(); i++) {
			if(state_mach.LinkBuff().get(i).src != state_mach.LinkBuff().get(i).s) {
				state_mach.LinkBuff().get(i).src.node.EmbedSLink(state_mach.LinkBuff().get(i));
			}
		}*/
		
		while(PolicyNode.root_map.size() > 12) {
			PolicyNode.MergeClosestNodes();
			System.out.println("Merge "+PolicyNode.root_map.size());
		}
		
		TSPSolver tsp = new TSPSolver();
		try {
			tsp.SolveSparseTSP();
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
		
		System.out.println("*************** "+BranchAndBound.PathLength());
		float val = BranchAndBound.OptimalPolicyGC(state_mach.RootState(), 5, state_mach);
		
		long prev_time = System.currentTimeMillis();
		/*try {
			opt_val = state_mach.FindOptimalPolicy(BranchAndBound.PathLength());
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		opt_sol_time = System.currentTimeMillis() - prev_time;
		
		
		System.out.println(val+" "+opt_val+" "+BranchAndBound.myopic_val+" "+BranchAndBound.cg_val+" "+PolicyNode.RootNodeNum()+" "+BranchAndBound.PathLength());
		System.exit(0);
		return BranchAndBound.myopic_val / opt_val;
	}
	
	// This returns the optimal value
	public static float OptVal() {
		return opt_val; 
	}
	
	// This returns the relative myopic payoff
	public static float MyopicPerf() {
		return BranchAndBound.myopic_val / opt_val; 
	}
	
	public static float CGPerf() {
		return BranchAndBound.cg_val / opt_val; 
	}

	// This returns the root state
	public State RootState() {
		return state_mach.RootState();
	}
	
	// This returns the optimal path construction
	public ArrayList<State> OptimalPath() throws IOException, CodeException {
		
		for(int i=0; i<state_buff.size(); i++) {
    		//state_mach.AssignPointPlots(state_buff.get(i));
    	}
		
		return state_buff;
	}
	
	// This constructs the optimal path
	public void ConstructOptimalPath(State s) throws IOException {
		
		if(s == null) {
			s = state_mach.RootState();
			state_buff = new ArrayList<State>();
		}
		
		StateLink link = s.forward_link;
		if(link == null) {
			return;
		}

		ArrayList<State> scenarios = new ArrayList<State>();
		State max_state = null;
		int prev_action = 0;
		double max = 0;

		while(link != null) {
			
			if(link.s.action_id != prev_action) {
				prev_action = link.s.action_id;
				if(max > 0) {
					AssignMaxState(s, scenarios, max_state);
				}
				
				max = 0;
				max_state = null;
				scenarios.clear();
			}

			if(link.s.action_id >= 0) {
				
				if(link.trav_prob * link.s.exp_state_util > max) {
					max = link.trav_prob * link.s.exp_state_util;
					max_state = link.s;
				}

			} else {
				// scenario
				max += link.trav_prob * link.s.exp_state_util;
				scenarios.add(link.s);
			}
			
			link = link.next_ptr;
		}
	
		if(max > 0) {
			AssignMaxState(s, scenarios, max_state);
		}
	}

	// This assigns the max action state
	private void AssignMaxState(State s, ArrayList<State> scenarios, State max_state) throws IOException {
		
		if(scenarios.size() > 0) {
			for(int i=0; i<scenarios.size(); i++) {
				if(s.flow_ptr != null) {
					state_buff.add(s);
					state_buff.add(scenarios.get(i));
				}
				ConstructOptimalPath(scenarios.get(i));
			}
		} else if(max_state != null) {
			if(s.flow_ptr != null) {
				state_buff.add(s);
				state_buff.add(max_state);
			}
			
			ConstructOptimalPath(max_state);
		}
	}
	
	// This returns the plot name for a give plot id
	public ArrayList<String> PlotNames() {
		return null;//state_mach.PlotNames();
	}
	
	// This finds the maximum expected utility path
	public double FindOptimalPath(State s, int depth) throws IOException {
		
		optimum_state_buff.add(s);
		
		if(depth == 0) {
			return s.state_util;
		}

		if(s == null) {
			s = state_mach.RootState();
		}
		
		if(depth == 0) {
			return s.state_util;
		}

		StateLink link = s.forward_link;
		
		if(link == null) {
			s.exp_state_util = s.state_util;
			return s.state_util;
		}
		
		State best_action = null;
		double max = -1000000000;

		while(link != null) {
			
			if(link.s.exp_state_util > max) {
				best_action = link.s;
				max = link.s.exp_state_util;
			}
			link = link.next_ptr;
		}
	
		return s.state_util + 0.95f * FindOptimalPath(best_action, depth - 1);
	}

}
