package optimizeTree;

import java.util.ArrayList;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;


import state.State;
import state.StateLink;
import stateMachine.RandomField;

public class PolicyNode {

	// This stores the state for the node
	public State s;
	// This stores the optimal neighbour node
	public PolicyNode opt_neigh_ptr = null;
	// This stores the optimal back ptr neighbour
	private PolicyNode opt_back_ptr = null;
	// This stores the total number of leaf nodes
	public int node_num = 0;
	// This stores the discounted return
	public float discount_rew = 0;
	// This stores the discount factor
	private float discount_factor = 1.0f;

	
	// This stores the parent ptr
	public PolicyNode parent_ptr = null;
	// This stores the left ptr
	public PolicyNode left_ptr = null;
	// This stores the right ptr
	public PolicyNode right_ptr = null;
	// This stores the update id
	public int update_id = -1;
	// This stores the path id used to recover the path
	public int path_id = 0;
		
	// This indicates whether the node is the 
	// root in the compression hierarchy
	public boolean is_node_set = false;
	// This stores the set of s-links
	public SLink s_links = null;
	// This is a predicate indicate an optimal action node
	// or a best neighbour node
	public boolean is_opt_act_node = true;
	
	// This stores the previous node in the LRU queue
	private PolicyNode prev_ptr = null;
	// This stores the next node in the LRU queue
	private PolicyNode next_ptr = null;
	// This stores the head LRU queue
	private static PolicyNode head_ptr = null;
	// This stores the tail LRU queue
	private static PolicyNode tail_ptr = null;
	// This stores the total number of root nodes
	private static int root_node_num = 0;
	// This stores the number of s-links
	private int s_link_num = 0;
	
	public double x;
	public double y;
	
	// This stores the set of decompressed nodes
	static ArrayList<PolicyNode> decomp_buff = new ArrayList<PolicyNode>();
	// This stores the set of discounted gamma factors
	static ArrayList<Float> discount_fact_buff = new ArrayList<Float>();
	// This stores the mapping between a state link and the set of corresponding s-links
	static HashMap<StateLink, SLink> s_link_map = new HashMap<StateLink, SLink>();
	// This stores the discount factor
	static public float FACTOR = 0.95f;
	// This is used to determine when links are subsumed or not
	static private int curr_update_id = 2;
	// This stores the maximum s-liink number
	static private int max_s_link_num = 30;
	
	static public HashSet<PolicyNode> root_map = new HashSet<PolicyNode>();
	
	public int comp_id = 0;
	private static int comp_id_num = 0;

	public PolicyNode(State s) {
		this.s = s;
		
		if(s != null) {
			node_num = 1;
		}
		
		AddRootNode();
		
		comp_id = comp_id_num++;
	}
	
	// This adds a root node to the set
	public void AddRootNode() {
		
		root_map.add(this);
		root_node_num++;
		if(head_ptr == null) {
			head_ptr = this;
			tail_ptr = this;
			return;
		}
		
		PolicyNode prev_ptr = head_ptr;
		head_ptr = this;
		head_ptr.next_ptr = prev_ptr;
		prev_ptr.prev_ptr = head_ptr;
	}
	
	// This sets the maximum s-link number
	public static void SetMaxSLinkNum(int s_link_num) {
		max_s_link_num = s_link_num;
	}
	
	// This removes this node from the LRU queue
	private void RemoveNodeFromLRU() {
		
		root_map.remove(this);
		if(prev_ptr == null && next_ptr == null && head_ptr != this) {
			return;
		}
		
		if(prev_ptr != null) {
			prev_ptr.next_ptr = this.next_ptr;
		}
		
		if(next_ptr != null) {
			next_ptr.prev_ptr = this.prev_ptr;
		}
		
		if(tail_ptr == this) {
			tail_ptr = prev_ptr;
		}
		
		if(head_ptr == this) {
			head_ptr = next_ptr;
		}
		
		if(tail_ptr == null && head_ptr != null) {
			//System.out.println("error");System.exit(0);
		}
		
		if(tail_ptr.next_ptr != null) {
			//System.out.println("Wrong Tail");System.exit(0);
		}
	
		prev_ptr = null;
		next_ptr = null;
		root_node_num--;
	}
	
