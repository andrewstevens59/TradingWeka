package ParticleFilter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;


import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;

public class ClassifyDynamics {
	
	// This stores the set of s-links
	class SLinkSet {
		// This stores the similarity measure
		float sim;
		// This stores the set of s-links
		SLink s_link;
	}

	// This stores the set of child classifiers
	private ArrayList<ClassifyDynamics> child_buff = new ArrayList<ClassifyDynamics>();
	// This stores the kd node 
	private KDNode knode;
	// This stores the set of ranked s-links
	private ArrayList<SLinkSet> s_link_buff = new ArrayList<SLinkSet>();
	// This stores the set of root nodes
	private static HashSet<ClassifyDynamics> root_map = new HashSet<ClassifyDynamics>();
	
	// This stores the set of class fitnesses
	private ArrayList<double []> train_buff = new ArrayList<double []>();
	// This stores the set of output values 
	private ArrayList<Integer> output_buff = new ArrayList<Integer>();
	// This stores the classifier
	private Classifier javamlsmo = null;
	
	// This stores the root classifier
	private static Classifier root_classify = null;
	// This stores the set of class fitnesses
	private static ArrayList<double []> root_train_buff = new ArrayList<double []>();
	// This stores the set of output values 
	private static ArrayList<Integer> root_output_buff = new ArrayList<Integer>();
	
	// This defines the maximum allowed number of s-links
	private static int MAX_S_LINK_NUM = 5;
	
