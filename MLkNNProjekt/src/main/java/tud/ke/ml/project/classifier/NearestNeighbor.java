package tud.ke.ml.project.classifier;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
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

		//what is the maximal vote
		Double maximal = votes.values().stream()
				.mapToDouble(d -> d)
				.max()
				.getAsDouble();

		//get all objects, which have this maximal value
		Set<Object> winners = votes.entrySet().stream()
				.filter(entry -> entry.getValue().doubleValue() == maximal.doubleValue())
				.map(entry -> entry.getKey())
				.collect(Collectors.toSet());

		//if there is only one winner, return the winner;
		if(winners.size() == 1)
			return winners.toArray()[0];

		//if there are multiple, count which is more often present in the whole dataset and return this one:

		Map<Object, Integer> counter = new HashMap<>(winners.size()); //initialize a counter-map
		winners.forEach(winnerAttr -> counter.put(winnerAttr.toString(), 0)); //initialize the counter

		//iterate over the data set
		this.data.stream()
				.map(instance -> instance.get(getClassAttribute())) 							//extract all class-attributes
				.filter(attr -> winners.contains(attr.toString()))								//extract only the necessary of the winner
				.forEach(attr -> counter.put(attr.toString(),counter.get(attr.toString()) + 1));//count them

		//choose the biggest
		winner = counter.entrySet().stream()
				.reduce((a, b) -> a.getValue() > b.getValue() ? a : b)
				.get()
				.getKey();

		//and finally return
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
				//this.data
				this.data.forEach(instance -> {
					for (int index = 0; index < instance.size(); index++) {
						if (instance.get(index) instanceof Double) {
							Double old = (Double) instance.get(index);
							Double newValue = (old + translation[index]) * scaling[index];
							instance.set(index, newValue);
						}
					}
				});
			}
			// incoming data
 			for(int index=0; index<data.size(); index++){
				if(data.get(index) instanceof Double){

					Double old = (Double) data.get(index);
					Double newValue = (old + translation[index]) * scaling[index];
					data.set(index, newValue);
				}
			}
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
				instance_distance.add(new Pair<>(instance, d));
			}
		}
		// Sort the list of pairs (instance, distance) in ascending order
		Collections.sort(instance_distance, (p1, p2)->
			    p1.getB() < p2.getB() ? -1 : 1
		);

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
			for(int i = 0; i<instance1.size(); i++){
				if(i == getClassAttribute())
					continue;// Does not compute the distance between the class attributes!
				Object attribute1 = instance1.get(i);
				Object attribute2 = instance2.get(i);
				// Doesn't change the distance if both attributes are equal
				if(!attribute1.equals(attribute2)){
					// 0/1 Distance for nominal attributes
					if(attribute1 instanceof String && attribute2 instanceof String){
						distance += 1;
					}
					// difference for numeric attributes
					else if(attribute1 instanceof Double && attribute2 instanceof Double){
							distance += Math.pow((Double)attribute1 - (Double)attribute2, 2);
					}
					else{
						System.out.println("Invalid attribute types or types does not match.");
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

	@Override
	protected double determineManhattanDistance(List<Object> instance1, List<Object> instance2) {
		// Do not consider missing values
		double distance = 0.00;
		if(instance1.size() == instance2.size()){

			for(int i = 0; i<instance1.size(); i++){
				if(i == getClassAttribute())
					continue;// Does not compute the distance between the class attributes!
				Object attribute1 = instance1.get(i);
				Object attribute2 = instance2.get(i);
				// Doesn't change the distance if both attributes are equal
				if(!attribute1.equals(attribute2)){
					// 0/1 Distance for nominal attributes
					if(attribute1 instanceof String && attribute2 instanceof String){
						distance += 1;
					}
					// difference for numeric attributes
					else if(attribute1 instanceof Double && attribute2 instanceof Double){
						distance += Math.abs((Double)attribute1 - (Double) attribute2);
					}
					else{
						System.out.println("Invalid attribute types or types does not match.");
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
