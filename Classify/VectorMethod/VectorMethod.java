package VectorMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;
import DecisionNetwork.DataSet;
import DecisionNetwork.NaiveBayes;

public class VectorMethod {

	// This stores a classifier pair
	class ClassPair {
		DecisionNode node[] = new DecisionNode[10];
		float weight;
	}
	
	// This stores the set of distances to neighbouring vectors
	class Dist {
		float dist;
		DecisionNode n;
	}
	
	// This stores the set of decision nodes
	private ArrayList<DecisionNode> node_buff = new ArrayList<DecisionNode>();
	// This stores the set of feature vectors
	private HashSet<String> feat_vec_map = new HashSet<String>();
	// This stores the selection pool factor
	private float select_factor = 0.95f;
	
	// This stores the true value
	static Boolean TRUE_VAL = new Boolean(true);
	// This stores the true value
	static Boolean FALSE_VAL = new Boolean(false);
	// This stores the current optimal classifier
	private DecisionNode best_node;
	
	// This builds the initial network 
	private DecisionNode BuildNetwork(DataSet d, int start, int end) {
		
		if(end - start <= 1) {
			DecisionNode v = new DecisionNode(start, null);
			v.AssignBestRule(d, d.DataSet().size(), 10);
			v.AssignClassError(d, d.DataSet().size());
			
			node_buff.add(v);
			return v;
		}
		
		int middle = (start + end) >> 1;
		int middle1 = (start + middle) >> 1;
		int middle2 = (middle + end) >> 1;
		
		DecisionNode l1 = BuildNetwork(d, start, middle1);
		DecisionNode l2 = BuildNetwork(d, middle1, middle);
		
		DecisionNode r1 = BuildNetwork(d, middle, middle2);
		DecisionNode r2 = BuildNetwork(d, middle2, end);
		
		ArrayList<DecisionNode> child_buff = new ArrayList<DecisionNode>();
		child_buff.add(l1);
		child_buff.add(l2);
		
		child_buff.add(r1);
		child_buff.add(r2);
		
		DecisionNode v = new DecisionNode(0, child_buff);
		v.AssignBestRule(d, d.DataSet().size(), 10);
		v.AssignClassError(d, d.DataSet().size());
		node_buff.add(v);
		
		return v;
	}
	
	// This finds the k -closest nodes
	private ArrayList<Dist> KClosest(DataSet d, DecisionNode root) {
		
		ArrayList<Dist> dist_buff = new ArrayList<Dist>();
		for(int i=0; i<node_buff.size(); i++) {
			
			float dist = 0;
			for(int l=0; l<root.StandardFeatVec().length; l++) {
				float delta = node_buff.get(i).StandardFeatVec()[l] - root.StandardFeatVec()[l];
				dist +=  delta * delta;
			}
			
			dist /= d.DimNum();
			dist = (float) Math.sqrt(dist);
			
			Dist d1 = new Dist();
			d1.dist = dist;
			d1.n = node_buff.get(i);
			dist_buff.add(d1);
		}
		
		Collections.sort(dist_buff, new Comparator<Dist>() {
			 
	        public int compare(Dist arg1, Dist arg2) {
	        	
	        	if(arg1.dist < arg2.dist) {
	    			return -1;
	    		}

	    		if(arg1.dist > arg2.dist) {
	    			return 1;
	    		}

	    		return 0; 
	        }
	    });
		
		return dist_buff;
	}
	
	// This creates the pair map
	private void CreatePairMap(DataSet d, ArrayList<ClassPair> pair_buff, ArrayList<Dist> neighbour) {
		
		Random r = new Random();
		for(int i=0; i<4000; i++) {
				
			ClassPair p = new ClassPair();
			for(int j=0; j<p.node.length; j++) {
				int id = r.nextInt(Math.min(neighbour.size(), neighbour.size()));
				p.node[j] = neighbour.get(id).n;
			}
			
			String str = new String();
			float mid_weight[] = new float[p.node[0].StandardFeatVec().length];
			for(int k=0; k<mid_weight.length; k++) {
				
				mid_weight[k] = 0;
				for(int j=0; j<p.node.length; j++) {
					mid_weight[k] += p.node[j].StandardFeatVec()[k];
				}
				
				mid_weight[k] /= p.node.length;
				str += Integer.toString((int)(mid_weight[k] * 1000)) + " ";
			}
			
			if(feat_vec_map.contains(str) == true) {
				continue;
			}
			
			ArrayList<Dist> dist_buff = new ArrayList<Dist>();
			for(int k=0; k<neighbour.size(); k++) {
				
				float dist = 0;
				for(int l=0; l<mid_weight.length; l++) {
					float delta = neighbour.get(k).n.StandardFeatVec()[l] - mid_weight[l];
					dist +=  delta * delta;
				}
				
				dist /= d.DimNum();
				dist = (float) Math.sqrt(dist);
				
				if(dist > 0.01f) {
					Dist d1 = new Dist();
					d1.dist = dist;
					d1.n = neighbour.get(k).n;
					dist_buff.add(d1);
				}
			}
			
			Collections.sort(dist_buff, new Comparator<Dist>() {
				 
		        public int compare(Dist arg1, Dist arg2) {
		        	
		        	if(arg1.dist < arg2.dist) {
		    			return -1;
		    		}

		    		if(arg1.dist > arg2.dist) {
		    			return 1;
		    		}

		    		return 0; 
		        }
		    });
			
			p.weight = 0;
			float sum = 0;
			for(int k=0; k<Math.min(dist_buff.size(), 3); k++) {
				sum += dist_buff.get(k).dist;
			}
			
			for(int k=0; k<Math.min(dist_buff.size(), 3); k++) {
				p.weight += dist_buff.get(k).n.class_error / (dist_buff.get(k).dist / sum);
			}
			
			pair_buff.add(p);
		}
	}
	
