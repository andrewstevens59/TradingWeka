package MoveParticle;

import java.io.DataOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import net.sf.javaml.core.Dataset;

public class Main {

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		FXData s = new FXData();
		s.ReadFXData("FXData");
		s.GenerateTrainingSet();
		s.BuildHierarchy();
		
		int train_num = 300;
		ArrayList<SampleGraphSet> graph = new ArrayList<SampleGraphSet>();
		
		for(int i=0; i<s.CurrPairNum(); i++) {
			graph.add(new SampleGraphSet(s.DataSet(i), train_num));
		}
		
		
		float num[] = new float[s.CurrencyNum()];
		float currency[] = new float[s.CurrencyNum()];
		for(int i=0; i<currency.length; i++) {
			currency[i] = 0;
			num[i] = 0;
		}
		
		for(int i=0; i<s.CurrPairNum(); i++) {
			num[s.CurrencyID1(i)]++;
			num[s.CurrencyID2(i)]++;
		}
		
		currency[s.CurrencyID("USD")] = 60000;
		
		float init = -1;
		for(int j=0; j<train_num; j++) {
			
			int usd_id = s.CurrencyID("USD");
			float usd = currency[usd_id];
			
			for(int i=0; i<s.CurrPairNum(); i++) {
				
				ArrayList<Train> train_buff = s.TrainBuff(i);
				KDNode.SetClassID(i);
				
				SampleGraphSet g = graph.get(i);
				int id = g.TrainInst();
				
				g.GrowSampleSet(true);
				g.ReduceSamples();
				
				
				int id2 = s.CurrencyID1(i);
				int id1 = s.CurrencyID2(i);
				
				if(id2 == usd_id) {
					float val = currency[id1];
					usd += val / train_buff.get(id).exch_rate;
				} else {
					float val = currency[id2];
					usd += val * train_buff.get(id).exch_rate;
				}
				
				int output = g.NextDayOutput();
				
				if(output > 1 && currency[id1] > 0) {
					float val = currency[id1] / num[id1];
					currency[id2] += val / train_buff.get(id).exch_rate;
					currency[id1] -= val;
				}
				
				if(output < -1 && currency[id2] > 0) {
					float val = currency[id2] / num[id2];
					currency[id1] += val * train_buff.get(id).exch_rate;
					currency[id2] -= val;
				}
				
				if((j % 50) == 0 && j > 0) {
					//g.UpdateGraphSet(s.DataSet(i), train_num);
				}
				//System.out.println(s.Currency2(i)+": "+currency[id1]+"  "+s.Currency1(i)+": "+currency[id2]);
			}
			
			if(init == -1) {
				init = usd;
			}
			
			System.out.println("Run "+j);
			System.out.println("Profit: "+(usd - init)+" *************************");
			
		}
		
	}

}
