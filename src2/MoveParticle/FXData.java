package MoveParticle;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sf.javaml.core.Dataset;


public class FXData {
	
	// This stores a point in the time series
	class Sample {
		Date d;
		// This stores the bid high
		float bid_high;
		// This stores the bid low
		float bid_low;
		// This stores the bid high
		float ask_high;
		// This stores the bid low
		float ask_low;
		// This stores the bid volume
		float bid_vol;
		// This stores the ask volume
		float ask_vol;
		// This stores the ask open
		float ask_open;
		// This stores the bid open
		float bid_open;
		// This stores the price in usd
		float exch_rate;
	}
	
	// This stores the fx time series
	private ArrayList<Sample[]> fx_buff = new ArrayList<Sample[]>();
	// This stores the set of training samples
	private TrainingSets train[];
	// This stores the set of currencies
	private HashMap<String, Integer> currency_map = new HashMap<String, Integer>();
	// This stores the first currency
	private ArrayList<String> currency1 = new ArrayList<String>();
	// This stores the second currency
	private ArrayList<String> currency2 = new ArrayList<String>();
	
	
	private void ReadTimeSeries(String fileName, int time_series_id, boolean is_ask) throws IOException, ParseException {
		

		BufferedReader br = new BufferedReader(
		        new InputStreamReader(new FileInputStream(fileName)));
		try {
			
		    String line = br.readLine();
		    int offset = 0;
		    while ((line = br.readLine()) != null) {
		        StringTokenizer strtok = new StringTokenizer(line, ", ");
		       // System.out.println(line);
		      //  Date d = (Date) new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH).parse(strtok.nextToken());

		        String date = strtok.nextToken();
		        String time = strtok.nextToken();
		        
		        if(date.equals("06.07.2013") == true) {
		        	break;
		        }
		        
		        if(offset >= fx_buff.size()) {
		        	Sample set[] = new Sample[10];
		        	for(int j=0; j<set.length; j++) {
		        		set[j] = new Sample();
		        	}
		        	fx_buff.add(set);
		        }
		        
		        Sample s = fx_buff.get(offset)[time_series_id];
		        
		        if(is_ask == true) {
		        	s.ask_open = Float.parseFloat(strtok.nextToken());
		        	s.ask_high = Float.parseFloat(strtok.nextToken());
			        s.ask_low = Float.parseFloat(strtok.nextToken());
		        } else {
		        	s.bid_open = Float.parseFloat(strtok.nextToken());
		        	s.bid_high = Float.parseFloat(strtok.nextToken());
			        s.bid_low = Float.parseFloat(strtok.nextToken());
		        }
		        
		        // skip close
		        strtok.nextToken();
		        
		        if(is_ask == true) {
		        	s.ask_vol = Float.parseFloat(strtok.nextToken());
		        } else {
		        	s.bid_vol = Float.parseFloat(strtok.nextToken());
		        }

		        fx_buff.get(offset)[time_series_id] = s;
		        offset++;
		    }
		} finally {
		    br.close();
		}
	}
	
	// This returns the first currency id
	public int CurrencyID1(int id) {
		return currency_map.get(currency1.get(id));
	}
	
	// This returns the first currency id
	public int CurrencyID2(int id) {
		return currency_map.get(currency2.get(id));
	}
	
	// This returns the currency pair
	public String Currency1(int id) {
		return currency1.get(id);
	}
	
	// This returns the currency pair
	public String Currency2(int id) {
		return currency2.get(id);
	}
	
	// This stores the total number of currencies
	public int CurrencyNum() {
		return currency_map.size();
	}
	
	// This returns the number of currency pairs
	public int CurrPairNum() {
		return currency1.size();
	}
	
	// This stores the id for a currency pair
	public int CurrencyID(String currency) {
		return currency_map.get(currency); 
	}

	public void ReadFXData(String directory) throws IOException, ParseException {
		

		int count = 0;
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		File actual = new File(directory);
        for( File f : actual.listFiles()){

        	String curr1 = f.getName().substring(0, 3);
        	String curr2 = f.getName().substring(3, 6);
        	
        	if(curr1.equals("CAD") == false && curr2.equals("CAD") == false) {
        		//continue;
        	}

        	Integer id = map.get(f.getName().substring(0, 6));
        	if(id == null) {
        		id = map.size();
        		map.put(f.getName().substring(0, 6), id);
        		
            	currency1.add(curr1);
            	currency2.add(curr2);
            	
            	if(currency_map.get(curr1) == null) {
            		currency_map.put(curr1, currency_map.size());
            	}
            	
            	if(currency_map.get(curr2) == null) {
            		currency_map.put(curr2, currency_map.size());
            	}
        	}
        	
        	ReadTimeSeries("I:/PhD/Code/RegimeSwitching/FXData/" + f.getName(), id, f.getName().contains("ASK"));
        	if(++count >= 2) {
        		break;
        	}
    
        }
	}
	
