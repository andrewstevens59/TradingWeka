package stateMachine;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import state.State;
import state.StateLink;
import state.StateVariable;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk._glp_arc;
import org.gnu.glpk._glp_graph;
import org.gnu.glpk._glp_java_arc_data;
import org.gnu.glpk._glp_java_vertex_data;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

public class BloodAlloc {
	
	// This is the linear system 
	glp_prob m_lp;

	// Substitution matrix
	int[][] SubOK = new int[][] {
	         {1,0,0,0,0,0,0,0}, 
	         {1,1,0,0,0,0,0,0},
	         {1,0,1,0,0,0,0,0},
	         {1,1,1,1,0,0,0,0},
	         {1,0,0,0,1,0,0,0},
	         {1,1,0,0,1,1,0,0},
	         {1,0,1,0,1,0,1,0},
	         {1,1,1,1,1,1,1,1}};
	
	String BloodType[] = new String[]{"AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"};
	
	int B = BloodType.length;
	int maxA = 5;
	int[] A = new int[]{0, 1, 2, 3, 4, 5, 6};
	int maxI = 50;
	int[] I = new int[maxI + 1];
	
	float[] SupplyProfile = new float[]{3.4f, 0.65f, 27.94f, 5.17f, 11.63f, 2.13f, 39.82f, 9.26f};
	float[] DemandProfile = new float[]{3.0f, 1.0f, 34.0f, 6.0f, 9.0f, 2.0f, 38.0f, 7.0f};
	
	int Infeasible = -999999999;
	float EPS = 0.001f;
	float gamma = 0.9f;
	
	ArrayList<State> state_group;
	// This stores the root state for the starting point
	State root_state = null;
	
	SWIGTYPE_p_int ind = GLPK.new_intArray(B + 1);
	SWIGTYPE_p_double val = GLPK.new_doubleArray(B + 1);
	
	public BloodAlloc() {
		for(int i=0; i<I.length; i++) {
			I[i] = i;
		}
	}
	
