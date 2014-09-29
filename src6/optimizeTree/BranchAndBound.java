package optimizeTree;

import gurobi.GRBException;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;

import state.State;
import state.StateLink;
import stateMachine.RandomField;


public class BranchAndBound {
	
	// This stores the set of back links
	class SBackLink {
		// This stores the link id
		SRootLink link_ptr;
		SBackLink next_ptr;
	}
	
	// This stores the dynamic programming score for each individual node
	class SDynamProgExpRew {
		// This stores a ptr to the set of edges
		SRootLink link_set;
		// This stores the src node
		PolicyNode src;
		
		public SDynamProgExpRew(SRootLink link_set, PolicyNode src) {
			this.link_set = link_set;
			this.src = src;
		}
	}
	
	// This stores the myopic optimal value
	public static float myopic_val;
	// This stores the cg optimal value
	public static float cg_val;
	// This stores the myopic optimal value
	public static long myopic_time;
		// This stores the cg optimal value
	public static long cg_time;
	// This stores the associated path length
	private static int path_length = 20;
	
	// This stores the set of node stats
	private static ArrayList<DecompState> decomp_stat_buff = new ArrayList<DecompState>();
	// This is used to store the outgoing edges for an incoming node
	private HashMap<PolicyNode, SRootLink> link_map = new HashMap<PolicyNode, SRootLink>();
	// This is used to store the outgoing edges for an incoming node
	private HashMap<PolicyNode, SBackLink> back_link_map = new HashMap<PolicyNode, SBackLink>();
	// This stores the set of dynamic programming exp rewards
	private ArrayList<SDynamProgExpRew> exp_rew_buff = new ArrayList<SDynamProgExpRew>();
	// This stores the optimal value for each node at a given time period
	private HashMap<String, Float> opt_pol_map = new HashMap<String, Float>();
	// This stores the link buffer
	private ArrayList<SRootLink> link_buff = new ArrayList<SRootLink>();
	// This stores the set of path links for each state
	private HashMap<PolicyNode, SPathLink> path_link_map = new HashMap<PolicyNode, SPathLink>();
	// This stores the current link id
	private int curr_link_id = 1;
	// This stores the path distance 
	static int path_dist = 30;
	
	// This sets the path length
	public static void SetPathLength(int len) {
		path_length = len;
	}
	
	// This performs a depth first search from the root node
	private void DepthFirstSearch(PolicyNode dst, PolicyNode comp_node, int depth) {
		
		if(depth == 0) {
			return;
		}
		
		SRootLink link_ptr = link_map.get(dst);
		if(link_ptr != null) {
			// this node combination has already been mapped
			return;
		}
		
		link_map.put(dst, new SRootLink());
		
		SLink forward_link = comp_node.s_links;
		while(forward_link != null) {
			
			ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
			
			if(forward_link.src != dst) {
				buff.add(forward_link.src);
			}
			
			buff.add(dst);
			NodeDist.update_id++;
			dst.s.action_id = 0;

			// check there is an edge between these two nodes
			SRootLink prev_ptr = link_ptr;
			link_ptr = new SRootLink();
			link_ptr.dst = forward_link.dst;
			link_ptr.comp_node = forward_link.dst.CompNode();
			link_ptr.state_util = NodeDist.exp_rew;
			link_ptr.next_ptr = prev_ptr;
			link_ptr.path_dist = NodeDist.node_num + 1; 
			link_ptr.link_id = curr_link_id++;
			link_ptr.path = buff;
			
			if(buff.get(buff.size()-1) != dst) {
				System.out.println("ko");System.exit(0);
			}
			
			link_buff.add(link_ptr);
			
			SPathLink prev_ptr1 = path_link_map.get(dst);
			SPathLink ptr = new SPathLink();
			ptr.link_ptr = link_ptr;
			ptr.next_ptr = prev_ptr1;
			path_link_map.put(dst, ptr);
			
			SBackLink prev_ptr2 = back_link_map.get(link_ptr.dst);
			SBackLink next_ptr = new SBackLink();
			next_ptr.next_ptr = prev_ptr2;
			next_ptr.link_ptr = link_ptr;
			back_link_map.put(link_ptr.dst, next_ptr);
		
			DepthFirstSearch(link_ptr.dst, link_ptr.comp_node, depth - 1);

			forward_link = forward_link.next_ptr;
		}
		
		if(link_ptr == null || comp_node.s_links == null) {

			comp_node.DecompressNode(1);
			NodeDist.update_id++;
			dst.s.action_id = 0;
			ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
			if(NodeDist.CreatePath(comp_node, dst, null, buff, 0, new HashMap<PolicyNode, Boolean>()) == false) {
				buff.add(dst);
			}

			//NodeDist.CheckPath(buff);
			
			link_ptr = new SRootLink(); 
			link_ptr.dst = null;
			link_ptr.comp_node = null;
			link_ptr.state_util = NodeDist.exp_rew;
			link_ptr.factor = 1.0f;
			link_ptr.path_dist = NodeDist.node_num + 1; 
			link_ptr.link_id = curr_link_id++;
			link_ptr.path = buff;
			
			link_buff.add(link_ptr);
			
			SBackLink prev_ptr1 = back_link_map.get(link_ptr.dst);
			SBackLink next_ptr = new SBackLink();
			next_ptr.next_ptr = prev_ptr1;
			next_ptr.link_ptr = link_ptr;
			back_link_map.put(link_ptr.dst, next_ptr);	
		}
		
		link_ptr.exp_rew_id = exp_rew_buff.size();
		exp_rew_buff.add(new SDynamProgExpRew(link_ptr, dst));
		link_map.put(dst, link_ptr);
	}
	
