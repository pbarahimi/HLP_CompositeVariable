
public class Load {
	public final int ID;
	public final Node origin;
	public final Node destination;
	public final double flow;
	
	public Load(int ID, Node origin, Node destination, double flow){
		this.ID = ID; //origin.ID + "_" + destination.ID;
		this.origin = origin;
		this.destination = destination;
		this.flow = flow;
	}
}
