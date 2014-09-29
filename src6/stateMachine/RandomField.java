package stateMachine;

import gurobi.GRB;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.StringTokenizer;

import optimizeTree.NodeDist;
import optimizeTree.OrienteeringProblem;
import optimizeTree.PolicyNode;
import state.State;
import state.StateLink;
import state.StateVariable;

public class RandomField {
	
	// This stores one of the possible expansion states
	class ExpState {
		State s;
		int time_step;
	}
	
	// THis defines the grid size
	static int GRID_SIZE = 100;

	// This is used to stores the random field
	static int random_field[][] = new int[GRID_SIZE][GRID_SIZE];
	// This stores the root state for the starting point
	private static State root_state = null;
	// This stores the complete set of states
	private HashMap<String, State> state_map = new HashMap<String, State>();
	// This stores the set of states
	private ArrayList<State> state_set = new ArrayList<State>();
	// This stores the set of state links
	private ArrayList<StateLink> link_buff = new ArrayList<StateLink>();
	private ArrayList<State> state_group;
	
	
	// This defines the maximum path length
	static private int path_length;
	// This stores the set of starting vertices
	static private HashSet<State> terminal_src_set = new HashSet<State>();
	// This stores the set of starting vertices
	static private HashSet<State> terminal_dst_set = new HashSet<State>();
	
	// This stores the set of terminal nodes
	private static ArrayList<State> termainal_buff = new ArrayList<State>();
	// THis stores the set of src nodes
	private static ArrayList<State> src_buff = new ArrayList<State>();
	
	// This stores the total number of nodes
	static private int node_num;
	// This stores the total number of edges
	static private int edge_num;
	
	static float FACTOR = 1.0f;
	// This defines the maximum number of edges per node 
	static int MAX_EDGE_NUM = 5;
	
	public RandomField() {
		

	}
	
	public static void GenerateGrid(int grid_size) {
		
		random_field = new int[grid_size][grid_size];
		
		Random r = new Random();
		for(int i=0; i<grid_size; i++) {
			for(int j=0; j<grid_size; j++) {
				random_field[i][j] = (int) (r.nextInt(100));
			}
		}
	}
	
	// This returns the total edge number
	public static int EdgeNum() {
		return edge_num;
	}
	
	// This returns the total node number
	public static int NodeNum() {
		return node_num;
	}
	
	// This reads a problem into memory
	public void ReadProblem(String str) {
		
		PolicyNode.ResetTree();
		
		 try{
			  // Open the file that is the first 
			  // command line parameter
			  FileInputStream fstream = new FileInputStream(str);
			  // Get the object of DataInputStream
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  
			  
			  int offset = 0;
			  strLine = br.readLine();
			  StringTokenizer strtok = new StringTokenizer(strLine, " ");
			  while(strtok.hasMoreTokens()) {
				  State s = new State();
				  s.state_id = state_set.size();
				  s.state_util = Float.parseFloat(strtok.nextToken());
				  state_set.add(s);
				  s.x = offset % 100;
				  s.y = offset / 100;
				  
				  offset++;
			  }

			  root_state = state_set.get(state_set.size() >> 1);
	
			  //Read File Line By Line
			  while ((strLine = br.readLine()) != null)   {
				  strtok = new StringTokenizer(strLine, " ");
				  int src = Integer.parseInt(strtok.nextToken());
				  int dst = Integer.parseInt(strtok.nextToken());
				  State s = state_set.get(src);
				  State n = state_set.get(dst);

				  StateLink prev_ptr = n.backward_link;
				  n.backward_link = new StateLink();
				  n.backward_link.s = s;
				  n.backward_link.next_ptr = prev_ptr;
				  n.backward_link.src = n;
				
				  prev_ptr = s.forward_link;
				  s.forward_link = new StateLink();
				  s.forward_link.s = n;
				  s.forward_link.next_ptr = prev_ptr;
				  s.forward_link.src = s;
				  link_buff.add(s.forward_link);
			  }
			  
			  //Close the input stream
			  in.close();
	     }catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		 }
		 
