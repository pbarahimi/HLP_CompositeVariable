import javax.print.attribute.standard.MediaSize.Other;

public class Node {
	public final int ID;
	public final double x;
	public final double y;
	public boolean isHub = false;

	public Node(int ID, double x, double y) {
		this.ID = ID;
		this.x = x;
		this.y = y;
	}

	// copy constructor
	public Node(Node other) {
		this.ID = other.ID;
		this.x = other.x;
		this.y = other.y;
		this.isHub = other.isHub;
	}

	@Override
	public int hashCode() {
		return this.ID;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Node))
			return false;
		if (obj == this)
			return true;

		Node other = (Node) obj;
		if (this.ID == other.ID)
			return true;
		else
			return false;
	}
	
	@Override
	public String toString(){
		return this.ID + "";	
	}
}
