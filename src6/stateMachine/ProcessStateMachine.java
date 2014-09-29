package stateMachine;

import java.io.BufferedWriter;

import java.io.FileWriter;
import java.io.IOException;

import gurobi.*;

import java.text.DecimalFormat;
import java.util.ArrayList;


import optimizeTree.BranchAndBound;
import optimizeTree.ConstructTree;
import optimizeTree.DecompState;

import state.*;

public class ProcessStateMachine {
	
	// This is used to create the state machine
	BuildStateMachine state_mach;
	// This stores the next state set 
	ArrayList<State> state_group;
	// This stores the root state
	State root_state = null;
	// This is a predicate indicating whether a function is being 
	// parsed to derive additional states or whether it's being 
	// parsed to simply derive plot and print information later
	boolean is_parse_aux = false;
	
	// This creates a new state
	private State NewState(State s) {

		State n = new State();
		n.action_id = s.action_id;
		n.flow_ptr = s.flow_ptr;
		
		StateLink prev_ptr = n.backward_link;
		n.backward_link = new StateLink();
		n.backward_link.s = s;
		n.backward_link.next_ptr = prev_ptr;
		
		prev_ptr = s.forward_link;
		s.forward_link = new StateLink();
		s.forward_link.s = n;
		s.forward_link.next_ptr = prev_ptr;
		
		return n;
	}
	
	// This initializes the root state
	private State InitializeStateMachine(char buffer[]) throws IOException, CodeException {
		
		root_state = new State();
		state_mach.ConstructStateSet(buffer);
		
		return root_state;
	}
	
	// This processes a branch function command
	public double ProcessBranchCommand(OpNode ptr, State s, boolean is_excl) throws IOException {
		//System.out.println("Branch Func $$$$$$$$$$$$$$$$$$$$$$$$");
		FlowNode next_ptr = state_mach.FunctionPtr(ptr.var_id);
		
		State n = NewState(s);
		
		if(ptr.child_ptr != null) {
			if(is_parse_aux == true) {
				// no more states in aux mode
				return 0;
			}
			
			n.state_util = ptr.child_ptr.CalcOpNode(n);
			n.flow_ptr = next_ptr;
			ptr = ptr.next_ptr;

			if(ptr != null) {
				if(ptr.op_type == OpType.BRANCH_PROB) {
					s.backward_link.trav_prob = (float) ptr.child_ptr.CalcOpNode(n);
					ptr = ptr.next_ptr;
				} else {
					s.backward_link.trav_prob = 1;
				}
				
				if(ptr != null) {
					ptr.CalcOpNode(n);
					n.init_assig_ptr = n.assign_ptr;
				}
			}
			
			n.is_active = true;
			state_group.add(n);
			return 0;
		}
		
		if(ptr.next_ptr != null) {
			ptr.next_ptr.CalcOpNode(n);
		}
		
		
		return ProcessFlowNode(next_ptr, n, is_excl);
	}
	
	// This is used to process a given program flow node 
	private FlowNode ProcessIndvFlowNode(FlowNode ptr, State s, int action_depth, boolean is_excl) throws IOException {
		
		while(ptr != null) {
			
			if(ptr.op_node != null) {
				//System.out.println("-----------------------");
				//ptr.op_node.PrintOpTree();
			} else {
				System.out.println("NULL ---------------");
				ptr = ptr.next_ptr;
				continue;
			}
			
			if(ptr.action_depth > 0) {
				ptr.action_depth--;
				//System.out.println("UP @@@@@@@@@@@@@@@@@@@@@@@@@");
				return ptr;
			}
			
			if(ptr.is_start_action == true || ptr.is_start_excl == true || 
					ptr.is_start_scenario == true || ptr.is_alt_action == true) {
				
				//System.out.println(s+" DOWN @@@@@@@@@@@@@@@@@@@@@@@@@");
				State n = NewState(s);
				
				if(ptr.is_start_action == true) {
					n.action_id++;
				}

				if(ptr.is_start_action == true) {System.out.println("start action");
					ptr.is_start_action = false;
					ptr = ProcessIndvFlowNode(ptr, n, action_depth + 1, false);
				} else if(ptr.is_alt_action == true) {System.out.println("alt action");
					ptr.is_alt_action = false;
					ptr = ProcessIndvFlowNode(ptr, n, action_depth + 1, true);
				} else if(ptr.is_start_excl == true) {System.out.println("start excl");
					ptr.is_start_excl = false;
					ptr = ProcessIndvFlowNode(ptr, n, action_depth + 1,  true);
				} else if(ptr.is_start_scenario == true) {System.out.println("start scenario");
					n.action_id = -s.action_id;
					ptr.is_start_scenario = false;
					ptr = ProcessIndvFlowNode(ptr, n, action_depth + 1,  true);
				}
				
				continue;
			}
		
			if(ptr.op_node.op_type == OpType.RETURN) {
				return ptr;
			}
			
			if(ptr.is_condition == true) {
				//System.out.println("Condition ---------------");
				if(ptr.op_node.CalcOpNode(s) == 0.0) {
					ptr = ptr.branch_ptr;
					//System.out.println("Branch ---------------");
					continue;
				}
				
				ptr = ptr.next_ptr;
				continue;
			}

			if(is_parse_aux == false && (ptr.op_node.op_type == OpType.PRINT || ptr.op_node.op_type == OpType.PLOT)) {
				ptr = ptr.next_ptr;
				continue;
			}
			
			if(ptr.op_node.op_type == OpType.BRANCH_FUNC) {
				ProcessBranchCommand(ptr.op_node, s, is_excl);
			} else {
				ptr.op_node.CalcOpNode(s);
			}
			
			if(is_excl == false) {
				if(s.action_id >= 0) {
					s.action_id++;
				} else {
					s.action_id--;
				}
			}
			
			ptr = ptr.next_ptr;
		}
		
		return null;
	}
	
