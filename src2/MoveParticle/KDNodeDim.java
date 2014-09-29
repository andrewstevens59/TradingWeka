package MoveParticle;

public class KDNodeDim {

	// This stores the kdnode
	public KDNode kdnode;
	// This stores the corresponding dimension in the sample for the kdnode
	public int dim;
	// This stores hte dimension weight
	public float weight;
	
	public KDNodeDim(KDNode kdnode, int dim, float weight) {
		this.kdnode = kdnode;
		this.dim = dim;
		this.weight = weight;
	}

}