	// This creates a new state
	private State NewState(State s) {

		State n = new State();
		n.action_id = s.action_id;
		n.flow_ptr = s.flow_ptr;
		n.state_util = s.state_util;
		n.time_period = s.time_period;
		
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
	
	private int PickOne(float alist[]) {
		  float t = 0.0f;
		  float u = (float) (100.0 * Math.random());
		 for(int l=0; l<alist.length; l++) {
		    t += alist[l];
		    if(t >= u) {
		      return l;
		    }
		 }
		 
		 System.out.println("#### Pickone Error");
		 return alist.length - 1;
	}
	
	private int[] GenSupply() {
		Random r = new Random();
		  int TotalSupply = (int) Math.max((int)(r.nextGaussian() * 150 + 150), 10);
		  int[] rlist = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		  for(int k=0; k<TotalSupply; k++) {
		     rlist[PickOne(SupplyProfile)]++;
		  }
		  
		  return rlist;
	}
	
	private int[] GenDemand() {
		Random r = new Random();
		  int TotalDemand = (int) Math.max((int)(r.nextGaussian() * 200 + 200), 10);
		  int[] rlist = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		  for(int k=0; k<TotalDemand; k++) {
		     rlist[PickOne(DemandProfile)]++;
		  }
		  
		  return rlist;  
	}
	
	private float MyopicValue(int b1, int[] d) {
		  // Calculate the no look ahead value of the demand
		
		  float Value = 0.0f;
		  int b2 = d[0];
		  int urgent = d[1];
		  int suballowed = d[2];

		  if(b1 != b2) {
		    if(suballowed == 0) {
		      return Infeasible;
		    }
		    
		    if(SubOK[b1][b2] == 0) {
		      return Infeasible;
		    }
		    
		    Value -= 10.0 + (b2 - b1) * 0.001f;
		  }
		  
		  if(BloodType[b1] == "O-") {
		    Value += 5.0f;
		  }
		  
		  if(urgent == 1) {
		    Value += 40.0f;
		  } else {
		    Value += 20.0f;
		  }
		  
		  return Value;
	}
	
	// This constructs the network flow problem 
	private void NetworkFlowProblem(State s, int action_id, float blood_alloc) {
		
		m_lp = GLPK.glp_create_prob();
		int[] D = GenDemand();
		
		for(int i=0; i<B; i++) {
			System.out.println(D[i]+" "+StateVariable.StateVar(s, i));
		}
		System.out.println("");
		int offset = 1;
		// create the blood demand variables
		GLPK.glp_set_obj_dir(m_lp, GLPKConstants.GLP_MAX);
        GLPK.glp_set_obj_coef(m_lp, 0, 0);

		HashMap<String, Integer> map = new HashMap<String, Integer>();
		// create the blood supply variables
		for(int i=0; i<B; i++) {
			for(int j=0; j<B; j++) {
				
				if(SubOK[i][j] == 1) {
					String str=  i+" "+j;
					map.put(str, offset);
					
					GLPK.glp_add_cols(m_lp, offset);
					GLPK.glp_set_col_kind(m_lp, offset, GLPKConstants.GLP_CV);
					GLPK.glp_set_col_bnds(m_lp, offset, GLPKConstants.GLP_LO, 0, 0);
					GLPK.glp_set_obj_coef(m_lp, offset++, 1);
				}
			}
		}
		
		System.out.println("Map Size: "+map.size());

		offset = 1;
		// place a limit on the maximum supply
		for(int i=0; i<B; i++) {
			
			int count = 1;
			for(int j=0; j<B; j++) {
				
				if(SubOK[i][j] == 1) {
					String str =  i+" "+j;
					Integer id = map.get(str);
					
					GLPK.intArray_setitem(ind, count, id);
					GLPK.doubleArray_setitem(val, count, 1);
					count++;
				}
			}
			
			if(count > B + 1) {
				System.out.println("exceed");System.exit(0);
			}
			
			if(StateVariable.StateVar(s, i) < -0.00001) {
				System.out.println("lo");System.exit(0);
			}
			
			GLPK.glp_add_rows(m_lp, 1);
			GLPK.glp_set_row_bnds(m_lp, offset, GLPKConstants.GLP_UP, 0, StateVariable.StateVar(s, i) * blood_alloc);
			GLPK.glp_set_mat_row(m_lp, offset, count - 1, ind, val);
			offset++;
		}
		
		// place a limit on the maximum demand
		for(int i=0; i<B; i++) {

			int count = 1;
			for(int j=0; j<B; j++) {
				
				if(SubOK[j][i] == 1) {
					String str =  j+" "+i;
					Integer id = map.get(str);
					
					GLPK.intArray_setitem(ind, count, id);
					GLPK.doubleArray_setitem(val, count, 1);
					count++;
				}
			}
			
			if(count > B + 1) {
				System.out.println("exceed");System.exit(0);
			}
			
			if(D[i] < 0) {
				System.out.println("lo1");System.exit(0);
			}
			
			GLPK.glp_add_rows(m_lp, 1);
			GLPK.glp_set_row_bnds(m_lp, offset, GLPKConstants.GLP_UP, 0, D[i]);
			GLPK.glp_set_mat_row(m_lp, offset, count - 1, ind, val);
			offset++;
		}
		
		glp_smcp parm = new glp_smcp();
        GLPK.glp_init_smcp(parm);
        int ret = GLPK.glp_simplex(m_lp, parm);

        // Retrieve solution
        if (ret != 0) {
            System.out.println("The problem could not be solved");
        }
        
        State n = NewState(s);
        n.state_util = 0;
        n.action_id = action_id;
        for(int i=0; i<B; i++) {
			
			double sum = 0;
			for(int j=0; j<B; j++) {
				
				if(SubOK[i][j] == 1) {
					String str =  i+" "+j;
					Integer id = map.get(str);
					sum += GLPK.glp_get_col_prim(m_lp, id);
				}
			}
		
			
            StateVariable.AssignStateVar(n, i, (StateVariable.StateVar(s, i) * blood_alloc) - sum);
            
            if((StateVariable.StateVar(s, i) * blood_alloc) + 0.001 < sum) {
            	System.out.println("out miss "+StateVariable.StateVar(s, i)+" "+sum);System.exit(0);
            }
		}
        
        for (int i = 0; i < B; i++) {
            
            double sum = 0;
            for(int j=0; j<B; j++) {
				
				if(SubOK[j][i] == 1) {
					String str =  j+" "+i;
					Integer id = map.get(str);
					
					sum += GLPK.glp_get_col_prim(m_lp, id);
				}
			}
            
            n.state_util += Math.abs(D[i] - sum);
            
            if(sum > D[i] + 0.0001) {
            	System.out.println("exceed demand "+sum+" "+D[i]);System.exit(0);
            }
        }
        
        int sum[] = new int[B];
        for(int i=0; i < B; i++) {
        	sum[i] = 0;
        }
        
        n.state_util = 1.0f / (n.state_util + 1.0f);
        for(int i=0; i < 1; i++) {
        	int[] S = GenSupply();
        	for(int i1=0; i1 < B; i1++) {
            	sum[i1] += S[i1];
            }
        }
        
        for(int i1=0; i1 < B; i1++) {
        	sum[i1] /= 10;
        }
        	
    	 State n1 = NewState(n);
    	 
    	 n1.time_period = s.time_period + 1;
    	 n1.state_util = n.state_util;
    	 
    	 for (int j = 0; j < B; j++) {
    		 StateVariable.AssignStateVar(n1, j, StateVariable.StateVar(n, j) + sum[j]);
    	 }
    	 
    	 state_group.add(n1);

        // Free memory
        GLPK.glp_delete_prob(m_lp);
	}
	
	// This returns the root state
	public State RootState() {
		return root_state;
	}
	
	// This is the entry function to begin the state machine
	public ArrayList<State> StateMachine(State s, char buffer[]) throws IOException, CodeException {

		System.out.println("innnnnnnnnnnnnnnnnnnnn");
		if(s == null) {
			int[] S = GenSupply();
			s = new State();
			s.time_period = 0;
			s.state_util = 0;
			root_state = s;
			for(int i=0; i<B; i++) {
				StateVariable.AssignStateVar(s, i, S[i]);
			}
		}
		
		state_group = new ArrayList<State>();
		if(s.time_period >= 5) {
			return state_group;
		}
		
		for(int i=0; i<5; i++) {
			NetworkFlowProblem(s, i, 1.0f / (i + 1));
		}
		
		System.out.println("outttttttttttttttttttttttttt");

		return state_group;
	}
}
