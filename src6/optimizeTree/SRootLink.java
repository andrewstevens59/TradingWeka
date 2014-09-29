package optimizeTree;

import java.util.ArrayList;

//This is used to store one of the root level links
class SRootLink {
	// This stores the dst node
	PolicyNode dst;
	// This stores the compression node
	PolicyNode comp_node;
	// This stores the state utility 
	float state_util;
	// This stores the discount factor for the link
	float factor;
	// This stores the path distance
	double path_dist;
	// This stores the next root link
	SRootLink next_ptr = null;
	// This stores the exp rew id
	int exp_rew_id;
	// This stores the link id
	int link_id;
	// This stores the src for the link
	PolicyNode src;
	// This stores the same cluster connecting node
	PolicyNode int_node;
	// This stores the individual nodes that make up the link
	ArrayList<PolicyNode> path;
}
