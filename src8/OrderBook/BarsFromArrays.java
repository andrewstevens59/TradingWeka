package OrderBook;

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


public class BarsFromArrays extends TimerTask {
	
	JFrame frame = new JFrame();
	
	// This draws the chart 
	private void DrawChart() throws IOException, JSONException {
		
		System.out.println("here");
		URL url = new URL("http://data.mtgox.com/code/data/getDepth.php");
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		String str = d.readLine();

		JSONArray MyList = new JSONObject(str).getJSONArray("asks");
		double[] xvalues1 = new double[MyList.length()];
	    double[] yvalues1 = new double[MyList.length()];
	      
		for(int i=0; i<MyList.length(); i++) {
			xvalues1[i] = ((JSONArray)MyList.get(i)).getDouble(0);
			yvalues1[i] = ((JSONArray)MyList.get(i)).getDouble(1);
		}
		
		MyList = new JSONObject(str).getJSONArray("bids");
		double[] xvalues2 = new double[MyList.length()];
	    double[] yvalues2 = new double[MyList.length()];
	      
		for(int i=0; i<MyList.length(); i++) {
			xvalues2[i] = ((JSONArray)MyList.get(i)).getDouble(0);
			yvalues2[i] = ((JSONArray)MyList.get(i)).getDouble(1);
		}

		
		
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