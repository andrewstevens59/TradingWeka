package DecisionNetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class DataSet {
	
	// This stores the set of prices
	private ArrayList<Float> price_buff = new ArrayList<Float>();
	// This stores the buy sell volume ratio 
	private ArrayList<DataVector> vol_ratio = new ArrayList<DataVector>();
	// This stores the bid ask spread
	private ArrayList<Float> bid_ask_buff = new ArrayList<Float>(); 
	// This stores the set of order volume modes
	private ArrayList<DataVector> mode_vol_buff = new ArrayList<DataVector>();
	// This stores the set of price gradients
	private ArrayList<DataVector> price_grad_buff = new ArrayList<DataVector>();
	// This stores the set of future price jumps for different time into the future
	private ArrayList<DataVector> price_jump_buff = new ArrayList<DataVector>();
	// This calculates the price volatility
	private ArrayList<DataVector> volatility_buff = new ArrayList<DataVector>();
	
	// This stores the data set
	private ArrayList<boolean []> data_set = new ArrayList<boolean []>();
	// This stores the corresponding output
	private ArrayList<boolean []> output_set = new ArrayList<boolean []>();
	
	// This stores the data set
	private ArrayList<boolean []> test_data_set = new ArrayList<boolean []>();
	// This stores the corresponding output
	private ArrayList<boolean []> test_output_set = new ArrayList<boolean []>();
	
	// This stores the set of binary operations
	private ArrayList<Integer> op_type_buff = new ArrayList<Integer>();
	// This stores the current operation being processed
	private int op_type_offset = 0;
	
	// This finds the volatility over a period of time
	private float Volatility(int start, int end) {
		
		float avg = 0;
		for(int i=start; i<end; i++) {
			avg += price_buff.get(i);
		}
		
		float diff = 0;
		avg /= end - start;
		for(int i=start; i<end; i++) {
			diff += (price_buff.get(i) - avg) * (price_buff.get(i) - avg);
		}
		
		return (float) Math.sqrt(diff);
	}
	
	// This finds the set of price volatilities 
	private void FindPriceVolatility() {
		
		for(int i=64; i<price_buff.size(); i++) {
			
			int size = 16;
			volatility_buff.add(new DataVector(3));
			
			for(int j=0; j<3; j++) {
				volatility_buff.get(volatility_buff.size()-1).val[j] = Volatility(i - size, i);
				size <<= 1;
			}
		}
	}
	
	// This calculates the set of price gradients
	private void FindPriceGrads() {
		
		
		for(int i=64; i<price_buff.size(); i++) {
			
			int size = 4;
			price_grad_buff.add(new DataVector(5));
			
			for(int j=0; j<5; j++) {
				LinearRegression.Regression(price_buff, i - size, i);
				price_grad_buff.get(price_grad_buff.size()-1).val[j] = LinearRegression.gradient;
				size <<= 1;
			}
		}
	}
	
	// This finds the set of price jumps for each point in time
	private void FindPriceJumps() {
		
		for(int i=0; i<price_buff.size()-320; i++) {
			
			int gap = 10;
			float price1 = price_buff.get(i);
			price_jump_buff.add(new DataVector(6));
			for(int j=0; j<6; j++) {
				float price2 = price_buff.get(i + gap);
				float ratio = (Math.max(price1, price2) / Math.min(price1, price2)) - 1;
				if(price2 < price1) {
					ratio *= -1;
				}
				
				ratio *= 100;
				price_jump_buff.get(price_jump_buff.size()-1).val[j] = ratio;
				gap <<= 1;
			}
		}
	}
	
	// This creates the final database 
	private void CreateDB(ArrayList<boolean []> data) {
		
		DataRange r4 = new DataRange(volatility_buff, 3);
		DataRange r1 = new DataRange(price_grad_buff, 5);
		
		for(int j=256; j<data.size(); j++) {
			
			int gap = 32;
			int offset = 0;
			data_set.add(new boolean[(data.get(j).length * 5) + ((r1.DimNum() + r4.DimNum()) * 6) + 36 ]);
			
			for(int k=0; k<data.get(j).length; k++) {
				data_set.get(data_set.size()-1)[offset++] = data.get(j)[k];
			}
			
			for(int i=0; i<4; i++) {
				for(int k=0; k<data.get(j - gap).length; k++) {
					data_set.get(data_set.size()-1)[offset++] = data.get(j - gap)[k];
				}
				
				gap <<= 1;
			}
			
			for(int i=0; i<r1.DimNum(); i++) {
				for(int k=0; k<6; k++) {
					data_set.get(data_set.size()-1)[offset++] = r1.GetDim(i, price_grad_buff.get(j-64).val[i])[k];
				}
			}
			
			for(int i=0; i<r4.DimNum(); i++) {
				for(int k=0; k<6; k++) {
					data_set.get(data_set.size()-1)[offset++] = r4.GetDim(i, volatility_buff.get(j-64).val[i])[k];
				}
			}
			
			for(int x=0; x<6; x++) {
				for(int y=6; y<12; y++) {
					float delta1 = Math.abs(mode_vol_buff.get(j).val[x] - price_buff.get(j));
					float delta2 = Math.abs(mode_vol_buff.get(j).val[y] - price_buff.get(j));
					
					if(delta1 < delta2) {
						data_set.get(data_set.size()-1)[offset++] = false;
					} else {
						data_set.get(data_set.size()-1)[offset++] = true;
					}
				}
			}
			
		}
		
		DataRange r3 = new DataRange(price_jump_buff, 6);

		for(int j=256; j<data.size()-320; j++) {
			
			int offset = 0;
			output_set.add(new boolean[r3.DimNum() * 6]);
			
			for(int i=0; i<r3.DimNum(); i++) {
				for(int k=0; k<6; k++) {
					output_set.get(output_set.size()-1)[offset++] = r3.GetDim(i, price_jump_buff.get(j).val[i])[k];
				}
			}
		}
	}
	
	// This loads the order book data set
	private void LoadOrderBook() throws NumberFormatException, IOException {
		
		BufferedReader br = new BufferedReader(new FileReader("order.txt"));
		
		String line;
		while ((line = br.readLine()) != null) {
			
			vol_ratio.add(new DataVector(6));
			mode_vol_buff.add(new DataVector(12));

			StringTokenizer strtok = new StringTokenizer(line, " ");
			for(int i=0; i<6; i++) {
				vol_ratio.get(vol_ratio.size()-1).val[i] = Float.parseFloat(strtok.nextToken());
			}
			
			bid_ask_buff.add(Float.parseFloat(strtok.nextToken()));
			
			for(int i=0; i<12; i++) {
				mode_vol_buff.get(mode_vol_buff.size()-1).val[i] = Float.parseFloat(strtok.nextToken());
			}
			
			price_buff.add(Float.parseFloat(strtok.nextToken()));
		}
		
		br.close();
		
		FindPriceGrads();
		FindPriceJumps();
		FindPriceVolatility();
		
		ArrayList<boolean []> data = new ArrayList<boolean []>();
		DataRange r3 = new DataRange(bid_ask_buff);
		DataRange r2 = new DataRange(vol_ratio, 6);
		
		for(int i=64; i<price_buff.size(); i++) {
			data.add(new boolean[(r2.DimNum() + r3.DimNum()) * 6]);
			
			int offset = 0;
			for(int k=0; k<r2.DimNum(); k++) {
				for(int j=0; j<6; j++) {
					data.get(data.size()-1)[offset++] = r2.GetDim(k, vol_ratio.get(i).val[k])[j];
				}
			}
			
			for(int k=0; k<r3.DimNum(); k++) {
				for(int j=0; j<6; j++) {
					data.get(data.size()-1)[offset++] = r3.GetDim(k, bid_ask_buff.get(i))[j];
				}
			}
		}

		CreateDB(data);
	}
	
	// This creates the binary opertion tree
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
		for(int j=0; j<sample_size; j++) {
			boolean sample[] = new boolean[100];
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
	
	// This parses the reduce order book and loads it into memory
	public DataSet() throws NumberFormatException, IOException {
			
		//LoadOrderBook();
		
		Random r = new Random();
		boolean sample[] = new boolean[100];
		
		CreateBinaryOpTree(0, sample.length);
		
		CreateArtificialDataSet(3000, data_set, output_set);
		
		CreateArtificialDataSet(1000, test_data_set, test_output_set);
		
		new KDNode(data_set, output_set, null);
		
		PrintWriter out = new PrintWriter("test.arff");
		
		/*out.println("@RELATION iris");
		for(int i=0; i<DimNum(); i++) {
			out.println("@ATTRIBUTE Dim"+i+" NUMERIC");
		}
		
		out.println("@ATTRIBUTE class {1, 0}");
		out.println("@Data");*/
        
		for(int i=0; i<data_set.size(); i++) {
			
			String str = new String();
			for(int j=0; j<data_set.get(i).length >> 1; j++) {
				if(data_set.get(i)[j] == true) {
					str += "1,";
				} else {
					str += "0,";
				}
			}
			
			if(output_set.get(i)[0] == true) {
				str += "1";
			} else {
				str += "0";
			}
			
			out.println(str);
		}
		
		//Close the output stream
        out.close();
	}
	
	public DataSet(ArrayList<boolean []> data_set, ArrayList<boolean []> output_set,
			ArrayList<boolean []> test_data_set, ArrayList<boolean []> test_output_set) {
		
		this.data_set = data_set;
		this.output_set = output_set;
		
		this.test_data_set = test_data_set;
		this.test_output_set = test_output_set;
	}
	
	// This returns the number of dimensions in the training set
	public int DimNum() {
		return data_set.get(0).length;
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
