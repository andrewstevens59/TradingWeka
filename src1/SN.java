
public class SN {

	// This stores the input data for the node
	float input_val;
	// This stores the output data for the node
	float output_val;
	// This stores the activation weight 
	float act_weight;
	// This stores the threshold value for activation
	float thresh;
	// This stores the id for the node
	int node_id = 0;
	
	// This stores the set of forward links
	SNLink forward_link = null;
	// This stores the set of backward links
	SNLink backward_link = null;

}
