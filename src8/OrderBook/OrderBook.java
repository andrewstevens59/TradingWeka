package OrderBook;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import java.io.InputStream;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.json.*;


public class OrderBook {
	
	public static BufferedReader read(String url) throws Exception{
		return new BufferedReader(
			new InputStreamReader(
				new URL(url).openStream()));
	}

	public static void main (String[] args) throws Exception{

		URL url = new URL("http://data.mtgox.com/code/data/getDepth.php");
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		String str = d.readLine();

		JSONArray MyList = new JSONObject(str).getJSONArray("asks");
		for(int i=0; i<MyList.length(); i++) {
			System.out.println(((JSONArray)MyList.get(i)).getDouble(0));
		}
		
		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.RELATIVE_FREQUENCY);

		double[] values = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
		dataset.addSeries("H1", values, 10, 0.0, 10.0);
	}
}
