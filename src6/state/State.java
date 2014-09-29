package state;

import optimizeTree.PolicyNode;
import stateMachine.FlowNode;

public class State {
	
	// This stores the state vector
	public double v[];
	// This stores the state utility
	public double state_util;
	// This stores the expected state utility
	public double exp_state_util = 1.0f;
	// This indicates if the predecessors for a state have been generated
	public boolean is_active = true; 
	// This stores the optimal action
	public int action_id = 1;
	// This stores the time period of the state
	public int time_period = 0;
	// This stores the corresponding tree node where the state is embedded 
	public PolicyNode node = new PolicyNode(this);
	// This stores the forward link ptr
	public StateLink forward_link = null;
	// This stores the forward link ptr
	public StateLink backward_link = null;
	// This stores the set of variables that have been assigned for this state
	public AssignVar assign_ptr;
	// This stores the initializing set of variables for this state for later rexecution
	public AssignVar init_assig_ptr = null;
	// This is used to store the print information for the state
	public String print_str = null;
	// This stores the set of point plots for this state
	public PointPlot points = null;
	// This stores the point in the program where the state is initiated
	public FlowNode flow_ptr = null;
	// This stores the traversal probability of the state
	public float trav_prob = 0;
	// This stores the utility of the node in terms of expansion
	public float choose_util = 0;
	// This stores the state id
	public int state_id = 0;
	// This stores the unique id
	public int node_id = 0;
	
	public double x;
	public double y;

}
