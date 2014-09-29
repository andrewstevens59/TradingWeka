package SOptimize;

public class TestDFT { 
	
	public static void main(String args[]) { 
		int N = 64; double T = 2.0; double tn, fk; double data[] = new double[2*N];  
		// A 2 Hz cosine function that is sampled // for two seconds. First load the amplitude data  
		for(int i=0; i<N; ++i) { data[2*i] = Math.cos(4.0*Math.PI*i*T/N); data[2*i+1] = 0.0; }  
		// Compute the DFT  
		double X[] = Fourier.discreteFT(data, N, true); 
		// Print out the frequency spectrum 
		for(int k=0; k<N; ++k) { fk = k/T; System.out.println("f["+k+"] = "+fk+"Xr["+k+ "] = "+X[2*k]+ " Xi["+k+"] = "+X[2*k + 1]); }
		// Reconstruct a 2 Hz cosine wave from its // frequency spectrum. First load the frequency // data.  
		for(int i=0; i<N; ++i) { data[2*i] = 0.0; data[2*i+1] = 0.0; if (i == 4 || i == N-4 ) { data[2*i] = 0.5; } }
		// Compute the DFT  
		double x[] = Fourier.discreteFT(data, N, false);  // Print out the amplitude vs time data.  
		System.out.println(); for(int n=0; n<N; ++n) { tn = n*T/N; System.out.println("t["+n+"] = "+tn+"xr["+n+ "] = "+x[2*n]+" xi["+n+"] ="+x[2*n + 1]); } 
	}
}
