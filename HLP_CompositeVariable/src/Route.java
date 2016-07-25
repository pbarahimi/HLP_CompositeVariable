
public class Route {
	public final Load load;
	public final Node hub1;
	public final Node hub2;
	public final double cost;
	
	/**
	 * Constructor
	 * 
	 * @param h1
	 * @param h2
	 * @param load
	 * @param alpha
	 */
	public Route ( Node h1, Node h2, Load load, double alpha ){
		this.load = load;
		this.hub1 = h1;
		this.hub2 = h2;
		this.cost = getRouteCost ( load, alpha );
	}
	
	/**
	 * calculates the distance of a route given a load,
	 * origin, destination and a discount factor. 
	 * 
	 * @param load
	 * @param alpha
	 * @return
	 */
	private double getRouteCost( Load load, double alpha ){
		double output = getDistance(load.origin, hub1, alpha);
		output += getDistance(hub1, hub2, alpha);
		output += getDistance (hub2, load.destination, alpha);	
//		System.out.println(load.origin + "_" + hub1 + "_" + hub2 + "_" + load.destination + ": " + output);
		return output;		
	}
	
	/**
	 * returns the Euclidean distance between the two nodes considering
	 * the discount factor if the two nodes are hubs.
	 * @param n1
	 * @param n2
	 * @return
	 */
	private double getDistance (Node n1, Node n2, double alpha ){
		double coefficient = 1;
		if (n1.isHub && n2.isHub) coefficient = 1 - alpha;
		return coefficient * (Math.sqrt(Math.pow(n1.x - n2.x, 2) + Math.pow(n1.y - n2.y, 2)));
	}
	
	@Override
	public String toString(){
		return "(" + this.load.origin + "," + this.hub1 + "," + this.hub2 + "," + this.load.destination + ")";
	}
}