	// This assigns the optimal neighbour node
	private boolean AssignOptNeigbour(PolicyNode max_neigh) {
		
		if(opt_neigh_ptr == null) {
			// has not yet been assigned a neighbour
			return true;
		}

		if(max_neigh == opt_neigh_ptr) {
			// nothing changes
			return false;
		}
		
		// remove all the s-links in the parent
		PolicyNode ptr = parent_ptr;
		while(ptr != null) {
			
			SLink temp_ptr = null;
			SLink s_link = ptr.s_links;
			while(s_link != null) {
				SLink next_ptr = s_link.next_ptr;
				if(s_link.src != this) {
					SLink prev_ptr = temp_ptr;
					temp_ptr = s_link;
					temp_ptr.next_ptr = prev_ptr;
				}
				s_link = next_ptr;
			}
			
			ptr.s_links = temp_ptr;
			ptr = ptr.parent_ptr;
		}
		
		// removes the node from the current position
		if(parent_ptr.left_ptr == this) {
			parent_ptr.left_ptr = null;
		} else {
			parent_ptr.right_ptr = null;
		}

		parent_ptr.CleanNode();
		parent_ptr = null;
		
		return true;
	} 
	
	// This removes a node from the hierarchy
	private void CleanNode() {
		
		if(left_ptr != null && right_ptr != null) {
			return;
		}
		
		PolicyNode node = null;
		if(left_ptr != null) {
			node = left_ptr;
		} else {
			node = right_ptr;
		}
		
		if(parent_ptr.left_ptr == this) {
			parent_ptr.left_ptr = node;
		} else {
			parent_ptr.right_ptr = node;
		}
		
		node.parent_ptr = parent_ptr;
	}
	
	// This returns the number of root nodes
	public static int RootNodeNum() {
		return root_node_num;
	}
	
	// This updates the nodes in the hierarchy with the current update id
	private void AssignUpdateID(int id) {
		update_id = id;
		if(parent_ptr != null) {
			parent_ptr.AssignUpdateID(id);
		}
	}
	
	// This propagates node statistics up to the parent
	private void PropagateNodeState() {
		
		node_num = left_ptr.node_num + right_ptr.node_num;
		discount_rew = left_ptr.discount_rew + right_ptr.discount_rew;
		
		if(parent_ptr != null) {
			parent_ptr.PropagateNodeState();
		}
	}
	
	// This returns the number of children
	public int ChildNum() {
		if(left_ptr != null) {
			return left_ptr.ChildNum() + right_ptr.ChildNum();
		}
		
		return 1;
	}

