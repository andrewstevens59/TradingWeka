package ParticleFilter;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;


import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;
import weka.core.Debug;
import weka.core.Instance;

public class Classify {
	
	// This stores the true value
	static public Integer BUY_VAL = new Integer(2);
	// This stores the true value
	static public Integer SELL_VAL = new Integer(-2);
	// This stores the true value
	static public Integer STAY_NEG = new Integer(-1);
	// This stores the true value
	static public Integer STAY_POS = new Integer(1);
	// This stores the classifier
	private Classifier javamlsmo = null;
	// This stores the dim num
	static private int dim_num;
	
	// This returns the number of dimensions
	static public int DimNum() {
		return dim_num;
	}
	
	// This returns a set of training instances 
	public static Dataset TrainingInstances(ArrayList<Train> train_buff, int start, int end) {
		
		Dataset data = new DefaultDataset();
		for(int k=start; k<end; k++) {
			
			Train t = train_buff.get(k);
			ArrayList<Float> val_buff = new ArrayList<Float>();
			for(int i=0; i<t.vol_diff.length; i++) {
				for(int j=i; j<t.vol_diff.length; j++) {
					val_buff.add(t.vol_diff[i][j]);
				}
			}
			
			for(int i=0; i<t.btc_vol_diff.length; i++) {
				for(int j=i+1; j<t.btc_vol_diff.length; j++) {
					val_buff.add(t.btc_vol_diff[i][j]);
				}
			}
			
			for(int i=0; i<t.usd_vol_diff.length; i++) {
				for(int j=i+1; j<t.usd_vol_diff.length; j++) {
					val_buff.add(t.usd_vol_diff[i][j]);
				}
			}
			
			for(int i=0; i<t.price_diff.length; i++) {
				for(int j=i; j<t.price_diff.length; j++) {
					val_buff.add(t.price_diff[i][j]);
				}
			}
			
			double val[] = new double[val_buff.size()];
			for(int i=0; i<val_buff.size(); i++) {
				val[i] = val_buff.get(i);
			}
			
			dim_num = val.length;
			
			float diff = t.price_jump;
			
			if(diff > 0.5) {
				data.add(new DenseInstance(val, BUY_VAL));
			} else if(diff < -0.5) {
				data.add(new DenseInstance(val, SELL_VAL));
			} else if(diff > 0) { 
				data.add(new DenseInstance(val, STAY_POS));
			} else {
				data.add(new DenseInstance(val, STAY_NEG));
			}
			
		}
		
		return data;
	}

	public Classify(ArrayList<Train> train_buff, int start, int end) throws FileNotFoundException, IOException {
		
		Dataset data = TrainingInstances(train_buff, start, end);
		
		System.out.println(data.size());
		SMO smo = new SMO();
		javamlsmo = new WekaClassifier(smo);
        javamlsmo.buildClassifier(data);
        
        int true_num = 0;
        float true_count = 0;
        for(int i=0; i<data.size(); i++) {
	        int offset = 0;
			int output = (Integer)data.instance(i).classValue();
			Set<Entry<Integer, Double>> s = data.instance(i).entrySet();
			double sample[] = new double[Classify.DimNum()];
			
			if(Math.abs(output) > 1) {
				
				if(output == 2 && Output(sample) == -2) {
					true_num--;
				}
				
				if(output == -2 && Output(sample) == 2) {
					true_num--;
				}
				
				if(output == 2 && Output(sample) == 2) {
					true_num++;
				}
				
				if(output == -2 && Output(sample) == -2) {
					true_num++;
				}
			}
			
			for(Entry<Integer, Double> val : s) {
				sample[offset++] = val.getValue();
			}
			
			if(output == Output(sample)) {
				true_count++;
			}
        }
        
        true_count /= data.size();
        System.out.println(true_count+ " "+true_num+"  ****");
	}
	
	// This returns the output of the classifier
	public int Output(double val[]) {
		
		if(val.length != dim_num) {
			System.out.println("dim mis "+val.length+" "+dim_num);System.exit(0);
		}
		
		return (Integer)javamlsmo.classify(new DenseInstance(val, STAY_NEG));
	}

}