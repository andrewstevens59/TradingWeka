package OrderBook;


import java.awt.Color;
import java.util.Random;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.util.Rotation;


public class Histogram extends JFrame {

  private static final long serialVersionUID = 1L;

  public Histogram(String applicationTitle, String chartTitle) {
        super(applicationTitle);
        // based on the dataset we create the chart
        JFreeChart chart = createAgeHistoChart();
        // we put the chart into a panel
        ChartPanel chartPanel = new ChartPanel(chart);
        // default size
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        // add it to our application
        setContentPane(chartPanel);

    }
    

    private IntervalXYDataset createDataset() {
    	         HistogramDataset dataset = new HistogramDataset();
    	          //dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    	          
    	          double[] values = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
    	          dataset.addSeries("H1", values, 20);
    	         return dataset;     
    	    }
    
    private static double[] gaussianData(int size, double shift) {
    	
    	Random random = new Random();
    	         double[] d = new double[size];
    	 	          for (int i = 0; i < d.length; i++) {
    	            d[i] = random.nextGaussian() + shift;
    	         }
    	        return d;
    	     }
    
    
/** * Creates a chart */

    private JFreeChart createAgeHistoChart() {

        JFreeChart chart = ChartFactory.createHistogram(
           "Age Distribution",
           "Age",
           "Count",
           createDataset(),
            PlotOrientation.VERTICAL,
            true,
            true,
            false);

        //CategoryDataset dataset1 = createDataset1();
       // NumberAxis rangeAxis1 = new NumberAxis("Age");
       // rangeAxis1.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        XYPlot plot = chart.getXYPlot();

        
        XYItemRenderer renderer1 = plot.getRenderer();
        renderer1.setSeriesPaint(0, Color.MAGENTA);
        return chart;
}
    
    public static void main(String[] args) {
    	Histogram demo = new Histogram("Comparison", "Which operating system are you using?");
        demo.pack();
        demo.setVisible(true);
    }
} 
