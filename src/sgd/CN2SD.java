package sgd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import sgd.Row.Type;

public class CN2SD {
	private ArrayList<SimpleCondition> simpleConditions;
	private DataSet ds;
	private int maxIterations;
	private int beamSize;
	private float minSignificance;
	private float minmWracc;
	private float gamma;
	
	public CN2SD(DataSet ds, int beamSize, int maxIterations, float minSignificance, float gamma) {
		// generate a list of SimpleConditions
		this.simpleConditions = new ArrayList<>();
		this.ds = ds;
		this.generateSimpleConditions();
		this.maxIterations = maxIterations;
		this.beamSize = beamSize;
		this.minSignificance = minSignificance;
		this.minmWracc = 0.0f;
		this.gamma = gamma;
		
	}
	
	private void generateSimpleConditions() {
		System.out.println("Generating Simple Conditions");
		Type[] types = this.ds.types;
		// for all types
		// iterate over all features
		for (int i = 0; i < types.length; i++) {
			if (types[i]==Row.Type.TAR) {
				continue;
			}
			// if the feature is categorical, make equals rels
			if (types[i]==Row.Type.CAT) {
				int f_index = this.ds.rows.get(0).feature_index[i];
				// first get arrayList of all unique values
				ArrayList<String> unique = new ArrayList<>();
				String value = "";
				for (Row r : this.ds.rows) {
					value = r.cat[f_index];
					if (!unique.contains(value)) {
						unique.add(value);
					}
				}
				for (String v : unique) {
					// constructors for CAT
					SimpleCondition cond1 = new SimpleCondition(Relationship.EQUAL, f_index, v);
					SimpleCondition cond2 = new SimpleCondition(Relationship.NOT_EQUAL, f_index, v);
					this.simpleConditions.add(cond1);
					this.simpleConditions.add(cond2);
				}
			// if it's numerical make greater than rels
			} else if (types[i]==Row.Type.NUM) {
				int f_index = this.ds.rows.get(0).feature_index[i];
				// first get arrayList of all unique values
				ArrayList<Float> unique = new ArrayList<>();
				float value = 0;
				for (Row r : this.ds.rows) {
					value = r.num[f_index];
					if (!unique.contains(value)) {
						unique.add(value);
					}
				}
				// then create conditions from those unique values
				for (float v : unique) {
					SimpleCondition cond1 = new SimpleCondition(Relationship.GREATER_THAN, f_index, v);
					SimpleCondition cond2 = new SimpleCondition(Relationship.LESSER_OR_EQUAL, f_index, v);
					this.simpleConditions.add(cond1);
					this.simpleConditions.add(cond2);
				}
			}
		}
		System.out.println("Done Generating Simple Conditions. Number of generated: "+this.simpleConditions.size());
	}

	public ArrayList<Rule> run() {
		// repeat for all classes
		String[] targets = this.ds.s_classes;
		ArrayList<Rule> ruleSet = new ArrayList<Rule>();
		int targetIndex = -1;
		for (String target : targets) {
			// set minimum mwracc
			this.minmWracc = 0.0f;
			// set minimum confidence
			targetIndex++;
			float defaultConfidence = 0;
			for (int i : this.ds.c_distribution) {
				defaultConfidence += i;
			}
			defaultConfidence = this.ds.c_distribution[targetIndex] / defaultConfidence;
			// set min confidence depending on the target
			//float minConfidence = 0f;
			float minConfidence = defaultConfidence;
			// first set whole ds as uncovered
			this.ds.uncover();
			// set weights of all examples to 1
			for (Row r : ds.rows) {
				r.cn2_wca = 1f;
				r.n_covered = 0;
			}
			// then iterate and look for rules
			Rule bestRule = this.findRules(target, minConfidence);
			if (bestRule != null) {
				// set new minimum mwracc - but only on first rule
				this.minmWracc = bestRule.mWRAcc * 0.9f;
			}
			while (bestRule != null) {
				ruleSet.add(bestRule);
				// cover examples covered and recalculate coverage weights
				bestRule.cover(ds, this.gamma);
				System.out.println("Found rule: "+bestRule);
				bestRule = this.findRules(target, minConfidence);
			}
		}
		return ruleSet;
		
	}

