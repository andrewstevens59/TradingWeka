package BitcoinExchange;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

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

public class Classify {
	
	// This stores the true value
	static Integer BUY_VAL = new Integer(0);
	// This stores the true value
	static Integer SELL_VAL = new Integer(1);
	// This stores the true value
	static Integer STAY_VAL = new Integer(2);
	// This defines the smallest time step
	static private long TIME_STEP = 3000;
	// This stores the classifier
	Classifier javamlsmo = null;

	public Classify(TimeSeries s1, TimeSeries s2) throws FileNotFoundException, IOException {
		

		long start_time = Math.max(s1.StartTime(), s2.StartTime());
		long end_time = Math.min(s1.EndTime(), s2.EndTime());
		
		Dataset data = new DefaultDataset();
		long start = start_time;
		while(start < end_time - (TIME_STEP * 5)) {
			
			double grad1[] = new double[4];
			double grad2[] = new double[4];
			long end = start + (TIME_STEP * grad1.length);
			float start_price = s2.Price(start);
			
			for(int i=0; i<grad1.length; i++) {
				
				grad1[i] = s1.PriceGrad(start, end);
				grad2[i] = s2.PriceGrad(start, end);
				start += TIME_STEP;
			}
			
			ArrayList<Double> val_buff = new ArrayList<Double>();
			for(int i=0; i<grad1.length; i++) {
				for(int j=i; j<grad1.length; j++) {
					val_buff.add(grad1[i] - grad2[j]);
				}
			}
			
			double val[] = new double[val_buff.size()];
			for(int i=0; i<val_buff.size(); i++) {
				val[i] = val_buff.get(i);
			}
			
			
			float diff = s2.Price(start) - start_price;
			System.out.println(diff);
			
			if(diff > 2) {
				data.add(new DenseInstance(val, BUY_VAL));
			} else if(diff < -2) {
				data.add(new DenseInstance(val, SELL_VAL));
			} else {
				data.add(new DenseInstance(val, STAY_VAL));
			}
			
		}
		
		SMO smo = new SMO();
		javamlsmo = new WekaClassifier(smo);
        javamlsmo.buildClassifier(data);
        
	}

}