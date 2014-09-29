package stateMachine;

import java.io.IOException;

import java.util.ArrayList;

import state.PointPlot;
import state.State;
import state.StateVariable;

public class OpNode {
	
	// This stores the type of operation 
	OpType op_type;
	// This stores the left branch operation 
	OpNode child_ptr = null;
	// This stores the left branch operation 
	OpNode next_ptr = null;
	// This stores the variable id
	int var_id = -1;
	// This stores a multiplication factor
	int mult_factor;
	// This stores the final value of the operation
	double op_val;
	// This stores a text string constant
	String str_const = null;
	
	// This returns the variable id for a node
	public int VarID(State s) throws IOException {
		
		if(child_ptr == null) {
			return var_id;
		}
		
		return var_id + (int)child_ptr.CalcOpNode(s);
	}
	
	// This is used to calculate the value of an operation node
	public double CalcOpNode(State s) throws IOException {
		
		if(op_type == OpType.BRANCH_VAR) {
			StateVariable.AssignStateVar(s, this.var_id, child_ptr.CalcOpNode(s));
			
			if(next_ptr != null) {
				return next_ptr.CalcOpNode(s);
			}
			return 0;
		}
		
		if(op_type == OpType.PLOT) {
			PointPlot prev_ptr = s.points;
			s.points = new PointPlot();
			s.points.next_ptr = prev_ptr;
			s.points.x = child_ptr.CalcOpNode(s);
			s.points.y = next_ptr.CalcOpNode(s);
			s.points.plot_id = var_id;
			return 0;
		}
		
		if(op_type == OpType.PRINT) {
			OpNode ptr = next_ptr; 
			if(s.print_str == null) {
				s.print_str = new String();
			}
			
			while(ptr != null) {
				if(ptr.str_const != null) {
					s.print_str += ptr.str_const;
				} else if(ptr.child_ptr != null) {
					s.print_str += ptr.child_ptr.CalcOpNode(s);
				} else {
					if(ptr.var_id < 0) {
						// constant
						s.print_str += ptr.op_val;
					} else {
						s.print_str += StateVariable.StateVar(s, ptr.var_id);
					}
				}
				
				ptr = ptr.next_ptr;
			}
			
			s.print_str += "\n";
			
			return -1;
		}
		
		if(op_type == OpType.INCREMENT) {
			int var_id = child_ptr.VarID(s);
			double val1 = StateVariable.StateVar(s, var_id);
			StateVariable.AssignStateVar(s, var_id, val1 + 1);
			return val1;
		}
		
		if(op_type == OpType.DECREMENT) {
			int var_id = child_ptr.VarID(s);
			double val1 = StateVariable.StateVar(s, var_id);
			StateVariable.AssignStateVar(s, var_id, val1 - 1);
			return val1;
		}
		
		if(op_type == OpType.ASSIGN_MULT_VAR) {
			int var_id = child_ptr.VarID(s);
			double val1 = StateVariable.StateVar(s, var_id);
			StateVariable.AssignStateVar(s, var_id, val1 * next_ptr.CalcOpNode(s));
			return val1;
		}
		
		if(op_type == OpType.ASSIGN_DIV_VAR) {
			int var_id = child_ptr.VarID(s);
			double val1 = StateVariable.StateVar(s, var_id);
			StateVariable.AssignStateVar(s, var_id, val1 / next_ptr.CalcOpNode(s));
			return val1;
		} 
		
		if(op_type == OpType.ASSIGN_ADD_VAR) {
			int var_id = child_ptr.VarID(s);
			double val1 = StateVariable.StateVar(s, var_id);
			StateVariable.AssignStateVar(s, var_id, val1 + next_ptr.CalcOpNode(s));
			return val1;
		} 
		
		if(op_type == OpType.ASSIGN_SUB_VAR) {
			int var_id = child_ptr.VarID(s);
			double val1 = StateVariable.StateVar(s, var_id);
			StateVariable.AssignStateVar(s, var_id, val1 - next_ptr.CalcOpNode(s));
			return val1;
		} 
		
		if(op_type == OpType.ASSIGN_VAR) {
			int var_id = child_ptr.VarID(s);
			double val1 = next_ptr.CalcOpNode(s);
			StateVariable.AssignStateVar(s, var_id, val1);
			return val1;
		}
		
		double val = 0;
		if(child_ptr == null) {
			if(var_id < 0) {
				// constant
				val = op_val;
			} else {
				val = StateVariable.StateVar(s, var_id);
			}
	
		} else {
			val = child_ptr.CalcOpNode(s);
		}

		if(op_type == OpType.DIVIDE) {
			return val / next_ptr.CalcOpNode(s);
		}
		
		if(op_type == OpType.ADD) {
			return val + next_ptr.CalcOpNode(s);
		}
		
		if(op_type == OpType.SUBTRACT) {
			return val - next_ptr.CalcOpNode(s);
		}

		if(op_type == OpType.MULTIPLY) {
			return val * next_ptr.CalcOpNode(s);
		}
		
		if(op_type == OpType.MULT_ADD) {
			if(next_ptr == null) {
				return (val * mult_factor);
			}
			return (val * mult_factor) + next_ptr.CalcOpNode(s);
		}
		
		if(op_type == OpType.ADD_TO_VAR) {
			// an array value
			return StateVariable.StateVar(s, var_id + (int)val);
		}
		
		if(op_type == OpType.AND) {
			if((val != 0.0) && (next_ptr.CalcOpNode(s) != 0.0)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.OR) {
			if((val != 0.0) || (next_ptr.CalcOpNode(s) != 0.0)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.NOT) {
			if((val == 0.0)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.EQOR) {
			if((val != 0.0) ^ (next_ptr.CalcOpNode(s) != 0.0)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.LT) {
			if(val < next_ptr.CalcOpNode(s)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.LTE) {
			if(val <= next_ptr.CalcOpNode(s)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.GT) {
			if(val > next_ptr.CalcOpNode(s)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.GTE) {
			if(val >= next_ptr.CalcOpNode(s)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		if(op_type == OpType.EQUALITY) {
			if(val == next_ptr.CalcOpNode(s)) {
				return 1.0f;
			}
			
			return 0.0;
		}
		
		return val;
}
	
	// This prints the operation tree
	public void PrintOpTree() {
		
		ArrayList<OpNode> buff = new ArrayList<OpNode>();
		
		OpNode ptr = this;
		while(ptr != null) {
			System.out.print(ptr+" "+ptr.op_val+" "+ptr.op_type+" "+ptr.var_id+" "+ptr.mult_factor+" \""+ptr.str_const+"\"   Child: "+ptr.child_ptr+"    <>    ");
			
			if(ptr.child_ptr != null) {
				buff.add(ptr.child_ptr);
			}
			
			ptr = ptr.next_ptr;
		}
		
		System.out.print("\n");
		for(int i=0; i<buff.size(); i++) {
			buff.get(i).PrintOpTree();
		}
	}
}
