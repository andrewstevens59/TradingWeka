package stateMachine;

public class TextAttr {

		// This stores the text string
		String attr;
		// This stores the line number on which the attr exits
		int line_number;
		// This stores the parent text attr
		TextAttr parent_ptr = null;
		// This stores the lower level of abstraction in the text file
		TextAttr child_ptr = null;
		// This stores the next sequential attribute at the given level
		TextAttr next_ptr = null;
		// This stores the delimiter type
		AttrType delim_type;
		// This stores a related numerical value
		OpNode op_node;
}
