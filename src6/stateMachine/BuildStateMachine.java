package stateMachine;

import java.io.IOException;
import java.util.*;

import state.State;
import state.StateVariable;

public class BuildStateMachine {
	
	// This defines the variable semantics
	class VarSemantics { 
		// This stores the variable offset in the global stack
		int var_offset;
		// This stores the size of each dimension
		int dim_size[];
	}

	// This is used to parse the document tree
	LexicalAnalyzer lex = new LexicalAnalyzer();
	// This stores the set of functions
	HashMap<String, Integer> function_map = new HashMap<String, Integer>();
	// This stores the variable name mapping
	HashMap<String, VarSemantics> variable_map = new HashMap<String, VarSemantics>();
	// This stores the mapping between function var_id and global var_id
	HashMap<String, Integer> variable_func_map = new HashMap<String, Integer>();
	// This stores the number of arguments for a given function
	ArrayList<Integer> func_arg_num = new ArrayList<Integer>();
	
	// This stores the branch stack
	ArrayList<BranchSet> branch_stack = new ArrayList<BranchSet>();
	// This stores the set of program flow nodes
	ArrayList<FlowNode> flow_buff = new ArrayList<FlowNode>();
	// This stores the flow_ptr for the start of each function
	ArrayList<FlowNode> function_buff = new ArrayList<FlowNode>();
	// This stores the plot string associated with each plot statement
	ArrayList<String> plot_str_buff = new ArrayList<String>();
	
	// This stores the tail of the state machine
	FlowNode tail_flow_ptr = null;
	// This stores the previous flow state
	FlowNode prev_flow_ptr = null;
	// This stores the entry point for the state machine
	FlowNode main_flow_ptr = null;
	
	// This stores the root of a loop such as for or while
	FlowNode root_loop_ptr_temp = null;
	
	// This stores the total number of variables
	int var_num = 0;
	// This indicates a local variable set is being processed
	boolean is_local_var;
	// This stores the current function id
	int curr_func_id = -1;
	// This stores the current line number
	int line_num = 0;
	
	// This creates a new FLOW NODE
	private void NewFlowNode() {
		if(tail_flow_ptr == null) {
			tail_flow_ptr = new FlowNode();
		} else {
			prev_flow_ptr = tail_flow_ptr;
			tail_flow_ptr.next_ptr = new FlowNode();
			tail_flow_ptr = tail_flow_ptr.next_ptr;
		}
		
		flow_buff.add(tail_flow_ptr);
	}
	
	// This returns the variable id for a text string
	private VarSemantics VarID(String str) {
		VarSemantics map = variable_map.get(str);
		if(map != null) {
			return map;
		}
		
		map = variable_map.get(str + curr_func_id);
		if(map != null) {
			return map;
		}
		
		return null;
	}
	
	// This prints the operation tree
	private void PrintOpTree(OpNode ptr) {
		
		ArrayList<OpNode> buff = new ArrayList<OpNode>();
		while(ptr != null) {
			System.out.print(ptr+" "+ptr.op_val+" "+ptr.op_type+" "+ptr.var_id+" "+ptr.mult_factor+"   Child: "+ptr.child_ptr+"    <>    ");
			
			if(ptr.child_ptr != null) {
				buff.add(ptr.child_ptr);
			}
			ptr = ptr.next_ptr;
		}
		
		System.out.print("\n");
		for(int i=0; i<buff.size(); i++) {
			PrintOpTree(buff.get(i));
		}
	}
	
	// This is used to resolve an array index
	private TextAttr ResolveArrayIndex(TextAttr ptr, VarSemantics sem, int dim, int offset) throws CodeException {
		
		OpNode tail_ptr = null;
		OpNode root_ptr = null;
		
		if(sem == null) {
			if(ptr.attr.length() > 0) {
				throw new CodeException("Unknown variable "+ptr.attr, ptr.line_number);
			} 
			throw new CodeException("Expecting statement ", ptr.line_number);
		}
		
		if(sem.dim_size == null) {
			throw new CodeException("Illegal index of singular variable "+ptr.attr, ptr.line_number);
		}
		
		while(ptr.delim_type == AttrType.L_SQUARE_BR) {
			
			if(tail_ptr == null) {
				tail_ptr = new OpNode();
				root_ptr = tail_ptr;
			} else {
				tail_ptr.next_ptr = new OpNode();
				tail_ptr = tail_ptr.next_ptr;
			}

			tail_ptr.child_ptr = ConstructOperationTree(ptr.child_ptr).op_node;
			
			if(offset == 0) {
				offset = 1; 
			} else {
				offset *= sem.dim_size[dim++]; 
			}
			
			tail_ptr.op_type = OpType.MULT_ADD;
			tail_ptr.mult_factor = offset;
			ptr = ptr.next_ptr;
		}
		
		ptr.op_node = new OpNode();
		ptr.op_node.op_type = OpType.ADD_TO_VAR;
		ptr.op_node.var_id = sem.var_offset;
		ptr.op_node.child_ptr = root_ptr;
		return ptr;
	}
	
