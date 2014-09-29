package ParticleFilter;

public class KDNodeDim {

	// This stores the kdnode
	public KDNode kdnode;
	// This stores the corresponding dimension in the sample for the kdnode
	public int dim;
	
	public KDNodeDim(KDNode kdnode, int dim) {
		this.kdnode = kdnode;
		this.dim = dim;
	}

}
