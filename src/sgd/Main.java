package sgd;

import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		// Here choose which experiment you want to try
		//SDIGA_experiment();
		CN2_experiment();
	}
	
	private static void CN2_experiment() {
		// parameters
		// maximum number of iterations during rule search - corresponds also to maximum complexity of rule (how many selectors it incorporates)
		int maxIterations = 10;
		// size of beam for beam search heuristic
		int beamSize = 5;
		// critical value from chi^2 tables, 9.24 corresponds to 2 degrees of freedom and 99% significance
		float minSignificance = 9.24f;
		// parameter for example weight update function
		float gamma = 0.7f;
		
		// select one of the datasets
		DataSet ds = new DataSet("diabetes.csv");
		//DataSet ds = new DataSet("balance.csv");
		//DataSet ds = new DataSet("breast-w.csv");
		
		// for cross validation
		int xValidationFolds = 5;
		TrainAndTest[] folds = ds.stratifiedKFold(xValidationFolds);
		ArrayList<ArrayList<Rule>> ruleSets = new ArrayList<>();
		
		for (int i = 0; i < xValidationFolds; i++) {
			System.out.println("Fold "+(i+1)+" out of "+ xValidationFolds);
			//SDIGA sgd = new SDIGA(min_conf, pop_size, n_labels, ds.rows.get(0).n_num, max_evaluations);
			CN2SD cn2sd = new CN2SD(folds[i].train, beamSize, maxIterations, minSignificance, gamma);
			//ArrayList<Chromosome> rules = sgd.run(folds[i].train);
			ArrayList<Rule> rules = cn2sd.run();
			for (Rule r : rules) {
				r.evaluate(folds[i].test);
			}
			ruleSets.add(rules);
		}
		
		// Prints out average quality measures for rule sets
		System.out.println(Evaluation.evaluateCN2SD(ruleSets));
		
	}

	public static void SDIGA_experiment() {
		// parameters
		// minimum confidence
		float min_conf = 0.9f;
		// population size
		int pop_size = 100;
		// number of linguistic labels for each variable
		int n_labels = 3;
		// max number of offspring evaluations
		int max_evaluations = 10000;
		// number of folds in cross-validation
		int xValidationFolds = 5;
		
		//DataSet ds = new DataSet("diabetes.csv", n_labels);
		//DataSet ds = new DataSet("balance.csv", n_labels);
		DataSet ds = new DataSet("breast-w.csv", n_labels);
		
		// for cross validation
		TrainAndTest[] folds = ds.stratifiedKFold(xValidationFolds);
		ArrayList<ArrayList<Chromosome>> ruleSets = new ArrayList<>();
		
		for (int i = 0; i < xValidationFolds; i++) {
			System.out.println("Fold "+(i+1)+" out of "+ xValidationFolds);
			SDIGA sgd = new SDIGA(min_conf, pop_size, n_labels, ds.rows.get(0).n_num, max_evaluations);
			ArrayList<Chromosome> rules = sgd.run(folds[i]);
			ruleSets.add(rules);
		}
		
		// Prints out average quality measures for rule sets
		System.out.println(Evaluation.evaluateSDIGA(ruleSets));
	}

}
