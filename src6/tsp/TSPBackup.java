package tsp;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import optimizeTree.OrienteeringProblem;
import optimizeTree.PolicyNode;
import state.StateLink;
import stateMachine.RandomField;

public class TSPBackup {
	

	private TSPSolver tsp;
	
	public TSPBackup(TSPSolver tsp) {
		this.tsp = tsp;
	}

	// This stores the average distance
	private double AvgDist(PolicyNode n1, PolicyNode n2) {
		
		ArrayList<PolicyNode> buff1 = new ArrayList<PolicyNode>();
		n1.LeafNodes(buff1);
		
		ArrayList<PolicyNode> buff2 = new ArrayList<PolicyNode>();
		n2.LeafNodes(buff2);
		
		double net_dist = 0;
		double num = 0;
		for(PolicyNode s1 : buff1) {
			for(PolicyNode s2 : buff2) {
				double dist = (s1.s.x - s2.s.x) * (s1.s.x - s2.s.x);
				dist += (s1.s.y - s2.s.y) * (s1.s.y - s2.s.y);
				dist = Math.sqrt(dist);
				net_dist += dist;
				num++;
			}
		}
		
		return net_dist / num;
	}
	
	// This updates the edge weight between two compressed vertices
	private void UpdateEdgeWeight(PolicyNode n1, PolicyNode n2) {
		
		ArrayList<PolicyNode> buff1 = new ArrayList<PolicyNode>();
		n1.LeafNodes(buff1);
		
		ArrayList<PolicyNode> buff2 = new ArrayList<PolicyNode>();
		n2.LeafNodes(buff2);

		for(PolicyNode s1 : buff1) {
			for(PolicyNode s2 : buff2) {
				String str = s1 +" " + s2;
				Integer num = tsp.edge_weight_map.get(str) == null ? 0 : tsp.edge_weight_map.get(str);
				tsp.edge_weight_map.put(str, num + 1);
				
				str = s2 +" " + s1;
				num = tsp.edge_weight_map.get(str) == null ? 0 : tsp.edge_weight_map.get(str);
				tsp.edge_weight_map.put(str, num + 1);
			}
		}
	}

