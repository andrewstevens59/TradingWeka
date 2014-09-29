package CGLSMethod;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;


import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;


import DecisionNetwork.DataSet;

public class CreateClassifier {
	
	/*// This stores the number of times two classifiers disagree
	class Pair {
		DecisionNode left;
		DecisionNode right;
		int match;
	}
	
	// This stores the set of local nodes
	private HashSet<DecisionNode> local_map = new HashSet<DecisionNode>();
	// This stores the set of matches
	private ArrayList<Pair> match_buff = new ArrayList<Pair>();
	// This stores the set of neighbour nodes
	private ArrayList<DecisionNode> neighbour_buff = new ArrayList<DecisionNode>();
	// This stores the set of children
	private ArrayList<DecisionNode> child_set;
	// This stores the max allocation
	private float best_util = 0;
	// This stores the correlation between each neighbour
	private int corr_mat[][];
	// This defines the number of conditions
	static int CHILD_NUM = 9;
	
	// This stores the number of times two classifiers disagree
	private void ClassificationCorr(DataSet d) {
		
		corr_mat = new int[neighbour_buff.size()][neighbour_buff.size()];
		for(int i=0; i<neighbour_buff.size(); i++) {
			for(int j=i+1; j<neighbour_buff.size(); j++) {
				corr_mat[i][j] = 0;
				
				Pair p = new Pair();
				p.left = neighbour_buff.get(i);
				p.right = neighbour_buff.get(j);
				p.match = 0;
				match_buff.add(p);
				
				for(int k=0; k<d.DataSet().size(); k++) {
					boolean sample[] = d.DataSet().get(k);
					
					if(p.left.Output(sample) != p.right.Output(sample)) {
						p.match++;
						corr_mat[i][j]++;
					}
				}
			}
		}
	}
	
	// This checks the ip solution 
	private void CheckIPSolution(glp_prob lp, ArrayList<DecisionNode> neighbour, DecisionNode host) {
		
		for(int i=0; i<match_buff.size(); i++) {
    		double d1 = GLPK.glp_mip_col_val(lp, i + 1);
    		Pair p = match_buff.get(i);
    		
    		if(d1 == 1.0f) {
    			if(GLPK.glp_mip_col_val(lp, p.left.var_id) == 0) {
    				System.out.println("error left");
    			}
    			
    			if(GLPK.glp_mip_col_val(lp, p.right.var_id) == 0) {
    				System.out.println("error right");
    			}
    		}
    	}
		
		int count = 0;
		for(int i=0; i<neighbour.size(); i++) {
			double d1 = GLPK.glp_mip_col_val(lp, neighbour.get(i).var_id);
			if(d1 == 1.0f) {
				count++;
			}
		}
		
		if(count > CHILD_NUM) {
			System.out.println("max child");System.exit(0);
		}
		
		double d1 = GLPK.glp_mip_col_val(lp, host.var_id);
		if(d1 == 0.0f) {
			System.out.println("not host");System.exit(0);
		}
	}
	
	// This finds the maximum entropy set
	private ArrayList<DecisionNode> FindChildren(DecisionNode host, ArrayList<DecisionNode> neighbour) {
		
		 glp_iocp iocp;
	    SWIGTYPE_p_int ind;
	    SWIGTYPE_p_double val;
	    int ret;

	//  Create problem    
	    glp_prob lp = GLPK.glp_create_prob();
	    System.out.println("Problem created");
	    GLPK.glp_set_prob_name(lp, "myProblem");
	    
	//  Define objective 
	    GLPK.glp_set_obj_name(lp, "obj");
	    GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MAX);
	    
	    GLPK.glp_add_cols(lp, match_buff.size() + neighbour.size());
	    
	    int count = 1;
	    for(int i=0; i<match_buff.size(); i++) {
	    	GLPK.glp_set_obj_coef(lp, count, match_buff.get(i).match);
		    GLPK.glp_set_col_kind(lp, count++, GLPKConstants.GLP_BV);
	    }
	    
	    for(int i=match_buff.size(); i<match_buff.size() + neighbour.size(); i++) {
	    	GLPK.glp_set_obj_coef(lp, count, 0);
		    GLPK.glp_set_col_kind(lp, count++, GLPKConstants.GLP_BV);
	    }

	    int curr_row = 1;
	    // 2e <= n1 + n2
	    for(int i=0; i<match_buff.size(); i++) {
	    	
	    	Pair p = match_buff.get(i);
			
			GLPK.glp_add_rows(lp, 1);
			ind = GLPK.new_intArray(4);
			val = GLPK.new_doubleArray(4);
			
			GLPK.intArray_setitem(ind, 1, p.left.var_id);
			GLPK.doubleArray_setitem(val, 1, -1);
			
			GLPK.intArray_setitem(ind, 2, p.right.var_id);
			GLPK.doubleArray_setitem(val, 2, -1);
			
			GLPK.intArray_setitem(ind, 3, i + 1);
			GLPK.doubleArray_setitem(val, 3, 2);
			
			GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_UP, 0, 0);
			GLPK.glp_set_mat_row(lp, curr_row++, 3, ind, val);
	    }
	    
	    // host state must be one
	    GLPK.glp_add_rows(lp, 1);
	    ind = GLPK.new_intArray(2);
		val = GLPK.new_doubleArray(2);
		GLPK.intArray_setitem(ind, 1, host.var_id);
		GLPK.doubleArray_setitem(val, 1, 1);
		
	    GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_LO, 1, 0);
		GLPK.glp_set_mat_row(lp, curr_row++, 1, ind, val);
		
		
		GLPK.glp_add_rows(lp, 1);
	    ind = GLPK.new_intArray(neighbour.size() + 1);
		val = GLPK.new_doubleArray(neighbour.size() + 1);
		
		// maximum number of children
		for(int i=0; i<neighbour.size(); i++) {
			GLPK.intArray_setitem(ind, i + 1, neighbour.get(i).var_id);
			GLPK.doubleArray_setitem(val, i + 1, 1);
		}
		
		GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_UP, 0, CHILD_NUM);
		GLPK.glp_set_mat_row(lp, curr_row++, neighbour.size(), ind, val);

	//  solve model
	    iocp = new glp_iocp();
	    GLPK.glp_init_iocp(iocp);
	    iocp.setPresolve(GLPKConstants.GLP_ON);
	    ret = GLPK.glp_intopt(lp, iocp);
	    
	    double val1  = GLPK.glp_mip_obj_val(lp);
	    ArrayList<DecisionNode> child_buff = new ArrayList<DecisionNode>();
	    
		//  Retrieve solution
	    if (ret == 0) {
	    	
	    	for(int i=0; i<neighbour.size(); i++) {
	    		double d1 = GLPK.glp_mip_col_val(lp, i + match_buff.size() + 1);
	    		
	    		if(d1 == 1.0f) {
	    			child_buff.add(neighbour.get(i));
	    		}
	    	}
	    	
	    	CheckIPSolution(lp, neighbour, host);
	    }
	    else {
	      System.out.println("The problemcould not be solved");
	    };
	    
	    // free memory
	    GLPK.glp_delete_prob(lp);
	    
	    return child_buff;
	}
	
	// This cycles through all the combinations and finds the best one
	public void Enumerate(int combo, int depth, int set_num, DecisionNode host) {
		
		if(depth >= neighbour_buff.size() || set_num <= 0) {
			
			int val = combo;
			
			boolean found = false;
			int local_num = 0;
			ArrayList<DecisionNode> buff = new ArrayList<DecisionNode>();
			for(int i=0; i<neighbour_buff.size(); i++) {
				
				if((val & 0x01) == 0x01) {
					neighbour_buff.get(i).var_id = i;
					buff.add(neighbour_buff.get(i));
					
					if(local_map.contains(neighbour_buff.get(i)) == true) {
						local_num++;
					}
					
					if(neighbour_buff.get(i) == host) {
						found = true;
					}
				}
				
				val >>= 1;
			}
			
			if(local_num >= buff.size() && host.InputNode().GlobalNeighbours().size() > 0) {
				// can't all be local links
				return;
			}
			
			//System.out.println(local_num+" "+found);
			if(local_num < Math.min(3, local_map.size())) {
				// at least 3 must be local
				return;
			}
			
			if(found == false) {
				return;
			}
			
			float util = 0;
			for(int i=0; i<buff.size(); i++) {
				for(int j=i+1; j<buff.size(); j++) {
					util += corr_mat[buff.get(i).var_id][buff.get(j).var_id];
				}
			}
			
			if(util > best_util) {
				best_util = util;
				child_set = buff;
			}
			
			return;
		}
		
		Enumerate(combo, depth + 1, set_num, host);
		Enumerate(combo | (1 << depth), depth + 1, set_num - 1, host);
	}

	public DecisionNode Classifier(DecisionNode host, DataSet d) {
		
		neighbour_buff.add(host);
		local_map.add(host);
		
		for(int i=0; i<host.InputNode().LocalNeighbours().size(); i++) {
			neighbour_buff.add(host.InputNode().LocalNeighbours().get(i).dec_node);
			local_map.add(host.InputNode().LocalNeighbours().get(i).dec_node);
		}
		
		System.out.println(host.InputNode().GlobalNeighbours()+" &&&&");
		if(host.InputNode().GlobalNeighbours().size() > 0) {
			
			Collections.sort(host.InputNode().GlobalNeighbours(), new Comparator<KDNode>() {
				 
		        public int compare(KDNode arg1, KDNode arg2) {
		        	
		        	if(arg1.dec_node.class_error < arg2.dec_node.class_error) {
		    			return 1;
		    		}

		    		if(arg1.dec_node.class_error > arg2.dec_node.class_error) {
		    			return -1;
		    		}

		    		return 0; 
		        }
		    });
			
			for(int i=0; i<Math.min(host.InputNode().GlobalNeighbours().size(), 3); i++) {
				neighbour_buff.add(host.InputNode().GlobalNeighbours().get(i).dec_node);
			}
		}
		
		System.out.println("neighbour size: "+neighbour_buff.size());
		ClassificationCorr(d);
		
		int num = CHILD_NUM;
		if(neighbour_buff.size() < CHILD_NUM) {
			num = neighbour_buff.size() - 2;
		}
		
		Enumerate(0, 0, num, host);
		
		System.out.print(host+"    ");
		for(int i=0; i<child_set.size(); i++) {
			System.out.print(child_set.get(i)+" ");
		}
		
		System.out.println("");
		
		return new DecisionNode(host.InputNode(), child_set);
	}*/

}
