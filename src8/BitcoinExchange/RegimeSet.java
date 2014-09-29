package BitcoinExchange;

import java.util.ArrayList;

public class RegimeSet {

	// This stores the set of observed input variables
	public ArrayList<boolean []> data_set = new ArrayList<boolean []>();
	// This stores the set of observed output variables
	public ArrayList<boolean []> output_set = new ArrayList<boolean []>();
	
	// This adds a test sample
	public void AddSampe(boolean sample[], boolean output[]) {
		data_set.add(sample);
		output_set.add(output);
	}
	
	// This retrieves a sample
	public boolean [] GetSample(int id) {
		return data_set.get(id);
	}
	
	// This retrieves the outcome of a sample
	public boolean Output(int id) {
		return output_set.get(id)[0];
	}

}
