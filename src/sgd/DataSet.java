package sgd;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sgd.Row.Type;

public class DataSet {

	public String[] header;
	public List<Row> rows;
	public Partitions partitions;
	public float[] classes;
	public int[] c_distribution;
	public String[] s_classes;
	public Type[] types;
	
	// for creation of empty dataset
	public DataSet(Partitions partitions, String[] header, float[] classes) {
		this.rows = new ArrayList<>();
		this.partitions = partitions;
		this.header = header;
		this.classes = classes;
	}
	
	// newly for datasets
	public DataSet(Partitions partitions, String[] header, String[] classes) {
		this.rows = new ArrayList<>();
		this.partitions = partitions;
		this.header = header;
		this.s_classes = classes;
	}
	
	// with partitions : for SDIGA
	public DataSet(String f, int n_labels) {
		// load csv and save into rows
		this.rows = loadCSV_flexible(f);
		//System.out.println(this.rows.get(0).);
		this.calculatePartitions(n_labels, this.rows.get(0).n_num);
		// define different classes in dataset
		this.countClasses();
	}
	
	// without partitions : for CN2
	public DataSet(String f) {
		// load csv and save into rows
		this.rows = loadCSV_flexible(f);
		// define different classes in dataset
		this.countClasses();
		
	}

	private void countClasses() {
		ArrayList<String> classes = new ArrayList<>();
		ArrayList<Integer> class_dist = new ArrayList<>();
		
		for (int i = 0; i < this.rows.size(); i++) {
			String target = this.rows.get(i).target;
			//System.out.println(target);
			if (classes.contains(target)) {
				int index = classes.indexOf(target);
				class_dist.set(index, class_dist.get(index)+1);
			} else {
				classes.add(target);
				class_dist.add(1);
			}
		}
		int[] class_d = new int[class_dist.size()];
		String[] c = new String[class_dist.size()];
		
		for (int i = 0; i < class_d.length; i++) {
			class_d[i] = class_dist.get(i);
			c[i] = classes.get(i);
		}
		this.s_classes = c;
		this.c_distribution = class_d;
	}
	
	public List<Row> loadCSV_flexible(String f) {
		List<Row> rows = new ArrayList<>();
		Path pathToFile = Paths.get(f);
		
		try (BufferedReader br = Files.newBufferedReader(pathToFile,
                StandardCharsets.US_ASCII)) {

            // first line are types
            String line = br.readLine();
            Type[] types = Row.getTypes(line);
            this.types = types;
            // second line are headers
            line = br.readLine();
            this.setHeader(line);
            
            line = br.readLine();

            // loop until all lines are read
            while (line != null) {
                String[] attributes = line.split(",");
                //System.out.println(line);
                Row row = new Row(attributes, types, this.header);
                rows.add(row);

                line = br.readLine();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return rows;
	}

	private void setHeader(String line) {
		this.header = line.split(",");
		
	}
	
	public void calculateDistribution() {
		this.c_distribution = new int[this.s_classes.length];
		for (int i = 0; i < this.rows.size(); i++) {
			for (int j = 0; j < this.s_classes.length; j ++) {
				//if (this.rows.get(i).values[target_index]==this.classes[j]) {
				if (this.rows.get(i).target.contentEquals(this.s_classes[j])) {
					this.c_distribution[j] = this.c_distribution[j] + 1;
				}
			}
		}
	}
	
	public void calculatePartitions(int n_labels, int n_features) {
		this.partitions = new Partitions(n_labels, n_features);
		int ds_size = this.rows.size();
		for (int i = 0; i < n_features; i++) {
			ArrayList<Float> list = new ArrayList<>();
			for (int j = 0; j < ds_size; j++) {
				list.add(this.rows.get(j).getNum(i));
			}
			Collections.sort(list);

			float max = list.get(ds_size-1);
			float min = list.get(0);
			float step = (max - min) / (float) (n_labels - 1);
			for (int j = 0; j < n_labels; j++) {
				float x1 = min + step * (j-1) ;
				float x2 = min + step * j;
				float x3 = min + step * (j+1);
				this.partitions.partitions[i][j] = new Interval(x1, x2, x3);
			}
		}
	}
	
	public TrainAndTest[] stratifiedKFold(int n_folds) {
		// first count number of rows
		int ds_size = this.rows.size();
		// make bins
		TrainAndTest[] folds = new TrainAndTest[n_folds];
		for (int i = 0; i < n_folds; i++) {
			// passing pointer = they will all share the same partitions, header and classes objects
			folds[i] = new TrainAndTest(this.partitions, this.header, this.s_classes);
		}
		// make counter of classes
		int n_classes = this.s_classes.length;
		int[] counter = new int[n_classes];
		for (int i = 0; i < n_classes; i++) {
			// beginning with zero
			counter[i] = 0;
		}
		// loop over the dataset and bin the rows based on classes
		for (int i = 0; i < ds_size; i++) {
			Row row = this.rows.get(i);
			for (int j = 0; j < n_classes; j++) {
				if (row.target.equals(this.s_classes[j])) {
					counter[j]++;
					int addToFold = counter[j] % n_folds;
					Row copy = row.clone();
					folds[addToFold].test.rows.add(copy);
				}
			}
		}
		// now there are stratified k splits in each TrainAndTest train member
		// finally add to training data of each fold testing data of other folds
		// for each fold
		for (int i = 0; i < n_folds; i++) {
			// consider every other fold
			int test_size = folds[i].test.rows.size();
			for (int j = 0; j < n_folds; j++) {
				// we don't want to put test data into train data of the fold itself
				if (i != j) {
					// for every row
					for (int k = 0; k < test_size; k++) {
						// from fold i put row k in fold j
						Row copy = folds[i].test.rows.get(k).clone();
						folds[j].train.rows.add(copy);
					}
				}
			}
		}
		
		// finally calculate class distributions for all folds
		for (int i = 0; i < n_folds; i++) {
			folds[i].test.calculateDistribution();
			folds[i].train.calculateDistribution();
			folds[i].test.types = this.types;
			folds[i].train.types = this.types;
		}
		return folds;
	}

	public void uncover() {
		for (Row row : this.rows) {
			row.covered = false;
			row.n_covered = 0;
		}
		
	}

}

class TrainAndTest {
	public DataSet train;
	public DataSet test;
	
	public TrainAndTest(Partitions partitions, String[] header, String[] classes) {
		this.train = new DataSet(partitions, header, classes);
		this.test = new DataSet(partitions, header, classes);
	}
}
