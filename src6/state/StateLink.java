package state;
import optimizeTree.SLink;

public class StateLink {

	// This stores a connecting state
	public State s;
	// This stores the transition probability
	public float trav_prob = 1.0f;
	// This stores the next state link
	public StateLink next_ptr;
	// This stores the ptr to an s-link
	public SLink s_link;
	// This stores the src node
	public State src;
	// This stores the euclidian distance
	public double dist;
}