		 node_num = state_set.size();
		 edge_num = link_buff.size();
	}
	
	// This writes the problem to file
	public void WriteProblem(String str) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(str));
		
		
		for(int i=0; i<state_set.size(); i++) {
			out.write(state_set.get(i).state_util+" ");
			if(state_set.get(i).state_id != i) {
				System.out.println("error");System.exit(0);
			}
		}
		
		out.write("\n");
		
		for(int i=0; i<state_set.size(); i++) {
			State s = state_set.get(i);
			StateLink link = s.forward_link;
			while(link != null) {
				out.write(link.src.state_id+" "+link.s.state_id+"\n");
				link = link.next_ptr;
			}
		}
		
		out.close();
	}
	
	// This returns the root state
	public static State RootState() {
		return root_state;
	}
	
	// This creates a new state
	private State NewState(State s, double x, double y, State state_ref[][]) {
		
		State n = state_ref[(int) x][(int) y];
		
		StateLink prev_ptr = n.backward_link;
		n.backward_link = new StateLink();
		n.backward_link.s = s;
		n.backward_link.next_ptr = prev_ptr;
		n.backward_link.src = n;
		
		prev_ptr = s.forward_link;
		s.forward_link = new StateLink();
		s.forward_link.s = n;
		s.forward_link.next_ptr = prev_ptr;
		s.forward_link.src = s;
		link_buff.add(s.forward_link);
		
		return n;
	}
	
	// This returns the set of state links
	public ArrayList<StateLink> LinkBuff() {
		return link_buff;
	}
	
	// This returns the maximum path length for a benchmark problem
	public static int MaxPathLength() {
		return path_length;
	}
	
	// This a predicate indicating a source or terminal vertex
	public static boolean IsDstTerminalVertex(State s) {
		return terminal_dst_set.contains(s);
	}
	
	// This a predicate indicating a source or terminal vertex
	public static boolean IsSrcTerminalVertex(State s) {
		return terminal_src_set.contains(s);
	}
	
	// This returns the set of terminal states
	public static ArrayList<State> TerminalStates() {
		return termainal_buff;
	}
	
	// This returns the set of src or root states
	public static ArrayList<State> RootStates() {
		return src_buff;
	}
	
	// This loads a benchmark problem
	public void LoadTSPBenchMark(String problem) throws IOException {
		
		
		String str = "F:/PhD/Code/BooleanSystemBackup/TSP/"+problem;
		System.out.println(str);
		BufferedReader br = new BufferedReader(new FileReader(str));
	
		String line;
		line = br.readLine();
		StringTokenizer tok = new StringTokenizer(line, " 	 ");
		
				
		while ((line = br.readLine()) != null) {
			tok = new StringTokenizer(line, " 	");
			
			System.out.println(line);
			
			if(tok.countTokens() != 3) {
				continue;
			}
			
			try {
				Integer.parseInt(tok.nextToken());
			} catch(NumberFormatException e) {
				continue;
			}
			
			State s = new State();
			s.x = Float.parseFloat(tok.nextToken());
			s.y = Float.parseFloat(tok.nextToken());
			s.node.x = s.x;
			s.node.y = s.y;
			s.node_id = state_set.size();
			state_set.add(s);
		}
		
		terminal_src_set.add(state_set.get(0));
		root_state = state_set.get(0);
		
		terminal_dst_set.add(state_set.get(1));
		termainal_buff.add(state_set.get(1));
		

		for(int j=0; j<state_set.size(); j++) {

			for(int i=0; i<state_set.size(); i++) {
				
				if(i == j) {
					continue;
				}
				
				State s = state_set.get(j);
				State n = state_set.get(i);
				
				if(s == n) {
					System.out.println("same1");System.exit(0);
				}

				double dist = 0;
				dist += (s.x - n.x) * (s.x - n.x);
				dist += (s.y - n.y) * (s.y - n.y);
				dist = Math.sqrt(dist);

				StateLink prev_ptr = n.backward_link;
			 	n.backward_link = new StateLink();
				n.backward_link.s = s;
				n.backward_link.next_ptr = prev_ptr;
				n.backward_link.src = n;
				n.backward_link.dist = dist;
				
				prev_ptr = s.forward_link;
				s.forward_link = new StateLink();
				s.forward_link.s = n;
				s.forward_link.next_ptr = prev_ptr;
				s.forward_link.src = s;
				s.forward_link.dist = dist;
				link_buff.add(s.forward_link);
			}
		}
		
		br.close();
		
		node_num = state_set.size();
		edge_num = link_buff.size();
	}
	
	// This loads a benchmark problem
	public void LoadOrienteeringBenchMark(int set, String problem) throws IOException {
		
		
		String str = "F:/PhD/Code/BooleanSystemBackup/Tsilligirides/Set"+set+"/tsiligirides_problem_"+set+"_budget_"+problem+".txt";
		System.out.println(str);
		BufferedReader br = new BufferedReader(new FileReader(str));
	
		String line;
		line = br.readLine();
		StringTokenizer tok = new StringTokenizer(line, " 	");
		path_length = Integer.parseInt(tok.nextToken());
		
				
		while ((line = br.readLine()) != null) {
			tok = new StringTokenizer(line, " 	");
			State s = new State();
			s.x = Float.parseFloat(tok.nextToken());
			s.y = Float.parseFloat(tok.nextToken());
			s.state_util = Integer.parseInt(tok.nextToken());
			s.node_id = state_set.size();
			state_set.add(s);
		}

		terminal_src_set.add(state_set.get(0));
		root_state = state_set.get(0);
		
		terminal_dst_set.add(state_set.get(1));
		termainal_buff.add(state_set.get(1));
		

		for(int j=0; j<state_set.size(); j++) {

			for(int i=0; i<state_set.size(); i++) {
				
				if(i == j) {
					continue;
				}
				
				State s = state_set.get(j);
				State n = state_set.get(i);
				
				if(s == n) {
					System.out.println("same1");System.exit(0);
				}

				double dist = 0;
				dist += (s.x - n.x) * (s.x - n.x);
				dist += (s.y - n.y) * (s.y - n.y);
				dist = Math.sqrt(dist);

				StateLink prev_ptr = n.backward_link;
			 	n.backward_link = new StateLink();
				n.backward_link.s = s;
				n.backward_link.next_ptr = prev_ptr;
				n.backward_link.src = n;
				n.backward_link.dist = dist;
				
				prev_ptr = s.forward_link;
				s.forward_link = new StateLink();
				s.forward_link.s = n;
				s.forward_link.next_ptr = prev_ptr;
				s.forward_link.src = s;
				s.forward_link.dist = dist;
				link_buff.add(s.forward_link);
			}
		}
		
		br.close();
		
		node_num = state_set.size();
		edge_num = link_buff.size();
	}
	
	// This generates a random graph with few adjacent neighbours
	public void GenerateRandomGraph(int node_num, int edge_num) {
		

		Random r = new Random();
		for(int i=0; i<node_num; i++) {
			State s = new State();
			s.state_util = (int) (r.nextInt(10000));
			s.state_id = state_set.size();
			
			state_set.add(s);
		}
		
		root_state = state_set.get(state_set.size() >> 1);
		
		for(int i=0; i<state_set.size(); i++) {
			for(int j=0; j<edge_num; j++) {
				State n = state_set.get(r.nextInt(state_set.size()));
				State s = state_set.get(i);
				
				StateLink prev_ptr = n.backward_link;
				n.backward_link = new StateLink();
				n.backward_link.s = s;
				n.backward_link.next_ptr = prev_ptr;
				n.backward_link.src = n;
				
				prev_ptr = s.forward_link;
				s.forward_link = new StateLink();
				s.forward_link.s = n;
				s.forward_link.next_ptr = prev_ptr;
				s.forward_link.src = s;
				link_buff.add(s.forward_link);
				
			}
		}
	}
	
	
	// This generates a random graph with few adjacent neighbours
	public void GenerateRandomGrid(int grid_size) {
		
		GenerateGrid(grid_size);

		Random r = new Random();
		
		State s_ref[][] = new State[grid_size][grid_size];
		
		for(int x=0; x<grid_size; x++) {
			for(int y=0; y<grid_size; y++) {
				State s = new State();
				s.state_util = (int) (r.nextInt(10000));
				s.state_id = state_set.size();
				
				state_set.add(s);
				s_ref[x][y] = s;
			}
		}
		
		root_state = s_ref[grid_size >> 1][grid_size >> 1];
		for(int x=0; x<grid_size; x++) {
			for(int y=0; y<grid_size; y++) {
				State s = s_ref[x][y];
				
				if(x > 0) {
					NewState(s, x-1, y, s_ref);
				}
				
				if(y > 0) {
					NewState(s, x, y-1, s_ref);
				}
				
				if(x < grid_size-1) {
					NewState(s, x+1, y, s_ref);
				}
				
				if(y < grid_size-1) {
					NewState(s, x, y+1, s_ref);
				}
				
				/*if(x > 1) {
					NewState(s, x-2, y, s_ref);
				}
				
				if(y > 1) {
					NewState(s, x, y-2, s_ref);
				}
				
				if(x < grid_size-2) {
					NewState(s, x+2, y, s_ref);
				}
				
				if(y < grid_size-2) {
					NewState(s, x, y+2, s_ref);
				}
				
				if(x > 0 && y > 0) {
					NewState(s, x-1, y-1, s_ref);
				}
				
				if(x > 0 && y < grid_size-1) {
					NewState(s, x-1, y+1, s_ref);
				}
				
				if(x < grid_size-1 && y > 0) {
					NewState(s, x+1, y-1, s_ref);
				}
				
				if(x < grid_size-1 && y < grid_size-1) {
					NewState(s, x+1, y+1, s_ref);
				}*/
			}
		}
	}
	
	// This generates a hard or disjointed tree
	public void GenerateTreeGraph(int node_num, int edge_num) {

		// This stores the set of expansion nodes
		PriorityQueue<State> state_queue = new PriorityQueue<State>(10, new Comparator<State>() {
			 
	        public int compare(State arg1, State arg2) {
	        	
	        	if(arg1.choose_util < arg2.choose_util) {
	    			return 1;
	    		}

	    		if(arg1.choose_util > arg2.choose_util) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		int grid_size = 30;
		GenerateGrid(grid_size);

		Random r = new Random();
		
		State s_ref[][] = new State[grid_size][grid_size];
		
		for(int x=0; x<grid_size; x++) {
			for(int y=0; y<grid_size; y++) {
				State s = new State();
				s.state_util = (int) (r.nextInt(10000));
				s.state_id = state_set.size();
				s.x = x;
				s.y = y;
				
				state_set.add(s);
				s_ref[x][y] = s;
			}
		}
		
		root_state = s_ref[grid_size >> 1][grid_size >> 1];
		state_queue.add(root_state);
		
		int offset = 0;
		while(offset < node_num && state_queue.size() > 0) {

			State s = state_queue.remove();
			offset++;
			
			for(int j=0; j<edge_num; j++) {
				State n = s_ref[r.nextInt(grid_size)][r.nextInt(grid_size)];
				
				StateLink prev_ptr = n.backward_link;
				n.backward_link = new StateLink();
				n.backward_link.s = s;
				n.backward_link.next_ptr = prev_ptr;
				n.backward_link.src = n;
				
				prev_ptr = s.forward_link;
				s.forward_link = new StateLink();
				s.forward_link.s = n;
				s.forward_link.next_ptr = prev_ptr;
				s.forward_link.src = s;
				link_buff.add(s.forward_link);
			
				state_queue.add(n);
			}
		}
	}
	
	// This is the entry function to begin the state machine
	public ArrayList<State> StateMachine(State s, char buffer[]) throws IOException, CodeException {
		
		state_group = new ArrayList<State>();
		
		int offset = 0;
		Random r = new Random();
		for(int i=0; i<12; i++) {
			int x = r.nextInt(GRID_SIZE);
			int y = r.nextInt(GRID_SIZE);
			State n = NewState(s,x, y, null);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		return state_group;
		
		/*if(s.forward_link != null) {
			return state_group;
		}
		
		int x = (int) StateVariable.StateVar(s, 0);
		int y = (int) StateVariable.StateVar(s, 1);
		int offset = 0;
		
		if(x > 0) {
			
			State n = NewState(s, x-1, y);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(y > 0) {
			State n = NewState(s, x, y-1);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x < GRID_SIZE-1) {
			State n = NewState(s, x+1, y);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(y < GRID_SIZE-1) {
			State n = NewState(s, x, y+1);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x > 1) {
			State n = NewState(s, x-2, y);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(y > 1) {
			State n = NewState(s, x, y-2);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x < 298) {
			State n = NewState(s, x+2, y);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(y < 298) {
			State n = NewState(s, x, y+2);
			if(n != null) {;
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x > 0 && y > 0) {
			State n = NewState(s, x-1, y-1);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x > 0 && y < GRID_SIZE-1) {
			State n = NewState(s, x-1, y+1);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x < GRID_SIZE-1 && y > 0) {
			State n = NewState(s, x+1, y-1);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		if(x < GRID_SIZE-1 && y < GRID_SIZE-1) {
			State n = NewState(s, x+1, y+1);
			if(n != null) {
				n.action_id = offset++;
				state_group.add(n);
			}
		}
		
		return state_group;*/
	}
	
	// This updates the optimal action set
	public float FindOptimalPolicy(int path_len) throws GRBException {
		
		GRBEnv    env   = new GRBEnv("mip1.log");
	    GRBModel  model = new GRBModel(env);
	    HashMap<State, GRBVar[]> var_map = new HashMap<State, GRBVar[]>();
	    HashMap<State, GRBVar> visit_map = new HashMap<State, GRBVar>();
	    
	    int count1 = 0;
		for(State n : state_set) {
			
			if(n.backward_link == null && n != RootState()) {
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

		
		model.update();
		
		GRBLinExpr expr = new GRBLinExpr();
		/*for(PolicyNode n : var_map.keySet()) {
			GRBVar set[] = var_map.get(n);
			for(int i=0; i<set.length; i++) {
				expr.addTerm(n.s.state_util, set[i]); 
			}
		}*/
		
		System.out.println(visit_map.size());
		for(State n : visit_map.keySet()) {
			GRBVar var = visit_map.get(n);
			expr.addTerm(n.state_util, var); 
		}
		
		model.setObjective(expr, GRB.MAXIMIZE);
		
		for(int i=1; i<path_len; i++) {
			for(State n : visit_map.keySet()) {
				StateLink link = n.backward_link;
				if(link == null) {
					continue;
				}
				
				int count = 0;
				expr = new GRBLinExpr();
				while(link != null) {
					if(var_map.get(link.s) == null) {
						link = link.next_ptr;
						continue;
					}
					expr.addTerm(1.0f, var_map.get(link.s)[i-1]); 
					link = link.next_ptr;
					count++;
				}
				
				if(count == 0) {
					continue;
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
		expr.addTerm(1.0f, var_map.get(RootState())[0]); 
		model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c7");
		
		// on one node at any time
		
		for(int i=0; i<path_len; i++) {
			
			expr = new GRBLinExpr();
			
			for(State n : var_map.keySet()) {
				GRBVar[] set = var_map.get(n);
				if(set[i] == null) {
					continue;
				}
				
				expr.addTerm(1.0f, set[i]); 
			}
			
			model.addConstr(expr, GRB.LESS_EQUAL, 1.0f, "c8");
		}
		
		// node visited if visited once through time
		
		for(State n : var_map.keySet()) {
			
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
		ArrayList<State> buff = new ArrayList<State>();
		for(int i=0; i<path_len; i++) {
			
			int sum = 0;
			for(State n : var_map.keySet()) {
				
				GRBVar set[] = var_map.get(n);

				if(set[i].get(GRB.DoubleAttr.X) > 0.95f) {
					util += n.state_util;
					buff.add(n);
					sum++;
					
					if(visit_map.get(n).get(GRB.DoubleAttr.X) != 1.0f) {
						//System.out.println("boo");System.exit(0);
					}
					
					if(i > 0) {
						int count = 0;
						StateLink link = n.backward_link;
						while(link != null) {
							GRBVar set1[] = var_map.get(link.s);
							if(set1 == null) {
								link = link.next_ptr;
								continue;
							}
							
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
		
		GRBVar var = var_map.get(RootState())[0];
		if(var.get(GRB.DoubleAttr.X) != 1.0f) {
			//System.out.println("error2");System.exit(0);
		}
		
		if(Math.floor(util) != Math.floor(model.get(GRB.DoubleAttr.ObjVal))) {
			//System.out.println("error3 "+util+" "+model.get(GRB.DoubleAttr.ObjVal));System.exit(0);
		}
		
		/*State s = RootState();
		for(int i=0; i<path_len-1; i++) {
			
			if(buff.get(i) != s) {
				//System.out.println("miss");System.exit(0);
			}
			
			boolean is_found = false;
			StateLink link = s.forward_link;
			while(link != null) {
				if(link.s == buff.get(i+1)) {
					s = buff.get(i+1);
					is_found = true;
					break;
				}
				link = link.next_ptr;
			}
			
			if(is_found == false) {
				//System.out.println("no link");System.exit(0);
			}
		}*/
		
		return util;
	}

	
	// This returns the full state set
	public ArrayList<State> StateSet() {
		return state_set;
	}
	

}