	private Rule findRules(String target, float minConfidence) {
		Rule bestRule = null;
		ArrayList<Rule> ruleList = new ArrayList<Rule>();
		
		// first create a population of Rules of Complex Conditions consisting only of simple conditions
		for (SimpleCondition sc : this.simpleConditions) {
			Rule r = new Rule(new ComplexCondition(sc), target);
			// evaluate them
			r.evaluate(ds);
			ruleList.add(r);
		}
		
		// sort them
		ruleList.sort(new RuleSorter());

		// trim them
		ruleList.subList(this.beamSize, ruleList.size()).clear();
		
		int currentIteration = 0;
		boolean isValid = false;
		ComplexCondition cc;
		Rule newRule;
		
		// threading
		BlockingQueue<Rule> bq = new ArrayBlockingQueue<Rule>(60000);
		MTEvaluator mte = new MTEvaluator(10, bq, ds);
		new Thread(mte).start();
		
		while (currentIteration < this.maxIterations) {
			// make new rules from best rules on beam
			
			for (int i = 0; i < this.beamSize; i++) {
				for (SimpleCondition sc : this.simpleConditions) {
					isValid = ruleList.get(i).condition.isValid(sc);
					if (isValid) {
						cc = ruleList.get(i).condition.addCondition(sc);
						newRule = new Rule(cc, target);
						// put new rules into evaluator queue which uses multithreading for maximum performance
						bq.add(newRule);
						ruleList.add(newRule);
					}
				}
			}
			
			while (!bq.isEmpty()) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// don't care
					//e.printStackTrace();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// don't care
				//e.printStackTrace();
			}
			
			// sort them
			ruleList.sort(new RuleSorter());
			// trim them
			ruleList.subList(this.beamSize, ruleList.size()).clear();
			// see it the best is good enough
			if (ruleList.get(0).significance >= minSignificance && ruleList.get(0).pClassCond > minConfidence && ruleList.get(0).mWRAcc > this.minmWracc) {
				if (bestRule == null || ruleList.get(0).mWRAcc > bestRule.mWRAcc) {
					bestRule = ruleList.get(0);
				}
			}
			currentIteration++;
		}
		mte.stop();
		return bestRule;
	}
	
	

}

enum Relationship {
	GREATER_THAN,
	LESSER_OR_EQUAL,
	EQUAL,
	NOT_EQUAL
}

class Rule {
	public ComplexCondition condition;
	public String target;
	public Float mWRAcc;
	public Float WRAcc;
	public float significance;
	public float pClassCond;
	public float pCond;
	public int[] c_dist;
	public ArrayList<Integer> supported;
	public int ds_size;
	
	public Rule(ComplexCondition cond, String target) {
		this.condition = cond;
		this.target = target;
	}

	public void cover(DataSet ds, float gamma) {
		for (Row r : ds.rows) {
			// CN2-SD doesn't specify whether it should mark as covered all covered examples or only positive ones
			if (this.condition.satisfies(r)) {
				r.n_covered += 1;
				// additive version
				//r.cn2_wca = 1.0f/(1.0f + r.n_covered);
				// multiplicative version
				r.cn2_wca = (float) Math.pow(gamma, r.n_covered);
			}
		}
	}

	public void evaluate(DataSet ds) {
		this.condition.evaluate(ds, this.target, this);
		
	}
	
	public String toString() {
		String dist = "[";
		for (Integer i : this.c_dist) {
			dist += i+",";
		}
		dist += "]";
		return "Target: "+this.target+" mWRAcc: "+this.mWRAcc+" WRAcc: "+this.WRAcc+" significance: "+this.significance+" pClassCond: "+this.pClassCond+" pCond: "+this.pCond+" Classes: "+dist;
	}
}

class RuleSorter implements Comparator<Rule>{
	@Override
    public int compare(Rule r1, Rule r2) {
        return r2.mWRAcc.compareTo(r1.mWRAcc);
    }
}

class ComplexCondition {
	public ArrayList<SimpleCondition> simpleConditions;
	public ComplexCondition() {
		this.simpleConditions = new ArrayList<SimpleCondition>();
	}

	// for generation of initial rules
	public ComplexCondition(SimpleCondition sc) {
		this.simpleConditions = new ArrayList<SimpleCondition>();
		this.simpleConditions.add(sc);
	}
	
