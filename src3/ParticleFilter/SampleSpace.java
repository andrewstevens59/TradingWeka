package ParticleFilter;

import java.util.ArrayList;

public class SampleSpace {

	// This stores the set of k-closest neighbours for the sample
	public ArrayList<KDNodeDim> k_neighbour = new ArrayList<KDNodeDim>();
	// This stores the set of k-nodes for each dimension in the sample space
	public ArrayList<KDNodeDim> kdnode_dim = new ArrayList<KDNodeDim>();
	// This stores the set of samples associated with this space 
	public ArrayList<Sample> sample_buff = new ArrayList<Sample>();

}
