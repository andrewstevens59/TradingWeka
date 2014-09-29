package ParticleFilter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sf.javaml.core.Dataset;


public class TimeSeries {
	
	// This stores a point in the time series
	class Sample {
		Date d;
		// This stores the btc volue
		float btc_vol;
		// This stores the USD volume
		float usd_vol;
		// This stores the price in usd
		float price;
	}
	
	// This stores the mtgox time series
	private ArrayList<Sample> mtgox_buff = new ArrayList<Sample>();
	// This stores the bitstamp time series
	private ArrayList<Sample> bitstamp_buff = new ArrayList<Sample>();
	// This stores the set of training samples
	private ArrayList<Train> train_buff = new ArrayList<Train>();
	// This stores the set of classifiers
	private ArrayList<Classify> class_buff = new ArrayList<Classify>();
	
	private void ReadTimeSeries(String fileName, ArrayList<Sample> buff) throws IOException, ParseException {
		
		BufferedReader br = new BufferedReader(
		        new InputStreamReader(new FileInputStream(fileName)));
		try {
			
			Date prev_date = null;
		    String line = br.readLine();
		    while ((line = br.readLine()) != null) {
		        StringTokenizer strtok = new StringTokenizer(line, ",");
		        System.out.println(line);
		        Date d = (Date) new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH).parse(strtok.nextToken());
		        Sample s = new Sample();
		        s.d = d;
		        
		        s.price = Float.parseFloat(strtok.nextToken());
		        strtok.nextToken();
		        strtok.nextToken();
		        strtok.nextToken();
		        s.btc_vol = Float.parseFloat(strtok.nextToken());
		        s.usd_vol = Float.parseFloat(strtok.nextToken());
		        
		        System.out.println(s.btc_vol+" "+s.usd_vol);
		        
		       /* if(prev_date != null) {
		        	long startTime = d.getTime();
		        	long endTime = prev_date.getTime();
		        	long diffTime = endTime - startTime;
		        	long diffDays = diffTime / (1000 * 60 * 60 * 24);
		        	while(diffDays > 1) {
		        		buff.add(s);
		        		diffDays--;
		        	}
		        }*/
		        buff.add(s);
		        prev_date = d;
		    }
		} finally {
		    br.close();
		}
	}

	public void ReadMtGox(String fileName) throws IOException, ParseException {
		ReadTimeSeries(fileName, mtgox_buff);
	}
	
	public void ReadBitStamp(String fileName) throws IOException, ParseException {
		ReadTimeSeries(fileName, bitstamp_buff);
	}
	
	// This generates the training set 
	public void GenerateTrainingSet() throws FileNotFoundException, IOException {
		
		if(mtgox_buff.size() != bitstamp_buff.size()) {
			System.out.println("size mis "+mtgox_buff.size()+" "+bitstamp_buff.size());System.exit(0);
		}
		
		for(int i=mtgox_buff.size()-6; i>=1; i--) {
			// oldest to newest
			if(mtgox_buff.get(i).d.getDate() != bitstamp_buff.get(i).d.getDate()) {
				System.out.println("wrong day");System.exit(0);
			}
			
			Train t = new Train();
			Sample s1[] = new Sample[4];
			Sample s2[] = new Sample[4];
			for(int j=0; j<4; j++) {
				s1[j] = mtgox_buff.get(i+j);
				s2[j] = bitstamp_buff.get(i+j);
			}
			
			for(int j=0; j<4; j++) {
				for(int k=0; k<4; k++) {
					t.vol_diff[j][k] = s1[j].btc_vol - s1[k].usd_vol;
				}
			}
			
			for(int j=0; j<4; j++) {
				for(int k=0; k<4; k++) {
					t.price_diff[j][k] = s1[j].price - s2[k].price;
				}
			}
			
			for(int j=0; j<4; j++) {
				for(int k=0; k<4; k++) {
					t.usd_vol_diff[j][k] = s2[j].usd_vol - s2[k].usd_vol;
				}
			}
			
			for(int j=0; j<4; j++) {
				for(int k=0; k<4; k++) {
					t.btc_vol_diff[j][k] = s2[j].btc_vol - s2[k].btc_vol;
				}
			}
			
			t.price_jump = bitstamp_buff.get(i-1).price - bitstamp_buff.get(i).price;
			t.price = bitstamp_buff.get(i).price;
			train_buff.add(t);
		}
		
		for(int i=0; i<train_buff.size(); i+=8) {
			
			if(i + 100 > train_buff.size()) {
				break;
			}
			
			Classify c = new Classify(train_buff, i, i + 100);
			class_buff.add(c);
		}
	}
	
	// This returns the set of training samples
	public ArrayList<Train> TrainBuff() {
		return train_buff;
	}
	
	// This creates the hierarchy of classifiers
	public Dataset BuildHierarchy() throws IOException {
		
		Dataset data = Classify.TrainingInstances(train_buff, 0, train_buff.size());
		
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		for(int i=0; i<class_buff.size(); i++) {
			buff.add(new KDNode(class_buff.get(i), i));
		}
		
		KDNode.BuildHierarchyTree(data, Classify.DimNum(), buff);
		
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
		
		return data;
	}

}
