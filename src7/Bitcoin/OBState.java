package Bitcoin;

// This stores an order book state
public class OBState {

	// This stores the price distribution for buy and sells for N orders
	public int dist[] = new int[8];
	// This stores the set of forward links 
	OBLink forward_link = null;
	// THis stores the total number of states
	static int state_num = 0;
	// This stores the state id
	int id = state_num++;

}
