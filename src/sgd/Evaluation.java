package sgd;

import java.util.ArrayList;

public class Evaluation {
	
	public static String evaluateSDIGA(ArrayList<ArrayList<Chromosome>> ruleSets) {
		// for each ruleSet
		int[] ruleSetSizes = new int[ruleSets.size()];
		float[] coverages = new float[ruleSets.size()];
		float[] supports = new float[ruleSets.size()];
		float[] wraccs = new float[ruleSets.size()];
		float[] significances = new float[ruleSets.size()];
		for (int i = 0; i < ruleSets.size(); i++) {
			ArrayList<Chromosome> ruleSet = ruleSets.get(i);	
			// for each rule
			float coverage_sum = 0;
			float wracc_sum = 0;
			float sig_sum = 0;
			ArrayList<Integer> supported = new ArrayList<>();
			int ds_size = 0;
			for (int j = 0; j < ruleSet.size(); j++) {
				Chromosome ch = ruleSet.get(j);
				ds_size = ch.ds_size;
				coverage_sum += ch.coverage;
				wracc_sum += ch.wracc;
				sig_sum += ch.significance;
				// for each individual supported by rule
				for (int k = 0; k < ch.supported.size(); k++) {
					int l = ch.supported.get(k);
					if (!supported.contains(l))
						supported.add(l);
				}
			}
			coverages[i] = (float) coverage_sum / ruleSet.size();
			supports[i] = (float) supported.size() / ds_size;
			wraccs[i] = (float) wracc_sum / ruleSet.size();
			significances[i] = sig_sum / ruleSet.size();
			System.out.println(significances[i]);
			ruleSetSizes[i] = ruleSet.size();
		}
		float mean_coverage = Evaluation.mean(coverages);
		float mean_support = Evaluation.mean(supports);
		float mean_wracc = Evaluation.mean(wraccs);
		float mean_significance = Evaluation.mean(significances);
		float mean_rss = Evaluation.mean(ruleSetSizes);
		
		float std_coverage = Evaluation.std(coverages, mean_coverage);
		float std_support = Evaluation.std(supports, mean_support);
		float std_wracc = Evaluation.std(wraccs, mean_wracc);
		float std_significance = Evaluation.std(significances, mean_significance);
		float std_rss = Evaluation.std(ruleSetSizes, mean_rss);
		
		return "Coverage: "+mean_coverage+"+-"+std_coverage+" Support: "+mean_support+"+-"+std_support+
				" WRACC: "+mean_wracc+"+-"+std_wracc+" Significance: "+mean_significance+"+-"+std_significance+
				" Size: "+mean_rss+"+-"+std_rss;
	}
	
	private static float std(int[] array, float mean) {
		float std = 0;
		for(int num: array) {
            std += Math.pow(num - mean, 2);
        }
		return (float) Math.sqrt(std);
	}

	private static float std(float[] array, float mean) {
		float std = 0;
		for(float num: array) {
            std += Math.pow(num - mean, 2);
        }
		return (float) Math.sqrt(std);
	}

	public static float mean(float[] array) {
		float sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum / array.length;
	}
	
	public static float mean(int[] array) {
		float sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum / array.length;
	}

	public static String evaluateCN2SD(ArrayList<ArrayList<Rule>> ruleSets) {
		int[] ruleSetSizes = new int[ruleSets.size()];
		float[] coverages = new float[ruleSets.size()];
		float[] supports = new float[ruleSets.size()];
		float[] wraccs = new float[ruleSets.size()];
		float[] significances = new float[ruleSets.size()];
		
		// for each ruleset
		int counter = -1;
		for (ArrayList<Rule> rs : ruleSets) {
			counter++;
			// for each rule in the ruleset
			ArrayList<Integer> supported = new ArrayList<Integer>();
			float ruleSetCoverage = 0;
			float ruleSetSignificance = 0;
			float ruleSetWRAcc = 0;
			int ds_size = 0;
			for (Rule r : rs) {
				System.out.println("Tested "+r);
				// get size of database for later calculations
				ds_size = r.ds_size;
				// first sort out support
				for (Integer i : r.supported) {
					if (!supported.contains(i)) {
						supported.add(i);
					}
				}
				
				ruleSetCoverage += r.pCond;
				ruleSetSignificance += r.significance;
				ruleSetWRAcc += r.WRAcc;
			}
			float meanRuleSetSupport = 0;
			float meanRuleSetCoverage = 0;
			float meanRuleSetSignificance = 0;
			float meanRuleSetWRAcc = 0;
			int ruleSetSize = rs.size();
			if (ds_size!=0) {
				meanRuleSetSupport = (float)supported.size() / ds_size;
				meanRuleSetCoverage = ruleSetCoverage / rs.size(); 
				meanRuleSetSignificance = ruleSetSignificance / rs.size();
				meanRuleSetWRAcc = ruleSetWRAcc / rs.size();
			}
			ruleSetSizes[counter] = ruleSetSize;
			coverages[counter] = meanRuleSetCoverage;
			supports[counter] = meanRuleSetSupport;
			wraccs[counter] = meanRuleSetWRAcc;
			significances[counter] = meanRuleSetSignificance;
		}
		
		float mean_coverage = Evaluation.mean(coverages);
		float mean_support = Evaluation.mean(supports);
		float mean_wracc = Evaluation.mean(wraccs);
		float mean_significance = Evaluation.mean(significances);
		float mean_rss = Evaluation.mean(ruleSetSizes);
		
		float std_coverage = Evaluation.std(coverages, mean_coverage);
		float std_support = Evaluation.std(supports, mean_support);
		float std_wracc = Evaluation.std(wraccs, mean_wracc);
		float std_significance = Evaluation.std(significances, mean_significance);
		float std_rss = Evaluation.std(ruleSetSizes, mean_rss);
		
		return "Coverage: "+mean_coverage+"+-"+std_coverage+" Support: "+mean_support+"+-"+std_support+
				" WRACC: "+mean_wracc+"+-"+std_wracc+" Significance: "+mean_significance+"+-"+std_significance+
				" Size: "+mean_rss+"+-"+std_rss;
	}

}
