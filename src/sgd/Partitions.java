package sgd;

public class Partitions {
	public Interval[][] partitions;
	private int n_labels;
	public Partitions(int n_labels, int n_features) {
		this.partitions = new Interval[n_features][n_labels];
		this.n_labels = n_labels;
	}
	
	// only membership on numerical - not categorical
	public float membership(Row row, Chromosome ch) {
		float largest = 1f;
		for (int i = 0; i < row.n_num; i++) {
			int[] gene = ch.genes[i];
			Interval[] labels = this.partitions[i];
			float value = row.getNum(i);
			float smallest = 0f;
			for (int j = 0; j < n_labels; j++) {
				if (gene[j] == 1) {
					//System.out.println("Yes");
					float mem = labels[j].membership(value);
					if (mem > smallest)
						smallest = mem;
				}
			}
			if (smallest < largest)
				largest = smallest;
		}
		return largest;
	}
	
	public Interval getInterval(int n_feature, int n_labels) {
		return this.partitions[n_feature][n_labels];
	}
}

class Interval {
	public float x1;
	public float x2;
	public float x3;
	
	public Interval(float x1, float x2, float x3) {
		this.x1 = x1;
		this.x2 = x2;
		this.x3 = x3;
	}
	
	public float membership(float x) {
		if (x <= this.x1 || x >= this.x3)
            return 0f;
        if (x < this.x2)
            return ((x-this.x1)*(1/(this.x2-this.x1)));
        if (x > this.x2)
            return ((this.x3-x)*(1/(this.x3-this.x2)));
        return 1f;
	}
	
	public String toString() {
		return "x1: "+x1+" x2: "+x2+"x3: "+x3;
	}
}