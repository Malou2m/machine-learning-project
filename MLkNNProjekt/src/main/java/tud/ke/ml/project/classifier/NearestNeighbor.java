package tud.ke.ml.project.classifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import sun.reflect.generics.tree.DoubleSignature;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tud.ke.ml.project.util.Pair;

/**
 * This implementation assumes the class attribute is always available (but probably not set).
 * 
 */
public class NearestNeighbor extends INearestNeighbor implements Serializable {
	private static final long serialVersionUID = 1L;

	protected double[] scaling;
	protected double[] translation;
	protected List<List<Object>> data;

	@Override
	public String getMatrikelNumbers() {
/**--------------------------- 
 * 
 * PLEASE INSERT YOUR MATRIKEL NUMBER HERE! 
 * 
 * ---------------------------------------------*/
		return("2346827,2696768,2709600");
	}

	@Override
	protected void learnModel(List<List<Object>> data) {
		this.data = data;
	}

	@Override
	protected Map<Object, Double> getUnweightedVotes(List<Pair<List<Object>, Double>> subset) {
		Map<Object, Double> votes = new HashMap<Object, Double>();
		// Return the frequency distribution of each class in the given neighbours.
                // => Majority voting
		for(Pair<List<Object>, Double> instance : subset){
			Object i_class = instance.getA().get(getClassAttribute());
			votes.putIfAbsent(i_class, 0.00);
			Double old_value = votes.get(i_class);
			votes.replace(i_class, old_value+1);
		}
		return votes;
	}

	@Override
	protected Map<Object, Double> getWeightedVotes(List<Pair<List<Object>, Double>> subset) {
		Map<Object, Double> votes = new HashMap<Object, Double>();
		Double allWeights=0.0;
                // Inverted Distance weighting (sum of 1/distance for each Class)
		for(Pair<List<Object>, Double> instance : subset) {
			Object i_class = instance.getA().get(getClassAttribute());
			votes.putIfAbsent(i_class, 0.00);
			Double old_value = votes.get(i_class);
			Double instanceWeight=(1 / instance.getB());
			votes.replace(i_class, old_value+instanceWeight);
			allWeights+=instanceWeight;
		}
		for (Map.Entry<Object, Double> entry : votes.entrySet()){
			votes.replace(entry.getKey(),entry.getValue()/allWeights);
		}
		return votes;
	}

	@Override
	protected Object getWinner(Map<Object, Double> votes) {
		Object winner = null;
		Double max = 0.00;
		// Sets the class with maximal frequency as a winner
		for(Entry<Object, Double> entry: votes.entrySet()){
			if(entry.getValue() > max){
				max = entry.getValue();
				winner = entry.getKey();
			}
		}
		return winner;
	}

	@Override
	protected Object vote(List<Pair<List<Object>, Double>> subset) {
		if(isInverseWeighting()){
			return getWinner(getWeightedVotes(subset));
		}else{
			return getWinner(getUnweightedVotes(subset));
		}

	}

	@Override
	protected List<Pair<List<Object>, Double>> getNearest(List<Object> data) {
		List<Pair<List<Object>, Double>> nearests = new ArrayList<Pair<List<Object>, Double>>();
		List<Pair<List<Object>, Double>> instance_distance = new ArrayList<Pair<List<Object>, Double>>();
		int limit = getkNearest();

		//Normalizing
 		if(isNormalizing()){
			if(scaling == null && translation == null) {
				double[][] norm = normalizationScaling();
				scaling = norm[0];
				translation = norm[1];
			}

			/*
			System.out.println("Before");
			System.out.println("------------------------------------");
			print();
			*/
 			for(List<Object> instance: this.data){
 				for(int index = 0; index<instance.size(); index++){
 					if(instance.get(index) instanceof Double){
 						Double old = (Double) new Double(((Double) instance.get(index)).doubleValue());
 						Double newValue = (old + translation[index]) * scaling[index];
 						instance.set(index, newValue);
 					}
 				}
 			}
 			for(int index=0; index<data.size(); index++){
				if(data.get(index) instanceof Double){

					Double old = (Double) new Double(((Double) data.get(index)).doubleValue());
					Double newValue = (old + translation[index]) * scaling[index];
					System.out.println(String.format("normalized[%d] %f to %f; scaling: %f, translation: %f", index, old, newValue, scaling[index], translation[index]));
					data.set(index, newValue);
				}
			}
			System.out.println("-------------------------------------\nnext");
			/*
 			System.out.println("After");
			System.out.println("------------------------------------");
			print();
			*/

		}
		// Assign a distance to each instance
		for(List<Object> instance : this.data){
			if(instance != data){
				double d;
				if(getMetric()==1){
					d = determineEuclideanDistance(instance, data);
				}else {
					d = determineManhattanDistance(instance, data);
				}
				instance_distance.add(new Pair<List<Object>, Double>(instance, d));
			}
		}
		// Sort the list of pairs (instance, distance) in ascending order
		Collections.sort(instance_distance, new Comparator<Pair<List<Object>, Double>>() {
			@Override
		    public int compare(Pair<List<Object>, Double> p1, Pair<List<Object>, Double> p2) {
		        if(p1.getB() < p2.getB()){
		            return -1;
		        } else {
		            return 1;
		        }
		    }
		});
		// get k nearest neighbours
		for(int i=0; i<limit; i++){
			nearests.add(instance_distance.get(i));
		}
		return nearests;
	}

