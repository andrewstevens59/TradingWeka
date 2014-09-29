package CGLSMethod;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Normalize;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import DecisionNetwork.DataSet;
import DecisionNetwork.Voter;
import Jama.Matrix;
import VectorMethod.VectorMethod;

public class Main {
	
	class Dim {
		int dim;
		double split;
		double gap;
	}

	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	
	// This stores the set of comparisons
	private ArrayList<Float> comp_buff = new ArrayList<Float>();
	// This stores the set of comparisons
	private ArrayList<Float> time_buff1 = new ArrayList<Float>();
	private ArrayList<Float> time_buff2 = new ArrayList<Float>();
	
	long time1 = 0;
	long time2 = 0;
	
	// This stores the nb model 
	Classifier cModel = null;
	// This stores the set of attributes
	FastVector fvWekaAttributes;
	// This stores the naive bayes classifier
	NaiveBayes nb;
	
	// This creates a naive bayes classifier for the output set
	private void CreateNBClassifier(DataSet d, ArrayList<DecisionNode> output_buff) throws Exception {
		
		/*Attribute atts[] = new Attribute[output_buff.size()];
		
		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(2);
		 fvClassVal.addElement("positive");
		 fvClassVal.addElement("negative");
		 Attribute ClassAttribute = new Attribute("theClass", fvClassVal);
	    
	    fvWekaAttributes = new FastVector(output_buff.size() + 1);
	    for(int i=0; i<output_buff.size(); i++) {
	    	fvClassVal = new FastVector(2);
			fvClassVal.addElement("positive");
			fvClassVal.addElement("negative");
			 
	    	atts[i] = new Attribute("att"+i, fvClassVal);
	    	fvWekaAttributes.addElement(atts[i]);
	    }
	    
	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    isTrainingSet = new Instances("Rel", fvWekaAttributes, d.DataSet().size()); 
	    isTrainingSet.setClassIndex(output_buff.size());
		
		for(int i=0; i<d.DataSet().size(); i++) {
			boolean sample[] = d.DataSet().get(i);
			boolean output = d.OutputSet().get(i)[0];
			
			Instance iExample = new Instance(output_buff.size() + 1);
			for(int j=0; j<output_buff.size(); j++) {
				boolean val = output_buff.get(j).Output(sample);
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(j), val == true ? "positive" : "negative");  
			}
			
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(output_buff.size()), output == true ? "positive" : "negative");     
			 
			// add the instance
			isTrainingSet.add(iExample);
		}
		
		cModel = (Classifier)new NaiveBayes();
		cModel.buildClassifier(isTrainingSet);*/
		
		ArrayList<boolean []> data_set = new ArrayList<boolean []>();
		ArrayList<Boolean> output_set = new ArrayList<Boolean>();
		for(int i=0; i<d.DataSet().size(); i++) {
			boolean sample[] = d.DataSet().get(i);
			boolean output = d.OutputSet().get(i)[0];
			data_set.add(sample);
			output_set.add(output);
		}
		
		nb = new NaiveBayes(data_set, output_set);
	}
	
	// This classifies a given instance 
	public boolean Output(boolean sample[], ArrayList<DecisionNode> output_buff) throws Exception {
		
		/*Instance inst = new Instance(output_buff.size());
		
		for(int j=0; j<output_buff.size(); j++) {
			boolean output = output_buff.get(j).Output(sample);
			inst.setValue((Attribute)fvWekaAttributes.elementAt(j), output == true ? "positive" : "negative");
		}

		System.out.println(cModel.classifyInstance(inst));
		return cModel.classifyInstance(inst) > 0;*/
		
		boolean set[] = new boolean[sample.length];
		for(int j=0; j<output_buff.size(); j++) {
			set[j] = output_buff.get(j).Output(sample);
		}
		
		return nb.Classify(set);
	}
	