	// This generates the training set for a particular currency pair
	public void GeneratePairTrainingSet(int set_id, ArrayList<Train> train_buff,
			ArrayList<Classify> class_buff) throws FileNotFoundException, IOException {
		
		for(int i=8; i<fx_buff.size()-1; i++) {
			
			Train t = new Train();
			Sample s[][] = new Sample[train.length][8];
			for(int k=0; k<train.length; k++) {
				for(int j=0; j<8; j++) {
					s[k][j] = fx_buff.get(i-j)[k];
				}
			}
			
			float bid_sum = 0;
			float ask_sum = 0;
			for(int k=0; k<4; k++) {
				bid_sum += s[set_id][k].bid_vol;
				ask_sum += s[set_id][k].ask_vol;
				
				t.ba_vol_diff[k] = (bid_sum - ask_sum) / (bid_sum + ask_sum);
			}
			
			t.exch_rate = (s[set_id][0].ask_open + s[set_id][0].bid_open) / 2;
			float min_rate = Float.MAX_VALUE;
			float max_rate = -Float.MAX_VALUE;
			for(int k=0; k<8; k++) {
				min_rate = Math.min(min_rate, s[set_id][k].bid_low);
				max_rate = Math.max(max_rate, s[set_id][k].ask_high);
				t.gap_rat[k] = (t.exch_rate - min_rate) / (max_rate - min_rate);
				t.volatility_gap[k] = max_rate - min_rate;
			}
			
			int offset = 0;
			for(int j=0; j<train.length; j++) {
				
				if(j == set_id) {
					continue;
				}
				
				for(int k=0; k<4; k++) {
					t.exch_rate_comp[offset][k] = s[set_id][0].exch_rate - s[j][k].exch_rate;
				}
				
				offset++;
			}
			
			t.exch_rate_jump = fx_buff.get(i+1)[set_id].exch_rate - fx_buff.get(i)[set_id].exch_rate;
			train_buff.add(t);
		}

		float error = 0;
		float buy_sell_error = 0;
		for(int i=0; i<train_buff.size(); i+=50) {
			
			if(i + 300 > train_buff.size()) {
				break;
			}
			
			Classify c = new Classify(train_buff, i, i + 300);
			class_buff.add(c);
			error += c.class_error;
			buy_sell_error += c.buy_sell_error;
		}
		
		buy_sell_error  /= class_buff.size();
		error /= class_buff.size();
		System.out.println(error+" "+buy_sell_error);
	}
	
	// This generates the training set 
	public void GenerateTrainingSet() throws FileNotFoundException, IOException {
		
		train = new TrainingSets[currency1.size()];
		for(int i=0; i<train.length; i++) {
			train[i] = new TrainingSets();
			
			for(int j=0; j<fx_buff.size(); j++) {
				fx_buff.get(j)[i].exch_rate = (fx_buff.get(j)[i].bid_open + fx_buff.get(j)[i].ask_open) / 2;
			}

			GeneratePairTrainingSet(i, train[i].train_buff, train[i].class_buff);
		}
	
	}
	
	// This returns the set of training samples
	public ArrayList<Train> TrainBuff(int id) {
		return train[id].train_buff;
	}
	
	// This returns the data set
	public Dataset DataSet(int id) {
		return train[id].data;
	}
	
	// This creates the hierarchy of classifiers
	public void BuildHierarchy() throws IOException {
		
		
		for(int j=0; j<train.length; j++) {
			train[j].data = Classify.TrainingInstances(train[j].train_buff, 0, train[j].train_buff.size());
			
			System.out.println(")SSSSS "+j);
			KDNode.SetClassID(j);
			
			ArrayList<KDNode> buff = KDNode.BuildHierarchyTree(train[j].data, Classify.DimNum(), train[j].class_buff);
			
			for(int i=0; i<buff.size()-1; i++) {
				SLink link = new SLink();
				link.src = buff.get(i);
				link.dst = buff.get(i+1);
				link.trav_prob = 1.0f;
				KDNode.EmbedSLink(link);
			}
			
			for(int i=0; i<buff.size()-2; i++) {
				SLink link = new SLink();
				link.src = buff.get(i);
				link.dst = buff.get(i+2);
				link.trav_prob = 0.25f;
				KDNode.EmbedSLink(link);
			}
		}
	}

}