	public void evaluate(DataSet ds, String target, Rule rule) {
		rule.supported = new ArrayList<>();
		rule.ds_size = ds.rows.size();
		// this for class distributions
		String[] classes = ds.s_classes;
		int[] c_distribution = new int[classes.length];
		// set all to zero just in case
		for (int c = 0; c < c_distribution.length; c++) {
			c_distribution[c] = 0;
		}
		
		float nCond = 0f;
		float nClassCond = 0f;
		// covering weights
		float CondW = 0;
		float ClassCondW = 0;
		float nW = 0;
		float ClassW = 0;
		float nClass = 0;
		boolean satisfies;
		int rowCounter = -1;
		// for all rows in dataset
		for (Row r : ds.rows) {
			rowCounter++;
			nW += r.cn2_wca;
			satisfies = this.satisfies(r);
			
			if (satisfies) {
				nCond++;
				CondW += r.cn2_wca;
				if (r.target.equals(rule.target)) {
					rule.supported.add(rowCounter);
					nClassCond++;
					ClassCondW += r.cn2_wca;
				}
				
				// add to class distribution
				for (int c = 0; c < c_distribution.length; c++) {
					if (r.target.equals(classes[c])) {
						c_distribution[c]++;
						break;
					}
				}
			}
			
			if (r.target.equals(rule.target)) {
				ClassW += r.cn2_wca;
				nClass += 1;
			}
				
		}
		
		// want to maximize this
		float mWRAcc = (CondW / nW) * ( (ClassCondW / CondW) - (ClassW/nW));
		float pCond = nCond / ds.rows.size();
		float pClassCond = nClassCond / nCond;
		float pClass = nClass / ds.rows.size();
		// if it's bigger than zero -> the rule is better than the default rule
		float WRAcc = pCond * (pClassCond - pClass);
		// this condition will fire when there are no examples covered
		if (nW==0 || CondW==0 || nCond==0) {
			mWRAcc = 0;
			WRAcc = 0;
		}
		float significance = this.significance(ds.c_distribution, c_distribution, pCond);
		// what to pass up to the rule
		rule.c_dist = c_distribution;
		rule.mWRAcc = mWRAcc;
		rule.WRAcc = WRAcc;
		rule.significance = significance;
		rule.pClassCond = pClassCond;
		rule.pCond = pCond;
	}
	
	public boolean satisfies(Row r) {
		boolean satisfies = true;
		// for all simple conditions
		int i = 0;
		while (i < this.simpleConditions.size() && satisfies) {
			satisfies = satisfies & this.simpleConditions.get(i).satisfies(r);
			i++;
		}
		return satisfies;
	}
	
	// this is the version in SDIGA and CN2-SD (allegedly)
	// absolute frequency of occurrences
	private float significance(int[] pop, int[] obs, float pCond) {
		float sig = 0f;
		for (int i = 0; i < pop.length; i++) {
			if (pop[i]==0 || obs[i]==0 || pCond==0)
				continue;
			sig += obs[i] * Math.log(obs[i]/(pop[i]*pCond));
		}
		sig = sig * 2;
		return sig;
	}
	
	// relative frequency of occurences
	// in terms of probabilities, not total numbers
	// https://github.com/seanysull/CN2-Rule-Based-Classifier/blob/master/CN2impl.py ?????
	private float significance(int[] pop, int[] obs) {
		float sig = 0f;
		float p_sum = 0f;
		float o_sum = 0f;
		for (int i = 0; i< pop.length; i++) {
			p_sum += pop[i];
			o_sum += obs[i];
		}
		for (int i = 0; i < pop.length; i++) {
			float p = (pop[i]==0 || p_sum==0) ? 0 : pop[i]/p_sum;
			float o = (obs[i]==0 || o_sum==0) ? 0 : obs[i]/o_sum;
			sig += obs[i] * Math.log(o/p);
		}
		sig = sig * 2;
		return sig;
	}

