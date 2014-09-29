package BitcoinExchange;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** @see http://stackoverflow.com/questions/5048852 */
public class DTSCTest extends ApplicationFrame {

    private static final String TITLE = "Bitcoin Exchange";
    private static final String START = "Start";
    private static final String STOP = "Stop";
    private static final float MINMAX = 100;
    private static final int COUNT = 2 * 60;
    private static final int FAST = 100;
    private static final int SLOW = FAST * 5;
    private static final Random random = new Random();
    private Timer timer;

    public DTSCTest(final String title) throws IOException {
        super(title);
        
        final DynamicTimeSeriesCollection dataset1 =
            new DynamicTimeSeriesCollection(1, COUNT, new Second());
        
        dataset1.setTimeBase(new Second(0, 0, 0, 1, 1, 2011));
        dataset1.addSeries(timeData("http://api.bitcoincharts.com/v1/trades.csv?symbol=bitstampUSD"), 0, "bitstamp");
        
        final DynamicTimeSeriesCollection dataset2 =
                new DynamicTimeSeriesCollection(1, COUNT, new Second());
            
        dataset2.setTimeBase(new Second(0, 0, 0, 1, 1, 2011));
        dataset2.addSeries(timeData("http://api.bitcoincharts.com/v1/trades.csv?symbol=mtgoxUSD"), 0, "mtgox");

        
        JFreeChart chart = createChart(null);
        final XYPlot plot = chart.getXYPlot();
        
        chart.setBackgroundPaint(Color.white);
        
        plot.setDataset(0, dataset1);
        plot.setDataset(1, dataset2);
        
        XYItemRenderer rend2 = new StandardXYItemRenderer();
        rend2.setSeriesPaint(0, Color.YELLOW);
        rend2.setSeriesStroke(0, new BasicStroke(4));
        plot.setRenderer(0, rend2);
        
        XYItemRenderer rend1 = new StandardXYItemRenderer();
        rend1.setSeriesPaint(0, Color.GREEN);
        rend1.setSeriesStroke(0, new BasicStroke(4));
        plot.setRenderer(1, rend1);
        

        this.add(new ChartPanel(chart), BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout());
        this.add(btnPanel, BorderLayout.SOUTH);

        timer = new Timer(SLOW, new ActionListener() {

            float[] newData1 = new float[1];
            float[] newData2 = new float[1];

            @Override
            public void actionPerformed(ActionEvent e) {
            	
                try {
					newData1[0] = (float)BitstampVal();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                dataset1.advanceTime();
                dataset1.appendData(newData1);
                
                try {
					newData2[0] = (float)MtgoxVal();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                dataset2.advanceTime();
                dataset2.appendData(newData2);
            }
        });
    }
    
    private double MtgoxVal() throws IOException, JSONException {
    	URL url = new URL("https://data.mtgox.com/api/2/BTCUSD/money/ticker_fast");
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		String str = d.readLine();
		JSONObject MyList = new JSONObject(str).getJSONObject("data");
		double val = MyList.getJSONObject("last").getDouble("value");
		
		return val;
    }
    
    private double BitstampVal() throws IOException, JSONException {
    	
    	URL url = new URL("https://www.bitstamp.net/api/ticker/");
    	
    	URLConnection conn = url.openConnection();
    	conn.addRequestProperty("User-Agent", 
        "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

    	conn.connect();
        
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		String str = d.readLine();
		JSONObject MyList = new JSONObject(str);
		return MyList.getDouble("last");
    }

    private float[] timeData(String url_str) throws IOException {
    	
    	URL url = new URL(url_str);
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));

		String str;
		ArrayList<Float> buff = new ArrayList<Float>();
		while((str = d.readLine()) != null) {
			StringTokenizer strtok = new StringTokenizer(str, ",");
			String tok1 = strtok.nextToken();
			String tok2 = strtok.nextToken();
			buff.add(Float.valueOf(tok2));
		}
		
		float val[] = new float[buff.size()];
		for(int i=0; i<buff.size(); i++) {
			val[i] = buff.get(i);
		}
		
        return val;
    }

    private JFreeChart createChart(final XYDataset dataset) {
    	
        final JFreeChart result = ChartFactory.createTimeSeriesChart(
            TITLE, "hh:mm:ss", "priceUSD", dataset, true, true, false);
        
        final XYPlot plot = result.getXYPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
        

        return result;
    }

    public void start() {
        timer.start();
    }

    public static void main(final String[] args) throws IOException, JSONException {
    	

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                DTSCTest demo = null;
				try {
					demo = new DTSCTest(TITLE);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                demo.pack();
                RefineryUtilities.centerFrameOnScreen(demo);
                demo.setVisible(true);
                demo.start();
            }
        });
    }
}