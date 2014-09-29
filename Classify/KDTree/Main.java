package KDTree;

import java.io.IOException;

import DecisionNetwork.DataSet;
import DecisionNetwork.NaiveBayes;
import DecisionNetwork.Voter;

public class Main {
	
	// This evaluates the performance of the predictor against naive bayes
	private void PredictionError(DataSet d, KDNode best_node) {
		
		NaiveBayes n = new NaiveBayes(d);
		
		float avg1 = 0;
		float avg2 = 0;
		for(int i=0; i<d.TestOutputSet().size(); i++) {
			
			float correct1 = 0;
			float correct2 = 0;
			
			boolean val = best_node.Output(d.TestDataSet().get(i));
			if(val == d.TestOutputSet().get(i)[0]) {
				correct1++;
			}
			
			boolean output[] = n.Classify(d, d.TestDataSet().get(i));
			if(output[0] == d.TestOutputSet().get(i)[0]) {
				correct2++;
			}
			
			avg1 += correct1;
			avg2 += correct2;
			System.out.println(correct1+" "+correct2);
		}
		
		avg1 /= d.TestOutputSet().size();
		avg2 /= d.TestOutputSet().size();
		System.out.println(avg1+" "+avg2);
	}
	
	public Main() throws NumberFormatException, IOException {
		
		DataSet d = new DataSet();
		KDNode n = new KDNode(d.DataSet(), d.OutputSet());
		PredictionError(d, n);
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		new Main();

	}

}