	// This is used to allocate an array
	private TextAttr AllocateArray(TextAttr ptr, boolean is_static) throws IOException {
		
		int net_size = 1;
		TextAttr var = ptr;
		ArrayList<Integer> set_size = new ArrayList<Integer>();
		while(ptr.delim_type == AttrType.L_SQUARE_BR) {
			set_size.add(Integer.parseInt(ptr.child_ptr.attr));
			net_size *= Integer.parseInt(ptr.child_ptr.attr);
			ptr = ptr.next_ptr;
		}
		
		VarSemantics sem = new VarSemantics();
		sem.dim_size = new int[set_size.size()];
		for(int i=0; i<set_size.size(); i++) {
			sem.dim_size[i] = set_size.get(i);
		}
		
		if(is_static == true) {
			for(int i=0; i<net_size; i++) {
				StateVariable.AddStaticVariable(var_num + i);
			}
		}
		
		sem.var_offset = var_num;
		if(curr_func_id >= 0) {
			variable_map.put(var.attr + curr_func_id, sem);
		} else {
			variable_map.put(var.attr, sem);
		}
		var_num += net_size;

		return ptr;
	}
	
	// This stops parsing at a particular delimiter type
	private boolean IsOperationType(AttrType att_type) {
		
		if(att_type == AttrType.SEMI_COLON) {
			return true;
		}
		
		if(att_type == AttrType.COMMA) {
			return true;
		}
		
		if(att_type == AttrType.COLON) {
			return true;
		}
		
		return false;
	}
	
	// This is used to construct a binary operation tree 
	private TextAttr ConstructOperationTree(TextAttr ptr) throws CodeException {
		
		OpNode tail_ptr = null;
		OpNode root_ptr = null;
		boolean is_next = false;
		boolean is_before = false;
		
		while(ptr != null) {
			
			if(IsOperationType(ptr.delim_type) == true && ptr.attr.length() == 0) {
				break;
			}
			
			if(tail_ptr == null) {
				tail_ptr = new OpNode();
				root_ptr = tail_ptr;
			} else if(ptr.attr.length() > 0) {
				tail_ptr.next_ptr = new OpNode();
				tail_ptr = tail_ptr.next_ptr;
			}
		
			if(ptr.attr.length() > 0) {
				if(ptr.delim_type == AttrType.L_CURL_BR) {
					// resolves a function call
					TextAttr next_ptr = ProcessFunctionCall(ptr);
					if(next_ptr != null) {
						ptr = next_ptr;
						tail_ptr.child_ptr = ptr.op_node;
						continue;
					}
				} 

				if(ptr.delim_type == AttrType.L_SQUARE_BR) {
					// resolves an array index
					VarSemantics sem = VarID(ptr.attr);
					ptr = ResolveArrayIndex(ptr, sem, 0, 0);
					tail_ptr.child_ptr = ptr.op_node;
	
				} else if(ptr.child_ptr == null) {
					try {
	
						tail_ptr.op_val = Double.parseDouble(ptr.attr);
			        
			        } catch (NumberFormatException ex) {
			        	VarSemantics sem = VarID(ptr.attr);
						if(sem != null) {
							tail_ptr.var_id = sem.var_offset;
						} else if(ptr.attr.length() > 0 && ptr.attr.charAt(0) == '"') {
							tail_ptr.str_const = ptr.attr.replace("\"", "");
						} else if(ptr.attr.length() > 0 && ptr.attr.charAt(0) == '\'') {
							tail_ptr.str_const = ptr.attr.replace("'", "");
						} else {
							if(ptr.attr.length() > 0) {
								throw new CodeException("Unknown variable "+ptr.attr, ptr.line_number);
							} 

							throw new CodeException("Expecting statement ", ptr.line_number);
						}
			        }
				} 
			}
			
			if(ptr.child_ptr != null) {
				if(is_before == true) {
					tail_ptr.next_ptr = new OpNode();
					tail_ptr = tail_ptr.next_ptr;
				}
				
				tail_ptr.child_ptr = ConstructOperationTree(ptr.child_ptr).op_node;
			}
			
			is_next = AssignOperationType(ptr, tail_ptr);
			
			is_before = true;
			if(IsOperationType(ptr.delim_type) == true) {
				break;
			}
			
			if(ptr.next_ptr == null) {
				break;
			}
			ptr = ptr.next_ptr;
		}
		
		if(is_next == true) {
			throw new CodeException("Illegal Expression", ptr.line_number);
		}
		
		ptr.op_node = root_ptr;
		return ptr;
	}

