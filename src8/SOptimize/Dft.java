package SOptimize;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

/* 
 * Discrete Fourier transform
 * By Nayuki Minase, 2014. Public domain.
 * http://nayuki.eigenstate.org/page/how-to-implement-the-discrete-fourier-transform
 */


public final class Dft {
	
	class InstCombo implements Comparable {
		public int insts[];
		public double hedge_ratio[];
		public double barrier;
		public double exit_barrier;
		public double barrier_ratio;
		public double variance;
		private double opt_val;
		
		public ArrayList<Double> buff;
		
		public void WriteRule(DataOutputStream os) throws IOException {
			
			os.writeInt(insts.length);
			for(int i=0; i<insts.length; i++) {
				os.writeInt(insts[i]);
				os.writeDouble(hedge_ratio[i]);
			}
			
			os.writeDouble(barrier);
			os.writeDouble(barrier_ratio);
		}
		
		public void ReadRule(DataInputStream os) throws IOException {
			int size = os.readInt();
			insts = new int[size];
			hedge_ratio = new double[size];
			
			for(int i=0; i<size; i++) {
				insts[i] = os.readInt();
				hedge_ratio[i] = os.readDouble();
			}
			
			barrier = os.readDouble();
			barrier_ratio = os.readDouble();
		}

		@Override
		public int compareTo(Object arg0) {
			InstCombo c = (InstCombo)arg0;
			if(c.opt_val < opt_val) {
				return 1;
			}
			
			if(c.opt_val > opt_val) {
				return -1;
			}
			
			return 0;
		}
	}
	
	/*Instruments inst[] = {AUDCAD 
			AUDCHF 
			AUDJPY 
			AUDNZD 
			AUDUSD 
			CADCHF 
			CADJPY 
			CHFJPY 
			EURAUD  
			EURCAD 
			EURCHF 
			EURGBP  
			EURJPY 
			EURNZD 
			EURUSD 
			GBPAUD 
			GBPCAD 
			GBPCHF 
			GBPJPY 
			GBPNZD 
			GBPUSD 
			NZDCAD 
			NZDCHF 
			NZDJPY 
			NZDSGD 
			NZDUSD 
			USDCAD 
			USDCHF 
			USDJPY };*/
	
	// This stores the set of instruments
	private String insts[] = {"AUDCAD", 
			"AUDCHF",  
			//"AUDJPY",  
			"AUDNZD",   
			"CADCHF",  
			//"CADJPY",  
			//"CHFJPY",  
			//"EURAUD",   
			//"EURCAD",  
			//"EURCHF",  
			//"EURGBP",   
			//"EURJPY",  
			//"EURNZD",  
			//"EURUSD",  
			//"GBPAUD",  
			//"GBPCAD",  
			//"GBPCHF",  
			//"GBPJPY",  
			//"GBPNZD",  
			"GBPUSD",  
			//"NZDCAD",  
			//"NZDCHF",  
			//"NZDJPY",   
			//"NZDUSD",  
			//"USDCAD",  
			//"USDCHF",  
			//"USDJPY"
	};
	
	// This stores the individual series 
	private ArrayList<double[]> series_buff = new ArrayList<double[]>();
	// This stores the set of baskets
	private ArrayList<InstCombo> combo_buff = new ArrayList<InstCombo>();
	// This stores the combo set
	private HashSet<String> combo_set = new HashSet<String>();
	// This stores the set of filled positions in time
	private HashSet<Integer> filled_set = new HashSet<Integer>();
	
	private double min_var = Double.MAX_VALUE;
	private ArrayList<Double> min_series_buff = null;
	

	// This loads a series
	private static double []LoadSeries(String dir) throws IOException {
		
		
		int offset = 0;
		BufferedReader br = new BufferedReader(new FileReader(dir));//"FXData/AUDCAD_UTC_Daily_Bid_2013.01.01_2014.01.01.csv"));
		String line;
		br.readLine();
		
		ArrayList<Double> buff = new ArrayList<Double>();
		while ((line = br.readLine()) != null) {
		   StringTokenizer strtok = new StringTokenizer(line, " ");
		   strtok.nextElement();
		   strtok.nextElement();
		   double v = Double.valueOf((String) strtok.nextToken());
		   buff.add(v);
		   buff.add((double)0);
		   
		   if(buff.size() >= 365) {
			   //break;
		   }
		}
		br.close();
		
		double fin[] = new double[buff.size()];
		for(int i=0; i<fin.length; i++) {
			fin[i] = buff.get(i);
		}

		return fin;
	}
	