	public void SolveRootProblem(double cull_ratio) throws GRBException {
		
		
		ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
		ArrayList<BackLink> edge_buff = new ArrayList<BackLink>();
		PolicyNode.RootNodes(buff);
		
		HashMap<PolicyNode, Queue<BackLink>> back_link_map = new HashMap<PolicyNode, Queue<BackLink>>();
		HashMap<PolicyNode, Queue<BackLink>> forward_link_map = new HashMap<PolicyNode, Queue<BackLink>>();
		HashSet<String> edge_map = new HashSet<String>();
		HashMap<String, GRBVar[]> var_map = new HashMap<String, GRBVar[]>();

		GRBEnv    env   = new GRBEnv("mip1.log");
	    GRBModel  model = new GRBModel(env);
	    
	    BuildGraph(buff, edge_buff, back_link_map, forward_link_map, edge_map,
				var_map, model, buff.size(), cull_ratio);
	    
	   GRBVar max_var = model.addVar(0.0, GRB.INFINITY, 1, GRB.CONTINUOUS, "max");
		
		model.update();
		
		GRBLinExpr expr = new GRBLinExpr();
		for(BackLink l : edge_buff) {
			for(int i=0; i<l.var1.length; i++) {
				expr.addTerm(0, l.var1[i]);
			}
		}
		
		expr.addTerm(1.0f, max_var);

		model.setObjective(expr, GRB.MINIMIZE);
		
		
		// incoming = outgoing
		for(int i=0; i<buff.size(); i++) {
			for(PolicyNode n : buff) {
				
				expr = new GRBLinExpr();
				Queue<BackLink> q1 = back_link_map.get(n);
				Queue<BackLink> q2 = forward_link_map.get(n);
				
				for(BackLink l : q1) {
					expr.addTerm(1, l.var1[i]);
				}
				
				for(BackLink l : q2) {
					expr.addTerm(-1, l.var1[(i+1) % buff.size()]);
				}
				
				model.addConstr(expr, GRB.EQUAL, 0.0, "c5" +n+i);
			}
		}
		
		// one edge for each time step
		for(int i=0; i<buff.size(); i++) {
			
			expr = new GRBLinExpr();
			for(BackLink l : edge_buff) {
				expr.addTerm(1, l.var1[i]);
			}
			
			model.addConstr(expr, GRB.EQUAL, 1, "c6" +i);
		}
		
		// at most one outgoing edge
		for(int i=0; i<buff.size(); i++) {
			
			Queue<BackLink> q2 = forward_link_map.get(buff.get(i));
			expr = new GRBLinExpr();
			for(BackLink l : q2) {
				for(int j=0; j<l.var1.length; j++) {
					expr.addTerm(1, l.var1[j]);
				}
			}
			
			model.addConstr(expr, GRB.LESS_EQUAL, 1, "c16" +i);
		}
		
		// at most one backward edge
		for(int i=0; i<buff.size(); i++) {
			
			Queue<BackLink> q2 = back_link_map.get(buff.get(i));
			expr = new GRBLinExpr();
			for(BackLink l : q2) {
				for(int j=0; j<l.var1.length; j++) {
					expr.addTerm(1, l.var1[j]);
				}
			}
			
			model.addConstr(expr, GRB.LESS_EQUAL, 1, "c17" +i);
		}
		
		// all path lengths less than the maximum
		for(BackLink l : edge_buff) {
			for(int j=0; j<l.var1.length; j++) {
				expr = new GRBLinExpr();
				expr.addTerm(l.dist, l.var1[j]);
				expr.addTerm(-1, max_var);
				model.addConstr(expr, GRB.LESS_EQUAL, 0, "c19" +l);
			}
		}
		
		
		model.optimize();
		
		for(BackLink l : edge_buff) {
			for(int i=0; i<l.var1.length; i++) {
				if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
					
					System.out.println(l.src+" "+l.dst+" "+i+"       "+l.src.IsLeafNode()+" "+l.dst.IsLeafNode());
					
					if(l.src.IsLeafNode() && l.dst.IsLeafNode()) {
						tsp.leaf_edges.add(l);
					}
				}
			}
		}
		
		for(PolicyNode n : buff) {
			
			int count1 = 0;
			Neighbour neigh = new Neighbour();
			Queue<BackLink> q = forward_link_map.get(n);
			for(BackLink l : q) {
				for(int i=0; i<l.var1.length; i++) {
					if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
						neigh.right = l.dst;
						count1++;
					}
				}
			}
			
			
			if(count1 != 1) {
				System.out.println("not link "+count1);System.exit(0);
			}
			