	// This assigns the operation type to an operation node
	private boolean AssignOperationType(TextAttr ptr, OpNode root_ptr) throws CodeException {
		
		boolean is_next = true;
		if(ptr.delim_type == AttrType.ADD) {
			root_ptr.op_type = (OpType) OpType.ADD;
		} else if(ptr.delim_type == AttrType.SUBTRACT) {
			root_ptr.op_type = (OpType) OpType.SUBTRACT;
		} else if(ptr.delim_type == AttrType.DIVIDE) {
			root_ptr.op_type = (OpType) OpType.DIVIDE;
		} else if(ptr.delim_type == AttrType.MULTIPLY) {
			root_ptr.op_type = (OpType) OpType.MULTIPLY;
		} else if(ptr.delim_type == AttrType.EQUALITY) {
			root_ptr.op_type = (OpType) OpType.EQUALITY;
		} else if(ptr.delim_type == AttrType.LT) {
			root_ptr.op_type = (OpType) OpType.LT;
		} else if(ptr.delim_type == AttrType.LTE) {
			root_ptr.op_type = (OpType) OpType.LTE;
		} else if(ptr.delim_type == AttrType.GT) {
			root_ptr.op_type = (OpType) OpType.GT;
		} else if(ptr.delim_type == AttrType.GTE) {
			root_ptr.op_type = (OpType) OpType.GTE;
		} else if(ptr.delim_type == AttrType.AND) {
			root_ptr.op_type = (OpType) OpType.AND;
		} else if(ptr.delim_type == AttrType.OR) {
			root_ptr.op_type = (OpType) OpType.OR;
		} else if(ptr.delim_type == AttrType.NOT) {
			root_ptr.op_type = (OpType) OpType.NOT;
		} else if(ptr.delim_type == AttrType.EQOR) {
			root_ptr.op_type = (OpType) OpType.EQOR;
		} else if(IsOperationType(ptr.delim_type) == true || 
				ptr.delim_type == AttrType.R_CURL_BR || ptr.delim_type == AttrType.R_SQUARE_BR
				|| ptr.delim_type == AttrType.L_CURL_BR || ptr.delim_type == AttrType.L_SQUARE_BR) {
			is_next = false;
		} else {
			throw new CodeException("Not Expecting "+ptr.delim_type, ptr.line_number);
		}
	
		return is_next;
	}

	// This is used to handle an assignment operation
	private TextAttr AssignVariable(TextAttr ptr, OpNode root_ptr) throws IOException, CodeException {
		
		OpNode prev_ptr = root_ptr;
		root_ptr = new OpNode();
		root_ptr.child_ptr = prev_ptr;
		
		if(ptr.delim_type == AttrType.INCREMENT) {
			root_ptr.op_type = (OpType) OpType.INCREMENT;
		} else if(ptr.delim_type == AttrType.DECREMENT) {
			root_ptr.op_type = (OpType) OpType.DECREMENT;
		} else if(ptr.delim_type == AttrType.MULT_EQUAL) {
			root_ptr.op_type = (OpType) OpType.ASSIGN_MULT_VAR;
		} else if(ptr.delim_type == AttrType.DIV_EQUAL) {
			root_ptr.op_type = (OpType) OpType.ASSIGN_DIV_VAR;
		} else if(ptr.delim_type == AttrType.PLUS_EQUAL) {
			root_ptr.op_type = (OpType) OpType.ASSIGN_ADD_VAR;
		} else if(ptr.delim_type == AttrType.MINUS_EQUAL) {
			root_ptr.op_type = (OpType) OpType.ASSIGN_SUB_VAR;
		} else {
			root_ptr.op_type = (OpType) OpType.ASSIGN_VAR;
		}
		
		
		ptr = ptr.next_ptr;
		if(root_ptr.op_type != OpType.INCREMENT && root_ptr.op_type != OpType.DECREMENT) {
			ptr = ConstructOperationTree(ptr);
			root_ptr.next_ptr = ptr.op_node;
			
			if(ptr.op_node == null) {
				throw new CodeException("Illegal Expression", ptr.line_number);
			}
		}
		
		ptr.op_node = root_ptr;
		tail_flow_ptr.op_node = root_ptr;
		System.out.println(tail_flow_ptr+"  %%%%%%%%%%%%");
		NewFlowNode();
		
		PrintOpTree(root_ptr);System.out.println("done");
	
		return ptr;
	}
	
