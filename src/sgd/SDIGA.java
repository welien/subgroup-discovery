package sgd;

import java.util.ArrayList;

public class SDIGA {

	private float min_conf;
	private int pop_size;
	private int n_labels;
	private int n_features;
	private int max_evaluations;

	public SDIGA(float min_conf, int pop_size, int n_labels, int n_features, int max_evaluations) {
		this.min_conf = min_conf;
		this.pop_size = pop_size;
		this.n_labels = n_labels;
		this.n_features = n_features;
		this.max_evaluations = max_evaluations;
	}
	
	// for training and testing
	public ArrayList<Chromosome> run(TrainAndTest tat) {
		ArrayList<Chromosome> rules = this.train(tat.train);
		this.test(tat.test, rules);
		// after training and testing the datasets are still marked - must be cleaned before further use (if there is further use)
		return rules;
	}
	
	// only for training
	public ArrayList<Chromosome> train(DataSet ds) {
		ArrayList<Chromosome> rules = new ArrayList<>();
		
		for (int class_index = 0; class_index < ds.s_classes.length; class_index++) {
			// uncover all rows when changing target
			ds.uncover();
			while (true) {
				String target = ds.s_classes[class_index];
				int evaluations = 0;
				Population population = new Population(n_labels, n_features, pop_size, ds, target);
				while (evaluations < max_evaluations) {
					population.crossover(ds, target);
					population.mutate(ds, target);
					evaluations += population.trim_and_sort();
				}
				
				// improve the best rule locally
				Chromosome best = population.individuals.get(0).localImprovement(ds, target, min_conf);
				// add it to rule list
				rules.add(best);
				// set ds as covered for which they are covered
				best.setDsAsCovered(ds);
				
				System.out.println("Trained "+best);
				
				if (best.fconfidence >= min_conf && best.sup3 > 0)
					continue;
				else
					break;
			}
		}
		return rules;
	}
	
	// only for testing, modifies rules in place
	public void test(DataSet ds, ArrayList<Chromosome> trainedRules) {
		int size = trainedRules.size();
		for (int i = 0; i < size; i++) {
			trainedRules.get(i).APC(ds, trainedRules.get(i).target);
			System.out.println("Tested "+trainedRules.get(i));
		}
	}

}
