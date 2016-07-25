import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class CompositeHLP_Main {
	private static final double[][] coordinates = MyArray.read("coordinates.txt");
	private static final double[][] tmpFlows = MyArray.read("w.txt");
	private static int nVar = tmpFlows.length;
	private static double[][] flows = new double[nVar][nVar];
	private static final double[][]	fixedCosts = MyArray.read("fixedcharge.txt");
	private static final int P = 3;
	private static final double alpha = 0.2;
	
	private static ArrayList<Node> nodes = new ArrayList<Node>();
	private static ArrayList<Load> loads = new ArrayList<Load>();
	private static ArrayList<HubComb> hubCombs = new ArrayList<HubComb>();
	private static RouteList[][] routes;
	
	public static void main(String[] args) throws GRBException{
		// Filling in the flows matrix assymetrically
		for (int i = 0; i < nVar; i++) {
			for (int j = 0; j < nVar; j++) {
				flows[i][j] = tmpFlows[i][j] + tmpFlows[j][i];
			}
		}
				
		// build Gurobi model and environment.
		GRBEnv env = new GRBEnv("RpHND.log");
		GRBModel model = new GRBModel(env);
		
		// initializing node objects.
		for (int i = 0 ; i < coordinates.length ; i++){
			nodes.add(new Node(i, coordinates[i][0], coordinates[i][1]));
		}
		
		// initializing loads.
		int counter = 0;
		for (int i = 0 ; i < nodes.size() ; i++){
			for (int j = i+1 ; j < nodes.size() ; j++){
				loads.add(new Load(counter++, nodes.get(i), nodes.get(j), flows[i][j]));
			}
		}
		
		// initializing hub combinations.
		int cntr = 0;
		generateHubCombs(nodes, P, hubCombs, fixedCosts);
		GRBVar[] y = new GRBVar[hubCombs.size()];
		
		for (HubComb h : hubCombs){
			h.ID = cntr++;
			y[h.ID] = model.addVar(0, 1, h.fixedCharge, GRB.CONTINUOUS, "y"+h.ID);
//			System.out.println(h);
		}
		
		// initializing sets of routes.
		routes = new RouteList[loads.size()][hubCombs.size()]; // Number of possible routes between each OD pair
		GRBVarList[][] x = new GRBVarList[loads.size()][hubCombs.size()];

		for (Load l : loads){
			for (HubComb h : hubCombs){
				routes[l.ID][h.ID] = new RouteList();
				routes[l.ID][h.ID].list = getAllRoutes(l,h);
				x[l.ID][h.ID] = new GRBVarList();
				for (Route r : routes[l.ID][h.ID].list){
					x[l.ID][h.ID].list.add(model.addVar(0, 1, flows[l.origin.ID][l.destination.ID] * r.cost, GRB.CONTINUOUS, "x_" + r.toString() + "_" + h.ID + "_" + l.ID/*r.toString()*/) );
				}
			}
		}
		
		model.update();
		
		// Adding constraint set (1) to the model
		GRBConstr[] c1 = new GRBConstr[loads.size()];
		GRBLinExpr expr;
		for ( Load l : loads ){
			
			expr = new GRBLinExpr();
			for ( HubComb h : hubCombs )
				for (GRBVar var : x[l.ID][h.ID].list)
					expr.addTerm(1, var);
			c1[l.ID] = model.addConstr(expr, GRB.EQUAL, 1, "c1_" + l.ID );

		}
		
		// Adding constraint (2)	
		expr = new GRBLinExpr();
		for (GRBVar var : y)
			expr.addTerm(1, var);
		GRBConstr c2 = model.addConstr(expr, GRB.EQUAL, 1, "c2"); 
		
				
		// Adding constraint set (3)
		GRBConstr[] c3 = new GRBConstr[hubCombs.size()];
		for ( HubComb h : hubCombs ){
			expr = new GRBLinExpr();
			
			for ( Load l : loads){
				for ( GRBVar var : x[l.ID][h.ID].list){
					expr.addTerm(1, var);
				}
			}
			expr.addTerm(-1*loads.size(), y[h.ID]);
			c3[h.ID] = model.addConstr(expr, GRB.LESS_EQUAL, 0, "c3_" + h.ID);
		}
		
		// solving the model
		model.optimize();
		model.write("d:/model.lp");	
		printSol(model);

//		for (HubComb h : hubCombs)
//			System.out.println(h);
	}
	
	/**
	 * Gets all possible routes for a given load and hub combination.
	 * @param l
	 * @param h
	 * @return
	 */
	static ArrayList<Route> getAllRoutes(Load l, HubComb h){
		ArrayList<Route> output = new ArrayList<Route>();
		int P = h.hubs.size();
		
		// updating isHub attribute of the nodes in the hub combination.
		for (Node n : h.hubs)
			n.isHub = true;
		
		if ( l.origin.isHub && l.destination.isHub ){
			// There is only one possible route with discounted distance.
			output.add( new Route(l.origin, l.destination, l, alpha));
		}else if ( l.origin.isHub ){
			for (Node n : h.hubs){
				output.add( new Route(l.origin, n, l, alpha));
			}
		}else if (l.destination.isHub ){
			for (Node n : h.hubs){
				output.add( new Route(n, l.destination, l, alpha) );
			}
		}else{
			// enumerate all possible routes.
			for (int i = 0 ; i < P ; i++ ){
				for ( int j = i ; j < P ; j++){
					Route r1 = new Route(h.hubs.get(i), h.hubs.get(j), l, alpha);
					Route r2 = new Route(h.hubs.get(j), h.hubs.get(i), l, alpha);
					if (r1.cost <= r1.cost)
						output.add(r1);
					else
						output.add(r2);
				}
			}
		}
		
		// setting the isHub attribute of the origin and destination to false.
		for (Node n : h.hubs)
			n.isHub = false;
		
		return output;
	}
	
	/**
	 * Generates all possible hub combinations of size k from the set of nodes.
	 * 
	 * @param nodes
	 * @param k
	 * @param hubCombs
	 * @param fixedCosts
	 */
	static void generateHubCombs(List<Node> nodes, int k, ArrayList<HubComb> hubCombs, double[][] fixedCosts) {
		int[] set = new int[nodes.size()];
		for (int i = 0 ; i < nodes.size() ; i++)
			set[i] = i;
		int[] subset = new int[k];
	    processLargerSubsets(set, subset, 0, 0, hubCombs, fixedCosts);
	}
	
	/**
	 * Sub-procedure used to generate hub combinations.
	 * @param set
	 * @param subset
	 * @param subsetSize
	 * @param nextIndex
	 * @param hubCombs
	 * @param fixedCosts
	 */
	static void processLargerSubsets(int[] set, int[] subset, int subsetSize, int nextIndex, ArrayList<HubComb> hubCombs, double[][] fixedCosts) {
	    if (subsetSize == subset.length) {
	    	System.out.println(Arrays.toString(subset));	        
	        hubCombs.add(new HubComb( subset, nodes, fixedCosts));
	    } else {
	        for (int j = nextIndex; j < set.length; j++) {
	            subset[subsetSize] = set[j];
	            processLargerSubsets(set, subset, subsetSize + 1, j + 1, hubCombs, fixedCosts);
	        }
	    }
	}
	
	/**
	 * prints the solution
	 * @param model
	 * @throws GRBException
	 */
	private static void printSol(GRBModel model) throws GRBException{
		for (GRBVar var : model.getVars()) 
			if (var.get(GRB.DoubleAttr.X)>0 /*&& var.get(GRB.StringAttr.VarName).contains("y")*/) 
				System.out.println(var.get(GRB.StringAttr.VarName) + " : " 
						+ var.get(GRB.DoubleAttr.X) + " - "
						+ var.get(GRB.DoubleAttr.Obj));
	}
}
