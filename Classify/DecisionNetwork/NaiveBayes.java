package DecisionNetwork;

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
	

	public NaiveBayes(DataSet d) {
		uncond_prob = new float[d.OutputNum()];
		for(int k=0; k<d.OutputNum(); k++) {
			uncond_prob[k] = 0;
		}
		
		condt_buff = new OutputProb[d.OutputNum()][d.DimNum()];
		
		for(int k=0; k<d.OutputNum(); k++) {
			for(int j=0; j<d.DimNum(); j++) {
				condt_buff[k][j] = new OutputProb();
			}
		}
		
		for(int i=0; i<d.OutputSet().size()-100; i++) {
			
			boolean output[] = d.OutputSet().get(i);
			boolean sample[] = d.DataSet().get(i);
			for(int k=0; k<d.OutputNum(); k++) {
				if(d.OutputSet().get(i)[k] == true) {
					uncond_prob[k]++;
				}
				
				for(int j=0; j<d.DimNum(); j++) {
					
					if(sample[j] == true) {
						if(output[k] == true) {
							condt_buff[k][j].prob[0][0]++;
						} else {
							condt_buff[k][j].prob[0][1]++;
						}
					} else {
						if(output[k] == true) {
							condt_buff[k][j].prob[1][0]++;
						} else {
							condt_buff[k][j].prob[1][1]++;
						}
					}
				}
			}
		}
		
		for(int k=0; k<d.OutputNum(); k++) {
			for(int j=0; j<d.DimNum(); j++) {
				condt_buff[k][j].prob[0][0] /= d.OutputSet().size();
				condt_buff[k][j].prob[0][1] /= d.OutputSet().size();
				condt_buff[k][j].prob[1][0] /= d.OutputSet().size();
				condt_buff[k][j].prob[1][1] /= d.OutputSet().size();
			}
		}
	}
	
	// This classifies a sample 
	public boolean[] Classify(DataSet d, boolean sample[]) {
		
		boolean output[] = new boolean[d.OutputNum()];
		for(int k=0; k<d.OutputNum(); k++) {
			
			float simp_prob1 = 1.0f;
			float simp_prob2 = 1.0f;
			for(int j=0; j<d.DimNum(); j++) {
				
				if(sample[j] == true) {
					simp_prob1 *= condt_buff[k][j].prob[0][0];
					simp_prob2 *= condt_buff[k][j].prob[0][1];
				} else {
					simp_prob1 *= condt_buff[k][j].prob[1][0];
					simp_prob2 *= condt_buff[k][j].prob[1][1];
				}
				
				simp_prob1 *= uncond_prob[k];
				simp_prob2 *= 1 - uncond_prob[k];
			}
			
			if(simp_prob1 > simp_prob2) {
				output[k] = true;
			} else {
				output[k] = false;
			}
			
		}
		
		return output;
	}

}
