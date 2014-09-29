package SVM;

import java.io.IOException;
import java.util.ArrayList;

import DecisionNetwork.DataSet;
import DecisionNetwork.DecisionNode;
import DecisionNetwork.Voter;

public class Main {
	
	// This builds the conditional bayesian network 
	public void BuildNetwork(ArrayList<SVMNode> buff, int num, int start, int end) {
		
		if(end - start <= 1) {
			buff.add(new SVMNode(start, null));
			return;
		}
		
		if(num == 0) {
			
			ArrayList<SVMNode> temp = new ArrayList<SVMNode>();
			
			int middle = (end + start ) >> 1;
			BuildNetwork(temp, 2, start, middle);
			BuildNetwork(temp, 2, middle, end);
			
			buff.add(new SVMNode(0, temp));
		}
		
		int middle = (end + start ) >> 1;
		BuildNetwork(buff, num - 1, start, middle);
		BuildNetwork(buff, num - 1, middle, end);
	}
	
	
	public Main() throws NumberFormatException, IOException {
		DataSet d = new DataSet();
		
		ArrayList<SVMNode> buff = new ArrayList<SVMNode>();
		BuildNetwork(buff, 2, 0, d.DimNum());
		
		SVMNode s = new SVMNode(0, buff);
		
		s.Train(d);
		
		
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		new Main();

	}

}
