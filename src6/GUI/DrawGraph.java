package GUI;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

import optimizeTree.ConstructTree;
import state.PointPlot;
import state.State;
import stateMachine.CodeException;

@SuppressWarnings("serial")
public class DrawGraph extends JPanel {
   private static int MIN_Y = 0x7FFFFFFF;
   private static int MAX_Y = -0x7FFFFFFF;
   private static int MIN_X = 0x7FFFFFFF;
   private static int MAX_X = -0x7FFFFFFF;
   
   private static final int PREF_W = 800;
   private static final int PREF_H = 650;
   private static final int BORDER_GAP = 30;
   private static final Color GRAPH_COLOR[] = {Color.green, Color.red, Color.blue, Color.yellow, Color.gray};
   private static final Color GRAPH_POINT_COLOR = new Color(150, 50, 50, 180);
   private static final Stroke GRAPH_STROKE = new BasicStroke(3f);
   private static final int GRAPH_POINT_WIDTH = 12;
   private static final int Y_HATCH_CNT = 10;
   private List<Double> scores;
   
   private ArrayList<State> state_buff;
   private ArrayList<Double> val_buff;
   private ConstructTree tree;
   private Timer timer;
   private List<SPoint> graphPoints = null;
   private SPoint show_info = null;
   
   // This stores a point in the graph
   class SPoint {
	   double x;
	   double y;
	   State s;
	   
	   public SPoint(double x, double y, State s) {
		   this.x = x;
		   this.y = y;
		   this.s = s;
	   }
   }
   
   // This stores a line in the graph
   class SLine {
	   // This stores the first point
	   SPoint pt1;
	   // This stores the second point
	   SPoint pt2;
	   // This stores the line id
	   int line_id;
	   
	   public SLine(SPoint pt1, SPoint pt2, int line_id) {
		   this.pt1 = pt1;
		   this.pt2 = pt2;
		   this.line_id = line_id;
	   }
   }
   
   
   // This is used to draw the graph
   private void DrawGraphAxis(Graphics2D g2, List<SPoint> graphPoints) {
	   
	   MIN_Y = 0x7FFFFFFF;
	   MAX_Y = -0x7FFFFFFF;
	   MIN_X = 0x7FFFFFFF;
	   MAX_X = -0x7FFFFFFF;
	   for (int i = 0; i < graphPoints.size(); i++) {
		   MIN_Y = (int) Math.min(MIN_Y, graphPoints.get(i).y);
		   MAX_Y = (int) Math.max(MAX_Y, graphPoints.get(i).y);
		   MIN_X = (int) Math.min(MIN_X, graphPoints.get(i).x);
		   MAX_X = (int) Math.max(MAX_X, graphPoints.get(i).x);
	   }
	   
	   double xScale = ((double) getWidth() - 2 * BORDER_GAP) / (MAX_X - MIN_X);
	   double yScale = ((double) getHeight() - 2 * BORDER_GAP) / (MAX_Y - MIN_Y);

	   for (int i = 0; i < graphPoints.size(); i++) {
		  graphPoints.get(i).x = (int) ((graphPoints.get(i).x - MIN_X) * xScale + BORDER_GAP);
	      graphPoints.get(i).y = (int) ((MAX_Y - graphPoints.get(i).y) * yScale + BORDER_GAP);
	   }
	   
	   // create x and y axes 
	   	  int x_offset = (int) ((Math.min(MAX_X, 0) - MIN_X) * xScale + BORDER_GAP);
	   	  int y_offset = (int) ((MAX_Y - Math.max(MIN_Y, 0)) * yScale + BORDER_GAP);

	      g2.drawLine(x_offset, getHeight(), x_offset, 0);
	      g2.drawLine(0, y_offset, getWidth(), y_offset);

	      // create hatch marks for y axis. 
	      for (int i = 0; i < Y_HATCH_CNT; i++) {
	         int x0 = x_offset;
	         int x1 = GRAPH_POINT_WIDTH + x_offset;
	         int y0 = getHeight() - (((i + 1) * (getHeight() - BORDER_GAP * 2)) / Y_HATCH_CNT + BORDER_GAP);
	         int y1 = y0;
	         g2.drawLine(x0, y0, x1, y1);
	      }

	      // and for x axis
	      for (int i = 0; i < graphPoints.size() - 1; i++) {
	         int x0 = (i + 1) * (getWidth() - BORDER_GAP * 2) / (graphPoints.size() - 1) + BORDER_GAP;
	         int x1 = x0;
	         int y0 = y_offset;
	         int y1 = y0 - GRAPH_POINT_WIDTH;
	         g2.drawLine(x0, y0, x1, y1);
	      } 
   }
   
