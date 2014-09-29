package VolumeTrading;

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
	
	private Attribute atts[]; 
	private Attribute ClassAttribute;
	private J48 cModel;
	private FastVector fvWekaAttributes;
	
	// This returns an instance for a training sample
	private Instance TrainInstance(Train t) {
		
		Instance iExample = new Instance(t.input.length + 1);
		
		for(int i=0; i<t.input.length; i++) {
			iExample.setValue(atts[i], t.input[i]);
		}

		if(t.side == true) {
			iExample.setValue(ClassAttribute, "positive"); 
		} else {
			iExample.setValue(ClassAttribute, "negative"); 
		}
		
		return iExample;
	}
	
	// This is used to classify an instance
	public double ClassifyInstance(Train t) throws Exception {
		
		Instances isTestSet = new Instances("Rel", fvWekaAttributes, 0); 
		isTestSet.setClass(ClassAttribute);
		isTestSet.add(TrainInstance(t));
	    
		double class_val = cModel.classifyInstance(isTestSet.firstInstance());
		
		return class_val;
	}

	public Classifier(ArrayList<Train> train_buff, int start, int end) throws Exception {
		

		// This stores the set of attributes
		fvWekaAttributes = new FastVector(train_buff.get(0).input.length + 1);
		atts = new Attribute[train_buff.get(0).input.length];


		for(int i=0; i<train_buff.get(0).input.length; i++) {
			atts[i] = new Attribute("price" + i); 
			fvWekaAttributes.addElement(atts[i]);
		}

		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(2);
		 fvClassVal.addElement("positive");
		 fvClassVal.addElement("negative");
		 ClassAttribute = new Attribute("theClass", fvClassVal);

	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, train_buff.size() >> 1); 
	    isTrainingSet.setClass(ClassAttribute);
		
		for(int i=0; i<train_buff.size(); i++) {
			
			if(i >= start && i < end) {
				continue;
			}
			
			Train t = train_buff.get(i);

			Instance iExample = TrainInstance(t);
			 
			// add the instance
			isTrainingSet.add(iExample);
		}
		
		cModel = new J48();
		cModel.buildClassifier(isTrainingSet);
	}

}
