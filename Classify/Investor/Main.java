package Investor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;

import DecisionNetwork.DataSet;
import DecisionNetwork.DecisionNode;
import DecisionNetwork.KDNode;
import DecisionNetwork.Voter;

public class Main {
	
	// This stores the set of decision nodes
	private ArrayList<Voter> node_buff = new ArrayList<Voter>();
	
	// This creates a new classifier 
	private Voter  CreateNewClassifier(DataSet d, Set<Voter> map, Voter v, int num) {
		
		if(num == 0) {
			return v;
		}
		
		map.add(v);
		Random r = new Random();
		int corr[] = new int[Voter.RootNodeNum()];
		for(int i=0; i<Voter.RootNodeNum(); i++) {
			corr[i] = 0;
		}
		
		for(int i=0; i<d.DataSet().size(); i++) {
		
			int id = i;//r.nextInt(d.DataSet().size());
			boolean sample[] = d.DataSet().get(id);
			
			int offset = 0;
			DecisionNode.IncOutputAssignID();
			boolean val = v.Output(sample);
			for(Voter v1 : Voter.RootNode()) {
				boolean val2 = v1.Output(sample);
				if(val != val2) {
					corr[offset]++;
				}
				
				offset++;
			}
		}
		
		Voter v2 = null;
		int max = 0;
		int offset = 0;
		for(Voter v1 : Voter.RootNode()) {
			if(corr[offset] > max && map.contains(v1) == false) {
				max = corr[offset];
				v2 = v1;
			}
			
			offset++;
		}
		
		map.add(v2);
		
		ArrayList<Voter> child_buff = new ArrayList<Voter>();
		for(Voter v1 : map) {
			child_buff.add(v1);
		}
		
		Voter v3 = new Voter(new DecisionNode(0, child_buff));
		
		DecisionNode.IncOutputAssignID();
		v3.node.AssignBestRule(d, 1000, 5);
		
		return CreateNewClassifier(d, map, v3, num - 1);
	}
	
	// This finds an SVM
	private void SVM( DataSet d) throws IOException {
		
		 /* Load data */
        Dataset data = FileHandler.loadDataset(new File("test.arff"), d.DimNum(), ",");
        /* Create Weka classifier */
        SMO smo = new SMO();
        /* Wrap Weka classifier in bridge */
        Classifier javamlsmo = new WekaClassifier(smo);
        /* Initialize cross-validation */
        CrossValidation cv = new CrossValidation(javamlsmo);
        /* Perform cross-validation */
        Map<Object, PerformanceMeasure> pm = cv.crossValidation(data);

        /* Output results */
        System.out.println(pm);
	}

	
	// This builds the initial network 
	private Voter BuildNetwork(int start, int end) {
		
		if(end - start <= 1) {
			Voter v = new Voter(new DecisionNode(start, null));
			node_buff.add(v);
			return v;
		}
		
		int middle = (start + end) >> 1;
		int middle1 = (start + middle) >> 1;
		int middle2 = (middle + end) >> 1;
		
		Voter l1 = BuildNetwork(start, middle1);
		Voter l2 = BuildNetwork(middle1, middle);
		
		Voter r1 = BuildNetwork(middle, middle2);
		Voter r2 = BuildNetwork(middle2, end);
		
		ArrayList<Voter> child_buff = new ArrayList<Voter>();
		child_buff.add(l1);
		child_buff.add(l2);
		
		child_buff.add(r1);
		child_buff.add(r2);
		
		Voter v = new Voter(new DecisionNode(0, child_buff));
		node_buff.add(v);
		
		return v;
	}
	