   // This plots the points and line on the graph
   private void DrawGraphLines(Graphics2D g2, List<SLine> graphLines) {
	   
	   Stroke oldStroke = g2.getStroke();
       g2.setStroke(GRAPH_STROKE);
       for (int i = 0; i < graphLines.size(); i++) {
    	   SLine line = graphLines.get(i);
    	   g2.setColor(GRAPH_COLOR[line.line_id]);
           int x1 = (int) line.pt1.x;
           int y1 = (int) line.pt1.y;
           int x2 = (int) line.pt2.x;
           int y2 = (int) line.pt2.y;
           
           System.out.println(x1+" "+y1+"       "+x2+" "+y2+"        "+line.line_id);
           g2.drawLine(x1, y1, x2, y2);         
       }

       g2.setStroke(oldStroke);      
       for (int i = 0; i < graphLines.size(); i++) {
    	   SLine line = graphLines.get(i);
    	   g2.setColor(GRAPH_COLOR[line.line_id]);
    	   
           int x = (int) (line.pt1.x - GRAPH_POINT_WIDTH / 2);
           int y = (int) (line.pt1.y - GRAPH_POINT_WIDTH / 2);
           int ovalW = GRAPH_POINT_WIDTH;
           int ovalH = GRAPH_POINT_WIDTH;
           g2.fillOval(x, y, ovalW, ovalH);
       }
   }

