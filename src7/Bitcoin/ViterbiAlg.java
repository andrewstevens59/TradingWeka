package Bitcoin;
import java.util.ArrayList;


// This class decodes the hidden state sequence 
public class ViterbiAlg {
	
	// This stores a state in the verterbi dynamic program
	class State {
		float prob = 0;
		// This stores the optimal action
		State opt_act = null;
		// This store the corresponding state
		OBState state;
	}
	
	// This stores the matrix of state values in the dynamic program
	State dyn_prog[][];
	
	// This normalizes an edge set 
	public void NormalizeEdges(OBLink forward_link) {
		
		float sum = 0;
		while(forward_link != null) {
			sum += forward_link.trav_prob;
			forward_link = forward_link.next_ptr;
		}
		
		while(forward_link != null) {
			forward_link.trav_prob /= sum;
			forward_link = forward_link.next_ptr;
		}
	}
	
	// This normalizes the hidden state and observed state edges
	public void NormalizeEdges(ArrayList<OBState> state_buff, ArrayList<PriceState> ob_buff) {
		
		for(int i=0; i<state_buff.size(); i++) {
			NormalizeEdges(state_buff.get(i).forward_link);
		}
		
		for(int i=0; i<ob_buff.size(); i++) {
			NormalizeEdges(ob_buff.get(i).forward_link);
		}
		
	}

	public ViterbiAlg(ArrayList<OBState> state_buff, ArrayList<PriceState> ob_buff) {
		
		NormalizeEdges(state_buff, ob_buff);
		dyn_prog = new State[ob_buff.size()][state_buff.size()];
		
		for(int i=0; i<state_buff.size(); i++) {
			dyn_prog[0][i] = new State();
		}
		
		OBLink forward_link = ob_buff.get(0).forward_link;
		
		while(forward_link != null) {
			dyn_prog[0][forward_link.dst.id].prob = forward_link.trav_prob;
			forward_link = forward_link.next_ptr;
		}
		
		for(int i=0; i<ob_buff.size()-1; i++) {
			
			for(int j=0; j<state_buff.size(); j++) {
				dyn_prog[i+1][j] = new State();
			}
			
			for(int j=0; j<state_buff.size(); j++) {
				forward_link = state_buff.get(i).forward_link;
				
				while(forward_link != null) {
					
					float val = forward_link.trav_prob * dyn_prog[i][forward_link.dst.id].prob;
					if(val > dyn_prog[i+1][j].prob) {
						dyn_prog[i+1][j].prob = val;
						dyn_prog[i+1][j].opt_act = dyn_prog[i][j];
					}

					forward_link = forward_link.next_ptr;
				}
			}
			
			forward_link = ob_buff.get(i+1).forward_link;
			
			while(forward_link != null) {
				dyn_prog[i+1][forward_link.dst.id].prob *= forward_link.trav_prob;
				forward_link = forward_link.next_ptr;
			}
		}
	}
	
	// This finds the optimal hidden state sequence 
	public void OptStateSequence(ArrayList<OBState> state_buff) {
		
		float max = 0;
		State s = null;
		for(int i=0; i<dyn_prog[0].length; i++) {
			
			if(dyn_prog[dyn_prog.length-1][i].prob >= max) {
				max = dyn_prog[dyn_prog.length-1][i].prob;
				s = dyn_prog[dyn_prog.length-1][i];
			}
		}

		while(s != null) {
			state_buff.add(s.state);
			s = s.opt_act;
		}
	}

}
