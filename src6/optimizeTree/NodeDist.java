package optimizeTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import state.State;
import state.StateLink;

public class NodeDist {
	
	// This stores the number of nodes between src and dst
	static public int node_num;
	// This stores the discounted expected reward
	static public float exp_rew;
	// This stores the update id
	static public int update_id = 1;
	// This stores the path id
	public static int path_id = 1;
	
	// This returns the discount factor for the distance between nodes
	public static float DiscountFactor() {
		return PolicyNode.DiscountFactor(node_num + 1);
	}
	
	// This is used to check the discounted reward between two nodes
	private static float CheckDiscountReward(PolicyNode src, PolicyNode dst) {
		
		float util = 0;
		float factor = 1.0f;
		while(src != dst) {
			util += src.s.state_util * factor;
			factor *= PolicyNode.FACTOR;
			src = src.opt_neigh_ptr;
		}
		
		util += dst.s.state_util * factor;
		
		return util;
	}
	
	// This checks if there is a path from the src to the node
	private static boolean IsPath(PolicyNode src, PolicyNode dst) {
		
		while(src != null && src != dst) {
			src.path_id = path_id;
			src = src.opt_neigh_ptr;
		}
		
		dst.path_id = path_id;
		return src == dst;
	}
	
	// This returns the path distance between two points
	private static int PathDist(PolicyNode src, PolicyNode dst) {
		
		int num = 0;
		while(src != dst) {
			num++;
			src = src.opt_neigh_ptr;
		}
		
		return num;
	}
	
	// This checks the solution
	private static float CheckSolution(State s, HashSet<Integer> active_map, HashSet<Integer> visit_map) {
		
		if(active_map.contains(s.state_id) == false) {
			return 0;
		}
		
		if(visit_map.contains(s.state_id) == true) {
			return 0;
		}
		
		visit_map.add(s.state_id);
		float util = (float) s.state_util;
		StateLink link = s.forward_link;
		while(link != null) {
			util += CheckSolution(link.s, active_map, visit_map);
			link = link.next_ptr;
		}
		
		return util;
	}
	
	// This checks the set of path nodes
	public static void CheckPath(ArrayList<PolicyNode> buff) {
		
		HashSet<Integer> active_map = new HashSet<Integer>();
		
		for(int i=0; i<buff.size(); i++) {
			active_map.add(buff.get(i).s.state_id);
		}
		
		HashSet<Integer> visit_map = new HashSet<Integer>();
		CheckSolution(buff.get(buff.size()-1).s, active_map, visit_map);
		
		if(active_map.size() != visit_map.size()) {
			System.out.println("path miss "+active_map.size()+" "+visit_map.size());System.exit(0);
		}
	}
	
	// THis creates the final path
	public static void CreatePath(PolicyNode src, ArrayList<PolicyNode> buff) {
		
		State s = src.s;
		while(s.action_id > 0) {
			buff.add(s.node);
			StateLink link = s.backward_link;
			
			boolean found = false;
			while(link != null) {
				if(link.s.action_id == s.action_id - 1 && link.s.node.update_id == update_id) {
					s = link.s;
					found = true;
					break;
				}
				link = link.next_ptr;
			}
			
			if(found == false) {
				System.out.println("errror");System.exit(0);
			}
		}
		
		buff.add(s.node);
	}