	// This finds the average
	private double Average(ArrayList<Double> buff) {
		
		double avg = 0;
		for(Double val : buff) {
			avg += val;
		}
		
		return avg / buff.size();
	}
	
	// This finds the average
	private double StdDev(ArrayList<Double> buff) {
		
		double avg = Average(buff);
		double stdev = 0;
		
		for(Double val : buff) {
			stdev += (val - avg) * (val - avg);
		}
		
		return Math.sqrt(stdev);
	}
	
	// This finds the barrier size
	private boolean BarrierSize(double inSeries[][], double weight[], InstCombo combo) {
		
		ArrayList<Double> buff = new ArrayList<Double>();
		
		double min_val = Double.MAX_VALUE;
		double max_val = -Double.MAX_VALUE;
		for(int i=0; i<inSeries[0].length; i++) {
			
			double sum = 0;
			for(int j=0; j<weight.length; j++) {
				sum += weight[j] * inSeries[j][i];
			}
			
			if(Math.abs(sum) > 0) {
				buff.add(sum);
				min_val = Math.min(sum, min_val);
				max_val = Math.max(sum, max_val);
			}
		}
		
		combo.barrier = Math.max(Math.abs(min_val), Math.abs(max_val)) - 0.03;
		combo.exit_barrier = Math.max(Math.abs(min_val), Math.abs(max_val)) + 0.03;
		combo.barrier_ratio = 0;
		
		int offset = 0;
		int filled_num = 0;
		for(Double val : buff) {
			offset++;
			if(Math.abs(val) > combo.barrier) {
				combo.barrier_ratio++;
				if(filled_set.contains(offset) == false) {
					filled_num++;
				}
			}
		}

		offset = 0;
		for(Double val : buff) {
			offset++;
			if(Math.abs(val) > combo.barrier) {
				filled_set.add(offset);
			}
		}
		
		combo.barrier_ratio /= buff.size();
		combo.variance = Math.max(Math.abs(min_val), Math.abs(max_val));
		combo.buff = buff;
		
		if(combo.variance < min_var) {
			min_var = combo.variance;
			min_series_buff = buff;
		}
		
		System.out.println("Min Var: "+combo.variance+" "+min_var);
		
		return true;
	}
	
	// This creates a basket 
	private void CreateCombo(ArrayList<Integer> series) throws Exception {
		
		double outDFT[][] = new double[series.size()][];
		double inSeries[][] = new double[series.size()][];
		int offset = 0;
		for(int id : series) {
			inSeries[offset] = series_buff.get(id);
			outDFT[offset++] = Fourier.discreteFT(series_buff.get(id), series_buff.get(id).length >> 1, true);
		}
		
		// Objective function
		double init_val[] = new double[series.size()];
		double[][] P = new double[series.size()][series.size()];
		for(int i=0; i<series.size(); i++) {
			init_val[i] = 1.0f;
			for(int j=0; j<series.size(); j++) {
				P[i][j] = 0;
			}
		}

		for(int k=0; k<outDFT[0].length; k++) {
			
			for(int i=0; i<series.size(); i++) {
				for(int j=i; j<series.size(); j++) {
					double v1 = outDFT[i][k];// / (i + 1);
					double v2 = outDFT[j][k];// / (j + 1);
					P[i][j] += v1 * v2;
				}
			}
		}
		
		for(int i=0; i<series.size(); i++) {
			for(int j=i+1; j<series.size(); j++) {
				P[j][i] = P[i][j];
			}
		}
	
		
		try {
			PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(P, null, 0);
			double[][] A = new double[1][series.size()];
			double[] b = new double[]{1.0f};
			for(int i=0; i<series.size(); i++) {
				A[0][i] = 1.0f;
			}
	
	
			//optimization problem
			OptimizationRequest or = new OptimizationRequest();
			or.setF0(objectiveFunction);
			or.setInitialPoint(init_val);
	
			or.setA(A);
			or.setB(b);
			or.setToleranceFeas(1.E-12);
			or.setTolerance(1.E-12);
			
			//optimization
			JOptimizer opt = new JOptimizer();
			opt.setOptimizationRequest(or);
			int returnCode = opt.optimize();
			
			double[] fvector = opt.getOptimizationResponse().solution;
			InstCombo combo = new InstCombo();
			combo.hedge_ratio = fvector;
			combo.opt_val = 0;
			
			for(int j=0; j<outDFT[0].length; j++) {
				
				double sum = 0;
				for(int i=0; i<outDFT.length; i++) {
					sum += outDFT[i][j] * fvector[i];
				}
				
				//sum /= (j + 1);
				combo.opt_val += sum * sum;
			}
			
			System.out.println(combo.opt_val);
			combo.insts = new int[fvector.length];
			for(int i=0; i<series.size(); i++) {
				combo.insts[i] = series.get(i);
			}
			
			if(BarrierSize(inSeries, fvector, combo) == true) {
				combo_buff.add(combo);	
			}
		} catch(Exception e) {
			
		}
	}
	