	// This processes the current flow node
	private double ProcessFlowNode(FlowNode ptr, State s, boolean is_excl) throws IOException {
		

		while(ptr != null) {
			
			if(ptr.op_node.op_type == OpType.RETURN) {
				return ptr.op_node.next_ptr.CalcOpNode(s);
			}
			
			ptr = ProcessIndvFlowNode(ptr, s, ptr.action_depth, is_excl);
		}
		
		return 0;
	}
	
	// This prints the state transition description
	private static void PrintStateTrans(State s) throws IOException {
		
		System.out.print(s+" " + s.is_active+":         ");
		ArrayList<State> buff = new ArrayList<State>();
		StateLink ptr = s.forward_link;
		while(ptr != null) {
			System.out.print(ptr.s+" "+ptr.s.action_id+"     ");
			buff.add(ptr.s);
			ptr = ptr.next_ptr;
		}

		System.out.print("\n");
		for(int i=0; i<buff.size(); i++) {
			PrintStateTrans(buff.get(i));
		}
	}
	
	// This removes redundant states
	private boolean RemoveNodes(State s) {
		
		if(s.forward_link == null) {
			return s.is_active;
		}
		
		s.action_id = Math.abs(s.action_id);
		boolean is_found = false;
		StateLink ptr = s.forward_link;
		StateLink prev_ptr = null;
		while(ptr != null) {
			if(RemoveNodes(ptr.s) == false) {
				if(prev_ptr == null) {
					s.forward_link = ptr.next_ptr;
				} else {
					prev_ptr.next_ptr = ptr.next_ptr;
				}
				ptr = ptr.next_ptr;
				continue;
			}
			
			is_found = true;
			prev_ptr = ptr;
			ptr = ptr.next_ptr;
		}
		
		s.is_active = is_found;
		return is_found;
	}
	
	// This returns the root state
	public State RootState() {
		return root_state;
	}
	
	// This copies the point plot and print information from one state to the next
	public void CopyState(State f_state, State t_state) throws IOException {
		
		t_state.points = f_state.points;
		t_state.print_str = f_state.print_str;
		f_state.points = null;
		f_state.print_str = null;
		
		StateLink forward_ptr1 = f_state.forward_link;
		StateLink forward_ptr2 = t_state.forward_link;
		
		while(forward_ptr1 != null) {
			CopyState(forward_ptr1.s, forward_ptr2.s);
			forward_ptr1 = forward_ptr1.next_ptr;
			forward_ptr2 = forward_ptr2.next_ptr;
		}
	}
	
