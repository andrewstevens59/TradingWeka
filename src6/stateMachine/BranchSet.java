package stateMachine;

//This is used to construct the branching tree
class BranchSet {
	// This stores the flow ptr for one of the conditions
	ConditionNode root_ptr = null;
	// This stores the tail ptr for the current condition 
	ConditionNode tail_ptr = null;
	// This stores the terminal operation 
	FlowNode term_op = null;
	// This stores the next branch set
	BranchSet next_ptr;
}
