package stateMachine;

public class FlowNode {

	// This stores an instance of an operation node
	OpNode op_node = null;
	// This stores the branch node if the operation evaluates to false
	FlowNode branch_ptr = null;
	// This stores the next flow node
	FlowNode next_ptr = null;
	// This stores a branch node for the condition
	BranchSet branch_node = null;
	// A predicate indicating a condition node
	boolean is_condition = false;
	// A predicate indicating a new action block being entered
	boolean is_start_action = false;
	// This indicates the start of an exclusive action region
	boolean is_start_excl = false;
	// This indicates an alternate action is being considered
	boolean is_alt_action = false;
	// This indicates a scenario is being started 
	boolean is_start_scenario = false;
	// This stores the depth of an action
	int action_depth = 0;
	
	// This sets the action set to false
	public void SetActionSetFalse() {
		is_start_action = false;
		is_start_excl = false;
		is_alt_action = false;
		is_start_scenario = false;
	}
}
