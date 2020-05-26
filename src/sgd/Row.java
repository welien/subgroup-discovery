package sgd;

public class Row {
	// for compatibility
	public boolean covered;
	private Type[] types;
	public String target;
	public int[] feature_index;
	public float num[];
	public String cat[];
	// number of rules that cover this row
	public int n_covered;
	public int n_features;
	public int n_num;
	public int n_cat;
	public String[] header;
	// weighted cover for CN2
	public float cn2_wca;
	
	static enum Type {
	    CAT,
	    NUM,
	    TAR,
	    IGN
	 }
	
	public Row(String[] attributes, Type[] types, String[] header) {
		this.n_covered = 0;
		this.header = header;
		this.n_features = attributes.length;
		this.feature_index = new int[n_features];
		int num_counter = 0;
		int cat_counter = 0;
		this.types = types;
		this.n_covered = 0;
		this.cn2_wca = 1f;
		
		// count number of nums and cats
		for (int i = 0; i < types.length; i++) {
			if (types[i]==Row.Type.CAT)
				this.n_cat++;
			else if (types[i]==Row.Type.NUM)
				this.n_num++;
		}
		
		this.cat = new String[this.n_cat];
		this.num = new float[this.n_num];
		
		for (int i = 0; i < attributes.length; i++) {
			if (types[i]==Row.Type.CAT) {
				this.cat[cat_counter] = attributes[i];
				this.feature_index[i] = cat_counter;
				cat_counter++;
			} else if (types[i]==Row.Type.NUM) {
				this.num[num_counter] = Float.parseFloat(attributes[i]);
				this.feature_index[i] = num_counter;
				num_counter++;
			} else if (types[i]==Row.Type.TAR) {
				this.target = attributes[i];
				// isn't added as feature
				this.feature_index[i] = -1;
			}
		}
	}
	
	public Row() {
		this.covered = false;
	}
	
	// shallow copy = for read only, no modifying
	public Row clone() {
		Row clone = new Row();
		clone.types = this.types;
		clone.target = this.target;
		clone.feature_index = this.feature_index;
		clone.num = this.num;
		clone.cat = this.cat;
		
		clone.n_covered = n_covered;
		clone.n_features = n_features;
		clone.n_num = n_num;
		clone.n_cat = n_cat;
		clone.header = header;
		clone.cn2_wca = 1f;
		
		return clone;
	}
	
	public static Type[] getTypes(String line) {
		String[] attributes = line.split(",");
		Type[] types = new Type[attributes.length];
		for (int i = 0; i < attributes.length; i++) {
			//System.out.println
			// if the variable is numerical
			if (attributes[i].equals("n")) {
				types[i] = Row.Type.NUM;
			// if the variable is categorical
			} else if (attributes[i].equals("c")) {
				types[i] = Row.Type.CAT;
			// if the variable is target class
			} else if (attributes[i].equals("t")) {
				types[i] = Row.Type.TAR;
			} else if (attributes[i].equals("i")) {
				// ignore attribute
				types[i] = Row.Type.IGN;
			}
		}
		return types;
	}
	
	public float getNum(int index) {
		return this.num[index];
	}
	
	public String getCat(int index) {
		return this.cat[index];
	}
}