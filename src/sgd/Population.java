package sgd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random; 

public class Population {

	
	private int n_labels;
	private int n_features;
	private int pop_size;
	public ArrayList <Chromosome>individuals;
	public float mutation_p;
	private int mu_next;
	private ArrayList <Chromosome>descendants;
	private Random rand;

	public Population(int n_labels, int n_features, int pop_size, DataSet ds, String target) {
		this.n_labels = n_labels;
		this.n_features = n_features;
		this.pop_size = pop_size;
		this.individuals = new ArrayList<>(pop_size + 3);
		for (int i = 0; i < pop_size; i++) {
			Chromosome ch = new Chromosome(n_labels, n_features);
			ch.randomize();
			ch.APC(ds, target);
			this.individuals.add(ch);
		}
		this.descendants = new ArrayList<>();
		
		// sort individuals
		
		this.individuals.sort(new FitnessSorter());
		this.mutation_p = 0.01f;
		this.rand = new Random(); 
		// don't know how it works??? but it improves time of convergence RE: KEEL SDIGA
		this.mu_next = ((int) Math.ceil (Math.log(rand.nextFloat()) / Math.log(1.0f - this.mutation_p)));
	}
	
	public void crossover(DataSet ds, String target) {
		Chromosome p1 = this.individuals.get(0);
		Chromosome p2 = this.individuals.get(1);
		
		// two point crossover
		int chromosome_size = this.n_features * this.n_labels;
		int r1 = this.rand.nextInt(chromosome_size);
		int r2 = r1;
		while (r1==r2) {
			r2 = this.rand.nextInt(chromosome_size);
		}
		
		Chromosome o1 = new Chromosome(this.n_labels, this.n_features);
		Chromosome o2 = new Chromosome(this.n_labels, this.n_features);
		
		boolean picker = true;
		for (int i = 0; i < this.n_features; i++) {
			for (int j = 0; j < this.n_labels; j++) {
				int current_index = i * n_labels + j;
				if (current_index==r1) {
					picker = false;
				} else if (current_index==r2) {
					picker = true;
				}
				
				if (picker) {
					o1.genes[i][j] = p1.genes[i][j];
					o2.genes[i][j] = p2.genes[i][j];
				} else {
					o1.genes[i][j] = p2.genes[i][j];
					o2.genes[i][j] = p1.genes[i][j];
				}
			}
		}
		
		o1.APC(ds, target);
		o2.APC(ds, target);
		
		this.descendants.add(o1);
		this.descendants.add(o2);
	}
	
	public void mutate(DataSet ds, String target) {
		// choose random gene
		int position = this.n_features * this.individuals.size();
		
		if (this.mutation_p > 0f) {
			while (this.mu_next < position) {
				int individual = this.mu_next / this.n_features;
				int gene = this.mu_next % this.n_features;
				
				// copy the individual
				Chromosome ch = new Chromosome(this.n_labels, this.n_features);
				for (int i = 0; i < this.n_features; i++) {
					for (int j = 0; j < this.n_labels; j++) {
						ch.genes[i][j] = this.individuals.get(individual).genes[i][j];
					}
				}
				
				
				int elimination = this.rand.nextInt(2);
				
				if (elimination == 1) {
					for (int i = 0; i < this.n_labels; i++) {
						ch.genes[gene][i] = 0;
					}
				} else {
					for (int i = 0; i < this.n_labels; i++) {
						ch.genes[gene][i] = this.rand.nextInt(2);
					}
				}
				ch.APC(ds, target);
				this.descendants.add(ch);
				
				if (this.mutation_p<1) {
                    float m = rand.nextFloat();
                    this.mu_next += Math.ceil (Math.log(m) / Math.log(1.0 - this.mutation_p));
                }
                else
                    this.mu_next += 1;
			}
			this.mu_next -= position;
		}
	}
	
	public int trim_and_sort() {
		int number_of_evaluations = this.descendants.size();
		this.individuals.addAll(descendants);
		this.descendants = new ArrayList<>();
		this.individuals.sort(new FitnessSorter());
		this.individuals.subList(this.pop_size, this.individuals.size()).clear();
		return number_of_evaluations;
	}

}

class FitnessSorter implements Comparator<Chromosome>{
	@Override
    public int compare(Chromosome ch1, Chromosome ch2) {
        return ch2.fitness.compareTo(ch1.fitness);
    }
}