	// This merges the node with a neighbour
	public void MergeNode(PolicyNode neighbour) {
		
		if(this == neighbour) {
			System.out.println("same1");System.exit(0);
		}
		
		if(ChildNum() > RandomField.NodeNum() * 0.2) {
			//return;
		}
		
		if(neighbour.parent_ptr != null) {
			// removes the node from the current location in the hierarchy
			PolicyNode parent = neighbour.parent_ptr;
			neighbour.parent_ptr = null;
			neighbour.RemoveNodeFromLRU();
			
			PolicyNode other = null;
			if(parent.left_ptr == neighbour) {
				other = parent.right_ptr;
				parent.right_ptr = null;
			} else {
				other = parent.left_ptr;
				parent.left_ptr = null;
			}
			
			other.parent_ptr = parent.parent_ptr;
			
			if(other.parent_ptr == other) {
				System.out.println("same");System.exit(0);
			}
			
			if(other.parent_ptr == null) {
				other.AddRootNode();
				
				if(other.left_ptr == null ^ other.right_ptr == null) {
					System.out.println("bo");System.exit(0);
				}
			} else {
				other.RemoveNodeFromLRU();
				
				if(other.parent_ptr.left_ptr == parent) {
					other.parent_ptr.left_ptr = other;
				} else if(other.parent_ptr.right_ptr == parent) {
					other.parent_ptr.right_ptr = other;
				} else {
					System.out.println("error "+other.parent_ptr.left_ptr+" "+other.parent_ptr.right_ptr);System.exit(0);
				}
				
				if(other.parent_ptr.left_ptr == null || other.parent_ptr.right_ptr == null) {
					System.out.println("bo");System.exit(0);
				}
			}
			
			parent.RemoveNodeFromLRU();
		} 
		
		this.RemoveNodeFromLRU();
		neighbour.RemoveNodeFromLRU();

		PolicyNode root = new PolicyNode(null);
		PolicyNode prev_parent = this.parent_ptr;
		
		if(prev_parent != null) {
			if(prev_parent.left_ptr == this) {
				prev_parent.left_ptr = root;
			} else {
				prev_parent.right_ptr = root;
			}
		}
		
		root.parent_ptr = prev_parent;
		
		if(root.parent_ptr == root) {
			System.out.println("same");System.exit(0);
		}
		
		if(root.parent_ptr != null) {
			root.RemoveNodeFromLRU();
		}
		
		root.left_ptr = this;
		root.right_ptr = neighbour;
		root.left_ptr.parent_ptr = root;
		root.right_ptr.parent_ptr = root;
		
		if(root.left_ptr.parent_ptr == root.left_ptr || root.right_ptr.parent_ptr == root.right_ptr) {
			System.out.println("same");System.exit(0);
		}
		
		if(root.left_ptr == null || root.right_ptr == null) {
			System.out.println("bo");System.exit(0);
		}
	}
	
	// This returns the discount factor for a particular path distance
	public static float DiscountFactor(int path_size) {
		
		float factor = discount_fact_buff.get(discount_fact_buff.size() - 1);
		for(int i=discount_fact_buff.size(); i<path_size; i++) {
			factor *= FACTOR;
			discount_fact_buff.add(factor);
		}
		
		return discount_fact_buff.get(path_size - 1);
	}
	
	
	// This decompresses the node
	public boolean DecompressNode(int depth) {
		
		decomp_buff.add(this);
		if(depth == 0) {
			return true;
		}
		
		if(left_ptr == null && right_ptr == null) {
			return false;
		}

		is_node_set = false;
		RemoveNodeFromLRU();
		left_ptr.SetDecompressed();
		right_ptr.SetDecompressed();
		left_ptr.AddRootNode();
		right_ptr.AddRootNode();
		root_node_num++;
		
		left_ptr.DecompressNode(depth - 1);
		right_ptr.DecompressNode(depth - 1);
		
		return true;
	}
	
	// 	This is used to reset the tree
	public static void ResetTree() {
		
		root_map.clear();
		for(int i=0; i<decomp_buff.size(); i++) {
			PolicyNode ptr = decomp_buff.get(i);
			while(ptr != null) {
				ptr.is_node_set = false;
				
				if(ptr.parent_ptr == null) {
					root_map.add(ptr);
				}
				ptr = ptr.parent_ptr;
			}
		}
		
		decomp_buff.clear();

		root_node_num = 0;	
		PolicyNode ptr = head_ptr;
		while(ptr != null) {
			root_node_num++;
			ptr = ptr.next_ptr;
		}
	}
	
	// This returns the set of root nodes
	public static void RootNodes(ArrayList<PolicyNode> buff) {
		for(PolicyNode ptr : root_map) {
			buff.add(ptr);
		}
	}
	
	// This sets the node as decompressed
	public void SetDecompressed() {
		decomp_buff.add(this);
		is_node_set = true;
	}
	
	// This returns true if this node is a leaf node
	public boolean IsLeafNode() {
		return left_ptr == null && right_ptr == null;
	}
	
