package OrderBook;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
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
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

/** @see http://stackoverflow.com/questions/5048852 */
public class DTSCTest extends ApplicationFrame {

    private static final String TITLE = "Dynamic Series";
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
        final DynamicTimeSeriesCollection dataset =
            new DynamicTimeSeriesCollection(1, COUNT, new Second());
        
        dataset.setTimeBase(new Second(0, 0, 0, 1, 1, 2011));
        dataset.addSeries(gaussianData(), 0, "Gaussian data");
        JFreeChart chart = createChart(dataset);

        final JButton run = new JButton(STOP);
        run.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                if (STOP.equals(cmd)) {
                    timer.stop();
                    run.setText(START);
                } else {
                    timer.start();
                    run.setText(STOP);
                }
            }
        });

        final JComboBox combo = new JComboBox();
        combo.addItem("Fast");
        combo.addItem("Slow");
        combo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if ("Fast".equals(combo.getSelectedItem())) {
                    timer.setDelay(FAST);
                } else {
                    timer.setDelay(SLOW);
                }
            }
        });

        this.add(new ChartPanel(chart), BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(run);
        btnPanel.add(combo);
        this.add(btnPanel, BorderLayout.SOUTH);

        timer = new Timer(FAST, new ActionListener() {

            float[] newData = new float[1];

            @Override
            public void actionPerformed(ActionEvent e) {
                newData[0] = 95;//randomValue();
                dataset.advanceTime();
                dataset.appendData(newData);
            }
        });
    }

    private float randomValue() {
        return (float) (random.nextGaussian() * MINMAX / 3);
    }

    private float[] gaussianData() throws IOException {
    	
    	URL url = new URL("http://api.bitcoincharts.com/v1/trades.csv?symbol=bitstampUSD");
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
            TITLE, "hh:mm:ss", "milliVolts", dataset, true, true, false);
        final XYPlot plot = result.getXYPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
       // ValueAxis range = plot.getRangeAxis();
      //  range.setRange(-MINMAX, MINMAX);
        return result;
    }

    public void start() {
        timer.start();
    }

    public static void main(final String[] args) {
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