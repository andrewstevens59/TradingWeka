import java.util.ArrayList;


import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;


public class SwitchingRules {
	
	// This stores the training set size 
	int train_size;
	// This returns the output layer size
	int output_layer_size;

	public SwitchingRules() {
		
	}
	
	// This returns the switching state id for a given sample
	private int SwitchNodeId(int node_id, int sample_id) {
		return (sample_id * CreateModel.NodeNum()) + node_id + 1;
	}
	
	// This returns the threshold value for a node id
	private int ThreshID(int node_id) {
		return (train_size * CreateModel.NodeNum()) + node_id + 1;
	}
	
	// This returns the output state id for a given node id and sample id
	private int OutputValID(int node_id, int sample_id) {
		return train_size * CreateModel.NodeNum() + CreateModel.NodeNum() + 
				(sample_id * CreateModel.NodeNum()) + node_id + 1;
	}
	
	// This returns the |error term| for a given output state
	private int ErrorTermID(int node_id, int sampe_id) {
		return train_size * CreateModel.NodeNum() + CreateModel.NodeNum()
	    		+ train_size * CreateModel.NodeNum() + 
	    		(sampe_id * output_layer_size) + node_id + 1;
	}
	
	// This checks the output of the results
	private void CheckResults(glp_prob lp, ArrayList<ArrayList<SN>> layer, 
			ArrayList<TrainingSample> training_data) {
		
		for(int j=0; j<training_data.size(); j++) {
			TrainingSample sample = training_data.get(j);
			
	    	for(int i=1; i<layer.size(); i++) {
	    		for(int k=0; k<layer.get(i).size(); k++) {
	    			
	    			SN node = layer.get(i).get(k);
	    			double d1 = GLPK.glp_mip_col_val(lp, OutputValID(node.node_id, j));
	    			
		    		SNLink link_ptr = node.backward_link;
		    		float sum = 0;
		    		
					while(link_ptr != null) {
						sum += GLPK.glp_mip_col_val(lp, OutputValID(link_ptr.dst.node_id, j)) * link_ptr.weight;
						link_ptr = link_ptr.next_ptr;
					}
					
					if(Math.abs(sum - d1) > 0.001) {
						System.out.println("out mis "+sum+" "+d1);System.exit(0);
					}
					
	    		}
	    	}
	    }
		
	}
	
	// This is used to fit the switching rules for the current model
	public void FindSwitchinRules(ArrayList<ArrayList<SN>> layer, 
			ArrayList<TrainingSample> training_data) {
		
		    train_size = training_data.size();
		    output_layer_size = layer.get(layer.size()-1).size();
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
		    GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);
		
		    
		    // switching state for each sample, threshold value for each node,
		    // weighted output of each node for each sample, number of absolute output layer values for each training sample
		    GLPK.glp_add_cols(lp, training_data.size() * CreateModel.NodeNum() + CreateModel.NodeNum()
		    		+ training_data.size() * CreateModel.NodeNum() + layer.get(layer.size()-1).size() * training_data.size());
		    
		    int count = 1;
		    // switching state for each sample
		    for(int j=0; j<training_data.size() * CreateModel.NodeNum(); j++) {
			    GLPK.glp_set_obj_coef(lp, count, 0);
				GLPK.glp_set_col_kind(lp, count++, GLPKConstants.GLP_BV);
		    }
		    
		    // threshold value for each node
		    for(int j=0; j<CreateModel.NodeNum(); j++) {
			    GLPK.glp_set_obj_coef(lp, count, 0);
				GLPK.glp_set_col_kind(lp, count, GLPKConstants.GLP_CV);
				GLPK.glp_set_col_bnds(lp, count++, GLPKConstants.GLP_LO, 0, 0);
		    }
		    
		    // weighted output of each node for each sample
		    for(int j=0; j<training_data.size() * CreateModel.NodeNum(); j++) {
			    GLPK.glp_set_obj_coef(lp, count, 0);
				GLPK.glp_set_col_kind(lp, count, GLPKConstants.GLP_CV);
				GLPK.glp_set_col_bnds(lp, count++, GLPKConstants.GLP_DB, -10000, 10000);
		    }
		    
		    // minimize the net error term sum_i |error_i|
		    for(int j=0; j<layer.get(layer.size()-1).size() * training_data.size(); j++) {
			    GLPK.glp_set_obj_coef(lp, count, 1);
				GLPK.glp_set_col_kind(lp, count, GLPKConstants.GLP_CV);
				GLPK.glp_set_col_bnds(lp, count++, GLPKConstants.GLP_DB, -10000, 10000);
		    }