	// This is used to assign a variable
	private TextAttr ProcessAssignment(TextAttr ptr)
			throws IOException, CodeException {
		
		OpNode var  = null;
		if(ptr.attr.length() == 0) {
			return ptr;
		}
		
		boolean is_static = is_local_var;
		if(ptr.attr.equals("static")) {
			ptr = ptr.next_ptr;
			is_static = true;
		}
		
		if(ptr.delim_type == AttrType.L_SQUARE_BR) {
			if(VarID(ptr.attr) == null) {
				// allocate the array
				ptr = AllocateArray(ptr, is_static);
			} else {
				ptr = ResolveArrayIndex(ptr, VarID(ptr.attr), 0, 0);
				var = ptr.op_node;
			}

		} else if(var == null) {
			
			var = new OpNode();
			if(VarID(ptr.attr) == null) {
				VarSemantics var_map = new VarSemantics();
				var_map.var_offset = var_num++;
				
				if(curr_func_id >= 0) {
					variable_map.put(ptr.attr + curr_func_id, var_map);
				} else {
					variable_map.put(ptr.attr, var_map);
				}
				
				if(is_static == true) {
					StateVariable.AddStaticVariable(var_map.var_offset);
				}
			}
			
			var.var_id = VarID(ptr.attr).var_offset;
		}
		
		if(ptr.delim_type == AttrType.MULT_EQUAL) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == AttrType.DIV_EQUAL) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == AttrType.PLUS_EQUAL) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == AttrType.MINUS_EQUAL) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == AttrType.EQ) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == AttrType.INCREMENT) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == AttrType.DECREMENT) {
			ptr = AssignVariable(ptr, var);
		} else if(ptr.delim_type == null || ptr.delim_type == AttrType.UNKNOWN) {
			System.out.println(ptr.attr);
			throw new CodeException("Unrecognized operation ", ptr.line_number);
		}
		
		return ptr;
	}
	
	// This parses an if condition
	private TextAttr ParseIfCondition(TextAttr ptr, int level) throws IOException, CodeException {
		
		ptr = ConstructOperationTree(ptr);
		
		tail_flow_ptr.op_node = ptr.op_node;
		tail_flow_ptr.branch_node = branch_stack.get(level);
		tail_flow_ptr.is_condition = true;

		NewFlowNode();
		return ptr;
	}
	
	// This is used to parse a for loop
	private TextAttr ParseForLoop(TextAttr ptr, int level) throws IOException, CodeException {
		
		ptr = ProcessAssignment(ptr);
		ptr = ptr.next_ptr;

		root_loop_ptr_temp = tail_flow_ptr;
		ptr = ParseIfCondition(ptr, level);
		ptr = ptr.next_ptr;
		
		return ptr;
	}
	
	// This processes a function and assigns operation variables to each passed argument
	private TextAttr ProcessFunctionCall(TextAttr ptr) throws CodeException {
		
		Integer func_id = function_map.get(ptr.attr);
		if(func_id == null) {
			if(ptr.attr.length() > 0 && ptr.attr.equals("if") == false && 
					ptr.attr.equals("while") == false && ptr.attr.equals("for") == false) {
				throw new CodeException("Unknown function "+ptr.attr, ptr.line_number);
			} 
			return null;
		}
		
		OpNode root_ptr = new OpNode();
		root_ptr.op_type = OpType.BRANCH_FUNC;
		root_ptr.var_id = func_id;
		OpNode tail_ptr = root_ptr;
		
		System.out.println("Process Func: "+ptr.attr);
		TextAttr child_ptr = ptr.child_ptr;
		int offset = 0;

		while(child_ptr != null) {
			
			OpNode node = new OpNode();
			child_ptr = ConstructOperationTree(child_ptr);
			node.child_ptr = child_ptr.op_node;
			node.op_type = OpType.BRANCH_VAR;
			
			String var_func = func_id + " " + offset++;
			Integer var_id = variable_func_map.get(var_func);
			if(var_id == null) {
				throw new CodeException("Function "+ptr.attr+" expects "+
						func_arg_num.get(func_id)+" arguments", ptr.line_number);
			}
			node.var_id = var_id;
			tail_ptr.next_ptr = node;
			tail_ptr = node;
			
			child_ptr = child_ptr.next_ptr;
		}

		if(func_arg_num.get(func_id) != offset) {
			throw new CodeException("Function "+ptr.attr+" expects "+
					func_arg_num.get(func_id)+" arguments", ptr.line_number);
		}
		
		ptr = ptr.next_ptr;
		if(ptr.delim_type == AttrType.R_ARROW) {
			ptr = ConstructOperationTree(ptr.next_ptr);
			OpNode obj = ptr.op_node;
			
			if(ptr.delim_type == AttrType.COLON) {
				ptr = ConstructOperationTree(ptr.next_ptr);
				OpNode prev_ptr = root_ptr.next_ptr;
				root_ptr.next_ptr = new OpNode();
				root_ptr.next_ptr.next_ptr = prev_ptr;
				root_ptr.next_ptr.child_ptr = ptr.op_node;
				 ptr.op_node.op_type = OpType.BRANCH_PROB;
			}
			
			root_ptr.child_ptr = obj;
			ptr = ptr.next_ptr;
		}
	
		ptr.op_node = root_ptr;
		
		return ptr;
	}
	
	// This adds a branch node for a new condition set
	private void AddNewBranchSet(int level) {
		
		BranchSet prev_ptr = branch_stack.get(level);
		branch_stack.set(level, new BranchSet());
		branch_stack.get(level).next_ptr = prev_ptr;
	}
	
	// This adds a new branch node to the branch set
	private void AddConditionNode(int level) {
		BranchSet ptr = branch_stack.get(level);
		
		if(ptr.root_ptr == null) {
			ptr.root_ptr = new ConditionNode();
			ptr.tail_ptr = ptr.root_ptr;
		} else {
			ptr.tail_ptr.next_ptr = new ConditionNode();
			ptr.tail_ptr = ptr.tail_ptr.next_ptr;
		}
		
		ptr.tail_ptr.flow_ptr = tail_flow_ptr;
	}
	
	// This is used to handle branching commands
	private TextAttr HandleBranchingCommands(TextAttr curr_ptr, TextAttr for_loop_end_ptr,
			int level, FlowNode root_loop_ptr, BranchSet for_branch) throws IOException, CodeException {
		
		if(curr_ptr.attr.equals("print")) {
			if(curr_ptr.delim_type != AttrType.L_CURL_BR) {
				throw new CodeException("Expect (", curr_ptr.line_number);
			}
			
			ConstructOperationTree(curr_ptr.child_ptr);
			OpNode root_ptr = new OpNode();
			root_ptr.op_type = OpType.PRINT;
			root_ptr.next_ptr = ConstructOperationTree(curr_ptr.child_ptr).op_node;
			tail_flow_ptr.op_node = root_ptr;
			 
			NewFlowNode();
			return curr_ptr.next_ptr;
		}
		
		if(curr_ptr.attr.equals("plot")) {
			if(curr_ptr.delim_type != AttrType.L_CURL_BR) {
				throw new CodeException("Expect (", curr_ptr.line_number);
			}
			
			OpNode root_ptr = new OpNode();
			root_ptr.op_type = OpType.PLOT;
			TextAttr next_ptr = ConstructOperationTree(curr_ptr.child_ptr);
			root_ptr.child_ptr = next_ptr.op_node;
			next_ptr = ConstructOperationTree(next_ptr.next_ptr);
			root_ptr.next_ptr = next_ptr.op_node;
			tail_flow_ptr.op_node = root_ptr;
			
			root_ptr.var_id = plot_str_buff.size();
			if(next_ptr.next_ptr != null) {
				plot_str_buff.add(next_ptr.next_ptr.attr.replace("'", "").replace("\"", ""));
			} else {
				plot_str_buff.add("Series "+(root_ptr.var_id + 1));
			}
			
			NewFlowNode();
			
			return curr_ptr.next_ptr;
		}
		
		// This checks for a function call
		if(curr_ptr.delim_type == AttrType.L_CURL_BR) {
			TextAttr next_ptr = ProcessFunctionCall(curr_ptr);
			if(next_ptr != null) {
				tail_flow_ptr.op_node = next_ptr.op_node;
				NewFlowNode();
				return next_ptr;
			}
		}
		
		
		if(curr_ptr.attr.equals("break")) {
			tail_flow_ptr.branch_node = for_branch;
			tail_flow_ptr.next_ptr = null;
			tail_flow_ptr = null;
			NewFlowNode();
			return curr_ptr.next_ptr;
		}
		
		if(curr_ptr.attr.equals("continue")) {
			EndForLoop(root_loop_ptr, for_loop_end_ptr);
			return curr_ptr.next_ptr;
		}
		
		if(curr_ptr.attr.equals("if")) {
			AddNewBranchSet(level);
			AddConditionNode(level);
			
			if(curr_ptr.delim_type != AttrType.L_CURL_BR) {
				throw new CodeException("Expect (", curr_ptr.line_number);
			}
			ParseIfCondition(curr_ptr.child_ptr, level);
			return curr_ptr.next_ptr;
		}
		
		if(curr_ptr.attr.equals("else")) {
			AddConditionNode(level);
			
			if(curr_ptr.next_ptr.attr.equals("if")) {
				curr_ptr = curr_ptr.next_ptr;
				ParseIfCondition(curr_ptr.child_ptr, level);
				return curr_ptr.next_ptr;
			}
			
			return null;
		}
		
		return null;
	}
	
	// This is used to perform the post increment for the for loop
	// and connect the program flow to the condition of the for loop
	private TextAttr EndForLoop(FlowNode root_loop_ptr,
			TextAttr for_loop_end_ptr) throws IOException, CodeException {
		
		if(for_loop_end_ptr == null) {
			return null;
		}
		
		FlowNode prev_ptr = null;
		while(for_loop_end_ptr != null) {
			System.out.println(for_loop_end_ptr.attr);
			prev_ptr = tail_flow_ptr;
			for_loop_end_ptr = ProcessAssignment(for_loop_end_ptr);
			
			if(for_loop_end_ptr.next_ptr == null && for_loop_end_ptr.delim_type != AttrType.R_CURL_BR) {
				throw new CodeException("Expect )", for_loop_end_ptr.line_number);
			}
			for_loop_end_ptr = for_loop_end_ptr.next_ptr;
		}

		NewFlowNode();
		prev_flow_ptr = tail_flow_ptr;
		
		if(root_loop_ptr != null) {
			prev_ptr.next_ptr = root_loop_ptr;
		}

		return for_loop_end_ptr;
	}
	
	// This is used to parse the entire document structure
	private boolean ParseDocument(TextAttr curr_ptr, int level, int action_depth, 
			BranchSet for_branch, FlowNode root_for_ptr, TextAttr end_for_ptr) throws IOException, CodeException {
		
		if(level >= branch_stack.size()) {
			branch_stack.add(null);
		}
		
		if(curr_ptr == null) {
			throw new CodeException("Expect {", line_num);
		}
		
		FlowNode root_loop_ptr = null;
		TextAttr for_loop_end_ptr = null;
		BranchSet for_loop_branch_ptr = null;
		
		while(curr_ptr != null) {
			
			line_num = curr_ptr.line_number;
			root_loop_ptr_temp = null;
			TextAttr next_ptr = HandleBranchingCommands(curr_ptr, end_for_ptr, level, root_for_ptr, for_branch);
			if(next_ptr != null) {
				curr_ptr = next_ptr;
				continue;
			}
			
			if(curr_ptr.attr.equals("for")) {
				AddNewBranchSet(level);
				
				if(curr_ptr.delim_type != AttrType.L_CURL_BR) {
					throw new CodeException("Expect (", curr_ptr.line_number);
				}
				for_loop_end_ptr = ParseForLoop(curr_ptr.child_ptr, level);
				root_loop_ptr = root_loop_ptr_temp;
				for_loop_branch_ptr = branch_stack.get(level);
				curr_ptr = curr_ptr.next_ptr;
				continue;
			}
			
			if(curr_ptr.attr.equals("while")) {
				AddNewBranchSet(level);
				if(curr_ptr.delim_type != AttrType.L_CURL_BR) {
					throw new CodeException("Expect (", curr_ptr.line_number);
				}
				ParseIfCondition(curr_ptr.child_ptr, level);
				root_loop_ptr = root_loop_ptr_temp;
				for_loop_branch_ptr = branch_stack.get(level);
				curr_ptr = curr_ptr.next_ptr;
				continue;
			}
			
			if(curr_ptr.attr.equals("return")) {
				curr_ptr = ConstructOperationTree(curr_ptr.next_ptr);
				tail_flow_ptr.op_node = new OpNode();
				tail_flow_ptr.op_node.next_ptr = curr_ptr.op_node;
				tail_flow_ptr.op_node.op_type = OpType.RETURN;
				NewFlowNode();
				return true;
			}
			
			if(curr_ptr.attr.equals("function")) {
				curr_ptr = ParseFunctionDeclaration(curr_ptr, level,
						action_depth, root_loop_ptr, for_loop_end_ptr,
						for_loop_branch_ptr);
				continue;
			}
			
			if(curr_ptr.attr.equals("action")) {
				tail_flow_ptr.is_start_action = true;
				ParseDocument(curr_ptr.child_ptr, level, action_depth + 1, for_loop_branch_ptr,
						root_loop_ptr, for_loop_end_ptr);
				tail_flow_ptr.action_depth++;
				
				while(curr_ptr.next_ptr.attr.equals("else")) {
					curr_ptr = curr_ptr.next_ptr;
					tail_flow_ptr.is_alt_action = true;
					ParseDocument(curr_ptr.child_ptr, level, action_depth + 1, for_loop_branch_ptr,
							root_loop_ptr, for_loop_end_ptr);
					tail_flow_ptr.action_depth++;
				} 
				
				curr_ptr = curr_ptr.next_ptr;
				continue;
			}
			
			if(curr_ptr.attr.equals("scenarios")) {
				tail_flow_ptr.is_start_scenario = true;
				ParseDocument(curr_ptr.child_ptr, level, action_depth + 1, for_loop_branch_ptr,
						root_loop_ptr, for_loop_end_ptr);
				tail_flow_ptr.action_depth++;
				curr_ptr = curr_ptr.next_ptr;
				continue;
			} 
			
			if(curr_ptr.attr.equals("exclusive")) {
				tail_flow_ptr.is_start_excl = true;
				ParseDocument(curr_ptr.child_ptr, level, action_depth + 1, for_loop_branch_ptr,
						root_loop_ptr, for_loop_end_ptr);
				tail_flow_ptr.action_depth++;
				curr_ptr = curr_ptr.next_ptr;
				continue;
			} 
			
			if(curr_ptr.child_ptr != null) {
				ParseDocument(curr_ptr.child_ptr, level + 1, action_depth, 
						for_loop_branch_ptr != null ? for_loop_branch_ptr : for_branch, 
						root_loop_ptr != null ? root_loop_ptr : root_for_ptr,
								for_loop_end_ptr != null ? for_loop_end_ptr : end_for_ptr);
				
				EndForLoop(root_loop_ptr, for_loop_end_ptr);
				
				root_loop_ptr = null;
				for_loop_end_ptr = null;
				for_loop_branch_ptr = null;
	
				if(branch_stack.get(level) != null) {
					prev_flow_ptr.branch_node = branch_stack.get(level);
					branch_stack.get(level).term_op = tail_flow_ptr;
				}
			}
			
			curr_ptr = ProcessAssignment(curr_ptr);
			curr_ptr = curr_ptr.next_ptr;
		}
		
		return false;
	}

	// This is used to parse the function definition and add a default return statement
	private TextAttr ParseFunctionDeclaration(TextAttr curr_ptr, int level,
			int action_depth, FlowNode root_loop_ptr,
			TextAttr for_loop_end_ptr, BranchSet for_loop_branch_ptr)
			throws IOException, CodeException {
		
		curr_ptr = curr_ptr.next_ptr;
		
		if(curr_ptr.attr.equals("main")) {
			main_flow_ptr = tail_flow_ptr;
		}
		
		int func_id = function_map.get(curr_ptr.attr);
		function_buff.set(func_id, tail_flow_ptr);
		curr_ptr = curr_ptr.next_ptr;
		
		is_local_var = true;
		curr_func_id = func_id;
		boolean is_ret = ParseDocument(curr_ptr.child_ptr, level + 1, action_depth, 
				for_loop_branch_ptr, root_loop_ptr, for_loop_end_ptr);
		curr_func_id = -1;
		is_local_var = false;
		
		if(is_ret == false) {
			// add a default return statement
			tail_flow_ptr.op_node = new OpNode();
			tail_flow_ptr.op_node.next_ptr = new OpNode();
			tail_flow_ptr.op_node.op_type = OpType.RETURN;
			NewFlowNode();
		}
		
		curr_ptr = curr_ptr.next_ptr;
		return curr_ptr;
	}
	
	// This parses the set of function names first to differentiate variables 
	private void ParseDocumentFunctions(TextAttr curr_ptr) throws IOException, CodeException {
		
		while(curr_ptr != null) {
			if(curr_ptr.attr.equals("function")) {
				
				if(function_map.get(curr_ptr.next_ptr.attr) != null) {
					throw new CodeException("Function "+curr_ptr.next_ptr.attr+" already declared", curr_ptr.line_number);
				}
				
				function_map.put(curr_ptr.next_ptr.attr, function_map.size());
				function_buff.add(null);
				
				curr_ptr = curr_ptr.next_ptr;
				int func_id = function_map.get(curr_ptr.attr);
				curr_func_id = func_id;
				
				TextAttr child_ptr = curr_ptr.child_ptr;
				int var_id = 0;
				
				while(child_ptr != null) {
					if(VarID(child_ptr.attr) == null) {
						VarSemantics var_map = new VarSemantics();
						var_map.var_offset = var_num++;
						variable_map.put(child_ptr.attr + func_id, var_map);
						
						String var_func = func_id + " " + var_id;
						variable_func_map.put(var_func, var_map.var_offset);
						var_id++;
					} else {
						throw new CodeException("Varible "+child_ptr.attr+" already declared", curr_ptr.line_number);
					}
					
					child_ptr = child_ptr.next_ptr;
				}
				
				func_arg_num.add(var_id);
			}
			
			curr_func_id = -1;
			if(curr_ptr.child_ptr != null) {
				ParseDocumentFunctions(curr_ptr.child_ptr);
			}
			
			curr_ptr = curr_ptr.next_ptr;
		}
	}
	
	// This is used to build the condition branch set for the program flow
	private void ConstructBranchFlow() throws IOException {
		
		for(int i=0; i<flow_buff.size(); i++) {
			FlowNode ptr = flow_buff.get(i);
			
			if(ptr.branch_node == null) {
				continue;
			}
			
			if(ptr.is_condition == false) {
				if(ptr.next_ptr == null) {
					// branch to terminal -> only for an if condition or break
					ptr.next_ptr = ptr.branch_node.term_op;
					
				}
				continue;
			}
			if(ptr.branch_node.root_ptr == null) {
				ptr.branch_ptr = ptr.branch_node.term_op;
				continue;
			}
			
			System.out.println(ptr.branch_node+" "+ptr.branch_node.root_ptr);
			// This process the branch structure for an if condition
			if(ptr.branch_node.root_ptr.flow_ptr != flow_buff.get(i)) {
				System.out.println("Branch Node Error");System.in.read();
			}
			
			ptr.branch_node.root_ptr = ptr.branch_node.root_ptr.next_ptr;
			
			if(ptr.branch_node.root_ptr != null) {
				// next if condition
				ptr.branch_ptr = ptr.branch_node.root_ptr.flow_ptr;
			} else {
				ptr.branch_ptr = ptr.branch_node.term_op;
			}
		}
	}
	
	// This returns the main entry point function
	public FlowNode MainFunction() {
		return main_flow_ptr;
	}
		
	// This returns the flow ptr for the start of a function
	public FlowNode FunctionPtr(int func_id) {
		return function_buff.get(func_id);
	}
	
	// This returns the plot name for a give plot id
	public ArrayList<String> PlotName() {
		return plot_str_buff;
	}
	
	// This is the entry function that builds the state machine
	public void ConstructStateSet(char buffer[]) throws IOException, CodeException {
		
		StateVariable.Clear();
		tail_flow_ptr = new FlowNode();
		prev_flow_ptr = tail_flow_ptr;
		flow_buff.add(tail_flow_ptr);
		
		TextAttr root_ptr = lex.BuildDocument(buffer);
		
		lex.PrintDocument(lex.RootAttr());
		ParseDocumentFunctions(root_ptr);
		
		ParseDocument(root_ptr, 0, 0, null, null, null);
		ConstructBranchFlow();
		
		if(main_flow_ptr == null) {
			throw new CodeException("No Main Function", -1);
		}
	
	}

}
