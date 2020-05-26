package sgd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random; 

public class Chromosome {

	public int[][] genes;
	public int n_labels;
	public int n_genes;
	public ArrayList<Integer> covered;
	public ArrayList<Integer> supported;
	private float w1;
	private float w2;
	public Float fitness;
	public float fconfidence;
	public int nClassCond;
	public float sup3;
	public int nCond;
	private int uncovered_by_previous;
	private int uncovered_by_previous_but_covered_by_current;
	public String target;
	public int target_index;
	public int class_index;
	public float coverage;
	public int ds_size;
	public float wracc;
	public float significance;
	
	public Chromosome(int n_labels, int n_genes) {
		this.genes = new int[n_genes][n_labels];
		this.n_labels = n_labels;
		this.n_genes = n_genes;
		this.w1 = 0.4f;
		this.w2 = 0.3f;
	}

	// randomly initiates chromosome
	public void randomize() {
		Random rand = new Random(); 
		
		for (int i = 0; i < n_genes; i++) {
			for (int j = 0; j < n_labels; j++) {
				this.genes[i][j] = rand.nextInt(2);
			}
		}
	}
	
	// antecedent part compatibility - even though this particular subroutine evaluates more than just APC
	public void APC(DataSet ds, String target) {
		this.target = target;
		this.covered = new ArrayList<>();
		this.supported = new ArrayList<>();
		int ds_size = ds.rows.size();
		int[] c_distribution = new int[ds.c_distribution.length];
		
		// set all to zero
		for (int i = 0; i < ds.c_distribution.length; i++) {
			c_distribution[i] = 0;
		}
		
		float apc_sum = 0;
		float apc_sum_supported = 0;
		int nClassCond = 0;
		int nCond = 0;
		
		int uncovered_by_previous = 0;
		int uncovered_by_previous_but_covered_by_current = 0;
		
		for (int i = 0; i < ds_size; i++) {
			Row row = ds.rows.get(i);
			//System.out.println(i);
			Partitions p = ds.partitions;
						
			p.membership(row, this);
			
			
			float apc = ds.partitions.membership(row, this);
			
			String row_target = row.target;
			//System.out.println(target);
			
			
			if (row.covered == false) {
				uncovered_by_previous += 1;
			}
			
			if (apc > 0f) {
				if (row.covered == false) {
					uncovered_by_previous_but_covered_by_current += 1;
				}
				
				// for all possible target classes
				for (int j = 0; j < c_distribution.length; j++) {
					if (row.target.equals(ds.s_classes[j])) {
						// increase occurrence by one
						c_distribution[j]++;
					}
				}
				
				apc_sum += apc;
				this.covered.add(i);
				nCond += 1;
				if (row_target.equals(target)) {
					this.supported.add(i);
					nClassCond += 1;
					apc_sum_supported += apc;
				}
			}
		}
		// fuzzy confidence
		
		float fconfidence = 0;
		if (apc_sum != 0) {
			fconfidence = apc_sum_supported / apc_sum;
		}
		
		float cconfidence = 0;
		if (nCond != 0 && nClassCond != 0) {
			cconfidence = (float) nClassCond / nCond;
		}
		
		float coverage = (float) nCond / ds_size;
		// these two exist for experimentation with different quality measures leading the rule search
		float sup1 = (float) nClassCond / ds_size;
		float sup2 = (float) nClassCond / ds.c_distribution[class_index];
		float sup3 = 0;
		if (uncovered_by_previous != 0) {
			sup3 = (float)uncovered_by_previous_but_covered_by_current / uncovered_by_previous;
			//System.out.println(3/768);
		}
		
		float default_confidence = (float) ds.c_distribution[class_index] / ds_size;
		float wracc = coverage * (cconfidence - default_confidence);
		
		this.fitness = this.fitness(fconfidence, sup3);
		this.fconfidence = fconfidence;
		this.nClassCond = nClassCond;
		this.sup3 = sup3;
		this.nCond = nCond;
		this.uncovered_by_previous = uncovered_by_previous;
		this.uncovered_by_previous_but_covered_by_current = uncovered_by_previous_but_covered_by_current;
		this.coverage = coverage;
		this.ds_size = ds_size;
		this.wracc = wracc;
		this.significance = this.significance(ds.c_distribution, c_distribution, ((float)nCond)/ds_size);
	}

	private float fitness(float fconfidence, float sup3) {
		return (sup3 * this.w1 + fconfidence * this.w2) / (this.w1 + this.w2);
	}
	
	private float significance(int[] pop, int[] obs, float pCond) {
		float significance = 0;
		for (int i = 0; i < pop.length; i++) {
			// if they're zero it doesn't count
			if (pCond==0 || pop[i]==0 || obs[i]==0)
				continue;
			significance += (float) obs[i] * Math.log((float) obs[i]/(pop[i] * pCond));
		}
		significance *= 2;
		
		return significance;
	}
	
	public Chromosome clone() {
		Chromosome ch = new Chromosome(this.n_labels, this.n_genes);
		ch.genes = Arrays.stream(this.genes).map(int[]::clone).toArray(int[][]::new);
		return ch;
	}
	
	public String toString() {
		return "Target: "+this.target+" Fconfidence: "+this.fconfidence+" Fitness: "+
				this.fitness+" nClassCond: "+this.nClassCond+" nCond: "+this.nCond+" sup3: "+
				this.sup3+" UBP: "+this.uncovered_by_previous+" UBP-BCBC: "+
				this.uncovered_by_previous_but_covered_by_current+
				" Significance: "+this.significance+
				" WRAcc: "+this.wracc;
	}
	
	public void setAllInGene(int gene_index, int value) {
		for (int i = 0; i < this.n_labels; i++) {
			this.genes[gene_index][i] = value;
		}
	}
	
	public Chromosome localImprovement(DataSet ds, String target, float min_conf) {
		boolean improvement = true;
		Chromosome best = this;
		while (improvement) {
			for (int i = 0; i < this.n_genes; i++) {
				// make the rule more general
				Chromosome copy = best.clone();
				copy.setAllInGene(i, 1);
				copy.APC(ds, target);
				if (copy.sup3 >= this.sup3 && copy.fconfidence >= this.fconfidence && copy.sup3 >= best.sup3) {
					best = copy;
					break;
				}
			}
			improvement = false;
		}
		if (best.fconfidence >= min_conf)
			return best;
		else
			return this;
	}

	public void setDsAsCovered(DataSet ds) {
		int ds_size = ds.rows.size();
		for (int i = 0; i < ds_size; i++) {
			Row row = ds.rows.get(i);
			float apc = ds.partitions.membership(ds.rows.get(i), this);
			
			if (apc > 0f) 
				row.covered = true;
		}
	}

}
