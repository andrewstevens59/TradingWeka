import java.util.ArrayList;


public class CreateModel {
	
	// This stores the first layer of switching nodes
	private static ArrayList<ArrayList<SN>> layer = new ArrayList<ArrayList<SN>>();
	// This stores the total number of nodes in the network
	private static int node_num = 0;
	
	// This creates the set of edges between layers in the network
	private void CreateEdges() {
		
		for(int i=0; i<layer.size()-1; i++) {
			
			for(int j=0; j<layer.get(i).size(); j++) {
				SN src = layer.get(i).get(j);
				
				for(int k=0; k<layer.get(i+1).size(); k++) {
					SN dst = layer.get(i+1).get(k);
					
					SNLink f_link = new SNLink();
					f_link.src = src;
					f_link.dst = dst;
					
					SNLink b_link = new SNLink();
					b_link.src = dst;
					b_link.dst = src;
					
					SNLink prev_ptr = src.forward_link;
					src.forward_link = f_link;
					f_link.next_ptr = prev_ptr;
					
					prev_ptr = dst.backward_link;
					dst.backward_link = b_link;
					b_link.next_ptr = prev_ptr;
				}
			}
		}
		
		// connect inputs to each other
		for(int j=0; j<layer.get(0).size(); j++) {
			SN src = layer.get(0).get(j);
			
			for(int k=0; k<layer.get(0).size(); k++) {
				SN dst = layer.get(0).get(k);
				
				SNLink f_link = new SNLink();
				f_link.src = src;
				f_link.dst = dst;
				
				SNLink b_link = new SNLink();
				b_link.src = dst;
				b_link.dst = src;
				
				SNLink prev_ptr = src.forward_link;
				src.forward_link = f_link;
				f_link.next_ptr = prev_ptr;
				
				prev_ptr = dst.backward_link;
				dst.backward_link = b_link;
				b_link.next_ptr = prev_ptr;
			}
		}
	}
	
	// This returns the model
	public static ArrayList<ArrayList<SN>> Model() {
		return layer;
	}
	
	// This returns the total number of nodes
	public static int NodeNum() {
		return node_num;
	}
	

	public CreateModel(int input_num, int output_num, int layer_num) {
		
		ArrayList<SN> input_layer = new ArrayList<SN>();
		for(int j=0; j<input_num; j++) {
			input_layer.add(new SN());
		}
		
		ArrayList<SN> output_layer = new ArrayList<SN>();
		for(int j=0; j<output_num; j++) {
			output_layer.add(new SN());
		}
		
		layer.add(input_layer);
		
		for(int i=0; i<layer_num; i++) {
			ArrayList<SN> hidden_layer = new ArrayList<SN>();
			for(int j=0; j<input_num; j++) {
				hidden_layer.add(new SN());
			}
			
			layer.add(hidden_layer);
		}
		
		layer.add(output_layer);
		
		node_num = 0;
		for(int i=0; i<layer.size(); i++) {
			
			for(int j=0; j<layer.get(i).size(); j++) {
				layer.get(i).get(j).node_id = node_num++;
			}
		}

		CreateEdges();
	}

}