	// This creates the next classifier
	private float CreateClassifier(DataSet d, int it_num, int cycle_num) {
		
		float max = 0;
		best_node = null;
		for(int i=0; i<node_buff.size(); i++) {
			float error = node_buff.get(i).class_error;
			if(error >= max) {
				max = error;
				best_node = node_buff.get(i);
			}
		}
		
		float true_count1 = 0;
		for(int i=0; i<d.TestDataSet().size(); i++) {
			boolean sample[] = d.TestDataSet().get(i);
			boolean output = d.TestOutputSet().get(i)[0];
			boolean val = best_node.Output(sample);
			if(val == output) {
				true_count1++;
			}
		}
		
		true_count1 /= d.TestDataSet().size();
		System.out.println(best_node+" "+true_count1+" "+max+" "+best_node.class_error+"     "+it_num+"   "+cycle_num);
		
		ArrayList<Dist> neighbour = KClosest(d, best_node);
		ArrayList<ClassPair> pair_buff = new ArrayList<ClassPair>();
		CreatePairMap(d, pair_buff, neighbour);
		
		if(pair_buff.size() == 0) {
			return true_count1;
		}
		
		Collections.sort(pair_buff, new Comparator<ClassPair>() {
			 
	        public int compare(ClassPair arg1, ClassPair arg2) {
	        	
	        	if(arg1.weight < arg2.weight) {
	    			return 1;
	    		}

	    		if(arg1.weight > arg2.weight) {
	    			return -1;
	    		}

	    		return 0; 
	        }
	    });
		
		Random r = new Random();
		int id = -(int) (Math.log(1-Math.random()) * pair_buff.size() * select_factor);
		id = Math.min(pair_buff.size() - 1, id);
		
		select_factor *= 0.95f;
		select_factor = Math.max(select_factor, 0.05f);
		
		ArrayList<DecisionNode> child_buff = new ArrayList<DecisionNode>();
		for(int i=0; i<pair_buff.get(id).node.length; i++) {
			child_buff.add(pair_buff.get(id).node[i]);
		}
		
		DecisionNode d2 = new DecisionNode(0, child_buff);
		
		String str = new String();
		for(int k=0; k<d2.StandardFeatVec().length; k++) {
			str += Integer.toString((int)(d2.StandardFeatVec()[k] * 1000)) + " ";
		}
		System.out.println(str);
		
		feat_vec_map.add(str);
		DecisionNode.IncOutputAssignID();
		d2.AssignBestRule(d, d.DataSet().size(), 10);
		d2.AssignClassError(d, d.DataSet().size());
		
		int true_count = 0;
		int false_count = 0;
		for(int i=0; i<d.DataSet().size(); i++) {
			boolean sample[] = d.DataSet().get(i);
			boolean val = d2.Output(sample);
			if(val == true) {
				true_count++;
			} else {
				false_count++;
			}
		}
		
		float ratio = (float)Math.min(true_count, false_count) / Math.max(true_count, false_count);
		System.out.println("ratio:  "+ratio);
		
		if(ratio > 0.01f) {
			node_buff.add(d2);
			//d2.PrintRule();
			d2.AssignClassError(d, d.DataSet().size());
		}
		
		return true_count1;
	}
	
	// This finds the set of classification errors
	public float FindClassError(DataSet d, int cycle_num, int it_num) throws NumberFormatException, IOException {
		
		KDNode.BuildTree(d);
		
		for(int i=0; i<KDNode.DimNum(); i++) {
			DecisionNode v5 = new DecisionNode(i, null);
			v5.AssignBestRule(d, d.DataSet().size(), 10);
			v5.AssignClassError(d, d.DataSet().size());
			node_buff.add(v5);
		}

		DecisionNode.IncOutputAssignID();
		DecisionNode v5 = BuildNetwork(d, 0, KDNode.DimNum());
		v5.AssignBestRule(d, d.DataSet().size(), 10);
		v5.AssignClassError(d, d.DataSet().size());
		node_buff.add(v5);
		
		Random r = new Random();
		for(int i=0; i<20; i++) {
			
			ArrayList<DecisionNode> child_buff = new ArrayList<DecisionNode>();
			for(int j=0; j<2; j++) {
				child_buff.add(node_buff.get(r.nextInt(node_buff.size())));
			}
			
			v5 = new DecisionNode(0, child_buff);
			v5.AssignBestRule(d, d.DataSet().size(), 10);
			v5.AssignClassError(d, d.DataSet().size());
			node_buff.add(v5);
		}

		float error = 0;
		for(int i=0; i<it_num; i++) {
			error = CreateClassifier(d, i, cycle_num);
		}
		
		return error;
	}
	
	// This is used to classify a sample
	public boolean Classify(boolean sample[]) {
		return best_node.Output(sample);
	}

}
