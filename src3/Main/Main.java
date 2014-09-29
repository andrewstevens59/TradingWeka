package Main;

import java.io.*;


import java.awt.*;

import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import weka.core.Debug.Random;

public class Main {
	
	// This stores the set of classifiers
	ArrayList<Classifier> class_buff = new ArrayList<Classifier>();
	
	// This creates the set of classifiers
	private void CreateClassifiers() throws IOException {
		DataSet d = new DataSet();
		d.WriteDataSet();
		
		ArrayList<KDNode> buff = new ArrayList<KDNode>();
		for(int i=0; i<d.RegimeSet().size(); i++) {
			
			Classifier c = new Classifier(d.RegimeSet().get(i), "Classify/class1_" + i);
			buff.add(new KDNode(c, i));
			class_buff.add(c);
		}
		
		KDNode.BuildHierarchyTree(d, buff);
		KDNode.WriteKDTree();
		
	}
	
	public Main() throws NumberFormatException, IOException {
		
		//CreateClassifiers();System.exit(0);
		
		DataSet d = new DataSet();
		d.ReadDataSet();
		
		KDNode.ReadKDTree();
		
		KDNode.CreateSLinks(d, 3000);
		
		HMM hmm = null;
			
		for(int i=0; i<5; i++) {
			System.out.println(i);
			hmm = new HMM(d);
			hmm.CreateHMM(d);
			
			System.out.println("***********************");
			System.out.println(hmm.TrainClassifier(d));
		}
		
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		//new Main();
		
		System.out.println("Creating Rengine (with arguments)");
		// 1) we pass the arguments from the command line
		// 2) we won't use the main loop at first, we'll start it later
		// 3) the callbacks are implemented by the TextConsole class above
		Rengine re=new Rengine(args, false, null);
	        System.out.println("Rengine created, waiting for R");
		// the engine creates R is a new thread, so we should wait until it's ready
	        if (!re.waitForR()) {
	            System.out.println("Cannot load R");
	            return;
	        }
	        
	        
	        
	        
	        re.eval("A <- matrix(nrow=5, ncol=2, c(1, 4, 2, 5, 23, 5, 2, 1, 7, 7))");
	        re.eval("G <- ar.ols(A, p=1)");
	       // REXP exp = re.eval("predict(G, n.ahead=1, CI=0.95)");

	        REXP exp = re.eval("paste(capture.output({print(G)}),collapse='\\n')");
	        System.out.println(exp.asString());
	        System.exit(0);
	

	        
	        double x[] = {4, 2, 54, 2, 4, 7, 8};
	        double y[] = {14, 12, 54, 12, 14, 17, 18};
	        
	        double z[] = new double[]{4, 45};
	        re.eval(" A = matrix( c(2, 4, 3, 1, 5, 7), nrow=2,             ncol=3,             byrow = TRUE) )");
	        re.assign("A[,1]", z);

	        
	     // Assign y to similarly named variable in R
	        re.assign("y", y);
	        // Lets see if it was assigned correctly
	        System.out.println(Arrays.toString(re.eval("y").asDoubleArray()));
	        // output [11.1, 13.2, 15.6, 27.4, 39.2, 113.1, 135.1]
	        // Assign x to similarly named variable in R
	        re.assign("x", x);
	        // fit to y=mx+c
	        re.eval("a=lm(y~x)");
	        // lets print the values
	        RVector fit = re.eval("a").asVector();
	        System.out.println("The intercept is " + fit.at(0).asDoubleArray()[0]);
	        // prints The intercept is -19.60087719298246
	        System.out.println("The slope is " + fit.at(0).asDoubleArray()[1]);
	        // prints The slope is 8.481140350877192
	        
	        System.exit(0);

			/*re.eval("data(iris)");
			System.out.println(re.eval("objects()"));
			System.out.println(re.eval("sqrt(36)"));

			System.exit(0);
			
	        // simple assignment like a<-"hello" (env=0 means use R_GlobalEnv)
	        long xp1 = re.rniPutString("hello");
	        re.rniAssign("a", xp1, 0);

	        // Example: how to create a named list or data.frame
	        double da[] = {1.2, 2.3, 4.5};
	        double db[] = {1.4, 2.6, 4.2};
	        long xp3 = re.rniPutDoubleArray(da);
	        long xp4 = re.rniPutDoubleArray(db);
	        
	        // now build a list (generic vector is how that's called in R)
	        long la[] = {xp3, xp4};
	        long xp5 = re.rniPutVector(la);

	        // now let's add names
	        String sa[] = {"a","b"};
	        long xp2 = re.rniPutStringArray(sa);
	        re.rniSetAttr(xp5, "names", xp2);

	        // ok, we have a proper list now
	        // we could use assign and then eval "b<-data.frame(b)", but for now let's build it by hand:       
	        String rn[] = {"1", "2", "3"};
	        long xp7 = re.rniPutStringArray(rn);
	        re.rniSetAttr(xp5, "row.names", xp7);
	        
	        long xp6 = re.rniPutString("data.frame");
	        re.rniSetAttr(xp5, "class", xp6);
	        
	        // assign the whole thing to the "b" variable
	        re.rniAssign("b", xp5, 0);
	        
	        {
	            System.out.println("Parsing");
	            long e=re.rniParse("data(iris)", 1);
	            System.out.println("Result = "+e+", running eval");
	            long r=re.rniEval(e, 0);
	            System.out.println("Result = "+r+", building REXP");
	            REXP x=new REXP(re, r);
	            System.out.println("REXP result = "+x);
	        }
	        {
	            System.out.println("Parsing");
	            long e=re.rniParse("iris", 1);
	            System.out.println("Result = "+e+", running eval");
	            long r=re.rniEval(e, 0);
	            System.out.println("Result = "+r+", building REXP");
	            REXP x=new REXP(re, r);
	            System.out.println("REXP result = "+x);
	        }
	        {
	            System.out.println("Parsing");
	            long e=re.rniParse("names(iris)", 1);
	            System.out.println("Result = "+e+", running eval");
	            long r=re.rniEval(e, 0);
	            System.out.println("Result = "+r+", building REXP");
	            REXP x=new REXP(re, r);
	            System.out.println("REXP result = "+x);
	            String s[]=x.asStringArray();
	            if (s!=null) {
	                int i=0; while (i<s.length) { System.out.println("["+i+"] \""+s[i]+"\""); i++; }
	            }
	        }
	        {
	            System.out.println("Parsing");
	            long e=re.rniParse("rnorm(10)", 1);
	            System.out.println("Result = "+e+", running eval");
	            long r=re.rniEval(e, 0);
	            System.out.println("Result = "+r+", building REXP");
	            REXP x=new REXP(re, r);
	            System.out.println("REXP result = "+x);
	            double d[]=x.asDoubleArray();
	            if (d!=null) {
	                int i=0; while (i<d.length) { System.out.print(((i==0)?"":", ")+d[i]); i++; }
	                System.out.println("");
	            }
	            System.out.println("");
	        }
	        {
	            REXP x=re.eval("1:10");
	            System.out.println("REXP result = "+x);
	            int d[]=x.asIntArray();
	            if (d!=null) {
	                int i=0; while (i<d.length) { System.out.print(((i==0)?"":", ")+d[i]); i++; }
	                System.out.println("");
	            }
	        }

	        re.eval("print(1:10/3)");*/
	        
		if (true) {
		    // so far we used R as a computational slave without REPL
		    // now we start the loop, so the user can use the console
		    System.out.println("Now the console is yours ... have fun");
		    re.startMainLoop();
		} else {
		    re.end();
		    System.out.println("end");
		}

	}

}