	// This finds the appropriate compression node for a state
	public PolicyNode CompNode() {
		
		PolicyNode ptr = this;
		while(ptr.parent_ptr != null && ptr.is_node_set == false) {
			ptr = ptr.parent_ptr;
		}
		
		return ptr;
	}
	
	// This prints the tree for a compression node
	public void PrintTree() {
		System.out.print(this+" "+node_num+" "+discount_rew+" "+left_ptr+" "+right_ptr+"  ");
		
		if(left_ptr == null && right_ptr == null) {
			System.out.println("Neighbour: "+opt_neigh_ptr);
		} else {
			System.out.println("");
			
			right_ptr.PrintTree();
			left_ptr.PrintTree();
		}
	}
	
	// This embeds an s-link in the hierarchy
	public void EmbedSLink(StateLink s_link) {
		
		PolicyNode ptr = s_link.src.node;
		while(ptr != null) {
			
			if(s_link.s.node.IsParent(ptr) == true) {
				// cannot merge in the same node
				break;
			}
			
			if((ptr.s_link_num < max_s_link_num)) {
				
				SLink s = new SLink();
				s.src = s_link.src.node;
				s.dst = s_link.s.node;
				
				SLink prev_ptr = ptr.s_links;
				ptr.s_links = s;
				s.next_ptr = prev_ptr;
				ptr.s_link_num++;
			}
			
			ptr = ptr.parent_ptr;
		}
	}
	
	// This checks if a node has a particular parent
	public boolean IsParent(PolicyNode parent) {
		
		PolicyNode ptr = this;
		while(ptr != null) {
			if(ptr == parent) {
				return true;
			}
			ptr = ptr.parent_ptr;
		}
		
		return false;
	}
	
	// This returns the set of root nodes
	public boolean RootNodesGrouped(ArrayList<PolicyNode> buff, int depth) {
		
		if(depth == 0) {
			buff.add(this);
			return false;
		}
		
		if(left_ptr == null && right_ptr == null) {
			buff.add(this);
			return false;
		}

		left_ptr.RootNodesGrouped(buff, depth - 1);
		right_ptr.RootNodesGrouped(buff, depth - 1);
		
		return true;
	}
	
	// This returns the set of leaf nodes
	public void LeafNodes(ArrayList<PolicyNode> buff) {
		if(left_ptr == null && right_ptr == null) {
			buff.add(this);
		}
		
		if(left_ptr != null) {
			left_ptr.LeafNodes(buff);
		}
		
		if(right_ptr != null) {
			right_ptr.LeafNodes(buff);
		}
	}
	
	// This merges the closest two nodes in space
	public static void MergeClosestNodes() {
		
		ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
		for(PolicyNode n : root_map) {
			buff.add(n);
		}
		
		PolicyNode b_n1 = null;
		PolicyNode b_n2 = null;
		double min_dist = Double.MAX_VALUE;
		for(int i=0; i<buff.size(); i++) {
			for(int j=0; j<buff.size(); j++) {
				if(i == j) {
					continue;
				}
				
				PolicyNode n1 = buff.get(i);
				PolicyNode n2 = buff.get(j);
				double dist = (n1.x - n2.x) * (n1.x - n2.x);
				dist += (n1.y - n2.y) * (n1.y - n2.y);
				
				if(dist < min_dist) {
					min_dist = dist;
					b_n1 = n1;
					b_n2 = n2;
				}
			}
		}
		
		b_n1.RemoveNodeFromLRU();
		b_n2.RemoveNodeFromLRU();

		PolicyNode root = new PolicyNode(null);
		b_n1.parent_ptr = root;
		b_n2.parent_ptr = root;
		root.left_ptr = b_n1;
		root.right_ptr = b_n2;
		
		ArrayList<PolicyNode> temp = new ArrayList<PolicyNode>();
		root.RootNodesGrouped(temp, 5);
		root.x = 0;
		root.y = 0;
		
		for(PolicyNode n : temp) {
			root.x += n.x;
			root.y += n.y;
		}
		
		root.x /= temp.size();
		root.y /= temp.size();
		
	}

}
