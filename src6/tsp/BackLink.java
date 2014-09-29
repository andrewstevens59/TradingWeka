package tsp;

import gurobi.GRBVar;
import optimizeTree.PolicyNode;

class BackLink {
	GRBVar var;
	PolicyNode src;
	PolicyNode dst;
	double dist;
	
	GRBVar var1[];
}
