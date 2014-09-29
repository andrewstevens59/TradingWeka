package ParticleFilter;

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
		TimeSeries s = new TimeSeries();
		s.ReadBitStamp("BITSTAMPUSD.csv");
		s.ReadMtGox("MTGOXUSD.csv");
		s.GenerateTrainingSet();
		Dataset data = s.BuildHierarchy();
		ClassifyDynamics.BuildHiearchy();
		
		ArrayList<Train> train_buff = s.TrainBuff();
		
		int train_num = 300;
		SampleGraph g = new SampleGraph(data, train_num);
		g.InitSampleSet();
		
		float usd = 1000;
		float btc = 0;

		for(int j=0; j<train_num; j++) {
			
			int id = g.TrainInst();
			
			g.GrowSampleSet(true);
			
			for(int i=0; i<2; i++) {
				g.DecompressMaXDim();
				g.GrowSampleSet(false);
			}
			
			g.UpdateTrainingInst();
			g.Reset();
			System.out.println("Run "+j);
			
			
			int output = g.NextDayOutput();
			
			if(output > 0 && usd > 0) {
				btc += usd / train_buff.get(id).price;
				usd = 0;
			}
			
			if(output < 0 && btc > 0) {
				usd += btc * train_buff.get(id).price;
				btc = 0;
			}
			
			
			System.out.println("USD: "+usd+"  BTC: "+btc);
		}
		
	}

}
