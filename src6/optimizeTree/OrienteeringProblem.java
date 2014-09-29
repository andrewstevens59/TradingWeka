package optimizeTree;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import state.State;
import state.StateLink;
import stateMachine.RandomField;

public class OrienteeringProblem {
	
	// This stores the utility for a given state and the current visited states
	class Node {
		// This stores the set of previous states;
		int history[] = new int[100];
		// This stores the sequence of steps
		State steps[] = new State[30];
		// This stores the current step
		int step = 0;
		// This stores the current state
		State s;
		// This stores the time step
		int time_step;
		// This stores the utility
		float util;
		
		public Node() {
			
			for(int i=0; i<history.length; i++) {
				history[i] = 0;
			}
		}
		
		// This converts the information to a string
		public String toString() {
			String str = new String();
			for(int i=0; i<history.length; i++) {
				str += Integer.toString(history[i])+" ";
			}
			return str + " " + Integer.toString(time_step) + " " + s;
		}
		
		// This sets the value of a node as visited
		public boolean SetNode(int id) {
			
			if((history[id / 32] & 1 << (id % 32)) == (1 << (id % 32))) {
				return false;
			}
			
			history[id / 32] |= 1 << (id % 32);
			return false;
		}
	}
	
	// This stores a back link
	class BackLink {
		PolicyNode n;
		PolicyNode src;
		BackLink next_ptr;
		double dist;
		double util;
		GRBVar var;
	}
	
	// This stores the set of visited nodes
	private HashSet<PolicyNode> node_map = new HashSet<PolicyNode>();
	// This stores the set of visited nodes
	private HashMap<PolicyNode, Integer> node_visit = new HashMap<PolicyNode, Integer>();
	// This stores the time the node was first visited
	private HashMap<PolicyNode, Integer> visit_time = new HashMap<PolicyNode, Integer>();

		
	// This stores the set of back links
	private HashMap<PolicyNode, BackLink> back_node_map = new HashMap<PolicyNode, BackLink>();
	// This stores the set of states and the utility
	private HashMap<String, Node> state_map = new HashMap<String, Node>();
	// This stores the set of state utilities
	private ArrayList<Float> state_util;
	// This stores the best state
	private Node best_n = new Node();
	
	// This stores the edge ratio for the fraction of edges in compressed problem
	private float edge_rat;
	// This stores the node ratio for the fraction of nodes in compressed problem
	private float node_rat;
	
	// This defines the maximum number of time steps
	private static final int MAX_TIME_STEPS = 16;
	
	private HashSet<String> duplicate_map = new HashSet<String>();

	public OrienteeringProblem(ArrayList<Float> state_util) {
		this.state_util = state_util;
	}
	
	// This adds a back link
	private double AddBackLink(PolicyNode src, PolicyNode dst, int time, int path_len) {
		
		if(time + 1 >= path_len + 1) {
			return 0;
		}
		
		String str = src +" " + dst;
		if(duplicate_map.contains(str) == true) {
			return 0;
		}
		
		duplicate_map.add(str);
		node_map.add(dst);
		node_map.add(src);
		
		Integer t = visit_time.get(src);
		if(t == null) {
			t = time;
		}
		
		t = Math.min(t, time);
		visit_time.put(src,  t);
		
		
		t = visit_time.get(dst);
		if(t == null) {
			t = time + 1;
		}
		
		t = Math.min(t, time + 1);
		visit_time.put(dst,  t);
		
		BackLink prev_ptr = back_node_map.get(dst);
		BackLink link = new BackLink();
		link.n = src;
		link.next_ptr = prev_ptr;
		link.dist = (dst.s.x - src.s.x) * (dst.s.x - src.s.x);
		link.dist += (dst.s.y - src.s.y) * (dst.s.y - src.s.y);
		link.dist = Math.sqrt(link.dist);
		link.util = dst.s.state_util;
		link.src = dst;
		back_node_map.put(dst, link);
		
		return link.dist;
	}
	
