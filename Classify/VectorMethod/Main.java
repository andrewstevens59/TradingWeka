package VectorMethod;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;

import DecisionNetwork.DataSet;
import DecisionNetwork.NaiveBayes;
import DecisionNetwork.Voter;


public class Main {

	
	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	
	// This stores the class error for naive bayes
	private ArrayList<Float> naive_b_error = new ArrayList<Float>();
	// This stores the class error for svm
	private ArrayList<Float> svm_error = new ArrayList<Float>();
	// This stores the class error for voter
	private ArrayList<Float> voter_error = new ArrayList<Float>();
	
	// This finds an SVM
	private float SVM( DataSet d) throws IOException {
		
		Dataset data = new DefaultDataset();
		for(int j=0; j<d.DataSet().size(); j++) {
			
			boolean sample[] = d.DataSet().get(j);
			boolean output = d.OutputSet().get(j)[0];
			
			double val[] = new double[sample.length >> 1];
			for(int i=0; i<val.length; i++) {
				val[i] = sample[i] == true ? 1.0f : 0.0f;
			}
			
			if(output == true) {
				data.add(new DenseInstance(val, TRUE_VAL));
			} else {
				data.add(new DenseInstance(val, FALSE_VAL));
			}
		}
		
		SMO smo = new SMO();
		Classifier javamlsmo = new WekaClassifier(smo);
        javamlsmo.buildClassifier(data);
        
        float true_count = 0;
        for(int i=0; i<d.TestDataSet().size(); i++) {
        	
        	boolean sample[] = d.TestDataSet().get(i);
			boolean output = d.TestOutputSet().get(i)[0];
			
			double val[] = new double[sample.length >> 1];
			for(int j=0; j<val.length; j++) {
				val[j] = sample[j] == true ? 1 : 0;
			}

			Boolean obj = (Boolean) javamlsmo.classify(new DenseInstance(val));
			
			if(obj == TRUE_VAL && output == true) {
				true_count++;
			} else if(output == false) {
				true_count++;
			}
        }
        
        return true_count / d.TestDataSet().size();
	}
	
	// This evaluates the performance of naive bayes
	private float NaiveBayesError(DataSet d) {
		
		NaiveBayes n = new NaiveBayes(d);
		
		float avg2 = 0;
		for(int i=0; i<d.TestOutputSet().size(); i++) {
			
			float correct2 = 0;
			boolean output[] = n.Classify(d, d.TestDataSet().get(i));
			if(output[0] == d.TestOutputSet().get(i)[0]) {
				correct2++;
			}
			
			avg2 += correct2;
		}
		
		return avg2 / d.TestOutputSet().size();
	}
	
	// This finds the set of classification errors
	private boolean FindClassError(int cycle_num) throws NumberFormatException, IOException {
		
		DataSet d = new DataSet();
		float naive_e = NaiveBayesError(d);
		float svm_e = SVM(d);
		if(svm_e > 0.85f) {
			return false;
		}
		
		VectorMethod m = new VectorMethod();
		float error = m.FindClassError(d, cycle_num, 220);
		
		System.out.println((error / svm_e)+" "+(error / naive_e)+" **********************");

		svm_error.add(error / svm_e);
		naive_b_error.add(error / naive_e);
		//voter_error.add(error);
		
		return true;
	}
	
	public Main() throws NumberFormatException, IOException {
		
		int offset = 0;
		while(offset < 4) {
			if(FindClassError(offset) == true) {
				offset++;
			}
		}
		
		float avg1 = 0;
		float avg2 = 0;
		for(int i=0; i<svm_error.size(); i++) {
			avg1 += svm_error.get(i);
			avg2 += naive_b_error.get(i);
			System.out.println(avg1+" "+avg2+" *****");
		}
		
		
		float var1 = 0;
		avg1 /= svm_error.size();
		for(int i=0; i<svm_error.size(); i++) {
			 var1 += (svm_error.get(i) - avg1) * (svm_error.get(i) - avg1);
		}
		
		float var2 = 0;
		avg2 /= naive_b_error.size();
		for(int i=0; i<naive_b_error.size(); i++) {
			var2 += (naive_b_error.get(i) - avg2) * (naive_b_error.get(i) - avg2);
		}
		
		var1 /= svm_error.size();
		var2 /= svm_error.size();
		
		var1 = (float) Math.sqrt(var1);
		var1 = (float) Math.sqrt(var2);
		
		System.out.println("**********************");
		System.out.println(avg1+" "+var1);
		System.out.println(avg2+" "+var2);
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		
		new Main();

	}

}