	// This performs PCA on the instances
	private float PCA(DataSet d) throws NumberFormatException, IOException {
		
		Attribute atts[] = new Attribute[d.DimNum()];
		
		// Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(2);
		 fvClassVal.addElement("positive");
		 fvClassVal.addElement("negative");
		 Attribute ClassAttribute = new Attribute("theClass", fvClassVal);
	    
	    fvWekaAttributes = new FastVector(d.DimNum() + 1);
	    for(int i=0; i<d.DimNum(); i++) {
	    	fvClassVal = new FastVector(2);
			fvClassVal.addElement("positive");
			fvClassVal.addElement("negative");
			 
	    	atts[i] = new Attribute("att"+i, fvClassVal);
	    	fvWekaAttributes.addElement(atts[i]);
	    }
	    
	    fvWekaAttributes.addElement(ClassAttribute);
	    
	    Instances  isTrainingSet = new Instances("Rel", fvWekaAttributes, d.DataSet().size()); 
	    isTrainingSet.setClassIndex(d.DimNum());
		
		for(int i=0; i<d.DataSet().size(); i++) {
			boolean sample[] = d.DataSet().get(i);
			boolean output = d.OutputSet().get(i)[0];
			
			Instance iExample = new Instance(d.DimNum() + 1);
			for(int j=0; j<d.DimNum(); j++) {
				boolean val = sample[j];
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(j), val == true ? "positive" : "negative");  
			}
			
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(d.DimNum()), output == true ? "positive" : "negative");     
			 