			int count2 = 0;
			q = back_link_map.get(n);
			for(BackLink l : q) {
				for(int i=0; i<l.var1.length; i++) {
					if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
						neigh.left = l.dst;
						count2++;
					}
				}
			}
			
			if(count2 != 1) {
				System.out.println("not link "+count2);System.exit(0);
			}
			
			if(neigh.left == neigh.right) {
				System.out.println("same1");System.exit(0);
			}
			
			tsp.neighbour_map.put(n, neigh);
		}
	}

	private void BuildGraph(ArrayList<PolicyNode> buff,
			ArrayList<BackLink> edge_buff,
			HashMap<PolicyNode, Queue<BackLink>> back_link_map,
			HashMap<PolicyNode, Queue<BackLink>> forward_link_map,
			HashSet<String> edge_map, HashMap<String, GRBVar[]> var_map,
			GRBModel model, int time_steps, double cull_ratio) throws GRBException {
		
		
		HashSet<PolicyNode> map = new HashSet<PolicyNode>();
	    for(PolicyNode n : buff) {
			back_link_map.put(n, new LinkedList<BackLink>());
			forward_link_map.put(n, new LinkedList<BackLink>());
			map.add(n);
	    }
	    
	    System.out.println("Root Size:    "+buff.size());
	    ArrayList<BackLink> link_buff = new ArrayList<BackLink>();
	    for(int i=0; i<buff.size(); i++) {
			for(int j=0; j<buff.size(); j++) {
				if(i == j) {
					continue;
				}
				
				PolicyNode src = buff.get(i);
				PolicyNode dst = buff.get(j);
				String str = src +" "+dst;
				BackLink l = new BackLink();
				l.src = src;
				l.dst = dst;
				l.dist = AvgDist(src, dst);
				link_buff.add(l);
			}
	    }
	    
	    Collections.sort(link_buff, new Comparator<BackLink>() {
			 
	        public int compare(BackLink arg1, BackLink arg2) {
	        	
	        	if(arg1.dist < arg2.dist) {
	    			return -1;
	    		}

	    		if(arg1.dist > arg2.dist) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });

		for(int i=0; i<link_buff.size(); i++) {
			PolicyNode src = link_buff.get(i).src;
			PolicyNode dst = link_buff.get(i).dst;
			
			double dist = AvgDist(src, dst);
			
			Queue<BackLink> q1 = back_link_map.get(dst);
			Queue<BackLink> q2 = forward_link_map.get(src);
			
			if(q1.size() > buff.size() * cull_ratio && q2.size() > buff.size() * cull_ratio) {
				continue;
			}
			
			String var_str = src+" "+dst;
			GRBVar var[] = var_map.get(var_str);
			if(var == null) {
				var = new GRBVar[time_steps];
				for(int k=0; k<var.length; k++) {
					var[k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, var.toString()+k);
				}
				
				BackLink l = new BackLink();
				l.var1 = var;
				l.dist = dist;
				l.src = src;
				l.dst = dst;
				edge_buff.add(l);
				var_map.put(var_str, var);
			}		
		
			
			String str = src+" "+dst+" f";
			//System.out.println("link  "+src+" "+dst+"      "+str);
			
			
			if(edge_map.contains(str) == false) {
				BackLink l = new BackLink();
				l.src = src;
				l.dst = dst;
				l.dist = dist;
				l.var1 = var;
				q2.add(l);
				
				edge_map.add(str);
			}
			
			str = dst+" "+src+" b";
			if(edge_map.contains(str) == false) {
				BackLink l = new BackLink();
				l.src = dst;
				l.dst = src;
				l.dist = dist;
				l.var1 = var;
				q1.add(l);
				
				edge_map.add(str);
			}
		}
	}
	
	// This solves a local subsystem through a decompression
	public ArrayList<PolicyNode> DecompressSystem(PolicyNode node, double cull_ratio, int depth_num) throws GRBException {
		
		node.DecompressNode(depth_num);
		
		ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
		Neighbour neigh = tsp.neighbour_map.get(node);
		
		if(neigh.left == neigh.right) {
			System.out.println("same");System.exit(0);
		}
		
		buff.add(neigh.left);
		node.RootNodesGrouped(buff, depth_num);
		buff.add(neigh.right);
		
		for(int j=1; j<buff.size()-1; j++) {
			if(tsp.node_used.contains(buff.get(j))) {
				//System.out.println("node already used");System.exit(0);
			}
			
			tsp.node_used.add(buff.get(j));
		}
		
		HashMap<PolicyNode, Queue<BackLink>> back_link_map = new HashMap<PolicyNode, Queue<BackLink>>();
		HashMap<PolicyNode, Queue<BackLink>> forward_link_map = new HashMap<PolicyNode, Queue<BackLink>>();
		HashSet<String> edge_map = new HashSet<String>();
		HashMap<String, GRBVar[]> var_map = new HashMap<String, GRBVar[]>();
		ArrayList<BackLink> edge_buff = new ArrayList<BackLink>();

		GRBEnv    env   = new GRBEnv("mip1.log");
	    GRBModel  model = new GRBModel(env);
	    
	    BuildGraph(buff, edge_buff, back_link_map, forward_link_map, edge_map,
				var_map, model, buff.size()-1, cull_ratio);
		
	    GRBVar max_var = model.addVar(0.0, GRB.INFINITY, 1, GRB.CONTINUOUS, "max");
		
		model.update();
		
		GRBLinExpr expr = new GRBLinExpr();
		for(BackLink l : edge_buff) {
			for(int i=0; i<l.var1.length; i++) {
				expr.addTerm(0, l.var1[i]);
			}
		}
		
		expr.addTerm(1.0f, max_var);

		model.setObjective(expr, GRB.MINIMIZE);
		
		
		// incoming = outgoing
		for(int i=1; i<buff.size()-2; i++) {
			for(int j=0; j<buff.size(); j++) {

				PolicyNode n = buff.get(j);
				expr = new GRBLinExpr();
				Queue<BackLink> q1 = back_link_map.get(n);
				Queue<BackLink> q2 = forward_link_map.get(n);
				
				for(BackLink l : q1) {
					expr.addTerm(1, l.var1[i]);
				}
				
				for(BackLink l : q2) {
					expr.addTerm(-1, l.var1[(i+1) % buff.size()]);
				}
				
				model.addConstr(expr, GRB.EQUAL, 0.0, "c5" +n+i);
			}
		}
		
		// one edge for each time step
		for(int i=0; i<buff.size()-1; i++) {
			
			expr = new GRBLinExpr();
			for(BackLink l : edge_buff) {
				expr.addTerm(1, l.var1[i]);
			}
			
			model.addConstr(expr, GRB.EQUAL, 1, "c6" +i);
		}
		
		// at most one outgoing edge
		for(int i=0; i<buff.size()-1; i++) {
			
			Queue<BackLink> q2 = forward_link_map.get(buff.get(i));
			expr = new GRBLinExpr();
			for(BackLink l : q2) {
				for(int j=0; j<l.var1.length; j++) {
					expr.addTerm(1, l.var1[j]);
				}
			}
			
			model.addConstr(expr, GRB.EQUAL, 1, "c16" +i);
		}
		
		// at most one backward edge
		for(int i=0; i<buff.size(); i++) {
			
			Queue<BackLink> q2 = back_link_map.get(buff.get(i));
			expr = new GRBLinExpr();
			for(BackLink l : q2) {
				for(int j=0; j<l.var1.length; j++) {
					expr.addTerm(1, l.var1[j]);
				}
			}
			
			model.addConstr(expr, GRB.LESS_EQUAL, 1, "c17" +i);
		}
		
		// one forward link at time step 0 from src
		Queue<BackLink> q2 = forward_link_map.get(neigh.left);
		expr = new GRBLinExpr();
		
		for(BackLink l : q2) {
			expr.addTerm(1, l.var1[0]);
		}
		
		model.addConstr(expr, GRB.EQUAL, 1, "c7");
		
		// one backward link at time step n from dst
		Queue<BackLink> q1 = back_link_map.get(neigh.right);
		expr = new GRBLinExpr();
		
		for(BackLink l : q1) {
			expr.addTerm(1, l.var1[buff.size()-2]);
		}
		
		model.addConstr(expr, GRB.EQUAL, 1, "c9");
		
		// all path lengths less than the maximum
		for(BackLink l : edge_buff) {
			for(int j=0; j<l.var1.length; j++) {
				expr = new GRBLinExpr();
				expr.addTerm(l.dist, l.var1[j]);
				expr.addTerm(-1, max_var);
				model.addConstr(expr, GRB.LESS_EQUAL, 0, "c19" +l);
			}
		}
		
		long prev_time = System.currentTimeMillis();
		
		model.optimize();
		
		tsp.process_time += System.currentTimeMillis() - prev_time;
		
		for(PolicyNode n : PolicyNode.root_map) {
			System.out.print(n+" ");
		}
		
		System.out.println("");
		
		for(int i=0; i<buff.size()-1; i++) {
			
			int count = 0;
			for(BackLink l : edge_buff) {
				if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
					count++;
				}
			}
			
			if(count != 1) {
				System.out.println("time step: "+i+"  "+count);System.exit(0);
			}
		}
		
		System.out.println("src dst "+neigh.left+" "+neigh.right+"      "+buff.size());
		HashMap<PolicyNode, Integer> src_map = new HashMap<PolicyNode, Integer>();
		HashMap<PolicyNode, Integer> dst_map = new HashMap<PolicyNode, Integer>();
		
		BackLink first_edge = null;
		BackLink last_edge = null;
		for(BackLink l : edge_buff) {
			for(int i=0; i<l.var1.length; i++) {
				if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
					
					Integer num = src_map.get(l.src) == null ? 0 : src_map.get(l.src);
					src_map.put(l.src, num + 1);
					
					num = dst_map.get(l.dst) == null ? 0 : dst_map.get(l.dst);
					dst_map.put(l.dst, num + 1);
					
					if(l.src.IsLeafNode() && l.dst.IsLeafNode()) {
						tsp.leaf_edges.add(l);
					}
					
					if(i == 0) {
						first_edge = l;
					}
					
					if(i == l.var1.length-1) {
						last_edge = l;
					}
					System.out.println(l.src+" "+l.dst+" "+i);
				}
			}
		}
		
		for(int i=1; i<buff.size()-1; i++) {

			if(src_map.get(buff.get(i)) != 1) {
				System.out.println("src num miss");System.exit(0);
			}
			
			if(dst_map.get(buff.get(i)) != 1) {
				System.out.println("dst num miss "+dst_map.get(buff.get(i))+"  "+buff.get(i));System.exit(0);
			}
		}
		
		if(src_map.get(buff.get(0)) != 1) {
			System.out.println("src num miss1");System.exit(0);
		}
		
		if(dst_map.get(buff.get(0)) != null) {
			System.out.println("dst num miss2");System.exit(0);
		}
		
		if(dst_map.get(buff.get(buff.size()-1)) != 1) {
			System.out.println("dst num miss3");System.exit(0);
		}
		
		if(src_map.get(buff.get(buff.size()-1)) != null) {
			System.out.println("src num miss4");System.exit(0);
		}
		
		for(int j=1; j<buff.size()-1; j++) {

			PolicyNode n = buff.get(j);
			
			int count1 = 0;
			Neighbour neigh1 = new Neighbour();
			Queue<BackLink> q = forward_link_map.get(n);
			for(BackLink l : q) {
				for(int i=0; i<l.var1.length; i++) {
					if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
						neigh1.right = l.dst;
						count1++;
					}
				}
			}
			
			
			if(count1 != 1) {
				//System.out.println("not link1 "+count1);System.exit(0);
			}
			
			int count2 = 0;
			q = back_link_map.get(n);
			for(BackLink l : q) {
				for(int i=0; i<l.var1.length; i++) {
					try {
						if(l.var1[i].get(GRB.DoubleAttr.X) > 0.5) {
							neigh1.left = l.dst;
							count2++;
						}
					} catch(GRBException e) {
						
					}
				}
			}
			
			if(count2 != 1) {
				//System.out.println("not link2 "+count2);System.exit(0);
			}
			
			tsp.neighbour_map.put(n, neigh1);
			
			if(neigh1.left == neigh1.right) {
				System.out.println("same1");System.exit(0);
			}
		}
		
		if(first_edge.src != neigh.left) {
			System.out.println("src not same");System.exit(0);
		}
		
		if(last_edge.dst != neigh.right) {
			System.out.println("dst not same "+last_edge.dst+" "+neigh.right);System.exit(0);
		}
		
		tsp.neighbour_map.remove(node);
		
		Neighbour temp = tsp.neighbour_map.get(neigh.left);
		if(temp.left == node) {
			temp.left = first_edge.dst;
		} else {
			temp.right = first_edge.dst;
		}
		
		if(temp.left == temp.right) {
			System.out.println("same1");System.exit(0);
		}
		
		temp = tsp.neighbour_map.get(neigh.right);
		if(temp.right == node) {
			temp.right = last_edge.src;
		} else {
			temp.left = last_edge.src;
		}
		
		if(temp.left == temp.right) {
			System.out.println("same1");System.exit(0);
		}
		
		CheckNeighbours();
		
		model.dispose();
	    env.dispose();
		
		return buff;
	}

	// This checks the set of connecting neighbours
	private void CheckNeighbours() {
		
		for(PolicyNode n : PolicyNode.root_map) {
			if(tsp.neighbour_map.get(n) == null) {
				System.out.println("no node");System.exit(0);
			}
		}
		
		HashMap<PolicyNode, Integer> src_map = new HashMap<PolicyNode, Integer>();
		HashMap<PolicyNode, Integer> dst_map = new HashMap<PolicyNode, Integer>();
		
		for(Neighbour n : tsp.neighbour_map.values()) {
			
			Integer num = src_map.get(n.left) == null ? 0 : src_map.get(n.left);
			src_map.put(n.left, num + 1);
			
			num = dst_map.get(n.right) == null ? 0 : dst_map.get(n.right);
			dst_map.put(n.right, num + 1);
		}
		
		for(Integer n : src_map.values()) {
			if(n != 1) {
				System.out.println("wrong "+n);System.exit(0);
			}
		}
		
		for(Integer n : dst_map.values()) {
			if(n != 1) {
				System.out.println("wrong "+n);System.exit(0);
			}
		}
	}
	
	// This finds the TSP tour
	public void FindTSPTour(boolean is_sparse, int depth_num) throws GRBException {
		
		int offset = 0;
		while(true) {
		
			if(offset == 0) {
				
				double cull_ratio = 0.25;
				while(true) {
					try {
						SolveRootProblem(cull_ratio);
						break;
					} catch(GRBException e) {
						cull_ratio *= 2;
					}
				}
				
				CheckNeighbours();
				offset++;
				continue;
			}
			
			ArrayList<RootNode> root_buff = new ArrayList<RootNode>();
			for(PolicyNode n : PolicyNode.root_map) {
				RootNode r = new RootNode();
				r.node = n;
				ArrayList<PolicyNode> buff = new ArrayList<PolicyNode>();
				n.LeafNodes(buff);
				r.node_num = buff.size();
				root_buff.add(r);
			}
			
			Collections.sort(root_buff, new Comparator<RootNode>() {
				 
		        public int compare(RootNode arg1, RootNode arg2) {
		        	
		        	if(arg1.node_num < arg2.node_num) {
		    			return 1;
		    		}

		    		if(arg1.node_num > arg2.node_num) {
		    			return -1;
		    		}

		    		return 0; 
		        }
		    });
			
			if(root_buff.get(0).node.IsLeafNode()) {
				break;
			}
			
			double cull_ratio = 0.25;
			while(true) {
				try {
					DecompressSystem(root_buff.get(0).node, cull_ratio, depth_num);
					break;
				} catch(GRBException e) {
					cull_ratio *= 2;
				}
			}
			
			


		}
		
		System.out.println(tsp.leaf_edges.size()+" "+RandomField.NodeNum());
		
		double max_path_len = 0;
		HashMap<PolicyNode, Integer> src_node_map = new HashMap<PolicyNode, Integer>();
		HashMap<PolicyNode, Integer> dst_node_map = new HashMap<PolicyNode, Integer>();
		for(BackLink l : tsp.leaf_edges) {
			System.out.println(l.src+" "+l.dst);
			Integer num = src_node_map.get(l.src);
			max_path_len = Math.max(max_path_len, l.dist);
			if(num == null) {
				src_node_map.put(l.src, 1);
			} else {
				src_node_map.put(l.src, 2);
			}
			
			num = dst_node_map.get(l.dst);
			if(num == null) {
				dst_node_map.put(l.dst, 1);
			} else {
				dst_node_map.put(l.dst, 2);
			}
		}
		
		System.out.println("path len: "+max_path_len+"      "+(tsp.process_time / 1000));
		
		if(src_node_map.size() != RandomField.NodeNum() || dst_node_map.size() != RandomField.NodeNum()) {
			System.out.println("Node Num miss "+src_node_map.size()+" "+dst_node_map.size());System.exit(0);
		}
		
		if(tsp.leaf_edges.size() != RandomField.NodeNum()) {
			System.out.println("Not tour");System.exit(0);
		}
		
		for(Entry<PolicyNode, Integer> n : src_node_map.entrySet()) {
			if(n.getValue() != 1) {
				System.out.println("Node Num miss "+n.getValue()+" "+n.getKey()+" "+n.getKey().comp_id);
			}
		}
		
		for(Entry<PolicyNode, Integer> n : dst_node_map.entrySet()) {
			if(n.getValue() != 1) {
				System.out.println("Node Num miss "+n.getValue()+" "+n.getKey()+" "+n.getKey().comp_id);
			}
		}
	}
	
	// This solves the dense and sparse version of the problem
	public void SolveSparseTSP() throws GRBException {
		FindTSPTour(false, 4);
	}

}