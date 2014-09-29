package state;

import java.io.IOException;

//This stores a state variable instance
public class StateVar {
	// This is the state
	State state;
	// This is the variable id
	int var_id;
	
	@Override
	public boolean equals(Object o) {
		System.out.println("eqqqqqqqqqqqqqq");
        if (!(o instanceof StateVar)) {
              return false;
        }
        
        if (((StateVar) o).state == state 
                    && ((StateVar) o).var_id == var_id) {
              return true;
        }
        try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return false;
	}
}
