package MoveParticle;

import java.util.ArrayList;
import java.util.HashMap;

public class SampleSpace {

	// This stores the set of k-nodes for each dimension in the sample space
	public ArrayList<KDNodeDim> kdnode_dim = new ArrayList<KDNodeDim>();
	// This stores the set of nodes in the space and their node ids
	public HashMap<KDNode, Integer> node_space = new HashMap<KDNode, Integer>();
	// This stores the set of nodes in the space and their weights
	public HashMap<KDNode, Float> node_weight = new HashMap<KDNode, Float>();
	// This stores the history length
	public int sample_history = 20;
	
	// This normalizes the class weight
	public void NormClassWeight() {
		
		float len = 0;
		for(int i=0; i<kdnode_dim.size(); i++) {
			len += kdnode_dim.get(i).weight * kdnode_dim.get(i).weight;
		}
		
		len = (float) Math.sqrt(len);
		for(int i=0; i<kdnode_dim.size(); i++) {
			kdnode_dim.get(i).weight /= len;
		}
	}

}
