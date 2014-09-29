package SOptimize;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Rule implements Comparable {

	public TradingSignals fxset;
	public int price;
	public boolean is_buy;
	public int width;
	public int up_num = 0;
	public int down_num = 0;
	
	@Override
	public boolean equals(Object o) {
		
		if((o instanceof Rule) == false) {
			return false;
		}
		
		Rule r = (Rule)o;
		if(r.fxset != fxset) {
			return false;
		}
		
		if(r.price != price) {
			return false;
		}
		
		if(r.width != width) {
			return false;
		}
		
		
		return true;
	}
	
	// This reutrns the risk to return ratio
	public double Ratio() {
		return Math.max(up_num, down_num) / (up_num + down_num);
	}
	
	@Override
	public int compareTo(Object o) {
		
		if((o instanceof Rule) == false) {
			return 1;
		}
		
		Rule r = (Rule)o;
		
		if(r.Ratio() < Ratio()) {
			return -1;
		}
		
		if(r.Ratio() > Ratio()) {
			return 1;
		}
		
		return  0;
		
	}
	
	public void WriteRule(DataOutputStream os) throws IOException {
		os.writeInt(up_num);
		os.writeInt(down_num);
		os.writeInt(width);
		os.writeInt(price);
	}
	
	public void ReadRule(DataInputStream os) throws IOException {
		up_num = os.readInt();
		down_num = os.readInt();
		width = os.readInt();
		price = os.readInt();
	}

}
