import java.util.ArrayList;


public class TrainSN {
	
	// This is used to create the set of switching rules
	SwitchingRules switch_rule = new SwitchingRules();
	// This is used to create the model
	CreateModel model = new CreateModel(10, 1,  0);	

	public TrainSN() {
		
	}
	
	// This returns the learning function
	private float TrainFunc(float inputs[]) {
		
		return 4 * inputs[0] + 6 * inputs[1];
	}
	
	// This is used to train the model
	public void TrainModel(int input_num, int ouput_num) {
		
		ArrayList<TrainingSample> buff = new ArrayList<TrainingSample>();
		
		for(int i=0; i<20; i++) {
			TrainingSample sample = new TrainingSample();
			sample.input_val = new float[input_num];
			sample.output_val = new float[ouput_num];
			
			for(int j=0; j<input_num; j++) {
				sample.input_val[j] = (float) Math.random();
			}
			
			sample.output_val[0] = TrainFunc(sample.input_val);
			buff.add(sample);
		}
		
		switch_rule.FindSwitchinRules(CreateModel.Model(), buff);
	}

}
