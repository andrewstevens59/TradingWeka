package Main;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import VectorMethod.DecisionNode;

public class DataSet {
	
	// This stores the data set
	private ArrayList<boolean []> data_set = new ArrayList<boolean []>();
		// This stores the corresponding output
	private ArrayList<boolean []> output_set = new ArrayList<boolean []>();

	// This stores the set of regime training data
	private ArrayList<RegimeSet> regime_buff = new ArrayList<RegimeSet>();
	// This stores the data set
	private ArrayList<boolean []> test_data_set = new ArrayList<boolean []>();
	// This stores the corresponding output
	private ArrayList<boolean []> test_output_set = new ArrayList<boolean []>();
	
	// This stores the set of binary operations
	private ArrayList<Integer> op_type_buff = new ArrayList<Integer>();
	// This stores the current operation being processed
	private int op_type_offset = 0;
	// This stores the transition probabilities between the different regimes
	private float trans_prob[][];
	
	// This creates the binary operation tree
	private boolean ClassifySample(int start, int end, boolean sample[]) {
		
		if(end - start <= 1) {
			return sample[(start + 4) % sample.length];
		}
		
		int middle = (start + end) >> 1;
		int op_type = op_type_buff.get(op_type_offset++);
		
		boolean left = ClassifySample(start, middle, sample);
		boolean right = ClassifySample(middle, end, sample);
		
		if(op_type == 0) {
			return left & right;
		}
		
		if(op_type == 1) {
			return left | right;
		}
		
		return left ^ right; 
	}
	
	// This creates the binary operation tree
	private void CreateBinaryOpTree(int start, int end) {
		
		if(end - start <= 1) {
			return;
		}
		
		Random r = new Random();
		int middle = (start + end) >> 1;
		
		/*if(end - start < 7) {
			op_type_buff.add(r.nextInt(2));
		} else {
			op_type_buff.add(r.nextInt(3));
		}*/
	
		op_type_buff.add(r.nextInt(3));
		
		CreateBinaryOpTree(start, middle);
		CreateBinaryOpTree(middle, end);
	}
	
	// This creates the artificial data set using AND, OR and XOR operations
	private void CreateArtificialDataSet(int sample_size, ArrayList<boolean []> data_set,
			ArrayList<boolean []> output_set) {
		
		Random r = new Random();
		int op_type = r.nextInt(3);
		for(int j=0; j<sample_size; j++) {
			boolean sample[] = new boolean[15];
			boolean output[] = new boolean[1];
			
			for(int i=0; i<sample.length; i++) {
				sample[i] = r.nextBoolean();
			}
			
			op_type_offset = 0;
			output[0] = ClassifySample(0, sample.length, sample);
			
			data_set.add(sample);
			output_set.add(output);
		}
	}
	
	// This chooses a random regime with a given bias
	private int ChooseRegime(int row) {
		
		float val = (float) Math.random();
		
		for(int i=0; i<trans_prob.length; i++) {
			if(trans_prob[row][i] >= val) {
				return i;
			}
			
			val -= trans_prob[row][i];
		}
		
		return trans_prob.length - 1;
	}
	
	// This parses the reduce order book and loads it into memory
	public DataSet() throws NumberFormatException, IOException {
			
		//LoadOrderBook();
		
		Random r = new Random();
		boolean sample[] = new boolean[15];
		ArrayList<ArrayList<Integer>> opt_buff = new ArrayList<ArrayList<Integer>>();
		
		// create 5 different regimes
		for(int i=0; i<5; i++) {	
			op_type_buff = new ArrayList<Integer>();
			op_type_offset = 0;
			CreateBinaryOpTree(0, sample.length);
			opt_buff.add(op_type_buff);
		}
		
		trans_prob = new float[5][5];
		for(int i=0; i<trans_prob.length; i++) {
			
			float sum = 0;
			for(int j=0; j<trans_prob.length; j++) {
				trans_prob[i][j] = r.nextFloat();
				sum += trans_prob[i][j];
			}
			
			for(int j=0; j<trans_prob.length; j++) {
				trans_prob[i][j] /= sum;
			}
		}
		
		int prev_id = -1;
		for(int i=0; i<100; i++) {
			RegimeSet rs = new RegimeSet();
			int id = r.nextInt(opt_buff.size());
			
			if(prev_id != -1) {
				id = ChooseRegime(prev_id);
			}
			
			op_type_buff = opt_buff.get(id);
			CreateArtificialDataSet(100, rs.data_set, rs.output_set);
			regime_buff.add(rs);
			prev_id = id;
		}
		
		prev_id = -1;
		for(int i=0; i<60; i++) {
			
			int id = r.nextInt(opt_buff.size());
			if(prev_id != -1) {
				id = ChooseRegime(prev_id);
			}
			
			op_type_buff = opt_buff.get(id);
			CreateArtificialDataSet(100, test_data_set, test_output_set);
		}
	}
	
