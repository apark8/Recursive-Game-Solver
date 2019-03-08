import java.util.*;
import java.io.*;

class PetDet {
	private enum NodeType { CAR, PET, HOME }
	private class Node {
		public Node(String name, int index) {
			this.name = name;
			this.index = index;

			if (name.contains("_home")) { type = NodeType.HOME; }
			else if (name.equals("car")) { type = NodeType.CAR; }
			else { type = NodeType.PET; }
		}

		String name;
		int index;
		NodeType type;
		boolean visited = false;
	}

	public PetDet(String args[]) throws IOException {
		//read in data and create weight matrix and ordered adjacency matrix
		initializeStructures(args);
		completeEdgeMatrix();
		generateAdjacencyMatrix();

		//get the car node and begin recursive search for solution. Print if found
		Node startNode = nodeReference.get("car");
		startNode.visited = true;
		boolean solutionFound = solvePuzzle(startNode, new ArrayList<Node>(), 0);
		printSolution(solutionFound);
	}

	//read in data and create nodes accordingly. Add onto map and record edge weights
	private void initializeStructures(String args[]) throws IOException {
		BufferedReader input_reader = new BufferedReader(new FileReader(args[0]));
		String nextLine = input_reader.readLine();

		//read in number of allowed moves
		Scanner scnr = new Scanner(nextLine);
		movesAllowed = scnr.nextInt();
		scnr.close();
		//read in rest of data and initialize map
		while ((nextLine = input_reader.readLine()) != null) {
			scnr = new Scanner(nextLine);
			String name1 = scnr.next();
			String name2 = scnr.next();
			int distance = scnr.nextInt();

			Node node1 = addToMap(name1);
			Node node2 = addToMap(name2);
			linkNodes(node1, node2, distance);
			scnr.close();
		}
		input_reader.close();
	}

	//generate ordered adjacency matrix by sorting the weight matrix by index
	private void generateAdjacencyMatrix() {
		for (int i = 0; i < nodeCount; i++) {
			boolean[] sorted = new boolean[nodeCount];
			ArrayList<Integer> adjacencyList = orderedAdjacencyMatrix.get(i);
			ArrayList<Integer> weightList = edgeMatrix.get(i);
			for (int j = 0; j < nodeCount; j++) {
				int min = INFINITY;
				int indexOfMin = INFINITY;
				for (int k = 0; k < nodeCount; k++) {
					if (weightList.get(k) < min && !sorted[k]) {
						min = weightList.get(k);
						indexOfMin = k;
					}
				}
				adjacencyList.add(indexOfMin);
				sorted[indexOfMin] = true;
			}
		}
	}

	//Use Floyd-Warshall algorithm to fill in rest of edgeMatrix
	private void completeEdgeMatrix() {
		for (int i = 0; i < nodeCount; i++) {
			for (int j = 0; j < nodeCount; j++) {
				if (i != j && edgeMatrix.get(i).get(j) == 0)
					edgeMatrix.get(i).set(j, INFINITY);
			}
		}
		for (int k = 0; k < nodeCount; k++) {
			for (int i = 0; i < nodeCount; i++) {
				for (int j = 0; j < nodeCount; j++) {
					int sum = edgeMatrix.get(i).get(k) + edgeMatrix.get(k).get(j);
					if (edgeMatrix.get(i).get(j) > sum)
						edgeMatrix.get(i).set(j, sum);
				}
			}
		}
	}

	//recursive solving method
	private boolean solvePuzzle(Node node, ArrayList<Node> passengers, int movesMade) {
		//exit condition for solving the game
		if (movesMade <= movesAllowed && allNodesVisited())
			return true;

		//backtrack if number of moves exceeded
		if (movesMade > movesAllowed)
			return false;

		//load or unload pets. If not possible, backtrack
		if (node.type == NodeType.HOME) {
			if (!unloadPet(node, passengers))
				return false;
		}
		else if (node.type == NodeType.PET) {
			if (passengers.size() == 4)
				return false;
			passengers.add(node);
		}

		//go to nearest unsearched node
		for (int i = 0; i < nodeCount; i++) {
			int adjacentNodeIndex = orderedAdjacencyMatrix.get(node.index).get(i);
			Node adjacentNode = nodeList.get(adjacentNodeIndex);
			if (!adjacentNode.visited) {
				int weight = edgeMatrix.get(node.index).get(adjacentNode.index);

				//save state of game in case of backtracking
				adjacentNode.visited = true;
				movesMade += weight;
				ArrayList<Node> temp = new ArrayList<Node>(passengers);

				//recursively solve puzzle. If a dead end is reached, backtrack to previous state
				if (!solvePuzzle(adjacentNode, passengers, movesMade)) {
					adjacentNode.visited = false;
					movesMade -= weight;
					passengers = temp;
				}
				else {
					stack.push(adjacentNode);
					return true;
				}
			}
		}
		//dead end. Backtrack if possible. If not possible, no solution found
		return false;
	}

	private boolean unloadPet(Node home, ArrayList<Node> passengers) {
		for (int i = 0; i < passengers.size(); i++) {
			Node pet = passengers.get(i);
			if (home.name.contains(pet.name)) {
				passengers.remove(i);
				return true;
			}
		}
		return false;
	}

	private boolean allNodesVisited() {
		for (Node node : nodeList) {
			if (!node.visited)
				return false;
		}
		return true;
	}

	//for each unique node, add onto list of unique nodes and expand edge matrix
	private Node addToMap(String name) {
		if (!nodeReference.containsKey(name)) {
			Node node = new Node(name, nodeCount++);
			nodeReference.put(name, node);
			nodeList.add(node);
			orderedAdjacencyMatrix.add(new ArrayList<Integer>());
			expandEdgeMatrix();
		}
		return nodeReference.get(name);
	}

	//fill in weight of edge connecting node1 and node2
	private void linkNodes(Node node1, Node node2, int distance) {
		int index1 = node1.index;
		int index2 = node2.index;
		edgeMatrix.get(index1).set(index2, distance);
		edgeMatrix.get(index2).set(index1, distance);
	}

	//dynamically expand weight matrix by 1 unit vertically and horizontally
	private void expandEdgeMatrix() {
		edgeMatrix.add(new ArrayList<Integer>());
		for (int i = 0; i < nodeCount; i++)
			edgeMatrix.get(nodeCount - 1).add(0);
		for (int i = 0; i < nodeCount - 1; i++)
			edgeMatrix.get(i).add(0);
	}

	//If solution was found, run through stack and print each move
	private void printSolution(boolean solutionFound) {
		if (solutionFound) {
			while (!stack.empty())
				System.out.printf("%s%n", stack.pop().name);
		}
		else
			System.out.printf("No solution found.%n");
	}

	static public void main(String args[]) throws IOException {
		new PetDet(args);
	}

	private int INFINITY = 9999;
	private int movesAllowed;
	private int nodeCount = 0;
	private HashMap<String, Node> nodeReference = new HashMap<String, Node>();
	private ArrayList<Node> nodeList = new ArrayList<Node>();
	private ArrayList<ArrayList<Integer>> orderedAdjacencyMatrix = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Integer>> edgeMatrix = new ArrayList<ArrayList<Integer>>();
	private Stack<Node> stack = new Stack<Node>();
}
