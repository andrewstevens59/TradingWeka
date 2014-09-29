package Bitcoin;
import java.util.ArrayList;


public class PriceState {
	
	// This stores the set of forward links
	OBLink forward_link = null;

	// This stores the gradient for different window sizes
	public int grad[] = new int[]{0, 0, 0, 0};
	
	// This finds the observed state vector from price history
	public void FindPriceState(ArrayList<Float> prices) {
		
		int window_size = 4;
		for(int i=0; i<grad.length; i++) {
			
			if(window_size <= prices.size()) {
				LinearRegression.Regression(prices, prices.size() - window_size, prices.size());
				grad[i] = (int) (LinearRegression.gradient * 8);
			}
			
			window_size <<= 1;
		}
	}
	
	// This is a toString method
	public String toString() {
		
		String temp = new String();
		for(int i=0; i<grad.length; i++) {
			temp += grad[i] + " ";
		}
		
		return temp;
	}

}