	public ClassifyDynamics(KDNode k) {
		
		SLink s_link = k.s_links;
		while(s_link != null) {
			SLinkSet s = new SLinkSet();
			s.s_link = s_link;
			s_link_buff.add(s);
			
			s.sim = 0;
			float sum1 = 0;
			float sum2 = 0;
			for(int i=0; i<s_link.dst.ClassVect().length; i++) {
				s.sim += s_link.dst.ClassVect()[i] * k.ClassVect()[i];
				sum1 += s_link.dst.ClassVect()[i] * s_link.dst.ClassVect()[i];
				sum2 += k.ClassVect()[i] * k.ClassVect()[i];
			}
			
			sum1 /= Math.sqrt(sum1);
			sum2 /= Math.sqrt(sum2);
			
			if(Math.abs(sum1 - 1) > 0.000001 || Math.abs(sum2 - 1) > 0.000001) {
				System.out.println("error "+sum1+" "+sum2);System.exit(0);
			}
			
			s_link = s_link.next_ptr;
		}
		
		Collections.sort(s_link_buff, new Comparator<SLinkSet>() {
			 
	        public int compare(SLinkSet arg1, SLinkSet arg2) {
	        	
	        	if(arg1.sim < arg2.sim) {
	    			return -1;
	    		}

	    		if(arg1.sim > arg2.sim) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		knode = k;
		k.SetClassDynm(this);
		if(k.HasChildren() == false) {
			return;
		}
		
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		k.DecompressNode(2, buff);
		
		for(int i=0; i<buff.size(); i++) {
			child_buff.add(new ClassifyDynamics(buff.get(i)));
		}
	}

	// This returns the nominal value for a class output
	private static Integer NominalVal(Integer output) {
		
		switch(output) {
			case -2: return Classify.SELL_VAL;
			case -1: return Classify.STAY_NEG;
			case 1: return Classify.STAY_POS;
			case 2: return Classify.BUY_VAL;
		}
		
		System.out.println("errror nom");System.exit(0);
		return null;
	}
	
	// This returns a normalized weight buffer
	private static double[] WeightBuff(ArrayList<Float> weight_buff) {
		
		float min = Float.MAX_VALUE;
		for(int i=0; i<weight_buff.size(); i++) {
			min = Math.min(min, weight_buff.get(i));
		}
		
		min = Math.abs(min) + 1;
		
		float sum = 0;
		double buff[] = new double[weight_buff.size()];
		for(int i=0; i<weight_buff.size(); i++) {
			buff[i] = weight_buff.get(i) + min;
			sum += buff[i];
		}
		
		for(int i=0; i<weight_buff.size(); i++) {
			buff[i] /= sum;
		}
		
		return buff;
	}
	

	// This returns the output of the classifier
	public int Output() {
		
		if(javamlsmo == null) {
			return knode.class_label;
		}
		
		int offset = 0;
		int link_num = Math.min(s_link_buff.size(), MAX_S_LINK_NUM);
		double val[] = new double[link_num + link_num + child_buff.size() + 1];
		ArrayList<Float> weight_buff = new ArrayList<Float>();
		for(int i=0; i<link_num; i++) {
			SLink s_link = s_link_buff.get(i).s_link;
			Integer out = s_link.dst.CompNode().class_label;
			
			weight_buff.add(s_link.dst.CompNode().fitness);
			val[offset++] = out;
		}
		
		for(int i=0; i<child_buff.size(); i++) {
			Integer out = child_buff.get(i).Output();
			val[offset++] = out;
		}
		
		double weight[] = WeightBuff(weight_buff);
		for(int i=0; i<weight.length; i++) {
			val[offset++] = weight[i];
		}
		
		val[offset++] = knode.class_label;
		
		for(int i=0; i<val.length; i++) {
			System.out.print(val[i]+" ");
		}

		System.out.println("");
		
		

		try {
			return (Integer) javamlsmo.classify(new DenseInstance(val, Classify.BUY_VAL));
		} catch(net.sf.javaml.tools.weka.WekaException e) {
			javamlsmo = null;
		}
		
		return knode.class_label;
	}
	
	// This returns the current class output
	public static Integer ClassLabel(double sample[]) {
		
		if(root_classify == null) {
			return null;
		}
		
		ArrayList<Float> weight_buff = new ArrayList<Float>();
		for(ClassifyDynamics c : root_map) {
			c.AssignClassLabel(sample);
			weight_buff.add(c.knode.fitness);
		}
		
		int offset = 0;
		double val[] = new double[(root_map.size() << 1) + 1];
		for(ClassifyDynamics c : root_map) {
			val[offset++] = c.Output();
		}
		
		double weight[] = WeightBuff(weight_buff);
		for(int i=0; i<weight.length; i++) {
			val[offset++] = weight[i];
		}
		
		val[offset++] = SampleGraph.ClassLabel();
		
		try {
			Integer out = (Integer) root_classify.classify(new DenseInstance(val, Classify.BUY_VAL));
			System.out.println(out+"   (((((((((((((((((((((((((((((((((((((((((((((((((");
			return out;
		} catch(net.sf.javaml.tools.weka.WekaException e) {
			root_classify = null;
		}
		
		return null;
	}
	
	// This builds the hierarchy of classifiers
	public static void BuildHiearchy() {
		
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		for(KDNode k : KDNode.RootSet()) {
			buff.add(k);
		}
		
		for(int i=0; i<buff.size(); i++) {
			ClassifyDynamics c = new ClassifyDynamics(buff.get(i));
			root_map.add(c);
		}
		
		KDNode.Reset();
	}
	
	// This assigns the class label for each of the k-nodes in the hierarchy
	private void AssignClassLabel(double sample[]) {
		
		knode.ClassLabel(sample);
		knode.AssignFitness();
				
		for(int i=0; i<child_buff.size(); i++) {
			child_buff.get(i).AssignClassLabel(sample);
		}
	
	}
	
	// This appends to the training data based upon current population dynamics
	private void UpdateTrainingData(double sample[], int output) {
		
		int offset = 0;
		int link_num = Math.min(s_link_buff.size(), MAX_S_LINK_NUM);
		double val[] = new double[link_num + link_num + child_buff.size() + 1];
		ArrayList<Float> weight_buff = new ArrayList<Float>();
		for(int i=0; i<link_num; i++) {
			SLink s_link = s_link_buff.get(i).s_link;
			Integer out = s_link.dst.CompNode().class_label;
			
			weight_buff.add(s_link.dst.CompNode().fitness);
			val[offset++] = out;
		}
		
		for(int i=0; i<child_buff.size(); i++) {
			Integer out = child_buff.get(i).knode.class_label;
			val[offset++] = out;
		}
		
		double weight[] = WeightBuff(weight_buff);
		for(int i=0; i<weight.length; i++) {
			val[offset++] = weight[i];
		}
		
		val[offset++] = knode.class_label;
		
		train_buff.add(val);
		output_buff.add(output);
		
		for(int i=0; i<val.length; i++) {
			System.out.print(val[i]+" ");
		}

		System.out.println("    "+output+"   "+this);
		
		Dataset data = new DefaultDataset();
		HashSet<Integer> label_map = new HashSet<Integer>();
		for(int k=0; k<train_buff.size(); k++) {
			data.add(new DenseInstance(train_buff.get(k), NominalVal(output_buff.get(k))));
			label_map.add(output_buff.get(k));
		}
		
		if(label_map.size() < 2) {
			return;
		}
		
		try {
			SMO smo = new SMO();
			javamlsmo = new WekaClassifier(smo);
	        javamlsmo.buildClassifier(data);
		} catch(net.sf.javaml.tools.weka.WekaException e) {
			javamlsmo = null;
		}
	}
	
	// This updates the training set for each node
	public static void AddTrainingSample(double sample[], int output) {
		
		ArrayList<Float> weight_buff = new ArrayList<Float>();
		for(ClassifyDynamics c : root_map) {
			c.AssignClassLabel(sample);
			weight_buff.add(c.knode.fitness);
		}
		
		int offset = 0;
		double val[] = new double[(root_map.size() << 1) + 1];
		for(ClassifyDynamics c : root_map) {
			c.UpdateTrainingData(sample, output);
			val[offset++] = c.knode.class_label;
		}
		
		double weight[] = WeightBuff(weight_buff);
		for(int i=0; i<weight.length; i++) {
			val[offset++] = weight[i];
		}
		
		val[offset++] = SampleGraph.ClassLabel();
		
		for(int i=0; i<val.length; i++) {
			System.out.print(val[i]+" ");
		}

		System.out.println("    "+output+"   ");
		
		root_train_buff.add(val);
		root_output_buff.add(output);

		HashSet<Integer> label_map = new HashSet<Integer>();
		Dataset data = new DefaultDataset();
		for(int k=0; k<root_train_buff.size(); k++) {
			data.add(new DenseInstance(root_train_buff.get(k), NominalVal(root_output_buff.get(k))));
			label_map.add(root_output_buff.get(k));
		}
		
		if(label_map.size() < 2) {
			return;
		}
		
		try {
			SMO smo = new SMO();
			root_classify = new WekaClassifier(smo);
			root_classify.buildClassifier(data);
		} catch(net.sf.javaml.tools.weka.WekaException e) {
			root_classify = null;
		}
	}

}