	// This assigns the set of plot points to a give state
	public void AssignPointPlots(State s) throws IOException, CodeException {
		
		State n = new State();
		n.action_id = s.action_id;
		n.state_util = s.state_util;
		n.flow_ptr = s.flow_ptr;
		
		AssignVar ptr = s.init_assig_ptr;
		while(ptr != null) {
			StateVariable.AssignStateVar(n, ptr.var_id, ptr.val);
			ptr = ptr.next_ptr;
		}
		
		StateLink link_ptr = s.backward_link;
		while(link_ptr != null) {
			StateLink prev_ptr = n.backward_link;
			n.backward_link = new StateLink();
			n.backward_link.s = link_ptr.s;
			n.backward_link.next_ptr = prev_ptr;
			link_ptr = link_ptr.next_ptr;
		}
		
		is_parse_aux = true;
		StateMachine(n, null);
		
		FlowNode flow_ptr = n.flow_ptr;
		ProcessFlowNode(flow_ptr, n, true);
		RemoveNodes(n);
		
		CopyState(n, s);
	}
		
	
	// This is the entry function to begin the state machine
	public ArrayList<State> StateMachine(State s, char buffer[]) throws IOException, CodeException {
		
		FlowNode ptr = null;
		if(s == null) {
			state_mach = new BuildStateMachine();
			s = InitializeStateMachine(buffer);
			ptr = state_mach.MainFunction();
		} else {
			ptr = s.flow_ptr;
		}
		 
		//System.out.println(s+" State Machine **********************");
		state_group = new ArrayList<State>();
		
		State n = NewState(s);
		
		ProcessFlowNode(ptr, n, true);
		
		RemoveNodes(s);
 
		return state_group;
	}
	
	// This returns the plot name for a give plot id
	public ArrayList<String> PlotNames() {
		return state_mach.PlotName();
	}
	
	// This calculates the average of a set
	public static float Avg(ArrayList<Float> buff) {
		
		float avg = 0;
		for(int i=0; i<buff.size(); i++) {
			avg += buff.get(i);
		}
		
		return avg / buff.size();
	}
	
	// This returns the standard error of a set
	public static float StdError(ArrayList<Float> buff) {
		
		float avg = Avg(buff);
		float diff = 0;
		for(int i=0; i<buff.size(); i++) {
			diff += (buff.get(i) - avg) * (buff.get(i) - avg);
		}
		
		diff /= buff.size();
		
		return (float) (Math.sqrt(diff) / Math.sqrt(buff.size()));
	}
	