	// This creates a set of classifiers for the current dimension set
	private float[] CreateClassifier(DataSet d) {
		
		Voter best_node = null;
		node_buff = new ArrayList<Voter>();
		DecisionNode.IncOutputAssignID();
		Voter v5 = BuildNetwork(0, KDNode.DimNum());
		v5.node.AssignBestRule(d, 10000, 10);
		node_buff.add(v5);
		 
		Random r = new Random();
		for(int i=0; i<KDNode.DimNum(); i++) {
			node_buff.add(new Voter(new DecisionNode(i, null)));
		}
		
		// create 100 investors
		for(int i=0; i<2 * KDNode.DimNum(); i++) {
			int id = r.nextInt(node_buff.size());
			new Investor(KDNode.DimNum(), node_buff.get(id), r.nextInt(4));
		}
		
		for(int k=0; k<20; k++) {
			
			Investor inv = Investor.ChooseInvestor();
			
			Set<Voter> map = new HashSet<Voter>();
			ArrayList<Voter> child_buff = new ArrayList<Voter>();
			while(child_buff.size() < 4) {
				Voter v = inv.ChooseClassifier(Voter.RootNode());
				if(map.contains(v) == false) {
					child_buff.add(v);
				}
			}

			//Voter v3 = inv.ChooseClassifier(Voter.RootNode());
			//Voter v = CreateNewClassifier(d, map, v3, 4);
			Voter v = new Voter(new DecisionNode(0, child_buff));
			node_buff.add(v);
			
			for(int i=0; i<2; i++) {
				new Investor(KDNode.DimNum(), v, r.nextInt(4));
			}
			
			ArrayList<Voter> buff = new ArrayList<Voter>();
			for(Voter v1 : Voter.RootNode()) {
				buff.add(v1);
			}
			
			Collections.sort(buff, new Comparator<Voter>() {
				 
		        public int compare(Voter arg1, Voter arg2) {
		        	
		        	if(arg1.Utility() < arg2.Utility()) {
		    			return 1;
		    		}

		    		if(arg1.Utility() > arg2.Utility()) {
		    			return -1;
		    		}

		    		return 0; 
		        }
		    });
			
			best_node = buff.get(0);
			System.out.println(buff.get(0).Utility());
			System.out.println(buff.get(buff.size()-1).Utility());
			System.out.println(buff.get(0).InvestorNum()+" " +Investor.InvestorNum());
			
			for(Voter v1 : Voter.RootNode()) {
				System.out.print(v1.InvestorNum()+" ");
			}
			
			System.out.println("");
			for(int i=0; i<buff.get(0).node.feat_weight.length; i++) {
				System.out.print(buff.get(0).node.feat_weight[i]+" ");
				
			}
			System.out.println("\n");
			DecisionNode.IncOutputAssignID();
			
			int dec = r.nextInt(1);
			if(dec == 0) {
				v.node.AssignBestRule(d, d.DataSet().size(), 5);
			} else {
				v.node.CreateTable(r.nextInt(1 << 4), 1 << 4);
			}
			
			for(int j=0; j<node_buff.size(); j++) {
				node_buff.get(j).util = 0;
				node_buff.get(j).age = 1;
			}
			
			for(int i=0; i<d.DataSet().size(); i++) {
				
				int id = i;//r.nextInt(d.DataSet().size());
				boolean sample[] = d.DataSet().get(id);
				boolean output = d.OutputSet().get(id)[0];
				DecisionNode.IncOutputAssignID();
				
				for(int j=0; j<node_buff.size(); j++) {
					node_buff.get(j).UpdateUtility(sample, output);
				}
				
				for(Investor j : Investor.InvestorSet()) {
					j.UpdateUtil(sample, output);
				}
			}
			
			for(Investor i : Investor.InvestorSet()) {
				i.UpdateInvestorClassifier(Voter.RootNode());
			}

			// update the investor set
			for(int i=0; i<10; i++) {
				Investor.KillInvestor();
				int id = r.nextInt(node_buff.size());
				new Investor(KDNode.DimNum(), node_buff.get(id), r.nextInt(4));
			}
			
			/*while(Voter.RootNodeNum() > 1000) {
				Voter.KillClassifier();
			}*/
			
		}
		
		return best_node.FeatureWeight();
	}
	
	public Main() throws NumberFormatException, IOException {
		 DataSet d = new DataSet();
		 
		 //SVM(d);System.in.read();
		 DecisionNode.SetTraining(d, 1000);
		 

		 for(int i=0; i<100; i++) {
			// Voter.ResetVoter();
			// Investor.ResetInvestor();
			 
			 float weight[] = CreateClassifier(d);
			 KDNode.UpdateCompression(weight);
		 }
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		new Main();

	}

}
