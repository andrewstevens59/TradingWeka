package DecisionNetwork;
import java.io.File;

import java.io.IOException;




import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.File;
import java.util.Map;

import org.json.JSONException;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.bayes.KDependentBayesClassifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;



public class Main {
	
	// This stores the set of decision nodes
	private ArrayList<Voter> node_buff = new ArrayList<Voter>();
	
	// This evaluates the performance of the predictor against naive bayes
	private void PredictionError(DataSet d, Voter best_node) {
		
		NaiveBayes n = new NaiveBayes(d);
		
		float avg1 = 0;
		float avg2 = 0;
		for(int i=0; i<d.TestOutputSet().size(); i++) {
			
			float correct1 = 0;
			float correct2 = 0;
			
			float count1 = 0;
			float count2 = 0;
			for(int j=0; j<node_buff.size(); j++) {
				
				if(node_buff.get(j).node.is_root == true) {
					boolean val = node_buff.get(j).Output(d.TestDataSet().get(i));
					if(val == true) {
						count1 += 1;
					} else {
						count2 += 1;
					}
				}
			}

			boolean val = count1 > count2;
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
	
	// This evaluates the performance of the predictor against naive bayes
	private void PredictionError(DataSet d, ExpandNetwork e) {
		
		NaiveBayes n = new NaiveBayes(d);
		
		float avg1 = 0;
		float avg2 = 0;
		for(int i=0; i<d.TestOutputSet().size(); i++) {
			
			int correct1 = 0;
			int correct2 = 0;
			
			boolean val = e.Classification(d.TestDataSet().get(i));
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
	
	// This finds an SVM
	private void SVM() throws IOException {
		
		 /* Load data */
        Dataset data = FileHandler.loadDataset(new File("test.arff"), 12, ",");
        /* Create Weka classifier */
        SMO smo = new SMO();
        /* Wrap Weka classifier in bridge */
        Classifier javamlsmo = new WekaClassifier(smo);
        /* Initialize cross-validation */
        CrossValidation cv = new CrossValidation(javamlsmo);
        /* Perform cross-validation */
        Map<Object, PerformanceMeasure> pm = cv.crossValidation(data);

        /* Output results */
        System.out.println(pm);
	}
	
	
	// This finds an K dependent bayes
	private void KDependentBayes(DataSet d) throws IOException {
		
		 /* Load data */
        Dataset data = FileHandler.loadDataset(new File("test.arff"), 12, ",");
        /* Wrap Weka classifier in bridge */
        
        int parent[] = new int[d.DimNum()];
        for(int i=0; i<d.DimNum(); i++) {
        	parent[i] = i;
        }
        
        Classifier javamlsmo = new KDependentBayesClassifier(false, 0.0, new int[] { 0, 1, 2, 4, 5, 8 });
        javamlsmo.buildClassifier(data);
        /* Initialize cross-validation */
        CrossValidation cv = new CrossValidation(javamlsmo);
        /* Perform cross-validation */
        Map<Object, PerformanceMeasure> pm = cv.crossValidation(data);

        /* Output results */
        System.out.println(pm);
	}
	
	public Main() throws NumberFormatException, IOException {
		DataSet d = new DataSet();
		
		//SVM();
		
		ExpandNetwork e = new ExpandNetwork(d, 10, 1);
		PredictionError(d, e);
		System.exit(0);
		
		
		for(int i=0; i<d.DimNum(); i++) {
			node_buff.add(new Voter(new DecisionNode(i, null)));
		}
		
		Random r = new Random();
		Voter best_node = node_buff.get(0);
		while(node_buff.size() < 600) {
			
			ArrayList<Voter> child_buff = new ArrayList<Voter>();
			for(int i=0; i<4; i++) {
				int id = r.nextInt(node_buff.size());
				child_buff.add(node_buff.get(id));
			}

			node_buff.add(new Voter(new DecisionNode(0, child_buff)));
		}
		
		float net_weight = 0;
		ArrayList<Float> weight = new ArrayList<Float>();
		for(int i=0; i<node_buff.size(); i++) {
			
			if(node_buff.get(i).node.is_root == true) {
				float predict = node_buff.get(i).node.AssignBestRule(d, 600, 3);
				net_weight += predict;
				weight.add(predict);
				System.out.println(i+" "+node_buff.size()+" "+predict);
			}
		}
		
		int offset = 0;
		for(int i=0; i<node_buff.size(); i++) {
			if(node_buff.get(i).node.is_root == true) {
				node_buff.get(i).node.predict_weight = weight.get(offset++) / net_weight;
			}
		}

		PredictionError(d, best_node);
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static void main(String[] args) throws IOException, JSONException {

		new Main();
	}

}
