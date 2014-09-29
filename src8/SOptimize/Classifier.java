package SOptimize;

import java.util.ArrayList;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

public class Classifier {
	
	private Attribute price = new Attribute("price"); 
	private Attribute window = new Attribute("window"); 
	private Attribute freq = new Attribute("freq"); 
	private Attribute ClassAttribute;
	
	// This returns an instance for a training sample
	private Instance TrainInstance(Train t) {
		
		Instance iExample = new Instance(2);
		iExample.setValue(price, t.price);  
		//iExample.setValue(window, t.window); 
		//iExample.setValue(freq, t.avg_freq); 

		if(t.barrier_side == 0) {
			iExample.setValue(ClassAttribute, "neutral");   
		} else if(t.barrier_side == 1) {
			iExample.setValue(ClassAttribute, "positive"); 
		} else if(t.barrier_side == -1) {
			iExample.setValue(ClassAttribute, "negative"); 
		}
		
		return iExample;
	}
	
	// This is used to classify an instance
	private void ClassifyInstance(Train t, double [][]coefficients) {
		
		System.out.println(coefficients.length+" "+coefficients[0].length);
		
	}

	public void BuildClassifier(ArrayList<Train> train_buff) throws Exception {
		
		// This stores the nb model 
		Logistic cModel = null;
		// This stores the set of attributes
		FastVector fvWekaAttributes = new FastVector(2);


		fvWekaAttributes.addElement(price);
		//fvWekaAttributes.addElement(window);
		//fvWekaAttributes.addElement(freq);

		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(2);
		 fvClassVal.addElement("positive");
		 fvClassVal.addElement("negative");
		 ClassAttribute = new Attribute("theClass", fvClassVal);

	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, train_buff.size() >> 1); 
	    isTrainingSet.setClass(ClassAttribute);
		
		for(int i=0; i<train_buff.size() >> 1; i++) {
			Train t = train_buff.get(i);

			Instance iExample = TrainInstance(t);
			 
			// add the instance
			isTrainingSet.add(iExample);
		}
		
		cModel = new Logistic();
		cModel.buildClassifier(isTrainingSet);
		double[][] coeff = cModel.coefficients();


		for(int i=0; i<train_buff.size() >> 1; i++) {
			Train t = train_buff.get(i);
			
			double sum = 1;
			for(int j=0; j<coeff[0].length; j++) {
				sum += Math.exp((t.price * coeff[0][j]) + (t.window * coeff[1][j]));
			}
			
			double prob1 = Math.exp((t.price * coeff[0][0]) + (t.window * coeff[1][0])) / sum;
			double prob2 = 1 / sum;

			double class_val = cModel.classifyInstance(isTrainingSet.instance(i));
			System.out.println(class_val+" "+isTrainingSet.instance(i).value(price)+" "+prob1+" "+prob2);
		}
	}

}
