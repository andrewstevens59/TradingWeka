package BitcoinExchange;

import java.io.IOException;

public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		//TimeSeries t1 = new TimeSeries("bitstampUSD", 0, 10);
		//TimeSeries t2 = new TimeSeries("mtgoxUSD", 0, 10);
		//Classify c = new Classify(t2, t1);
		
		
		TimeSeries t = new TimeSeries();
		t.RetrieveTimeSeries("mtgoxUSD");

	}

}
