package OrderBook;

import java.util.ArrayList;

public class GameTheory {
	
	// This defines a strategy for an investor
	class Strategy {
		// This stores the price point
		int price;
		// This stores the investor utility
		float util;
	}

	// This stores the game matrix
	private ArrayList<ArrayList<Strategy>> game_mat = new ArrayList<ArrayList<Strategy>>();
	
	// This updates the game matrix for the set of buys
	public void CreateGameMat(ArrayList<Order> orders) {
		
		float net = 0;
		for(int i=0; i<orders.size(); i++) {
			orders.get(i).net_volume = net;
			net += orders.get(i).volume;
		}

		for(int i=0; i<orders.size(); i++) {
			ArrayList<Strategy> buff = new ArrayList<Strategy>();
			
			for(int j=Math.max(0, i - 5); j<Math.min(i + 5, orders.size()); j++) {
				if(j == i) {
					continue;
				}
				
				Strategy s = new Strategy();
				s.price = j;
				s.util = j / orders.get(j).net_volume;
				buff.add(s);
			}
			
			game_mat.add(buff);
		}
	}
	
	// This updates the game matrix for the set of buys
	public void CreateGameMat(ArrayList<Order> buy_orders, 
			ArrayList<Order> sell_orders) {
		
		CreateGameMat(buy_orders);
		
		CreateGameMat(sell_orders);
	}

}