		    int curr_row = 1;
		    // weighted input - a(switching state/sample) >= threshold
		    for(int j=0; j<training_data.size(); j++) {
		    	TrainingSample sample = training_data.get(j);
		    	
	    		for(int k=0; k<layer.get(0).size(); k++) {

	    			SN node = layer.get(0).get(k);
		    		SNLink link_ptr = node.backward_link;
		    		float input = 0;
		    		
					while(link_ptr != null) {
						input += sample.input_val[link_ptr.dst.node_id] * link_ptr.weight;
						link_ptr = link_ptr.next_ptr;
					}
					
					
			    	ind = GLPK.new_intArray(3);
					val = GLPK.new_doubleArray(3);

					GLPK.intArray_setitem(ind, 1, SwitchNodeId(node.node_id, j));
					GLPK.doubleArray_setitem(val, 1, -1);
					
					GLPK.intArray_setitem(ind, 2, ThreshID(node.node_id));
					GLPK.doubleArray_setitem(val, 2, -1);
			    	
			    	GLPK.glp_add_rows(lp, 1);
				    GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_LO, -input, 0);
					GLPK.glp_set_mat_row(lp, curr_row++, 2, ind, val);
	    		}
		    }
		    
		  /*  // construct the output value as sum of input values for switching rules only
		    // (sum input)a_i = c_i
		    for(int j=0; j<training_data.size(); j++) {
		    	TrainingSample sample = training_data.get(j);
		    	
	    		for(int k=0; k<layer.get(0).size(); k++) {
	    			
	    			SN node = layer.get(0).get(k);
		    		SNLink link_ptr = node.backward_link;
		    		float input = 0;
		    		
					while(link_ptr != null) {
						input += sample.input_val[link_ptr.dst.node_id] * link_ptr.weight;
						link_ptr = link_ptr.next_ptr;
					}
					
					ind = GLPK.new_intArray(3);
					val = GLPK.new_doubleArray(3);

					GLPK.intArray_setitem(ind, 1, SwitchNodeId(node.node_id, j));
					GLPK.doubleArray_setitem(val, 1, input);
					
					GLPK.intArray_setitem(ind, 2, OutputValID(node.node_id, j));
					GLPK.doubleArray_setitem(val, 2, -1);
			    	
			    	GLPK.glp_add_rows(lp, 1);
				    GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_DB, 0, 0.001);
					GLPK.glp_set_mat_row(lp, curr_row++, 2, ind, val);
	    		}
		    }*/
		    
		    // construct output values as sum of inputs for all other passive layers
		    // sum a_{t-1, a_i}w_j  = c_{i, t}
		    
		    for(int j=0; j<training_data.size(); j++) {
		    	
	    		for(int k=0; k<layer.get(layer.size()-1).size(); k++) {
	    			
	    			SN node = layer.get(layer.size()-1).get(k);
		    		SNLink link_ptr = node.backward_link;
		    		int input_num = 0;
		    		
					while(link_ptr != null) {
						input_num++;
						link_ptr = link_ptr.next_ptr;
					}
					
					ind = GLPK.new_intArray(input_num + 2);
					val = GLPK.new_doubleArray(input_num + 2);
					
					int offset = 1;
					link_ptr = node.backward_link;
					while(link_ptr != null) {
						GLPK.intArray_setitem(ind, offset, SwitchNodeId(link_ptr.dst.node_id, j));
						GLPK.doubleArray_setitem(val, offset++, link_ptr.weight);
						link_ptr = link_ptr.next_ptr;
					}
					
					GLPK.intArray_setitem(ind, offset, OutputValID(node.node_id, j));
					GLPK.doubleArray_setitem(val, offset++, -1);

			    	GLPK.glp_add_rows(lp, 1);
				    GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_DB, 0, 0.000001);
					GLPK.glp_set_mat_row(lp, curr_row++, input_num + 1, ind, val);
	    		}
		    }
		    
		    // construct the absolute value error term from c_i
		    // e_i >= o_i - c_i, e_i >= c_i - o_i
		    for(int j=0; j<training_data.size(); j++) {
		    	TrainingSample sample = training_data.get(j);
		    	
	    		for(int k=0; k<layer.get(layer.size()-1).size(); k++) {
	    			
	    			SN node = layer.get(layer.size()-1).get(k);
					ind = GLPK.new_intArray(3);
					val = GLPK.new_doubleArray(3);

					// e_i >= o_i - c_i
					GLPK.intArray_setitem(ind, 1, ErrorTermID(k, j));
					GLPK.doubleArray_setitem(val, 1, 1);
					
					GLPK.intArray_setitem(ind, 2, OutputValID(node.node_id, j));
					GLPK.doubleArray_setitem(val, 2, 1);
			    	
			    	GLPK.glp_add_rows(lp, 1);
				    GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_LO, sample.output_val[k], 0);
					GLPK.glp_set_mat_row(lp, curr_row++, 2, ind, val);
					

					// e_i >= c_i - o_i
					ind = GLPK.new_intArray(3);
					val = GLPK.new_doubleArray(3);

					GLPK.intArray_setitem(ind, 1, ErrorTermID(k, j));
					GLPK.doubleArray_setitem(val, 1, 1);
					
					GLPK.intArray_setitem(ind, 2, OutputValID(node.node_id, j));
					GLPK.doubleArray_setitem(val, 2, -1);
			    	
			    	GLPK.glp_add_rows(lp, 1);
				    GLPK.glp_set_row_bnds(lp, curr_row, GLPKConstants.GLP_LO, -sample.output_val[k], 0);
					GLPK.glp_set_mat_row(lp, curr_row++, 2, ind, val);
	    		}
		    }
		    
		    
		//  solve model
		    iocp = new glp_iocp();
		    GLPK.glp_init_iocp(iocp);
		    iocp.setPresolve(GLPKConstants.GLP_ON);
		//  GLPK.glp_write_lp(lp, null, "yi.lp");
		    ret = GLPK.glp_intopt(lp, iocp);
		    
		    double val1  = GLPK.glp_mip_obj_val(lp);
		    
			//  Retrieve solution
		    if (ret == 0) {
		    	
		    	CheckResults(lp, layer, training_data);
		    }
		    else {
		      System.out.println("The problemcould not be solved");
		    };
		    
		    // free memory
		    GLPK.glp_delete_prob(lp);
	}

}
