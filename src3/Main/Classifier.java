package Main;
import java.io.IOException;
import java.util.ArrayList;

import VectorMethod.VectorMethod;


public class Classifier {
	
	// This stores the classifier
	VectorMethod vm = new VectorMethod();

	
	public Classifier(RegimeSet d, String name) throws NumberFormatException, IOException {
		
		DataSet d2 = new DataSet(d.data_set, d.output_set, d.data_set, d.output_set);
		float error = vm.FindClassError(d2, 0, 10);
		
		if(error < 0.80) {
			System.exit(0);
		}

		vm.WriteClassifier(name);	
		
		//vm = new VectorMethod();
		//vm.ReadClassifier(name);
		
		int true_num = 0;
		int false_num = 0;
		float true_count = 0;
		for(int i=0; i<d.data_set.size(); i++) {
			boolean sample[] = d.data_set.get(i);
			boolean val = Output(sample);
			if(val == d.output_set.get(i)[0]) {
				true_count++;
			}
			
			if(val == true) {
				true_num++;
			} else {
				false_num++;
			}
		}
		
		true_count /= d.data_set.size();
		if(true_count < error) {
			System.out.println("boo");System.exit(0);
		}
		
		System.out.println("Write Done     "+true_num+"  "+false_num);
	}
	
	public Classifier(String name, RegimeSet d) throws IOException {
		vm = new VectorMethod();
		vm.ReadClassifier(name);
		
		int true_num = 0;
		int false_num = 0;
		float true_count = 0;
		for(int i=0; i<d.data_set.size(); i++) {
			boolean sample[] = d.data_set.get(i);
			boolean val = Output(sample);
			if(val == d.output_set.get(i)[0]) {
				true_count++;
			}
			
			if(val == true) {
				true_num++;
			} else {
				false_num++;
			}
		}
		
		true_count /= d.data_set.size();
		if(true_count < 0.8f) {
			System.out.println("boo");System.exit(0);
		}
		
		System.out.println("Write Done     "+true_num+"  "+false_num);
		
	}
	
	// This prints a regime set
	public static void PrintRegime(RegimeSet r) {
		
		for(int i=0; i<r.data_set.size(); i++) {
			boolean sample[] = r.data_set.get(i);
			
			System.out.print(r.output_set.get(i)[0]+"     ");
			for(int j=0; j<sample.length; j++) {
				System.out.print(sample[j]+" ");
			}
			System.out.println("");
		}
	}
	
	public Classifier(String name) throws IOException {
		vm = new VectorMethod();
		vm.ReadClassifier(name);
	}
	
	// This classifies a sample 
	public boolean Output(boolean sample[]) {
		return vm.Classify(sample);
	}

}
