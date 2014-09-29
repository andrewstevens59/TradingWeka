package MoveParticle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class Sample {
	
	// This stores the N most recent scores
	class NScore {
		float score;
		NScore prev_ptr = null;
		NScore next_ptr = null;
	}

	// This stores the number of root scores
	public int score_num = 0;
	// This stores the most recent of the N scores
	public NScore head_score = null;
	// This stores the least recent of the N scores
	public NScore tail_score = null;
	// This stores the sample
	public float class_weight[];
	// This stores the motion vector
	public float motion_vect[];
	// This stores the weight of the sample based on posterior
	public float weight = 0;
	// This stores the normalized weight
	public float norm_weight = 0;
	// This stores the sample space for the sample
	public SampleSpace space;
	
	public Sample(SampleSpace space) {
		this.space = space;
	}
	
	// This assigns the motion vector for the particle's current position
	public void AssignMotionVect() {
		
		HashMap<KDNode, Integer> node_space = new HashMap<KDNode, Integer>();
		ArrayList<KDNodeDim> set = space.kdnode_dim;
		
		for(int i=0; i<set.size(); i++) {
			node_space.put(set.get(i).kdnode, set.get(i).dim);
		}
		
		motion_vect = new float[node_space.size()];
		for(int i=0; i<set.size(); i++) {
			motion_vect[i] = 0;
		}
		
		for(int i=0; i<set.size(); i++) {

			float w[] = set.get(i).kdnode.TransProb(space);
			if(w == null) {
				continue;
			}
			
			float dot = 0;
			for(int j=0; j<class_weight.length; j++) {
				dot += w[j] * class_weight[j];
			}
			
			for(int j=0; j<set.size(); j++) {
				motion_vect[j] += w[j] * dot;
			}
		}
		
		float len = 0;
		for(int i=0; i<set.size(); i++) {
			len += motion_vect[i];
		}
		
		if(len == 0) {
			return;
		}
		
		NormVect(motion_vect);
	}
	
	// This normalizes a vector
	public static void NormVect(float vect[]) {
		
		float len = 0;
		for(int j=0; j<vect.length; j++) {
			len += vect[j] * vect[j];
		}
		
		
		len = (float) Math.sqrt(len);
		
		for(int j=0; j<vect.length; j++) {
			vect[j] /= len;
		}
	}
	
	public static void NormVect(double vect[]) {
		
		float len = 0;
		for(int j=0; j<vect.length; j++) {
			len += vect[j] * vect[j];
		}
		
		
		len = (float) Math.sqrt(len);
		
		for(int j=0; j<vect.length; j++) {
			vect[j] /= len;
		}
	}
	
	// This generates a random sample
	public void GenerateSample(int dim_num) {
		
		class_weight = new float[dim_num];
			
		for(int i=0; i<class_weight.length; i++) {
			class_weight[i] = (float) Math.random();
		}
		
		NormVect(class_weight);
		
		for(int i=0; i<space.kdnode_dim.size(); i++) {
			class_weight[i] *= space.kdnode_dim.get(i).weight;
		}
		
		NormVect(class_weight);

		AssignMotionVect();
	}
	
	// This classifies a sample
	public int Classify(double sample[]) {
		return Classify(sample, class_weight);
	}
	
	// This classifies a sample
	public int Classify(double sample[], float vect[]) {
		
		float class_count[] = new float[]{0, 0, 0, 0};
		for(int i=0; i<space.kdnode_dim.size(); i++) {
			KDNodeDim dim = space.kdnode_dim.get(i);
			int label =  dim.kdnode.Output(sample);
			class_count[label] += vect[dim.dim];
		}
		
		float max = -Float.MAX_VALUE;
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
	
	// This returns the weight of the sample
	public float Weight() {
		return weight / score_num;
	}
	
	// This adds a new score to the set
	private void AddScore(float score) {
		
		NScore prev_ptr = head_score;
		head_score = new NScore();
		head_score.next_ptr = prev_ptr;
		head_score.score = score;
		
		if(prev_ptr != null) {
			prev_ptr.prev_ptr = head_score;
		}
		
		score_num++;
		if(tail_score == null) {
			tail_score = head_score;
		}
		
		weight += score;
		
		if(score_num >= space.sample_history) {
			weight -= tail_score.score;
			tail_score.prev_ptr.next_ptr = null;
			tail_score = tail_score.prev_ptr;
			score_num--;
		}
	}
	
	// This copies the util history from another sample
	public void CopySampleHistory(Sample s) {
		
		NScore ptr = s.head_score;
		for(int i=0; i<space.sample_history; i++) {
			if(ptr == null) {
				break;
			}
			
			AddScore(ptr.score);
			ptr = ptr.next_ptr;
		}
		
	}
	
	// This updates the sample weight based upon its prediction accuracy
	public void UpdateSampleWeight(double sample[], int output, 
			ArrayList<double []> train_buff, ArrayList<Integer> output_buff) {
		
		int label = Classify(sample, class_weight);
		
		float score = 0;
		if(output == 2 && label == 2) {
			score = 5;
		} else if(output == -2 && label == -2) {
			score = 5;
		} else if(output == label) {
			score = 2;
		} else if(output > 0 && label > 0) {
			score = 1;
		} else if(output < 0 && label < 0) {
			score = 1;
		}
		
		AddScore(score);

		for(int i=0; i<class_weight.length; i++) {
			class_weight[i] += motion_vect[i] * 0.1f;
		}

		NormVect(class_weight);
		
		AssignMotionVect();
	}
}
