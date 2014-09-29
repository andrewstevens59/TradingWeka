

import java.awt.EventQueue;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYBarDataset;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;
 
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;


public class BarsFromArrays extends TimerTask {
	
	JFrame frame = new JFrame();
	TimeSeriesChartDemo1 time_ser = new TimeSeriesChartDemo1("sdfs");
	
	// This draws the chart 
	private void DrawChart() throws IOException, JSONException {
		
		URL url = new URL("https://www.bitstamp.net/api/order_book/");
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		String str = d.readLine();
		
		System.out.println(str);
		
		JSONArray MyList = new JSONObject(str).getJSONArray("asks");
		int len = Math.min(20, MyList.length());
		double[] xvalues1 = new double[len];
	    double[] yvalues1 = new double[len];
	      
		for(int i=0; i<len; i++) {
			xvalues1[i] = ((JSONArray)MyList.get(i)).getDouble(0);
			yvalues1[i] = ((JSONArray)MyList.get(i)).getDouble(1);
		}

		MyList = new JSONObject(str).getJSONArray("bids");
		len = Math.min(20, MyList.length());
		double[] xvalues2 = new double[len];
	    double[] yvalues2 = new double[len];
	      
		for(int i=0; i<len; i++) {
			xvalues2[i] = ((JSONArray)MyList.get(i)).getDouble(0);
			yvalues2[i] = ((JSONArray)MyList.get(i)).getDouble(1);
		}

		time_ser.updateTimeSeries(xvalues1, yvalues1, xvalues2, yvalues2);
		
      double[][] valuepairs1 = new double[2][];
      valuepairs1[0] = xvalues1;
      valuepairs1[1] = yvalues1;
      
      double[][] valuepairs2 = new double[2][];
      valuepairs2[0] = xvalues2;
      valuepairs2[1] = yvalues2;
      
      DefaultXYDataset set = new DefaultXYDataset();
      set.addSeries("Asks",valuepairs1);  

      set.addSeries("Bids",valuepairs2);
      
      
      XYBarDataset barset = new XYBarDataset(set,0.05);
      
      JFreeChart chart = ChartFactory.createXYBarChart(
         "Order Book","Price",false,"Volume",
         barset,PlotOrientation.VERTICAL,true, true, false);
      
      frame.setContentPane(new ChartPanel(chart));
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
      frame.repaint();
	}
	
	public BarsFromArrays() throws IOException, JSONException {
		
   }
	
	public void run() {
		try {
			DrawChart();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
   public static void main(String[] args) throws JSONException, IOException {
	   
	 //1- Taking an instance of Timer class.
       Timer timer = new Timer();

       //2- Taking an instance of class contains your repeated method.
       BarsFromArrays t = new BarsFromArrays();

       
       timer.scheduleAtFixedRate(t, 0, 5000);
   }
}