package MoveParticle;

import java.util.ArrayList;

import net.sf.javaml.core.Dataset;

public class TrainingSets {

	// This stores the training set for a particular currency pair
	public ArrayList<Train> train_buff = new ArrayList<Train>();
	// This stores the set of classifiers
	public ArrayList<Classify> class_buff = new ArrayList<Classify>();
	// This stores the set of data sets
	public Dataset data;
}