	public boolean isValid(SimpleCondition toAdd) {
		// make sure that:
		// a) the condition isn't contradictory (e.g. bigger than 5 AND lesser than 5
		// b) the condition isn't redundant (e.g. bigger than 5 AND bigger than 6 -> redundant)
		for (SimpleCondition cond : this.simpleConditions) {
			// if it's the same feature
			if (cond.feature==toAdd.feature) {
				if (cond.type==Type.NUM) {
					// if they're both GT then one of them is surely redundant
					if (cond.rel==Relationship.GREATER_THAN && toAdd.rel==Relationship.GREATER_THAN) {
						return false;
					}
					// if they're both LOE then one of them is surely redundant
					if (cond.rel==Relationship.LESSER_OR_EQUAL && toAdd.rel==Relationship.LESSER_OR_EQUAL) {
						return false;
					}
					// if they're different then we must make sure they're not contradictory
					if (cond.rel==Relationship.LESSER_OR_EQUAL && toAdd.rel==Relationship.GREATER_THAN) {
						if (cond.num_value <= toAdd.num_value) {
							return false;
						}
					}
					if (cond.rel==Relationship.GREATER_THAN && toAdd.rel==Relationship.LESSER_OR_EQUAL) {
						if (cond.num_value >= toAdd.num_value) {
							return false;
						}
					}
				} else if (cond.type==Type.CAT) {
					// if they are two different (or same) equals it's invalid or redundant
					// however algorithm could be altered so that it considers them in disjunctive form
					if (cond.rel==Relationship.EQUAL && toAdd.rel==Relationship.EQUAL) {
						return false;
					}
					// if they are a pair of equal/not_equal then they're redundant
					if (cond.rel==Relationship.EQUAL && toAdd.rel==Relationship.NOT_EQUAL) {
						return false;
					}
					if (cond.rel==Relationship.NOT_EQUAL && toAdd.rel==Relationship.EQUAL) {
						return false;
					}
					// if they're both not equal
					if (cond.rel==Relationship.NOT_EQUAL && toAdd.rel==Relationship.NOT_EQUAL) {
						// if they're both unequal on the same value it's redundant
						if (cond.cat_value.equals(toAdd.cat_value)) {
							return false;
						}
					}
				}
			}
		}
		// if no condition was triggered, add it
		return true;
	}
	
	public ComplexCondition addCondition(SimpleCondition toAdd) {
		// first make a copy of the ComplexCondition
		ComplexCondition copy = new ComplexCondition();
		for (SimpleCondition cond : this.simpleConditions) {
			copy.simpleConditions.add(cond);
		}
		// then add it
		copy.simpleConditions.add(toAdd);
		// sort it by feature index
		copy.simpleConditions.sort(new FeatureSorter());
		return copy;
	}
	
}

class FeatureSorter implements Comparator<SimpleCondition>{
	@Override
    public int compare(SimpleCondition sc1, SimpleCondition sc2) {
		// in ascending order (I think?)
        return sc1.feature - sc2.feature;
    }
}

class SimpleCondition {
	public Relationship rel;
	// index of the feature in dataset
	public int feature;
	public float num_value;
	public String cat_value;
	public Type type;

	// two constructors for two different types of variables
	public SimpleCondition(Relationship rel, int feature, float value) {
		this.rel = rel;
		this.feature = feature;
		this.num_value = value;
		this.type = Type.NUM;
	}
	
	public boolean satisfies(Row r) {
		// if it's categorical
		if (this.type==Type.CAT) {
			boolean equals =  r.cat[this.feature].equals(this.cat_value);
			if (this.rel==Relationship.EQUAL) {
				return equals;
			// if it's NOT_EQUALS
			} else {
				return !equals;
			}
		// if it's numerical
		} else {
			boolean greater_than = r.num[this.feature] > this.num_value;
			if (this.rel==Relationship.GREATER_THAN) {
				return greater_than;
			// if it's lesser or equal
			} else {
				return !greater_than;
			}
		}
	}

	public SimpleCondition(Relationship rel, int feature, String value) {
		this.rel = rel;
		this.feature = feature;
		this.cat_value = value;
		this.type = Type.CAT;
	}
}

class MTEvaluator implements Runnable{
	private int nt;
	private HelperThread[] threads;
	private BlockingQueue<Rule> bq;
	private DataSet ds;

	public MTEvaluator(int n_threads, BlockingQueue<Rule> bq, DataSet ds) {
		this.nt = n_threads;
		this.threads = new HelperThread[this.nt];
		this.bq = bq;
		this.ds = ds;
	}
	
	public void run() {
		for (int i = 0; i < this.nt; i++) {
			this.threads[i] = new HelperThread(this.bq, this.ds);
			this.threads[i].start();
		}
	}
	
	public void stop() {
		for (int i = 0; i < this.nt; i++) {
			this.threads[i].interrupt();
		}
	}
}

class HelperThread extends Thread {
	
	private BlockingQueue<Rule> queue;
	private DataSet ds;

	public HelperThread(BlockingQueue<Rule> queue, DataSet ds) {
		this.queue = queue;
		this.ds = ds;
	}

	public void run() 
    { 
        try
        { 
        	while (true) {
        		this.queue.take().evaluate(this.ds);
        	}
  
        } 
        catch (Exception e) 
        { 
            // Throwing an exception 
            //System.out.println ("Exception is caught"); 
        } 
    } 
}
