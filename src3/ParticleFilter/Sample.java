package ParticleFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class Sample {

	// This stores the sample
	public float class_weight[];
	// This stores the weight of the sample based on posterior
	public float weight = 1;
	// This stores the normalized weight
	public float norm_weight = 0;
	// This stores the sample space for the sample
	public SampleSpace space;
	
	public Sample(SampleSpace space) {
		this.space = space;
	}
	
	
	// This generates a random sample
	public void GenerateSample(int dim_num) {
		class_weight = new float[dim_num];
		
		float sum = 0;
		for(int i=0; i<class_weight.length; i++) {
			class_weight[i] = (float) Math.random();
			sum += class_weight[i];
		}
		
		for(int i=0; i<class_weight.length; i++) {
			class_weight[i] /= sum;
		}
	}
	
	// This classifies a sample
	public int Classify(double sample[]) {
		
		float class_count[] = new float[]{0, 0, 0, 0};
		for(int i=0; i<space.kdnode_dim.size(); i++) {
			KDNodeDim dim = space.kdnode_dim.get(i);
			int label =  dim.kdnode.Output(sample);
			class_count[label] += class_weight[dim.dim];
		}
		
		float max = 0;
		int label_set[] = new int[]{-2, -1, 1, 2};
		int label = 0;
		for(int i=0; i<4; i++) {
			
			if(class_count[i] > max) {
				max = class_count[i];
				label = label_set[i];
			}
		}  
		
		return label;
	}
	
	// This updates the sample weight based upon its prediction accuracy
	public void UpdateSampleWeight(double sample[], int output) {
		
		int label = Classify(sample);
		
		if(output == 2 && label == -2) {
			weight = weight * 0.95f - 1.0f;
		}
		
		if(output == -2 && label == 2) {
			weight = weight * 0.95f - 1.0f;
		}
		
		if(output == 2 && label == 2) {
			weight = weight * 0.95f + 1.0f;
		}
		
		if(output == -2 && label == -2) {
			weight = weight * 0.95f + 1.0f;
		}
	}
	
	// This generates a new sample by perturbing an existing sample
	public void GenerateSample(Sample s, ArrayList<double []> train_buff,
			ArrayList<Integer> output_buff) {
		
		if(s != null) {
			weight = s.weight;
			class_weight = new float[s.class_weight.length];
			
			for(int i=0; i<class_weight.length; i++) {
				class_weight[i] = s.class_weight[i];
			}
		}

		for(int i=0; i<space.k_neighbour.size(); i++) {
			// only permute dimensions that belong to the current subspace
			KDNodeDim dim = space.k_neighbour.get(i);
			class_weight[dim.dim] = (float) Math.random() - 0.5f;
		}
		
		float len = 0;
		for(int i=0; i<space.k_neighbour.size(); i++) {
			KDNodeDim dim = space.k_neighbour.get(i);
			len += class_weight[dim.dim] * class_weight[dim.dim];
		}
		
		len = (float) Math.sqrt(len);
		
		float sum = 0;
		HashSet<KDNode> kneighbour_map = new HashSet<KDNode>();
		for(int i=0; i<space.k_neighbour.size(); i++) {
			KDNodeDim dim = space.k_neighbour.get(i);
			class_weight[dim.dim] /= len;
			class_weight[dim.dim] += 4;
			
			if(s != null) {
				class_weight[dim.dim] += s.class_weight[dim.dim];
			}
			
			sum += class_weight[dim.dim];
			kneighbour_map.add(dim.kdnode);
		}
		
		for(int i=0; i<space.k_neighbour.size(); i++) {
			KDNodeDim dim = space.k_neighbour.get(i);
			class_weight[dim.dim] /= sum;
		}
		
		sum = 0;
		for(int i=0; i<space.kdnode_dim.size(); i++) {
			KDNodeDim dim = space.kdnode_dim.get(i);
			if(kneighbour_map.contains(dim.kdnode) == false) {
				sum += class_weight[dim.dim];
			}
		}

		for(int i=0; i<space.kdnode_dim.size(); i++) {
			KDNodeDim dim = space.kdnode_dim.get(i);
			if(kneighbour_map.contains(dim.kdnode) == false) {
				class_weight[dim.dim] /= sum;
			}
		}
		
		for(int i=Math.max(train_buff.size()-3, 0); i<train_buff.size(); i++) {
			UpdateSampleWeight(train_buff.get(i), output_buff.get(i));
		}
	}
	
	// This adds the sample to the set of k neighbours
	public void AddToKNeighbours(int max_num) {
		
		for(int i=0; i<space.k_neighbour.size(); i++) {
			KDNodeDim dim = space.k_neighbour.get(i);
			dim.kdnode.AddSample(this, max_num, dim.dim);
		}
	}
}
