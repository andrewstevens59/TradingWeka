package OrderBook;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;

public class OrderBookHistory extends TimerTask {
	
	PrintStream out = null;

	public OrderBookHistory() throws FileNotFoundException {

		    String id = new String(Double.toString(Math.random()));
		    out = new PrintStream(new FileOutputStream("History/order" + id + ".txt"));
		
	}


	@Override
	public void run() {
		
		URL url = null;
		try {
			url = new URL("http://data.mtgox.com/code/data/getDepth.php");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		URLConnection conn = null;
		try {
			conn = url.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = null;
		try {
			in = new DataInputStream ( conn.getInputStream (  )  );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		try {
			String str = d.readLine();
			System.out.println(str);
			out.println(str);
		    out.flush();
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		   
	    //1- Taking an instance of Timer class.
       Timer timer = new Timer();

       //2- Taking an instance of class contains your repeated method.
       OrderBookHistory t = new OrderBookHistory();

       
       timer.scheduleAtFixedRate(t, 0, 60000);
    }

}
