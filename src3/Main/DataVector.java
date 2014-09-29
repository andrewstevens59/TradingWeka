package Main;


public class DataVector {

	public float val[];
	
	public DataVector(int dim) {
		val = new float[dim];
	}
	
	// This returns the string
	public String toString() {
		
		String str = new String();
		for(int i=0; i<val.length; i++) {
			str += val[i]+ " ";
		}
		
		return str;
	}

}