			// add the instance
			isTrainingSet.add(iExample);
		}
		
		Instances  isTestSet = new Instances("Rel", fvWekaAttributes, d.TestDataSet().size()); 
	    isTrainingSet.setClassIndex(d.DimNum());
		
		for(int i=0; i<d.TestDataSet().size(); i++) {
			boolean sample[] = d.TestDataSet().get(i);
			boolean output = d.TestOutputSet().get(i)[0];
			
			Instance iExample = new Instance(d.DimNum() + 1);
			for(int j=0; j<d.DimNum(); j++) {
				boolean val = sample[j];
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(j), val == true ? "positive" : "negative");  
			}
			
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(d.DimNum()), output == true ? "positive" : "negative");     
			 
			// add the instance
			isTestSet.add(iExample);
		}
		
		PrincipalComponents pca = new PrincipalComponents();
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(8);
        AttributeSelection selection = new AttributeSelection();
        selection.setEvaluator(pca);
        
        Normalize normalizer = new Normalize();
        try {
                normalizer.setInputFormat(isTrainingSet);
                isTrainingSet = Filter.useFilter(isTrainingSet, normalizer);
                isTestSet = Filter.useFilter(isTestSet, normalizer);
                
                selection.setSearch(ranker);
                selection.SelectAttributes(isTrainingSet);
                isTrainingSet = selection.reduceDimensionality(isTrainingSet);
                isTestSet = selection.reduceDimensionality(isTestSet);
        } catch (Exception e) {
                e.printStackTrace();
        }
        
        ArrayList<boolean []> data_set = new ArrayList<boolean []>();
        ArrayList<boolean []> output_set = new ArrayList<boolean []>();
        
        ArrayList<boolean []> test_data_set = new ArrayList<boolean []>();
        ArrayList<boolean []> test_output_set = new ArrayList<boolean []>();
        
        for(int i=0; i<d.DataSet().size(); i++) {
        	Instance inst = isTrainingSet.instance(i);
        	boolean out = d.OutputSet().get(i)[0];
        	
        	boolean sample[] = new boolean[8];
        	boolean output[] = new boolean[]{out};
        	output_set.add(output);
        	
        	double val[] = inst.toDoubleArray();
        	
        	for(int j=0; j<sample.length; j++) {
        		if(val[j] > 0) {
        			sample[j] = true;
        		} else {
        			sample[j] = false;
        		}
        	}
        	
        	data_set.add(sample);
        }
        
        for(int i=0; i<d.TestDataSet().size(); i++) {
        	Instance inst = isTestSet.instance(i);
        	boolean out = d.TestOutputSet().get(i)[0];
        	
        	boolean sample[] = new boolean[8];
        	boolean output[] = new boolean[]{out};
        	test_output_set.add(output);
        	
        	double val[] = inst.toDoubleArray();
        	
        	for(int j=0; j<sample.length; j++) {
        		if(val[j] > 0) {
        			sample[j] = true;
        		} else {
        			sample[j] = false;
        		}
        	}
        	
        	test_data_set.add(sample);
        }
        
        DataSet d2 = new DataSet(data_set, output_set, test_data_set, test_output_set);
        
        VectorMethod vm = new VectorMethod();
        float error = vm.FindClassError(d2, 0, 60);
        
        System.out.println("PCA Dim Red: "+error);
        return error;
	}
	
	// This creates and tests a given instance of the model
	private boolean TestInstance() throws NumberFormatException, IOException {
		
		DataSet d = new DataSet();
		
		int[] group = new int[d.DataSet().size()];
		double[][] data = new double[d.DataSet().size()][d.DimNum()];
		
		for(int i=0; i<d.DataSet().size(); i++) {
			boolean sample[] = d.DataSet().get(i);
			boolean output = d.OutputSet().get(i)[0];
			group[i] = (output == true) ? 1 : 2;
			
			for(int j=0; j<sample.length; j++) {
				data[i][j] = sample[j] == true ? 1.0f : 0.0f;
			}
		}

		LDA test = new LDA(data, group, true);
		
		float true_count = 0;
		double test_data[] = new double[d.DimNum()];
		for(int i=0; i<d.TestDataSet().size(); i++) {
			boolean sample[] = d.TestDataSet().get(i);
			boolean output = d.TestOutputSet().get(i)[0];
			
			for(int j=0; j<sample.length; j++) {
				test_data[j] = sample[j] == true ? 1.0f : 0.0f;
			}
			
			boolean val = test.predict(test_data) == 1 ? true : false; 
			if(val == output) {
				true_count++;
			}
		}
		
		true_count /= d.TestDataSet().size();
		
		if(true_count > 0.7f) {
			return false;
		}
		
		
		for(int i=0; i<d.DimNum(); i++) {
			new KDNode(i);
		}
		
		long prev_time = System.currentTimeMillis();
		ArrayList<DecisionNode> buff = KDNode.BuildTree(d);
		
		while(buff.size() > 1) {
			buff = KDNode.BuildTree(d, buff);
		}
		
		time1 = System.currentTimeMillis() - prev_time;
		
		prev_time = System.currentTimeMillis();
		double error = PCA(d);
		time2 = System.currentTimeMillis() - prev_time;
		
		System.out.println("LDA Method: "+true_count);
		System.out.println("Decomp Method: "+KDNode.best_class_error);
		
		comp_buff.add((float) (KDNode.best_class_error / error));
		
		time_buff1.add((float)time1);
		time_buff2.add((float)time2);
		
		return true;
	}
	
	public Main() throws Exception {

		int offset = 0;
		while(offset < 10) {
			if(TestInstance() == true) {
				offset++;
			}
		}
		
		float avg1 = 0;
		for(int i=0; i<comp_buff.size(); i++) {
			avg1 += comp_buff.get(i);
		}
		
		
		float var1 = 0;
		avg1 /= comp_buff.size();
		for(int i=0; i<comp_buff.size(); i++) {
			 var1 += (comp_buff.get(i) - avg1) * (comp_buff.get(i) - avg1);
		}
		
		var1 /= comp_buff.size();
	    var1 = (float) Math.sqrt(var1);
	    var1 /= Math.sqrt(comp_buff.size());
	    var1 *= 1.96;
	    
	    float t1 = 0;
	    for(int i=0; i<time_buff1.size(); i++) {
	    	t1 += time_buff1.get(i);
	    }
	    
	    float t2 = 0;
	    for(int i=0; i<time_buff2.size(); i++) {
	    	t2 += time_buff2.get(i);
	    }
	    
	    t1 /= time_buff1.size();
	    t2 /= time_buff1.size();
	    
	    System.out.println(avg1+" "+var1+" "+t1+" "+t2);
		
	}
	
		
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		new Main();
	}

}
