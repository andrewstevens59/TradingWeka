package SOptimize;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

public class AnaylzeVWAP {
	
	class MaxVal implements Comparable {
		 int id;
		 double val;
		 double price;
		 
		public MaxVal(int id, double val, double price) {
			this.id = id;
			this.val = val;
			this.price = price;
		}
		 
		@Override
		public int compareTo(Object arg0) {
			MaxVal v = (MaxVal)arg0;
			if(v.price < price) {
				return -1;
			}
			
			if(v.price > price) {
				return 1;
			}
			
			if(v.id < id) {
				return 1;
			}
			
			if(v.id > id) {
				return -1;
			}
			
			return 0;
		}
	}
	
	class AvgRecTime implements Comparable {
		long avg_time;
		int num;
		int quant;
		
		public AvgRecTime(int quant) {
			avg_time = 0;
			num = 0;
			this.quant = quant;
		}

		@Override
		public int compareTo(Object o) {
			AvgRecTime v = (AvgRecTime)o;
			if(v.quant < quant) {
				return -1;
			}
			
			if(v.quant > quant) {
				return 1;
			}
			return 0;
		}
	}
	
	
	// This stores the set of instruments
	private String insts[] = {"AUDCAD",  
			"EURUSD",
			"CADCHF",
			"EURNZD",
			"GBPUSD",
			"GBPAUD",
			"GBPUSD",
			"NZDCAD",
			"NZDUSD",
			"USDCHF",
			"AUDNZD"
	};
	
	private void ProcessQueue(int offset, PriorityQueue<MaxVal> queue, 
			double price, double v, HashMap<Integer, AvgRecTime> avg_rec_map) {
		
		MaxVal m = queue.peek();
		while(m != null && price < m.price) {
			if(Math.abs(offset - m.id) < 60 * 5) {
				break;
			}
			
			queue.remove();
			ProcessQueue(offset, avg_rec_map, m);
			
			m = queue.peek();
		}
		
		queue.add(new MaxVal(offset, v, v < 0 ? -price : price));
	}

	private void ProcessQueue(int offset,
			HashMap<Integer, AvgRecTime> avg_rec_map, MaxVal m) {
		
		int quant = Math.abs((int) (m.val * 1));
		AvgRecTime t = avg_rec_map.get(quant);
		
		if(t == null) {
			avg_rec_map.put(quant, new AvgRecTime(quant));
			t = avg_rec_map.get(quant);
		}
		
		t.avg_time += Math.abs(m.id - offset);
		t.num++;
	}
	
	private double ReadSeries(String dir) throws IOException {
		
		FileInputStream fos = new FileInputStream("C:/Users/finzyholly/Desktop/JForex1/VWAP/vwap_data_"+dir);
		DataInputStream dos = new DataInputStream(fos);
		
		int offset = 0;
		PriorityQueue<MaxVal> queue_low = new PriorityQueue<MaxVal>();
		PriorityQueue<MaxVal> queue_high = new PriorityQueue<MaxVal>();
		HashMap<Integer, AvgRecTime> avg_rec_map = new HashMap<Integer, AvgRecTime>();
		
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		while(dos.available() > 0) {
			double v = dos.readDouble();
			double price = dos.readDouble();
			if(String.valueOf(v).equals("NaN")) {
				continue;
			}
			max = Math.max(max, v);
			min = Math.min(min, v);
			
			if(v < 0) {
				ProcessQueue(offset, queue_low, price, v, avg_rec_map);
			} else {
				ProcessQueue(offset, queue_high, price, v, avg_rec_map);
			}
			
			offset++;
			
		}
		
		dos.close();
		
		while(queue_low.size() > 0) {
			MaxVal m = queue_low.peek();
			ProcessQueue(m.id - (60 * 100), avg_rec_map, m);
			queue_low.remove();
		}
		
		while(queue_high.size() > 0) {
			MaxVal m = queue_high.peek();
			ProcessQueue(m.id - (60 * 100), avg_rec_map, m);
			queue_high.remove();
		}

		ArrayList<AvgRecTime> buff = new ArrayList<AvgRecTime>();
		for(AvgRecTime v : avg_rec_map.values()) {
			buff.add(v);
		}
		
		int curr_quant = -1;
		AvgRecTime avg = new AvgRecTime(0);
		Collections.sort(buff);
		for(AvgRecTime v : buff) {
			avg.avg_time += v.avg_time;
			avg.num += v.num;
			curr_quant = v.quant;
			
			double time = avg.avg_time / avg.num;
			if(time > 60 * 24 * 2) {
				break;
			}
		}
		
		return curr_quant * 0.75;
	}
		
	public AnaylzeVWAP() throws IOException {
		
		System.out.print("private double cutoff[] = {");
		for(int i=0; i<insts.length; i++) {
			double val = ReadSeries(insts[i]);
			System.out.print(val);
			if(i < insts.length-1) {
				System.out.print(",");
			}
		}
		
		System.out.println("};");
		
		System.out.print("private Instrument insts[] = {");
		for(int i=0; i<insts.length; i++) {
			System.out.print("Instrument."+insts[i]);
			if(i < insts.length-1) {
				System.out.print(",");
			}
		}
		
		System.out.println("};");
		
	}
				

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		new AnaylzeVWAP();

	}

}
