package CGLSMethod;

import java.util.ArrayList;

public class NaiveBayes {
	
	// This stores the set of conditional output probabilities
	class OutputProb {
		float[][] prob = new float [][]{{0, 0}, {0, 0}};
	}
	
	// This stores the set of conditional probabilities
	OutputProb [][] condt_buff;
	// This stores the set of unconditional probabilities
	float uncond_prob[];
	

	public NaiveBayes(ArrayList<boolean []> data_set, ArrayList<Boolean> output_set) {
		
		uncond_prob = new float[data_set.get(0).length];
		for(int k=0; k<1; k++) {
			uncond_prob[k] = 0;
		}
		
		condt_buff = new OutputProb[1][data_set.get(0).length];
		
		for(int k=0; k<1; k++) {
			for(int j=0; j<data_set.get(0).length; j++) {
				condt_buff[k][j] = new OutputProb();
			}
		}
		
		for(int i=0; i<data_set.size(); i++) {
			
			boolean output = output_set.get(i);
			boolean sample[] = data_set.get(i);

			if(output_set.get(i) == true) {
				uncond_prob[0]++;
			}
			
			for(int j=0; j<data_set.get(0).length; j++) {
				
				if(sample[j] == true) {
					if(output == true) {
						condt_buff[0][j].prob[0][0]++;
					} else {
						condt_buff[0][j].prob[0][1]++;
					}
				} else {
					if(output == true) {
						condt_buff[0][j].prob[1][0]++;
					} else {
						condt_buff[0][j].prob[1][1]++;
					}
				}
			}
		}
		
		for(int k=0; k<1; k++) {
			for(int j=0; j<data_set.get(0).length; j++) {
				condt_buff[k][j].prob[0][0] /= output_set.size();
				condt_buff[k][j].prob[0][1] /= output_set.size();
				condt_buff[k][j].prob[1][0] /= output_set.size();
				condt_buff[k][j].prob[1][1] /= output_set.size();
			}
		}
	}
	
	// This classifies a sample 
	public boolean Classify(boolean sample[]) {

		float simp_prob1 = 1.0f;
		float simp_prob2 = 1.0f;
		for(int j=0; j<sample.length; j++) {
			
			if(sample[j] == true) {
				simp_prob1 *= condt_buff[0][j].prob[0][0];
				simp_prob2 *= condt_buff[0][j].prob[0][1];
			} else {
				simp_prob1 *= condt_buff[0][j].prob[1][0];
				simp_prob2 *= condt_buff[0][j].prob[1][1];
			}
			
			simp_prob1 *= uncond_prob[0];
			simp_prob2 *= 1 - uncond_prob[0];
		}
	
		return simp_prob1 > simp_prob2;
	}

}