	// This calculates the decomp stats
	private static void FindDecompStats(ArrayList<ArrayList<DecompState>> decomp_buff,
			ArrayList<Float> opt_soln) {

		for(int j=0; j<decomp_buff.get(0).size(); j++) {
			
			ArrayList<Float> edge_buff = new ArrayList<Float>();
			ArrayList<Float> node_buff = new ArrayList<Float>();
			ArrayList<Float> obj_buff = new ArrayList<Float>();
			ArrayList<Float> time_buff = new ArrayList<Float>();
			
			for(int i=0; i<decomp_buff.size(); i++) {
				ArrayList<DecompState> temp = decomp_buff.get(i);
			
				DecompState s1 = temp.get(j);
				float edge_rat = s1.edge_rat;
				float node_rat = s1.node_rat;
				float obj_rat = (s1.obj / opt_soln.get(i));
				
				edge_buff.add(edge_rat);
				node_buff.add(node_rat);
				obj_buff.add(obj_rat);
				time_buff.add((float)s1.time / 1000);
			}
			
			DecimalFormat numberFormat = new DecimalFormat("0.00");

			System.out.print(numberFormat.format(Avg(obj_buff) * 100)+" $\\pm$ "+numberFormat.format(StdError(obj_buff) * 100)+"\\% : ");
			
			System.out.print(numberFormat.format(Avg(time_buff))+" $\\pm$ "+numberFormat.format(StdError(time_buff))+" (s)");
			System.out.print(" & "+numberFormat.format(Avg(edge_buff) * 100)+" $\\pm$ "+numberFormat.format(StdError(edge_buff) * 100)+"\\%");
			System.out.println(" & "+numberFormat.format(Avg(node_buff) * 100)+" $\\pm$ "+numberFormat.format(StdError(node_buff) * 100)+"\\%");
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException, CodeException {
		// TODO Auto-generated method stub
		
		 try {
			 BufferedWriter out = new BufferedWriter(new FileWriter("test.txt"));
			 
			 BranchAndBound.SetPathLength(19);
			 ArrayList<Float> val_buff1 = new ArrayList<Float>();
			 ArrayList<Float> val_buff2 = new ArrayList<Float>();
			 ArrayList<Float> val_buff3 = new ArrayList<Float>();
			 ArrayList<Float> time_buff1 = new ArrayList<Float>();
			 ArrayList<Float> time_buff2 = new ArrayList<Float>();
			 ArrayList<Float> time_buff3 = new ArrayList<Float>();
			 
			 ArrayList<Float> edge_buff = new ArrayList<Float>();
			 ArrayList<Float> node_buff = new ArrayList<Float>();
			 ArrayList<ArrayList<DecompState>> decomp_buff = new ArrayList<ArrayList<DecompState>>();
			 
			 for(int i=0; i<4; i++) {
				 
		    	ConstructTree tree2 = new ConstructTree(null);
		    	tree2.GenerateStateSpaceMyopic(1000, false, i);
		    	val_buff1.add(ConstructTree.MyopicPerf());
		    	val_buff2.add(ConstructTree.CGPerf());
		    	val_buff3.add(ConstructTree.OptVal());
		    	
		    	/*if(BranchAndBound.PathLength() < 5) {
		    		i--;
		    		continue;
		    	}*/
		    	
		    	time_buff1.add((float)BranchAndBound.myopic_time / 1000);
		    	time_buff2.add((float)BranchAndBound.cg_time / 1000);
		    	time_buff3.add((float)ConstructTree.opt_sol_time / 1000);
		    	
		    	ArrayList<DecompState> temp = new ArrayList<DecompState>(BranchAndBound.DecompBuff());
		    	
		    	edge_buff.add(temp.get(temp.size()-1).edge_rat);
		    	node_buff.add(temp.get(temp.size()-1).node_rat);
		    	decomp_buff.add(temp);
		    	
			 }
			 
			 //FindDecompStats(decomp_buff, val_buff3);System.exit(0);
			 
			 System.out.println("boooooooooooooooo");
			 
			 DecimalFormat numberFormat = new DecimalFormat("0.00");
			 
			 System.out.println("Nodes: "+numberFormat.format(Avg(node_buff) * 100)+" $\\pm$ "+numberFormat.format(StdError(node_buff) * 100)+"\\%");
			 System.out.println("Edges: "+numberFormat.format(Avg(edge_buff) * 100)+" $\\pm$ "+numberFormat.format(StdError(edge_buff) * 100)+"\\%");
			 
			 System.out.print(numberFormat.format(Avg(val_buff1) * 100)+" $\\pm$ "+numberFormat.format(StdError(val_buff1) * 100)+"\\% : "+numberFormat.format(Avg(time_buff1))+" $\\pm$ "+numberFormat.format(StdError(time_buff1))+" (s)");
			 System.out.print(" & "+numberFormat.format(Avg(val_buff2) * 100)+" $\\pm$ "+numberFormat.format(StdError(val_buff2) * 100)+"\\% : "+numberFormat.format(Avg(time_buff2))+" $\\pm$ "+numberFormat.format(StdError(time_buff2))+" (s)");
			 System.out.println(" & "+numberFormat.format(Avg(time_buff3))+" $\\pm$ "+numberFormat.format(StdError(time_buff3))+" (s)");
			 out.close();
		 }
		 catch (IOException e)
		 {
		 System.out.println("Exception ");
		 }
		
    	
    	System.exit(0);
    	
    	ConstructTree tree = new ConstructTree(null);
    	tree.ConstructOptimalPath(null);
    	
    	ArrayList<State> buff = tree.OptimalPath();
    	for(int i=0; i<buff.size(); i++) {
    		/*if(buff.get(i).print_str != null) {
    			System.out.println(buff.get(i).print_str);
    		}*/
    		
    		 PointPlot ptr = buff.get(i).points;
			  while(ptr != null) {
				  System.out.print(ptr.x+" "+ptr.y+" "+ptr.plot_id+"     ");
				  ptr = ptr.next_ptr;
			  }
			  
			  System.out.print("\n***********\n");
    	}
    	
    	for (int i = 0; i < buff.size(); i+=2) {
    	  PointPlot ptr1 = buff.get(i).points;
    	  PointPlot ptr2 = buff.get(i + 1).points;
		  while(ptr1 != null && ptr2 != null) {
			  if(ptr1.plot_id == ptr2.plot_id) {
				  if(ptr1.x != ptr2.x || ptr1.y != ptr2.y) {
					  System.out.println(ptr1.x+" "+ptr1.y+"       "+ptr2.x+" "+ptr2.y);
				  }
			  }
			  ptr1 = ptr1.next_ptr;
			  ptr2 = ptr2.next_ptr;
		  }
      }

	}

}
