package SVM;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.tools.weka.WekaClassifier;
import weka.classifiers.functions.SMO;

import DecisionNetwork.DataSet;

public class SVMNode {
	
	// This stores the input id
	private int input_id;
	// This stores the set of children
	private ArrayList<SVMNode> child_buff;
	// This stores the classifier 
	private Classifier javamlsmo;
	// This stores the final output
	private boolean is_output = true;

	public SVMNode(int input_id, ArrayList<SVMNode> child_buff) {
		
		this.input_id = input_id;
		this.child_buff = child_buff;
		
		if(child_buff != null) {
			for(int i=0; i<child_buff.size(); i++) {
				child_buff.get(i).is_output = false;
			}
		}
	}
	
	// This creates the output label 
	public double Output(boolean sample[]) {
		
		if(child_buff == null) {
			return sample[input_id] == true ? 1.0f : 0.0f;
		}
		
		double input[] = new double[child_buff.size()];
		for(int i=0; i<child_buff.size(); i++) {
			
			input[i] = child_buff.get(i).Output(sample);
		}
		
		Instance io = new DenseInstance(input);
		javamlsmo.classify(io);
		Double output = (Double) javamlsmo.classify(io);
		return output;
	}
	
	// This trains the SVM 
	public void Train(DataSet d) throws IOException {
		
		if(child_buff == null) {
			return;
		}
		
		for(int i=0; i<child_buff.size(); i++) {
			child_buff.get(i).Train(d);
		}
	
		PrintWriter out = new PrintWriter("test.set");
		
		for(int j=0; j<d.DataSet().size(); j++) {
			
			boolean sample[] = d.DataSet().get(j);
			boolean output = d.OutputSet().get(j)[0];
			
			int bit_depth = 32 / child_buff.size();
			
			int val = 0;
			int offset = 0;
			String str = new String();
			int input[] = new int[child_buff.size()];
			for(int i=0; i<child_buff.size(); i++) {
				input[i] = (int)child_buff.get(i).Output(sample);
				str += Integer.toString(input[i]) + " ";
				val |= input[i] << offset;
				offset += bit_depth;
			}
			
			str += Integer.toString(val);
			
			/*if(is_output == false) {
				str += Integer.toString(val);
			} else {
				if(output == true) {
					str += "1";
				} else {
					str += "0";
				}
			}*/
			
			out.println(str);
		}
		
		//Close the output stream
        out.close();
		
		Dataset data = FileHandler.loadDataset(new File("test.set"), child_buff.size(), " ");
        /* Create Weka classifier */
        SMO smo = new SMO();
        /* Wrap Weka classifier in bridge */
        javamlsmo = new WekaClassifier(smo);
        javamlsmo.buildClassifier(data);
		
	}

}
