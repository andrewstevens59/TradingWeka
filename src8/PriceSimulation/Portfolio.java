package PriceSimulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Scanner;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.doublealgo.Statistic;

public class Portfolio {
	
	// This stores the time series for each stock
	private ArrayList<ArrayList<Float>> stock_price = new ArrayList<ArrayList<Float>>();
	// This stores the set of financial returns
	DoubleMatrix2D return_mat;
	// This stores the covariance matrix
	DoubleMatrix2D covar_mat;
	
	// This optimizes the portfolio by minimising the variance
	private void OptimizePortfolio() throws Exception {
		
		// Objective function
		double[][] P = new double[stock_price.size()][stock_price.size()];
		for(int i=0; i<stock_price.size(); i++) {
			for(int j=i; j<stock_price.size(); j++) {
				P[i][j]	= covar_mat.get(i, j);
			}
		}
		
		for(int i=0; i<stock_price.size(); i++) {
			for(int j=i; j<stock_price.size(); j++) {
				P[i][j]	= P[j][i];
			}
		}
		
		PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(P, null, 0);

		//equalities
		double[][] A = new double[1][stock_price.size()];
		double[] b = new double[]{1};
		
		for(int i=0; i<stock_price.size(); i++) {
			A[0][i] = 1.0f;
		}

		//inequalities
		//ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
		//inequalities[0] = new LinearMultivariateRealFunction(new double[]{-1, 0}, 0);
		//inequalities[1] = new LinearMultivariateRealFunction(new double[]{0, -1}, 0);
		
		//optimization problem
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);

		double pos[] = new double[stock_price.size()];
		for(int i=0; i<stock_price.size(); i++) {
			pos[i] = 1.0f / stock_price.size();
		}
		
		or.setInitialPoint(pos);
		//or.setFi(inequalities); //if you want x>0 and y>0
		or.setA(A);
		or.setB(b);
		or.setToleranceFeas(1.E-12);
		or.setTolerance(1.E-12);
		
		// optimization
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);
		int returnCode = opt.optimize();
		
		double[] sol = opt.getOptimizationResponse().solution;
		for(int i=0; i<stock_price.size(); i++) {
			System.out.println(sol[i]);
		}
	}
	
	public Portfolio() throws Exception {
		File folder = new File("stock_data/");
		File[] listOfFiles = folder.listFiles();
		
		int min_size = 999999999;
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		        System.out.println(file.getName());
		        ArrayList<Float> buff = new ArrayList<Float>();
		   
		        
		        String content = new Scanner(new File("stock_data/"+file.getName())).useDelimiter("\\Z").next();
		        String set[] = content.split(" ");
		        for(int i=0; i<set.length; i++) {
		        	buff.add(Float.parseFloat(set[i]));
		        }
		        
		        if(buff.size() < 40) {
		        	continue;
		        }
		        
		        stock_price.add(buff);
		        min_size = Math.min(min_size, buff.size());
		    }
		}
		
		DoubleFactory2D F = DoubleFactory2D.dense;
		return_mat = F.make(min_size - 1, stock_price.size());
		System.out.println(min_size + " " + stock_price.size());
		
		for(int i=0; i<stock_price.size(); i++) {
			for(int j=0; j<min_size-1; j++) {
				float val1 = stock_price.get(i).get(j);
				float val2 = stock_price.get(i).get(j + 1);
				return_mat.set(j, i, val2 / val1);
			}
		}
		
		covar_mat = Statistic.covariance(return_mat);
		
		OptimizePortfolio();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		
         new Portfolio();


	}

}