   public DrawGraph(String program) throws IOException, CodeException {


      val_buff = new ArrayList<Double>();
  	  tree = new ConstructTree(program.toCharArray());
  	  this.scores = val_buff;
  	  
  	  
  	  timer = new Timer(1, new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
    	//...Update the progress bar...

        /*	try {
				tree.GenerateStateSpaceCG(1000, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

        	repaint();
        }    
       });
    

       timer.start();
       
       addMouseListener(new MouseListener() {
           @Override
           public void mouseReleased(MouseEvent e) {
           }
           @Override
           public void mousePressed(MouseEvent e) {
        	   
        	   if(state_buff == null) {
        		   return;
        	   }
        	   
        	   int min = 99999;
        	   int x = (int) e.getPoint().getX();
        	   int y = (int) e.getPoint().getY();
        	   for(int i=0; i<graphPoints.size(); i++) {
        		   int diffx = (int) (graphPoints.get(i).x - x);
        		   int diffy = (int) (graphPoints.get(i).y - y);
        		   int dist = (diffx * diffx) + (diffy * diffy);
        		   if(dist < min) {
        			   min = dist;
        			   show_info = graphPoints.get(i);
        		   }
        	   }
        	   
        	   System.out.println(min+" &&&");
        	   if(min > 30) {
        		   show_info = null;
        	   }
        	   
        	   repaint();
        	   
           }
           @Override
           public void mouseExited(MouseEvent e) {
           }
           @Override
           public void mouseEntered(MouseEvent e) {
           }
           @Override
           public void mouseClicked(MouseEvent e) {
           }
       });
       
   }
   
   // This stops drawing the improvement graph
   public void TerminateTimer() {
	   timer.stop();
   }
   
   // This draws the key plot names for the graph
   private void DrawPlotKey(Graphics2D g2) {
	   
	   ArrayList<String> keys = tree.PlotNames();
	   Font font = new Font("Serif", Font.PLAIN, 20);
	   g2.setFont(font);
	   
	   int max_width = 0;
	   for(int i=0; i<keys.size(); i++) {
		   max_width = Math.max(max_width, keys.get(i).length());
	   }
	   
	   max_width *= 15;
	   for(int i=0; i<keys.size(); i++) {
		   
		   g2.setColor(GRAPH_COLOR[i]);
    	   
		   int x1 = getWidth() - max_width;
		   int y1 = getHeight() - (18 * i + 10);
           int x = (int) (x1 - GRAPH_POINT_WIDTH / 2);
           int y = (int) (y1 - GRAPH_POINT_WIDTH / 2) - 5;
           int ovalW = GRAPH_POINT_WIDTH;
           int ovalH = GRAPH_POINT_WIDTH;
           g2.fillOval(x, y, ovalW, ovalH);
           
           g2.setColor(Color.black);
		   g2.drawString(keys.get(i), x1 + GRAPH_POINT_WIDTH, y1);
	   }
	   
	   int height = getHeight() - 15 * keys.size() - 20;
	   int width = getWidth() - max_width - 20;
	   g2.drawLine(width, height, getWidth(), height);   
	   g2.drawLine(width, getHeight(), width, height); 
   }
   
   // This draws the information associated with a state
   private void ShowState(Graphics2D g2) {
	   if(show_info == null) {
		   return;
	   }
	   
	   Font font = new Font("Serif", Font.PLAIN, 16);
	   g2.setFont(font);
	  
	   int start = 0;
	   int x = (int) show_info.x;
	   int y = (int) show_info.y;
	   ArrayList<String> buff = new ArrayList<String>();
	   String str = show_info.s.print_str;
	   for(int i=0; i<str.length(); i++) {
		   if(str.charAt(i) == '\n') {
			   buff.add(str.substring(start, i));
			   start = i + 1;
		   }
	   }

	   int max_width = 0;
	   for(int i=0; i<buff.size(); i++) {
		   max_width = Math.max(max_width, buff.get(i).length());
	   }
	   
	   max_width *= 9;
	   g2.setColor(Color.black);
	   g2.setPaint(Color.white);
	
	   int height = 16 * buff.size() + 20;
	   int x1 = x, y1 = y;
	   if(x > getWidth() / 2) {
		   x1 = x - max_width;
	   } 
	   
	   if(y > getHeight() / 2) {
		   y1 = y - height;
	   } 
	   
	   g2.fill(new Rectangle2D.Double(x1, y1, max_width, height));
	   
	   g2.setPaint(Color.black);
	   g2.drawRoundRect(x1, y1, max_width, height, 0, 0);
	   
	   for(int i=0; i<buff.size(); i++) {
		   g2.drawString(buff.get(i), x1 + 2, y1 + (16 * i + 15));
	   }
   }

   @Override
   protected void paintComponent(Graphics g) {
	   
	  super.setBackground(Color.WHITE);
      super.paintComponent(g);
      
      Graphics2D g2 = (Graphics2D)g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      
      graphPoints = new ArrayList<SPoint>();
      List<SLine> graphLines = new ArrayList<SLine>();
      
      if(state_buff == null) {
	      for (int i = 0; i < scores.size(); i++) {
	         graphPoints.add(new SPoint((double)i, scores.get(i), null));
	      }
	      
	      for (int i = 0; i < graphPoints.size() - 1; i++) {
	    	  graphLines.add(new SLine(graphPoints.get(i), graphPoints.get(i+1), 0));
	      }
      } else {
    	  
    	  try {
		      ArrayList<State> states = tree.OptimalPath();
		      for (int i = 0; i < states.size(); i+=2) {
		    	  PointPlot ptr1 = states.get(i).points;
		    	  PointPlot ptr2 = states.get(i + 1).points;
				  while(ptr1 != null && ptr2 != null) {
					  if(ptr1.plot_id == ptr2.plot_id) {
						  if(ptr1.x != ptr2.x || ptr1.y != ptr2.y) {
							  int offset = graphPoints.size();
							  graphPoints.add(new SPoint(ptr1.x, ptr1.y, states.get(i)));
							  graphPoints.add(new SPoint(ptr2.x, ptr2.y, states.get(i + 1)));
							  graphLines.add(new SLine(graphPoints.get(offset), graphPoints.get(offset+1), ptr1.plot_id));
						  }
					  }
					  ptr1 = ptr1.next_ptr;
					  ptr2 = ptr2.next_ptr;
				  }
		      }
		      
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      }

      DrawGraphAxis(g2, graphPoints);
      DrawGraphLines(g2, graphLines);
      
      if(state_buff != null) {
    	  DrawPlotKey(g2);
    	  ShowState(g2);
      }
   }
   
   // This is used to construct the optimal tree path
   public void ConstructOptimalPath() throws IOException, CodeException {
	   tree.ConstructOptimalPath(null);
	   state_buff = tree.OptimalPath();
   }

   @Override
   public Dimension getPreferredSize() {
      return new Dimension(PREF_W, PREF_H);
   }
   

}