	public Dft() throws IOException {
		
		
	}
	
	public void ProcessSeries() throws Exception {
		for(String str : insts) {
			double buff[] = LoadSeries("FXData/" + str + "_UTC_Daily_Bid_2008.01.01_2012.12.31.csv");
			series_buff.add(buff);
		}
		
		Random r = new Random();
		for(int i=0; i<30; i++) {
			
			ArrayList<Integer> series_set = new ArrayList<Integer>();
			
			String str = new String();
			for(int j=0; j<4; j++) {
				int id = r.nextInt(insts.length);
				str += id + " ";
				series_set.add(id);
			}
			
			if(combo_set.contains(str)) {
				continue;
			}

			CreateCombo(series_set);
			combo_set.add(str);
		}
		
		Collections.sort(combo_buff);
		
		InstCombo co = combo_buff.get(0);
		combo_buff.clear();
		combo_buff.add(co);
		
		for(int j=0; j<co.buff.size(); j++) {
			System.out.println(co.buff.get(j));
		}
		System.out.println("");
		
		for(int i=0; i<co.insts.length; i++) {
			System.out.print("Instrument."+insts[co.insts[i]]+",");
		}
		
		System.out.println("");
		
		for(int i=0; i<co.hedge_ratio.length; i++) {
			System.out.print(co.hedge_ratio[i]+",");
		}
		
		System.out.println("\nBarrier: "+co.barrier);
		System.out.println("Exit: "+co.exit_barrier);
		System.out.println("\n\n\n");
		
		DataOutputStream os = new DataOutputStream(new FileOutputStream("combo_set"));
		
		for(InstCombo o : combo_buff) {
			o.WriteRule(os);
		}
		
		os.close();
	}
	
	public void ReadSet() throws IOException {
		DataInputStream os = new DataInputStream(new FileInputStream("combo_set"));
		
		while(os.available() > 0) {
			InstCombo inst = new InstCombo();
			inst.ReadRule(os);
			combo_buff.add(inst);
		}
		
		int offset = 0;
		System.out.print("public int combo[][] = {");
		for(InstCombo o : combo_buff) {
			System.out.print("{");
			for(int i=0; i<o.insts.length; i++) {
				System.out.print(o.insts[i]);
				if(i < o.insts.length - 1) {
					System.out.print(",");
				}
			}
			System.out.print("}");
			if(offset < combo_buff.size() - 1) {
				System.out.print(",");
			}
			offset++;
		}
		
		System.out.println("};");
		
		offset = 0;
		System.out.print("public double hedge[][] = {");
		for(InstCombo o : combo_buff) {
			System.out.print("{");
			for(int i=0; i<o.hedge_ratio.length; i++) {
				System.out.print(o.hedge_ratio[i]);
				if(i < o.insts.length - 1) {
					System.out.print(",");
				}
			}
			System.out.print("}");
			if(offset < combo_buff.size() - 1) {
				System.out.print(",");
			}
			offset++;
		}
		
		System.out.println("};");
		
		offset = 0;
		System.out.print("public double barrier[] = {");
		for(InstCombo o : combo_buff) {
			System.out.print(o.barrier);
			if(offset < combo_buff.size() - 1) {
				System.out.print(",");
			}
			offset++;
		}
		
		System.out.println("};");
		
		System.out.print("public Instrument instruments[] = {");
		for(int i=0; i<insts.length; i++) {
			System.out.print("Instrument."+insts[i]);
			if(i < insts.length - 1) {
				System.out.print(",");
			}
		}
		
		System.out.println("};");
		
		os.close();
	}
	

	public static void main(String args[]) throws Exception {
		
		Dft dft = new Dft();
		dft.ProcessSeries();
		
		dft.ReadSet();
		
	}

	
}