	// This finds the optimal policy for the set of root compression nodes 
	public float OptimalPolicy(State s, ArrayList<PolicyNode> buff, RandomField r) {
		
		path_link_map.put(s.node, null);
		DepthFirstSearch(s.node, s.node.CompNode(), 30);
		
		System.out.println(path_link_map.size()+"  ^^^^^");
		ArrayList<Float> buff1 = new ArrayList<Float>();
		for(int i=0; i<r.StateSet().size(); i++) {
			buff1.add((float)r.StateSet().get(i).state_util);
		}

		float val1 = 0;
		OrienteeringProblem op = new OrienteeringProblem(buff1);
			
		try {
			
			val1 = op.SolveByIP1(s.node, path_link_map, RandomField.MaxPathLength(), buff);
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//op.CheckSolution(s);
		
		DecompState stat = new DecompState();
		stat.edge_rat = op.EdgeRatio();
		stat.node_rat = op.NodeRatio();
		decomp_stat_buff.add(stat);
		
		if(val1 != 0) {
			return val1;
		}
		
		float val3 = op.OptimalPath(path_length, s.node, null, path_link_map);
		
		int visit[] = op.VisitStates();
		
		float util = 0;
		int offset = 0;
		HashSet<Integer> visit_map = new HashSet<Integer>();
		for(int i=0; i<visit.length; i++) {
			int val = visit[i];
			for(int j=0; j<32; j++) {
				
				if((val & 0x01) == 0x01) {
					buff.add(r.StateSet().get(offset).node);
					util += r.StateSet().get(offset).state_util;
					visit_map.add(r.StateSet().get(offset).state_id);
				}
				
				val >>= 1;
				offset++;
				if(offset >= r.StateSet().size()) {
					break;
				}
			}
			
			if(offset >= r.StateSet().size()) {
				break;
			}
		}

		return util;
	}
	
	
	// This finds the optimal path length
	public static int PathLength() {
		return path_dist;
	}
	
	// This returns the percentage increase information
	public static ArrayList<DecompState> DecompBuff() {
		return decomp_stat_buff;
	}
	
	
	// This is used to find the optimal policy under a graph compression 
	public static float OptimalPolicyGC(State s, int exp_num, RandomField r) {
		
		PolicyNode.ResetTree();

		//while(RandomField.RootState().node.CompNode().DecompressNode(1) == true);
		
		decomp_stat_buff.clear();
		long prev_time = System.currentTimeMillis();
		ArrayList<Float> util_buff = new ArrayList<Float>();
		ArrayList<Integer> root_num_buff = new ArrayList<Integer>();
		
		float util1 = 0;
		for(int j=0; j<exp_num; j++) {
			
			long prev_time1 = System.currentTimeMillis();
			ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
			BranchAndBound bb = new BranchAndBound();
			float val = bb.OptimalPolicy(s, buff, r);
			
			if(val > util1) {
				path_dist = buff.size();
				util1 = val;
			}
			
			decomp_stat_buff.get(decomp_stat_buff.size()-1).obj = util1;
			decomp_stat_buff.get(decomp_stat_buff.size()-1).time = System.currentTimeMillis() - prev_time1;
			
			int decomp_num = 0;
			util_buff.add(util1);
			root_num_buff.add(PolicyNode.RootNodeNum());
			for(int i=0; i<buff.size(); i++) {
				if(buff.get(i).CompNode().DecompressNode(1)) {
					decomp_num++;
				}
			}
			
			if(decomp_num == 0) {
				System.out.println("no more");System.exit(0);
				break;
			}
		} 
		
		cg_time = (System.currentTimeMillis() - prev_time) / exp_num;
		
		for(int i=0; i<util_buff.size(); i++) {
			System.out.println(util_buff.get(i)+" "+root_num_buff.get(i));
		}
		
		cg_val = util1;

		prev_time = System.currentTimeMillis();
		ArrayList<PolicyNode> myopic_buff = new ArrayList<PolicyNode>();
		float util2 = OptimalPolicyMyopic(s, path_dist, myopic_buff);
		myopic_time = System.currentTimeMillis() - prev_time;

		return util1 / util2;
	}
	
	// This finds the optimal policy using a myopic expansion
	public static float OptimalPolicyMyopic(State s, int depth, ArrayList<PolicyNode> buff) {
		
		float opt_val = 0;
		float factor = 1.0f;
		HashMap<State, Boolean> visit_map = new HashMap<State, Boolean>();
		
		System.out.println(depth);
		for(int i=0; i<depth; i++) {
			
			buff.add(s.node);
			if(visit_map.get(s) == null) {
				opt_val += s.state_util * factor;
				//factor *= 0.95f;
				visit_map.put(s, false);
			}
			
			float max = -999999999;
			StateLink link_ptr = s.forward_link;
			
			s = null;
			while(link_ptr != null) {
				float util = (float) link_ptr.s.state_util;
				if(visit_map.get(link_ptr.s) != null) {
					util = 0;
				}
				
				if(util > max || max == -999999999) {
					max = util;
					s = link_ptr.s;
				}
				link_ptr = link_ptr.next_ptr;
			}
			
			if(s == null) {
				break;
			}
		}
		
		myopic_val = opt_val;
		
		return opt_val;
	}

}