	@Override
	protected double determineEuclideanDistance(List<Object> instance1, List<Object> instance2) {
		// Do not consider missing values
		double distance = 0.00;
		if(instance1.size() == instance2.size()){
                        // Does not compute the distance between the class attributes!
			for(int i = 0; i<instance1.size()-1; i++){
				Object attribute1 = instance1.get(i);
				Object attribute2 = instance2.get(i);
				// Doesn't change the distance if both attributes are equal
				if(!attribute1.equals(attribute2)){
					// 0/1 Distance for nominal attributes
					if(attribute1.getClass().equals(String.class) &&
						attribute2.getClass().equals(String.class)){
						distance += 1;
					}
					// difference for numeric attributes
					else if(attribute1.getClass().equals(Double.class) &&
							attribute2.getClass().equals(Double.class)){
							// normalized numeric value
							Double doub1=new Double(attribute1.toString());
							Double doub2=new Double(attribute2.toString());
							distance+=Math.pow(doub1-doub2,2);
					}
					else{
						System.out.println("Invalide attribute types or types does not match.");
					}
				}
			}
			return Math.sqrt(distance);
		}
		else{
			System.out.println("Distance cannot be computed, as both instances don't have the"
					+ "same length");
			return -999.99;
		}
	}

	private Double[] getMinAndMax(int i) {
		Double[] min_max = {Double.MAX_VALUE, 0.00};
		for(List<Object> instance: data){
			if((Double)instance.get(i) < min_max[0]){
				min_max[0] = (Double)instance.get(i);
			}
			if((Double)instance.get(i) > min_max[1]){
				min_max[1] = (Double)instance.get(i);
			}
		}
		return min_max;
	}

	@Override
	protected double determineManhattanDistance(List<Object> instance1, List<Object> instance2) {
		// Do not consider missing values
		double distance = 0.00;
		if(instance1.size() == instance2.size()){
                        // Does not compute the distance between the class attributes!
			for(int i = 0; i<instance1.size()-1; i++){
				Object attribute1 = instance1.get(i);
				Object attribute2 = instance2.get(i);
				// Doesn't change the distance if both attributes are equal
				if(!attribute1.equals(attribute2)){
					// 0/1 Distance for nominal attributes
					if(attribute1.getClass().equals(String.class) &&
							attribute2.getClass().equals(String.class)){
						distance += 1;
					}
					// difference for numeric attributes
					else if(attribute1.getClass().equals(Double.class) &&
							attribute2.getClass().equals(Double.class)){
						Double doub1=new Double(attribute1.toString());
						Double doub2=new Double(attribute2.toString());
						Double save=doub1-doub2;
						if(save<0){
							save*=-1;
						}
						distance+=save;
					}
					else{
						System.out.println("Invalide attribute types or types does not match.");
					}
				}
			}
			return distance;
		}
		else{
			System.out.println("Distance cannot be computed, as both instances don't have the"
					+ "same length");
			return -999.99;
		}
	}

	@Override
	protected double[][] normalizationScaling() {
		System.out.println("was here in normalizationScaling\n--------------");
		int size = data.get(0).size();
 		double[] scaling = new double[size];
 		double[] translation =  new double[size];
 
 		double[] max = new double[size];
 		double[] min = new double[size];
 		for(int i = 0; i < size; i++){
 			max[i] = 0;
 			min[i] = Double.MAX_VALUE;
 		}
 
 		//getting max and min values for all attributes
 		for(List<Object> instance : data){
 			for(int i = 0; i< size; i++){
 				Object attr = instance.get(i);
 				if ( attr instanceof Double) {
 					Double d = (Double) attr;
 					if (min[i] > d)
 						min[i] = d;
 					if (max[i] < d)
 						max[i] = d;
 				}
 			}
 		}

		for(int i=0; i<size; i++){
 			translation[i] = -1 * min[i];
			double diff = (max[i] - min[i]);
 			scaling[i] = diff > 0 ? 1/diff : 1; // to avoid infinity

 		}
 		return new double[][] {scaling, translation};
	}
	void print(){
		this.data.subList(0,100).forEach(instance -> System.out.println("[" + instance.stream().filter(obj -> obj instanceof  Double).map(obj -> String.format("%.4f", obj)).collect(Collectors.joining("\t")) + "]"));
	}

}
