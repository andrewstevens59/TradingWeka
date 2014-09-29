package DecisionNetwork;

import java.util.ArrayList;

public class DataRange {
	
	// This stores the set of ranges
	class Range {
		float min = 9999999;
		float max = -9999999;
		float avg = 0;
		float gap_min;
		float gap_max;
	}
	
	// This stores the set of data ranges
	Range[] range;
	// This stores the number of true values
	int true_val[][];
	// This stores the number of false values
	int false_val[][];

	public DataRange(ArrayList<DataVector> buff, int dim_num) {
		
		true_val = new int[dim_num][6];
		false_val = new int[dim_num][6];

		range = new Range[dim_num];
		for(int j=0; j<range.length; j++) {
			range[j] = new Range();
			for(int i=0; i<buff.size(); i++) {
				buff.get(i).val[j] *= 100;
				range[j].min = Math.min(range[j].min, buff.get(i).val[j]);
				range[j].max = Math.max(range[j].max, buff.get(i).val[j]);
				range[j].avg += buff.get(i).val[j];
			}
			
			if(range[j].min < 0) {
				range[j].avg = 0;
			} else {
				range[j].avg /= buff.size();
			}
			
			if(range[j].min == range[j].max) {
				System.out.println("Same");System.exit(0);
			}
			
			Range r = range[j];
			r.gap_min = (float) Math.exp((float) (Math.log(Math.abs(r.avg - r.min)) / 3));
			r.gap_max = (float) Math.exp((float) (Math.log(Math.abs(r.avg - r.max)) / 3));

			for(int i=0; i<6; i++) {
				true_val[j][i] = 0;
				false_val[j][i] = 0;
			}
		}
	}
	
	// This returns the true to false ratio for a given dimension and data range
	public float TrueFalseRatio(int dim, int r) {
		System.out.println(true_val[dim][r]+" "+false_val[dim][r]);
		return (true_val[dim][r] + 1) / (false_val[dim][r] + 1);
	}
	
    public DataRange(ArrayList<Float> buff) {
    	
    	true_val = new int[1][6];
		false_val = new int[1][6];
		for(int i=0; i<6; i++) {
			true_val[0][i] = 0;
			false_val[0][i] = 0;
		}
		
    	range = new Range[1];
    	range[0] = new Range();
    	for(int i=0; i<buff.size(); i++) {
    		buff.set(i, buff.get(i) * 100);
			range[0].min = Math.min(range[0].min, buff.get(i));
			range[0].max = Math.max(range[0].max, buff.get(i));
			range[0].avg += buff.get(i);
		}
    	
    	if(range[0].min == range[0].max) {
			System.out.println("Same "+range[0].min+" "+range[0].max);System.exit(0);
		}
		
		if(range[0].min < 0) {
			range[0].avg = 0;
		} else {
			range[0].avg /= buff.size();
		}
		
		Range r = range[0];
		r.gap_min = (float) Math.exp((float) (Math.log(Math.abs(r.avg - r.min)) / 3));
		r.gap_max = (float) Math.exp((float) (Math.log(Math.abs(r.avg - r.max)) / 3));
	}
    
    // This returns the number of dimensions
    public int DimNum() {
    	return range.length;
    }
	
	// This returns the feature vector for a given dimension
	public boolean[] GetDim(int dim, float val) {
		
		boolean set[] = new boolean[]{false, false, false, false, false, false};
		if(val < range[dim].avg) {
			
			float pow = range[dim].gap_min;
			for(int j=0; j<2; j++) {
				
				if(val > range[dim].avg - pow) {
					set[2-j] = true;
					true_val[dim][2-j]++;
					for(int k=0; k<6; k++) {
						if(k != 2-j) {
							false_val[dim][k]++;
						}
					}
					return set;
				}
				
				pow *= range[dim].gap_min;
			}
			
			set[0] = true;
			true_val[dim][0]++;
			for(int k=0; k<6; k++) {
				if(k != 0) {
					false_val[dim][k]++;
				}
			}
			
			return set;
		}
		
		float pow = range[dim].gap_max;
		for(int j=0; j<2; j++) {
			
			if(val < range[dim].avg + pow) {
				set[3+j] = true;
				true_val[dim][3+j]++;
				for(int k=0; k<6; k++) {
					if(k != 3+j) {
						false_val[dim][k]++;
					}
				}
				
				return set;
			}
			
			pow *= range[dim].gap_max;
		}
		
		set[5] = true;
		true_val[dim][5]++;
		for(int k=0; k<6; k++) {
			if(k != 5) {
				false_val[dim][k]++;
			}
		}
		
		return set;
	}

}
