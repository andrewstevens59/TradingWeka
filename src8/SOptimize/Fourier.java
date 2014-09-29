package SOptimize;

public class Fourier {  // Discrete Fourier transform  
	
	public static double[] discreteFT(double[] data, int N, boolean forward) { 
		double X[] = new double[2*N]; 
		double omega; int k, ki, kr, n;  
		// If this is a inverse transform, reverse the // sign of the angle so the sin() terms will // change sign.  
		if (forward) { omega = 2.0*Math.PI/N; } else { omega = -2.0*Math.PI/N; }  
		// Perform the discrete Fourier transform. // The real and imaginary data are stored in the // x[] and X[] vectors as pairs, one real and // one imaginary value for each sample point.  
		for(k=0; k<N; ++k) { kr = 2*k; ki = 2*k + 1; X[kr] = 0.0; X[ki] = 0.0; for(n=0; n<N; ++n) { X[kr] += data[2*n]*Math.cos(omega*n*k) + data[2*n+1]*Math.sin(omega*n*k); X[ki] += -data[2*n]*Math.sin(omega*n*k) + data[2*n+1]*Math.cos(omega*n*k); } }  
		// If this is a forward transform, multiply // in the 1/N terms  
		if ( forward ) { for(k=0; k<N; ++k) { X[2*k] /= N; X[2*k + 1] /= N; } }  
		// Return the transformed data.  
		
		return X; 
		

	} 

}