	// This finds and create a path between two states using slow method
	public static boolean CreatePath(PolicyNode comp_node, PolicyNode src, PolicyNode dst,
			ArrayList<PolicyNode> buff, int depth, HashMap<PolicyNode, Boolean> map) {
		
		src.update_id = update_id;
		map.put(src, false);
		if(src == dst) {
			CreatePath(src, buff);
			return true;
		}
		
		if(depth > 40) {
			return false;
		}
		
		ArrayList<PolicyNode> dst_buff = new ArrayList<PolicyNode>();
		SLink forward_link = src.s_links;
		while(forward_link != null) {
			
			if(forward_link.dst == null && dst == null) {
				CreatePath(src, buff);
				return true;
			}
			
			if(src != forward_link.dst && forward_link.dst.IsParent(comp_node) == true) {
				dst_buff.add(forward_link.dst);
			}
			
			forward_link = forward_link.next_ptr;
		}
		
		Collections.sort(dst_buff, new Comparator<PolicyNode>() {
			 
	        public int compare(PolicyNode arg1, PolicyNode arg2) {
	        	
	        	if(arg1.s.state_util < arg2.s.state_util) {
	    			return 1;
	    		}

	    		if(arg1.s.state_util > arg2.s.state_util) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		for(int i=0; i<dst_buff.size(); i++) {
			if(map.get(dst_buff.get(i)) != null) {
				continue;
			}
			
			dst_buff.get(i).s.action_id = src.s.action_id + 1;
			if(CreatePath(comp_node, dst_buff.get(i) , dst, buff, depth + 1, map) == true) {
				return true;
			}
		}
		
		return false;
	}
	
	// This finds the weighted path if one exists between two nodes
	public static boolean FindPath(PolicyNode src, PolicyNode dst) {
		
		//System.out.println("************************** "+src+" "+dst);
		
		update_id++;
		path_id++;
		PolicyNode src_parent = src;
		PolicyNode ptr = src;
		while(ptr != null) {
			ptr.update_id = update_id;
			src_parent = ptr;
			ptr = ptr.parent_ptr;
		}
		
		ptr = dst;
		PolicyNode dst_parent = dst;
		while(ptr != null && ptr.update_id != update_id) {
			ptr.update_id = update_id;
			dst_parent = ptr;
			ptr = ptr.parent_ptr;
		}
		
		if(ptr != null && ptr.update_id == update_id && ptr.is_opt_act_node == true) {
			return DiscountReward(ptr, src, dst);
		}
		
		if(ptr != null) {
			return FindPath(ptr, src, dst);
		}
		
		SLink link_ptr = src_parent.s_links;
		while(link_ptr != null) {
			if(link_ptr.dst.IsParent(dst_parent) == true) {
				boolean src_path = FindPath(src_parent, src, link_ptr.src);
				boolean dst_path = FindPath(dst_parent, link_ptr.dst, dst);
				
				if(src_path == true && dst_path == true) {
					return true;
				}
			}
			link_ptr = link_ptr.next_ptr;
		}
		
		return false;
	}
	
	// This finds the path if one exists 
	private static boolean FindPath(PolicyNode node, PolicyNode src, PolicyNode dst) {
		
		if(src.IsParent(node) == false || dst.IsParent(node) == false) {
			System.out.println("wrong parent");System.exit(0);
		}
		
		//System.out.println("########################### "+node+" "+src+" "+dst);
		//node.PrintTree();
		if(node.is_opt_act_node == true) {
			return DiscountReward(node, src, dst);
		}
		
		HashMap<PolicyNode, Boolean> map = new HashMap<PolicyNode, Boolean>();
		HashMap<PolicyNode, Boolean> src_map = new HashMap<PolicyNode, Boolean>();
		HashMap<PolicyNode, Boolean> dst_map = new HashMap<PolicyNode, Boolean>();
		
		update_id += 2;
		PolicyNode ptr = src;
		while(ptr != node) {
			ptr.update_id = update_id;
			map.put(ptr, false);
			src_map.put(ptr, false);
			ptr = ptr.parent_ptr;
		}
		
		ptr = dst;
		update_id++;
		while(ptr != node) {
			ptr.update_id = update_id;
			map.put(ptr, false);
			dst_map.put(ptr, false);
			ptr = ptr.parent_ptr;
		}
		
		if(map.get(node.left_ptr) == null ^ map.get(node.right_ptr) == null) {
			if(Math.abs(node.left_ptr.update_id - node.right_ptr.update_id) <= 1) {
				System.out.println("same error");System.exit(0);
			}
		}
		
		if(Math.abs(node.left_ptr.update_id - node.right_ptr.update_id) > 1) {
			// go down the same child branch
			if(node.left_ptr.update_id == update_id) {
				
				if(map.get(node.left_ptr) == null) {
					System.out.println("wrong branch1");System.exit(0);
				}
				return FindPath(node.left_ptr, src, dst);
			}
			
			if(node.right_ptr.update_id != update_id) {
				System.out.println("no wrong branch");System.exit(0);
			}
			
			if(map.get(node.right_ptr) == null) {
				System.out.println("wrong branch2");System.exit(0);
			}
			
			return FindPath(node.right_ptr, src, dst);
		}
		
		PolicyNode src_child = null;
		PolicyNode dst_child = null;
		if(node.left_ptr.update_id < node.right_ptr.update_id) {
			src_child = node.left_ptr;
			dst_child = node.right_ptr;
		} else {
			src_child = node.right_ptr;
			dst_child = node.left_ptr;
		}
		
		if(src_map.get(src_child) == null || dst_map.get(dst_child) == null) {
			System.out.println("wrong child");System.exit(0);
		}
		
		SLink link_ptr = src_child.s_links;
		while(link_ptr != null) {
			if(link_ptr.dst.IsParent(dst_child) == true) {
				
				if(link_ptr.src.IsParent(src_child) == false) {
					System.out.println("no parent src");System.exit(0);
				}
				boolean src_path = FindPath(src_child, src, link_ptr.src);
				
				if(link_ptr.dst.IsParent(dst_child) == false || dst.IsParent(dst_child) == false) {
					System.out.println("no parent dst");System.exit(0);
				}
				
				boolean dst_path = FindPath(dst_child, link_ptr.dst, dst);
				
				if(src_path == true && dst_path == true) {
					return true;
				}
			}
			link_ptr = link_ptr.next_ptr;
		}
		
		return false;
	}
	
	// This is used to recover the path once it has been created
	public static boolean RecoverPath(PolicyNode src, PolicyNode dst, ArrayList<PolicyNode> buff, int dist) {
		
		if(dist > 10) {
			return false;
		}
		
		buff.add(src);
		if(src == dst || src == null) {
			return true;
		}
		
		boolean is_found = false;
		SLink s_link = src.s_links;
		while(s_link != null) {
			if(s_link.dst.path_id == path_id) {
				RecoverPath(s_link.dst, dst, buff, dist + 1);
				is_found = true;
			}
			s_link = s_link.next_ptr;
		}
		
		return is_found || src.s_links == null;
	}

	// This calculates the discounted reward between two nodes
	public static boolean DiscountReward(PolicyNode node, PolicyNode src, PolicyNode dst) {
		
		//System.out.println("*************************** "+src+" "+dst+"     "+node.parent_ptr);
		
		/*if(src.IsParent(node) == false || (dst != null && dst.IsParent(node) == false)) {
			System.out.println("no parent");System.exit(0);
		}*/

		//node.PrintTree();
		if(src == dst) {
			exp_rew = (float) src.s.state_util;
			node_num = 0;
			IsPath(src, dst);
			return true;
		}
		
		update_id++;
		PolicyNode parent = node;
		PolicyNode ptr = src;
		while(ptr != node.parent_ptr && ptr != null) {
			ptr.update_id = update_id;
			ptr = ptr.parent_ptr;
		}
		
		//parent.PrintTree();
		exp_rew = parent.discount_rew;
		node_num = parent.node_num;
		int right_num = 0;
		
		while(parent.left_ptr != null || parent.right_ptr != null) {
			if(parent.right_ptr.update_id == update_id) {
				parent = parent.right_ptr;
				continue;
			}
			
			exp_rew -= parent.right_ptr.discount_rew;
			node_num -= parent.right_ptr.node_num;
			right_num += parent.right_ptr.node_num;
			parent = parent.left_ptr;
		}
		
		if(dst == null) {
			IsPath(src, dst);
			return true;
		}
		
		boolean is_path = IsPath(src, dst);
		
		if(dst.update_id == update_id) {
			System.out.println("already assigned");System.exit(0);
		}
		
		update_id++;
		parent = node;
		ptr = dst;
		while(ptr != node.parent_ptr && ptr != null) {
			
			if(ptr.update_id == update_id - 1) {
				// This node has been traversed by the src
				if(ptr.left_ptr.update_id == update_id - 1) {
					if(is_path == true) {
						System.out.println("is path");System.exit(0);
					}
					
					// the dst cannot precede the src
					return false;
				}
			}
			
			ptr.update_id = update_id;
			ptr = ptr.parent_ptr;
		}
		
		while(parent.left_ptr != null || parent.right_ptr != null) {
			if(parent.left_ptr.update_id == update_id) {
				parent = parent.left_ptr;
				continue;
			}
			
			node_num -= parent.left_ptr.node_num;
			exp_rew -= parent.left_ptr.discount_rew;
			parent = parent.right_ptr;
		}
		
		if(right_num > 0) {	
			exp_rew /= PolicyNode.DiscountFactor(right_num);
		}
		
		node_num--;
		/*if(node_num != PathDist(src, dst)) {
			System.out.println("path dist error "+node_num+" "+PathDist(src, dst));System.exit(0);
		}*/
		
		/*if(Math.abs(exp_rew - CheckDiscountReward(src, dst)) > 1) {
			System.out.println("mis rew "+exp_rew+" "+CheckDiscountReward(src, dst)+"    "+right_num);
			
			while(src != dst) {
				System.out.println(src.s.state_util);
				src = src.opt_neigh_ptr;
			}
			
			System.out.println(src.s.state_util);
			
			
			System.exit(0);
		}*/
		
		IsPath(src, dst);
		return true;
	}

}