	// This writes the dataset to file
	public void WriteDataSet() throws IOException {
		
		FileOutputStream fos = new FileOutputStream("data_set");
		DataOutputStream dos = new DataOutputStream(fos);
		
		dos.writeInt(DimNum());
		
		dos.writeInt(test_data_set.size());
		for(int i=0; i<test_data_set.size(); i++) {
			
			boolean sample[] = test_data_set.get(i);
			boolean output = test_output_set.get(i)[0];
			
			for(int j=0; j<sample.length; j++) {
				dos.writeBoolean(sample[j]);
			}
			
			dos.writeBoolean(output);
		}
		
		dos.writeInt(regime_buff.size());
		for(int i=0; i<regime_buff.size(); i++) {
			RegimeSet r = regime_buff.get(i);
			
			dos.writeInt(r.data_set.size());
			for(int j=0; j<r.data_set.size(); j++) {
				
				boolean sample[] = r.data_set.get(j);
				boolean output = r.output_set.get(j)[0];
				
				for(int k=0; k<sample.length; k++) {
					dos.writeBoolean(sample[k]);
				}
				
				dos.writeBoolean(output);
			}
			
		}
        
        dos.close();
		
	}
	
	// This reads the dataset from file
	public void ReadDataSet() throws IOException {

		data_set = new ArrayList<boolean []>();
		output_set = new ArrayList<boolean []>();
		
		test_data_set = new ArrayList<boolean []>();
		test_output_set = new ArrayList<boolean []>();
		regime_buff = new ArrayList<RegimeSet>();
		
		FileInputStream fos = new FileInputStream("data_set");
		DataInputStream dos = new DataInputStream(fos);
		
		int dim_num = dos.readInt();
		int set_num = dos.readInt();
		
		for(int i=0; i<set_num; i++) {
			
			boolean sample[] = new boolean[dim_num];
			boolean output[] = new boolean[1];
			for(int j=0; j<sample.length; j++) {
				sample[j] = dos.readBoolean();
			}
			
			output[0] = dos.readBoolean();
			
			test_data_set.add(sample);
			test_output_set.add(output);
		}
		
		int regime_num = dos.readInt();
		for(int i=0; i<regime_num; i++) {
			RegimeSet r = new RegimeSet();
			set_num = dos.readInt();
			
			for(int j=0; j<set_num; j++) {
				boolean sample[] = new boolean[dim_num];
				boolean output[] = new boolean[1];
				for(int k=0; k<sample.length; k++) {
					sample[k] = dos.readBoolean();
				}
				
				output[0] = dos.readBoolean();
				
				r.data_set.add(sample);
				r.output_set.add(output);
			}
			
			regime_buff.add(r);
		}

		
		dos.close();
		
	}
	
	public DataSet(ArrayList<boolean []> data_set, ArrayList<boolean []> output_set,
			ArrayList<boolean []> test_data_set, ArrayList<boolean []> test_output_set) {
		
		this.data_set = data_set;
		this.output_set = output_set;

		this.test_data_set = test_data_set;
		this.test_output_set = test_output_set;
	}
	
	// This returns the set of regimes
	public ArrayList<RegimeSet> RegimeSet() {
		return regime_buff;
	}
	
	// This returns the number of dimensions in the training set
	public int DimNum() {
		return test_data_set.get(0).length;
	}
	
	// This returns the number of outputs
	public int OutputNum() {
		return output_set.get(0).length;
	}
	
	// This returns the data set
	public ArrayList<boolean []> DataSet() {
		return data_set;
	}
	
	// This returns the output set
	public ArrayList<boolean []> OutputSet() {
		return output_set;
	}
	
	// This returns the data set
	public ArrayList<boolean []> TestDataSet() {
		return test_data_set;
	}
	
	// This returns the output set
	public ArrayList<boolean []> TestOutputSet() {
		return test_output_set;
	}

}