	// This loads the set of back links for the graph
	private void LoadBackLinks(PolicyNode s, HashMap<PolicyNode, SPathLink> path_link_map, int depth, int path_len) {
		
		if(node_visit.get(s) != null) {
			return;
		}
		
		if(depth > path_len) {
			return;
		}
		
		node_visit.put(s, depth);
		node_map.add(s);
		
		Integer t = visit_time.get(s);
		if(t == null) {
			t = depth;
		}
		
		t = Math.min(t, depth);
		visit_time.put(s,  t);
		
		SPathLink link_ptr = path_link_map.get(s);
    	while(link_ptr != null) {
    		
    		int offset = 0;
    		for(int i=link_ptr.link_ptr.path.size()-2; i>=0; i--) {
    			offset += AddBackLink(link_ptr.link_ptr.path.get(i+1), link_ptr.link_ptr.path.get(i), depth + offset, path_len);
    		}
    		
    		offset += AddBackLink(link_ptr.link_ptr.path.get(0), link_ptr.link_ptr.dst, depth + offset, path_len);
    		
    		if(link_ptr.link_ptr.path.get(link_ptr.link_ptr.path.size()-1) != s) {
    			System.out.println("Not Src");System.exit(0);
    		}
    		
    		LoadBackLinks(link_ptr.link_ptr.dst, path_link_map, depth + offset, path_len);
    		link_ptr = link_ptr.next_ptr;
    	}
		
	}
	
	// This checks the best solution for correctness
	public float CheckSolution(State s) {
		
		float util = 0;
		HashSet<State> map = new HashSet<State>();
		for(int i=0; i<best_n.step; i++) {
			
			System.out.print(best_n.steps[i].state_id+" ");
			if(best_n.steps[i] != s) {
				System.out.println("error "+i+" "+best_n.steps[i]+" "+s);System.exit(0);
			}
			
			if(map.contains(best_n.steps[i]) == false) {
				util += best_n.steps[i].state_util;
			}
			
			map.add(best_n.steps[i]);
			
			if(i < best_n.step - 1) {
				
				boolean found = false;
				StateLink link = s.forward_link;
				while(link != null) {
					if(best_n.steps[i+1] == link.s) {
						s = link.s;
						found = true;
						break;
					}
					link = link.next_ptr;
				}
				
				if(found == false) {
					System.out.println("no neighbour");System.exit(0);
				}
			}
		}
		
		System.out.println("     "+util);
		return util;
	}
	
	// This returns the edge ratio
	public float EdgeRatio() {
		return edge_rat;
	}
	
	// This returns the node ratio
	public float NodeRatio() {
		return node_rat;
	}
	
