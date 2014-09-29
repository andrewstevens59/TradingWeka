package VolumeTrading;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class CreateTrainingSet {

	class Bar {
		Double low = Double.MAX_VALUE;
		Double high = -Double.MAX_VALUE;
		Double open;
		Double close;
		Double volume;
	}
	
	// This stores the set of squares
	private ArrayList<Bar> bar_set = new ArrayList<Bar>();
	// This stores the volume histogram
	private HashMap<Integer, Double> volume_hist_map = new HashMap<Integer, Double>();
	// This stores the set of bars
	private Queue<Bar> bar_queue = new LinkedList<Bar>();
	// This stores the sort bar queue
	private Queue<Bar> short_bar_queue = new LinkedList<Bar>();
	// This creates the set of training instances
	private ArrayList<Train> train_buff = new ArrayList<Train>();
	
	// This stores the set of bagged classifiers
	private ArrayList<Classifier> bag_class_set = new ArrayList<Classifier>();

	public CreateTrainingSet() throws Exception {
    	
    	
    	Bar b = new Bar();
    	Bar b1 = new Bar();
    	BufferedReader br = new BufferedReader(new FileReader("FXData/AUDCAD_UTC_Daily_Bid_2008.01.01_2012.12.31.csv"));
    	String line;
    	br.readLine();
    	int count = 0;
    	while ((line = br.readLine()) != null) {
    		String set[] = line.split(" ");
    		Double open = Double.parseDouble(set[2]);
    		double volume = Double.parseDouble(set[6]);

    		if(count == 0) {
    			b = new Bar();
    			
    			if(bar_set.size() == 0) {
    				b.open = open;
    			} else {
    				b.open = bar_set.get(bar_set.size()-1).close;
    			}
    			
    			b.volume = volume;
    		}

    		b.high = Math.max(b.high, open);
    		b.low = Math.min(b.low, open);
    		
    		b1.high = Math.max(b1.high, open);
    		b1.low = Math.min(b1.low, open);

    		if(++count >= 1) {
    			b.close = open;
    			if(b.volume > 0) {
    				bar_set.add(b);
    			}
    			count = 0;
    		}
    	}
    	br.close();
    	
    	buildHistMap();
    }
    	
	// This creates the training sample
	private void createSample(double price, int offset) {
		
		if(short_bar_queue.size() < 10) {
			return;
		}
		
		ArrayList<Double> buff = new ArrayList<Double>();
	
		double p = price;
		for(int i=0; i<5; i++) {
			p += 1;
			Double vol = volume_hist_map.get((int)p);
			buff.add(vol == null ? 0 : vol);
		}

		p = price;
		for(int i=0; i<5; i++) {
			p -= 1;
			Double vol = volume_hist_map.get((int)p);
			buff.add(vol == null || Math.abs(vol) < 0.1 ? 0 : vol);
		}
		
		for(int i=0; i<buff.size(); i++) {
			System.out.print(buff.get(i)+" ");
		}
		
		System.out.println("");
		
		for(Bar b : short_bar_queue) {
			buff.add(b.volume);
		}
		
		Train t = new Train();
		t.input = new Double[buff.size()];
		t.side = bar_set.get(offset + 2).close > bar_set.get(offset).close;
		buff.toArray(t.input);
		train_buff.add(t);
	}
	
	private void buildHistMap() throws Exception {
		
		for(int i=0; i<bar_set.size() - 10; i++) {
			Bar b = bar_set.get(i);
			
			double price = (b.open + b.close) / 2;
			price *= 10000;
			price = Math.floor(price);
			
			short_bar_queue.add(b);
			if(short_bar_queue.size() > 10) {
				short_bar_queue.remove();
			}
			
			Double vol = volume_hist_map.get((int)price);
			if(vol == null) {
				vol = (double) 0;
			}
			
			vol += b.volume;
			volume_hist_map.put((int)price, vol);
			
			//System.out.println(volume_hist_map.get((int)price)+" "+price);
			
			bar_queue.add(b);
			if(bar_queue.size() > 50) {
				Bar b1 = bar_queue.remove();
				double price1 = (b1.open + b1.close) / 2;
				price1 *= 10000;
				price1 = Math.floor(price1);
				double vol1 = volume_hist_map.get((int)price1);
				volume_hist_map.put((int)price1, vol1 - b1.volume);
			}
			
			createSample(price, i);
		}
		
		int width = train_buff.size() / 10;
		for(int i=0; i<10; i++) {
			int start = i * width;
			int end = (i + 1) * width;
			
			bag_class_set.add(new Classifier(train_buff, start, end));
		}
		
		TestSamples();
	}
	
	private void TestSamples() throws Exception {

		int right = 0;
		int wrong = 0;
		for(Train t : train_buff) {
			
			double sum = 0;
			for(Classifier c : bag_class_set) {
				sum += c.ClassifyInstance(t) > 0 ? 1 : -1;
			}
			
			if(t.side == (sum > 0)) {
				right++;
			} else {
				wrong++;
			}
		}
		
		System.out.println(right+" "+wrong);
	}
	
	public static void main(String args[]) throws Exception {
		new CreateTrainingSet();
	}
}
