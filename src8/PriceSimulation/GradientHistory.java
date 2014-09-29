package PriceSimulation;

import java.util.ArrayList;

public class GradientHistory {
	
	// This stores the set of trend lines
	class TrendLine {
		// This stores the intercept
		float intercept;
		// This stores the gradient
		float grad;
		// This stores the starting time
		int time;
		
		public TrendLine(float grad, float intercept) {
			this.grad = grad;
			this.intercept = intercept;
		}
	}

	// This stores the set of gradients 
	private ArrayList<TrendLine> grad_buff = new ArrayList<TrendLine>();
	// This stores the window size
	private int window_size;
	// This store the discount factor for this window size
	private float discount_fact;
	
	public GradientHistory(int window_size, float discount_fact) {
		this.window_size = window_size;
		this.discount_fact = discount_fact;
	}
	
	// This updates the current gradient buffer
	public void UpdateGradHistory(ArrayList<Float> buff) {
		
		if(buff.size() < window_size) {
			return;
		}
		
		LinearRegression.Regression(buff, buff.size() - window_size, buff.size());
		grad_buff.add(new TrendLine(LinearRegression.gradient, LinearRegression.intercept));
	}
	
	// This is used to find the distribution of gradient changes 
	private void GradientChange(ArrayList<Float> buff, int size) {
		
		if(grad_buff.size() == 0) {
			return;
		}
		
		for(int i=Math.max(0, grad_buff.size() - size); i<grad_buff.size() - 1; i++) {
			float change = grad_buff.get(i + 1).grad / grad_buff.get(i).grad;
			buff.add(change);
		}
	}
	
	// This generates the price distribution for this gradient history
	private void GeneratePriceDist(State s, int depth, ArrayList<Float> dist, float gamma) {
		
		if(depth == 10) {
			return;
		}
		
		int time = s.time_period + 1;
		
		TrendLine trend = grad_buff.get(grad_buff.size() - 1);
		float price = trend.grad * time + trend.intercept;
		State ns = s.AddState((int)price, gamma);
		
		GeneratePriceDist(ns, depth + 1, dist, gamma * discount_fact);
		
		for(int i=0; i<dist.size(); i++) {
			trend = grad_buff.get(grad_buff.size() - 1);
			float prev_price = trend.grad * s.time_period + trend.intercept;
			float new_trend = trend.grad * dist.get(i) + prev_price; 
			ns = s.AddState((int)new_trend, gamma);
			
			GeneratePriceDist(ns, depth + 1, dist, gamma * discount_fact);
		}
	}
	
	// This generates the price distribution for this gradient history
	public void GeneratePriceDist(float gamma, int time) {
		State.Clear();

		ArrayList<Float> dist = new ArrayList<Float>();
		GradientChange(dist, 10);
		
		State s = new State();
		s.time_period = time;
		
		GeneratePriceDist(s, 5, dist, 1.0f);
	}

}