	// This creates the set of links to the terminal states
	private void CreateTerminalEdges(ArrayList<State> terminal_buff) {
		
		for(int i=0; i<terminal_buff.size(); i++) {
			PolicyNode t = terminal_buff.get(i).node;
			
			for(PolicyNode n : node_map) {
				if(t == n) {
					continue;
				}
				
				BackLink prev_ptr = back_node_map.get(t);
				BackLink link = new BackLink();
				link.n = n;
				link.next_ptr = prev_ptr;
				link.dist = (n.s.x - t.s.x) * (n.s.x - t.s.x);
				link.dist += (n.s.y - t.s.y) * (n.s.y - t.s.y);
				link.dist = Math.sqrt(link.dist);
				link.util = t.s.state_util;
				link.src = t;
				back_node_map.put(t, link);
			}
		}
		
		for(int i=0; i<terminal_buff.size(); i++) {
			PolicyNode t = terminal_buff.get(i).node;
			node_map.add(t);
		}
	}
	
	
	// This solves the orienteering problem using IP
	public float SolveByIP1(PolicyNode s, HashMap<PolicyNode, SPathLink> path_link_map, 
			int path_len, ArrayList<PolicyNode> buff) throws GRBException {
		
		LoadBackLinks(s, path_link_map, 0, path_len);
		CreateTerminalEdges(RandomField.TerminalStates());
		
		GRBEnv    env   = new GRBEnv("mip1.log");
	    GRBModel  model = new GRBModel(env);
	    HashMap<PolicyNode, GRBVar[]> var_map = new HashMap<PolicyNode, GRBVar[]>();
	    HashMap<PolicyNode, GRBVar> visit_map = new HashMap<PolicyNode, GRBVar>();
	    ArrayList<BackLink> edge_var_buff = new ArrayList<BackLink>();
	    
	    int count1 = 0;
		for(PolicyNode n : node_map) {
			
			BackLink link = back_node_map.get(n);
			while(link != null) {
				link.var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, link.toString());
				edge_var_buff.add(link);
				link = link.next_ptr;
			}
			
			if(back_node_map.get(n) == null && n != s) {
				continue;
			}

			GRBVar set[] = new GRBVar[MAX_TIME_STEPS];
			for(int i=0; i<set.length; i++) {
				set[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, n.toString());
				count1++;
			}

		    var_map.put(n, set);
		    
		    GRBVar var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, n.toString());
		    visit_map.put(n, var);
		}
		
		int edge_num = 0;
		for(PolicyNode n : visit_map.keySet()) {
			BackLink link = back_node_map.get(n);
			while(link != null) {
				edge_num++;
				link = link.next_ptr;
			}
		}
		
		edge_rat = (float)edge_num / RandomField.EdgeNum();
		node_rat = (float)visit_map.size() / RandomField.NodeNum();
		
		model.update();
		
		GRBLinExpr expr = new GRBLinExpr();
		for(PolicyNode n : visit_map.keySet()) {
			GRBVar var = visit_map.get(n);
			expr.addTerm(n.s.state_util, var); 
		}
		
		for(BackLink l : edge_var_buff) {
			expr.addTerm(0, l.var);
		}
		
		model.setObjective(expr, GRB.MAXIMIZE);
		
		for(int i=1; i<MAX_TIME_STEPS; i++) {
			for(PolicyNode n : visit_map.keySet()) {
				BackLink link = back_node_map.get(n);
				if(link == null) {
					continue;
				}
				
				int count = 0;
				expr = new GRBLinExpr();
				while(link != null) {
					if(RandomField.IsDstTerminalVertex(link.n.s)) {
						// cannot link to terminal vertex
						link = link.next_ptr;
						continue;
					}
					expr.addTerm(1.0f, var_map.get(link.n)[i-1]); 
					link = link.next_ptr;
					count++;
				}
		
				expr.addTerm(-1.0f, var_map.get(n)[i]); 
				model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c5" + i);
			}
		}
		
		// must contain the dst vertex
		expr = new GRBLinExpr();
		for(PolicyNode n : visit_map.keySet()) {
			
			if(RandomField.IsDstTerminalVertex(n.s)) {
				GRBVar var = visit_map.get(n);
				expr.addTerm(1.0f, var);
			}
		}
		
		model.addConstr(expr, GRB.GREATER_EQUAL, 1, "c11");

		
		// at most the path length
		expr = new GRBLinExpr();
		for(BackLink l : edge_var_buff) {
			expr.addTerm(l.dist, l.var); 
		}
		
		model.addConstr(expr, GRB.LESS_EQUAL, path_len, "c6");
		
		expr = new GRBLinExpr();
		
		// must include the src
		expr.addTerm(1.0f, var_map.get(s)[0]); 
		model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c7");
		
		// on one node at any time
		
		for(int i=0; i<MAX_TIME_STEPS; i++) {
			
			expr = new GRBLinExpr();
			
			for(PolicyNode n : var_map.keySet()) {
				GRBVar[] set = var_map.get(n);
				if(set[i] == null) {
					continue;
				}
				
				expr.addTerm(1.0f, set[i]); 
			}
			
			model.addConstr(expr, GRB.LESS_EQUAL, 1.0f, "c8");
		}
		
		// node visited if visited once through time
		
		for(PolicyNode n : var_map.keySet()) {
			
			GRBVar[] set = var_map.get(n);
			
			expr = new GRBLinExpr();
			for(int i=0; i<MAX_TIME_STEPS; i++) {
				if(set[i] == null) {
					continue;
				}
				
				expr.addTerm(-1.0f, set[i]); 
			}
			
			expr.addTerm(1.0f, visit_map.get(n)); 
			
			model.addConstr(expr, GRB.LESS_EQUAL, 0.0f, "c9");
		}
		
		// an edge is used if two connecting nodes through time exist
		for(BackLink l : edge_var_buff) {
			GRBVar[] set1 = var_map.get(l.n);
			GRBVar[] set2 = var_map.get(l.src);
			for(int i=1; i<MAX_TIME_STEPS; i++) {
				
				expr = new GRBLinExpr();
				expr.addTerm(-1.0f, set1[i-1]); 
				expr.addTerm(-1.0f, set2[i]);
				expr.addTerm(1.0f, l.var);
				
				model.addConstr(expr, GRB.GREATER_EQUAL, -1, "c190"+l+i);
			}
		}
		
		
		model.optimize();
		
		double net_dist = 0;
		for(BackLink l : edge_var_buff) {
			if(l.var.get(GRB.DoubleAttr.X) == 1.0f) {
				System.out.println(l.n.s+" "+l.src.s);
				net_dist += l.dist;
			}
		}
		
		System.out.println("SrcandTerm: "+RandomField.RootState()+"  "+RandomField.TerminalStates().get(0));
		
		float util = 0;
		ArrayList<PolicyNode> buff2 = new ArrayList<PolicyNode>();
		HashSet<PolicyNode> map = new HashSet<PolicyNode>();
		for(int i=0; i<MAX_TIME_STEPS; i++) {
			
			for(PolicyNode n : var_map.keySet()) {
				
				GRBVar set[] = var_map.get(n);

				if(set[i].get(GRB.DoubleAttr.X) == 1.0f) {
					buff2.add(n);
					System.out.println(n.s+" "+i);
					if(map.contains(n) == false) {
						util += n.s.state_util;
						map.add(n);
					}
				}
			}
		}
		
		net_dist = 0;
		for(int i=1; i<buff2.size(); i++) {
			BackLink link = back_node_map.get(buff2.get(i));
			
			boolean found = false;
			while(link != null) {
				if(link.n == buff2.get(i-1)) {
					
					net_dist += link.dist;
					GRBVar[] set1 = var_map.get(link.n);
					GRBVar[] set2 = var_map.get(link.src);
					
					for(BackLink l : edge_var_buff) {
						if(l.src == buff2.get(i) && l.n == buff2.get(i-1)) {
							if(set1[i-1].get(GRB.DoubleAttr.X) == 1.0 && set2[i].get(GRB.DoubleAttr.X) == 1.0) {
								if(link.var.get(GRB.DoubleAttr.X) == 1.0) {
									found = true;
									break;
								}
							}
						}
					}
				}
				link = link.next_ptr;
			}
			
			if(found == false) {
				System.out.println("no back link");System.exit(0);
			}
		}
		
		System.out.println("Net Dist: "+net_dist+"   "+path_len);
		System.out.println("Util: "+util);
		
		return util;
		
	}
	
	// This solves the orienteering problem using IP
	public float SolveByIP(PolicyNode s, HashMap<PolicyNode, SPathLink> path_link_map, 
			int path_len, ArrayList<PolicyNode> buff) throws GRBException {
		
		LoadBackLinks(s, path_link_map, 0, path_len);
		
		GRBEnv    env   = new GRBEnv("mip1.log");
	    GRBModel  model = new GRBModel(env);
	    HashMap<PolicyNode, GRBVar[]> var_map = new HashMap<PolicyNode, GRBVar[]>();
	    HashMap<PolicyNode, GRBVar> visit_map = new HashMap<PolicyNode, GRBVar>();
	    
	    int count1 = 0;
		for(PolicyNode n : node_map) {
			
			if(back_node_map.get(n) == null && n != s) {
				continue;
			}

			GRBVar set[] = new GRBVar[path_len];
			for(int i=0; i<set.length; i++) {
				set[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, n.toString());
				count1++;
			}

		    var_map.put(n, set);
		    
		    GRBVar var = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, n.toString());
		    visit_map.put(n, var);
		}
		
		int edge_num = 0;
		for(PolicyNode n : visit_map.keySet()) {
			BackLink link = back_node_map.get(n);
			while(link != null) {
				edge_num++;
				link = link.next_ptr;
			}
		}
		
		edge_rat = (float)edge_num / RandomField.EdgeNum();
		node_rat = (float)visit_map.size() / RandomField.NodeNum();
		
		model.update();
		
		GRBLinExpr expr = new GRBLinExpr();
		/*for(PolicyNode n : var_map.keySet()) {
			GRBVar set[] = var_map.get(n);
			for(int i=0; i<set.length; i++) {
				expr.addTerm(n.s.state_util, set[i]); 
			}
		}*/
		
		System.out.println(visit_map.size());
		for(PolicyNode n : visit_map.keySet()) {
			GRBVar var = visit_map.get(n);
			expr.addTerm(n.s.state_util, var); 
		}
		
		model.setObjective(expr, GRB.MAXIMIZE);
		
		for(int i=1; i<path_len; i++) {
			for(PolicyNode n : visit_map.keySet()) {
				BackLink link = back_node_map.get(n);
				if(link == null) {
					continue;
				}
				
				int count = 0;
				expr = new GRBLinExpr();
				while(link != null) {
					expr.addTerm(1.0f, var_map.get(link.n)[i-1]); 
					link = link.next_ptr;
					count++;
				}
		
				expr.addTerm(-1.0f, var_map.get(n)[i]); 
				model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c5" + i);
			}
		}

		
		// at most the path length
		expr = new GRBLinExpr();
		for(GRBVar[] n : var_map.values()) {
			for(int j=0; j<path_len; j++) {
				
				if(n[j] == null) {
					continue;
				}
				
				expr.addTerm(1.0f, n[j]); 
			}
		}
		
		model.addConstr(expr, GRB.LESS_EQUAL, path_len, "c6");
		
		expr = new GRBLinExpr();
		
		// must include the src
		expr.addTerm(1.0f, var_map.get(s)[0]); 
		model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c7");
		
		// on one node at any time
		
		for(int i=0; i<path_len; i++) {
			
			expr = new GRBLinExpr();
			
			for(PolicyNode n : var_map.keySet()) {
				GRBVar[] set = var_map.get(n);
				if(set[i] == null) {
					continue;
				}
				
				expr.addTerm(1.0f, set[i]); 
			}
			
			model.addConstr(expr, GRB.LESS_EQUAL, 1.0f, "c8");
		}
		
		// node visited if visited once through time
		
		for(PolicyNode n : var_map.keySet()) {
			
			GRBVar[] set = var_map.get(n);
			
			expr = new GRBLinExpr();
			for(int i=0; i<path_len; i++) {
				if(set[i] == null) {
					continue;
				}
				
				expr.addTerm(-1.0f, set[i]); 
			}
			
			expr.addTerm(1.0f, visit_map.get(n)); 
			
			model.addConstr(expr, GRB.LESS_EQUAL, 0.0f, "c9");
		}
		
		
		model.optimize();
		
		float util = 0;
		for(int i=0; i<path_len; i++) {
			
			int sum = 0;
			for(PolicyNode n : var_map.keySet()) {
				
				GRBVar set[] = var_map.get(n);

				if(set[i].get(GRB.DoubleAttr.X) == 1.0f) {
					util += n.s.state_util;
					best_n.steps[i] = n.s;
					best_n.step++;
					buff.add(n);
					sum++;
					
					if(visit_map.get(n).get(GRB.DoubleAttr.X) != 1.0f) {
						//System.out.println("boo");System.exit(0);
					}
					
					if(i > 0) {
						int count = 0;
						BackLink link = back_node_map.get(n);
						while(link != null) {
							GRBVar set1[] = var_map.get(link.n);
							if(set1[i-1].get(GRB.DoubleAttr.X) == 1.0f) {
								count++;
							}
							link = link.next_ptr;
						}
						
						if(count == 0) {
							//System.out.println("no blink");System.exit(0);
						}
					}
				}
			}
			
			if(sum != 1) {
				//System.out.println("error1");System.exit(0);
			}
		}
		
		GRBVar var = var_map.get(s)[0];
		if(var.get(GRB.DoubleAttr.X) != 1.0f) {
			//System.out.println("error2");System.exit(0);
		}
		
		if(Math.floor(util) != Math.floor(model.get(GRB.DoubleAttr.ObjVal))) {
			//System.out.println("error3 "+util+" "+model.get(GRB.DoubleAttr.ObjVal));System.exit(0);
		}
		
		return util;
	}
	
	// This finds the optimal path
	public float OptimalPath(int time_steps, State s, Node prev_n) {
		
		if(time_steps == 0) {
			
			float util = 0;
			int offset = 0;
			for(int i=0; i<prev_n.history.length; i++) {
				int val = prev_n.history[i];
				for(int j=0; j<32; j++) {
					
					if((val & 0x01) == 0x01) {
						util += state_util.get(offset);
					}
					
					val >>= 1;
					offset++;
					if(offset >= state_util.size()) {
						break;
					}
				}
				
				if(offset >= state_util.size()) {
					break;
				}
			}
			
			return util;
		}
		
		Node n = new Node();
		
		if(prev_n != null) {
			for(int i=0; i<n.history.length; i++) {
				n.history[i] = prev_n.history[i];
			}
		}
		
		n.SetNode(s.state_id);
		n.s = s;
		
		String str = n.toString();
		if(state_map.get(str) != null) {
			return 0;
		}
		
		state_map.put(str, n);
		
		float util = 0;
		StateLink link = s.forward_link;
		while(link != null) {
			util = Math.max(util, OptimalPath(time_steps - 1, link.s, n));
			link = link.next_ptr;
		}
		
		return util;
	}
	
	// This returns the set of visited states
	public int[] VisitStates() {
		return best_n.history;
	}
	
	// This returns the solution length
	public int SolnLength() {
		return best_n.step;
	}
	
	// This finds the optimal path
	public float OptimalPath(int time_steps, PolicyNode s, Node prev_n, 
			HashMap<PolicyNode, SPathLink> path_link_map) {
		
		float util = 0;
		SPathLink link_ptr = path_link_map.get(s);
		if(time_steps <= 0 || link_ptr == null) {
			
			return EvaluatePolicy(prev_n);
		}

    	while(link_ptr != null) {
    		
    		if(link_ptr.link_ptr.path.get(link_ptr.link_ptr.path.size()-1).s != s.s) {
    			System.out.println("bobo2 "+prev_n);System.exit(0);
    		}
    		
    		Node n = new Node();
    		if(prev_n != null) {
    			n.step = prev_n.step;
    		}

    		if(prev_n != null) {
    			for(int i=0; i<n.history.length; i++) {
    				n.history[i] = prev_n.history[i];
    			}
    			
    			for(int i=0; i<n.step+1; i++) {
    				n.steps[i] = prev_n.steps[i];
    			}
    		}
    		
    		int num = 0;
    		int count = 0;
    		for(int i=link_ptr.link_ptr.path.size()-1; i>=0; i--) {
    			int node_id = link_ptr.link_ptr.path.get(i).s.state_id;
    			if(n.SetNode(node_id) == false) {
    				num++;
    			}
    			
    			n.steps[n.step++] = link_ptr.link_ptr.path.get(i).s;
    			
    			count++;
    			if(time_steps - count <= 0) {
    				break;
    			}
    		}
    		
    		if(num == 0) {
    			EvaluatePolicy(n);
    			link_ptr = link_ptr.next_ptr;
    			continue;
    		}
    		
    		n.s = link_ptr.link_ptr.dst.s;
    		
    		
    		String str = n.toString();
    		if(state_map.get(str) != null) {
    			link_ptr = link_ptr.next_ptr;
    			continue;
    		}
    		
    		state_map.put(str, n);
    		
    		util = Math.max(util, OptimalPath(time_steps - count, link_ptr.link_ptr.dst, n, path_link_map));
    		link_ptr = link_ptr.next_ptr;
    	}

		return util;
	}

	private float EvaluatePolicy(Node prev_n) {
		
		float util = 0;
		int offset = 0;
		for(int i=0; i<prev_n.history.length; i++) {
			int val = prev_n.history[i];
			for(int j=0; j<32; j++) {
				
				if((val & 0x01) == 0x01) {
					util += state_util.get(offset);
				}
				
				val >>= 1;
				offset++;
				if(offset >= state_util.size()) {
					break;
				}
			}
			
			if(offset >= state_util.size()) {
				break;
			}
		}
		
		if(util > best_n.util) {
			best_n = prev_n;
			best_n.util = util;
		}
		
		return util;
	}
}
