package com.glaikunt.framework.pathfinding;

import com.glaikunt.framework.pathfinding.heuristics.ClosestHeuristic;

import java.util.ArrayList;
import java.util.Collections;


/**
 * A path finder implementation that uses the AStar heuristic based algorithm
 * to determine a path. 
 * 
 * @author Kevin Glass with modifications from Vault101 / Peter D Bell
 */
public class AStarPathFinder implements PathFinder {

	/**
	 * A single node in the search graph
	 */
	private static class Node implements Comparable<Node> {
		/** The x coordinate of the node */
		private final int x;
		/** The y coordinate of the node */
		private final int y;
		/** The path cost for this node */
		private float cost;
		/** The parent of this node, how we reached it in the search */
		private Node parent;
		/** The heuristic cost of this node */
		private float heuristic;
		/** The search depth of this node */
		private int depth;
		
		/**
		 * Create a new node
		 * 
		 * @param x The x coordinate of the node
		 * @param y The y coordinate of the node
		 */
		public Node(final int x, final int y) {
			this.x = x;
			this.y = y;
		}
		
		/**
		 * @see Comparable#compareTo(Object)
		 */
		public int compareTo(final Node other) {
			final Node o = other;
			
			final float f = heuristic + cost;
			final float of = o.heuristic + o.cost;
			
			if (f < of) {
				return -1;
			} else if (f > of) {
				return 1;
			} else {
				return 0;
			}
		}
		
		/**
		 * Set the parent of this node
		 * 
		 * @param parent The parent node which lead us to this node
		 * @return The depth we have no reached in searching
		 */
		public int setParent(final Node parent) {
			depth = parent.depth + 1;
			this.parent = parent;
			
			return depth;
		}
	}

	/** The set of nodes that have been searched through */
	private final ArrayList<Node> closed = new ArrayList<>();
	/** The set of nodes that we do not yet consider fully searched */
	private final ArrayList<Node> open = new ArrayList<>();
	
	/** The map being searched */
	private final TileBasedMap map;
	/** The complete set of nodes across the map */
	private final Node[][] nodes;
	
	/** True if we allow diagonal movement */
	private final boolean allowDiagMovement;

	/** The heuristic we're applying to determine which nodes to search first */
	private final AStarHeuristic heuristic;
	
	/** The maximum depth of search we're willing to accept before giving up */
	private int maxSearchDistance;
	
	/**
	 * Create a path finder with the default heuristic - closest to target.
	 * 
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diagonal movement
	 */
	public AStarPathFinder(final TileBasedMap map, final int maxSearchDistance, final boolean allowDiagMovement) {
		this(map, maxSearchDistance, allowDiagMovement, new ClosestHeuristic());
	}
	
	/**
	 * Create a path finder 
	 * 
	 * @param heuristic The heuristic used to determine the search order of the map
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diagonal movement
	 */
	public AStarPathFinder(final TileBasedMap map, final int maxSearchDistance, 
						   final boolean allowDiagMovement, final AStarHeuristic heuristic) {
		this.heuristic = heuristic;
		this.map = map;
		this.maxSearchDistance = maxSearchDistance;
		this.allowDiagMovement = allowDiagMovement;
		
		nodes = new Node[map.getWidthInTiles()][map.getHeightInTiles()];
		for (int x=0;x<map.getWidthInTiles();x++) {
			for (int y=0;y<map.getHeightInTiles();y++) {
				nodes[x][y] = new Node(x,y);
			}
		}
	}

	/**
	 * Add a node to the closed list
	 * 
	 * @param node The node to add to the closed list
	 */
	protected void addToClosed(final Node node) {
		closed.add(node);
	}
	
	/**
	 * Add a node to the open list
	 * 
	 * @param node The node to be added to the open list
	 */
	protected void addToOpen(final Node node) {
		open.add(node);
		Collections.sort(open);
	}
	
	/**
	 * @see PathFinder#findPath(Mover, int, int, int, int)
	 */
	public Path findPath(final Mover mover, final int sx, final int sy, final int tx, final int ty, final boolean diagonalCost) {
		// easy first check, if the destination is blocked, we can't get there
		if (map.blocked(mover, tx, ty)) {
			return null;
		}
		
		// initial state for A*. The closed group is empty. Only the starting
		// tile is in the open list and it's cost is zero, i.e. we're already there
		nodes[sx][sy].cost = 0;
		nodes[sx][sy].depth = 0;
		closed.clear();
		open.clear();
		open.add(nodes[sx][sy]);
		
		nodes[tx][ty].parent = null;
		
		// while we haven't found the goal and haven't exceeded our max search depth
		int maxDepth = 0;
		while ((maxDepth < maxSearchDistance) && !open.isEmpty()) {
			// pull out the first node in our open list, this is determined to 
			// be the most likely to be the next step based on our heuristic
			final Node current = getFirstInOpen();
			if (current == nodes[tx][ty]) {
				break;
			}
			
			removeFromOpen(current);
			addToClosed(current);
			
			maxDepth = searchAllNeighbours(mover, sx, sy, tx, ty, diagonalCost, current, maxDepth);
		}

		// since we've got an empty open list or we've run out of search 
		// there was no path. Just return null
		if (nodes[tx][ty].parent == null) {
			return null;
		}
		
		// At this point we've definitely found a path so we can uses the parent
		// references of the nodes to find out way from the target location back
		// to the start recording the nodes on the way.
		final Path path = new Path();
		Node target = nodes[tx][ty];
		while (target != nodes[sx][sy]) {
			path.prependStep(target.x, target.y, target.cost);
			target = target.parent;
		}
		path.prependStep(sx,sy, nodes[sx][sy].cost);
		
		// thats it, we have our path 
		return path;
	}
	
	private int searchAllNeighbours(final Mover mover, final int sx, final int sy, final int tx, final int ty, final boolean diagonalCost,
			final Node current, int maxDepth) {
		// search through all the neighbours of the current node evaluating
		// them as next steps
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++) {
				// not a neighbour, its the current tile
				if ((x == 0) && (y == 0)) {
					continue;
				}

				// if we're not allowing diagonal movement then only
				// one of x or y can be set
				if (!allowDiagMovement) {
					if ((x != 0) && (y != 0)) {
						continue;
					}
				} else {
					// [B][#][B]
					// [#][A][#]
					// [B][#][B]
					// if we ARE allowing diagonal movement, AND we are moving
					// diagonally,
					// we cannot move diagonally between two impassable walls
					if ((x != 0) && (y != 0)) {
						final int lx = current.x;
						final int ly = y + current.y;

						final int rx = x + current.x;
						final int ry = current.y;

						if (!isValidLocation(mover, sx, sy, lx, ly)
								|| !isValidLocation(mover, sx, sy, rx, ry)) {
							continue;
						}
					}
				}

				// determine the location of the neighbour and evaluate it
				final int xp = x + current.x;
				final int yp = y + current.y;

				if (isValidLocation(mover, sx, sy, xp, yp)) {
					// the cost to get to this node is cost the current plus the
					// movement
					// cost to reach this node. Note that the heuristic value is
					// only used
					// in the sorted open list
					float nextStepCost = current.cost
							+ getMovementCost(mover, current.x, current.y, xp,
									yp);
					if (allowDiagMovement && diagonalCost && (x != 0) && (y != 0)) {
						// up let's be honest the diagonal tile cost should
						// really be root 2, not 1
						nextStepCost += 0.41421f; // add the 'diagonal extra' to
													// it
					}
					final Node neighbour = nodes[xp][yp];
					map.pathFinderVisited(xp, yp);

					// if the new cost we've determined for this node is lower
					// than
					// it has been previously makes sure the node hasn't been
					// discarded. We've
					// determined that there might have been a better path to
					// get to
					// this node so it needs to be re-evaluated
					if (nextStepCost < neighbour.cost) {
						if (inOpenList(neighbour)) {
							removeFromOpen(neighbour);
						}
						if (inClosedList(neighbour)) {
							removeFromClosed(neighbour);
						}
					}

					// if the node hasn't already been processed and discarded
					// then
					// reset it's cost to our current cost and add it as a next
					// possible
					// step (i.e. to the open list)
					if (!inOpenList(neighbour) && !(inClosedList(neighbour))) {
						neighbour.cost = nextStepCost;
						neighbour.heuristic = getHeuristicCost(mover, xp, yp, tx, ty);
						maxDepth = Math.max(maxDepth, neighbour.setParent(current));
						addToOpen(neighbour);
					}
				}
			}
		}
		
		return maxDepth;
	}

	/**
	 * Get the first element from the open list. This is the next
	 * one to be searched.
	 * 
	 * @return The first element in the open list
	 */
	protected Node getFirstInOpen() {
		return open.get(0);
	}
	
	/**
	 * Get the heuristic cost for the given location. This determines in which 
	 * order the locations are processed.
	 * 
	 * @param mover The entity that is being moved
	 * @param x The x coordinate of the tile whose cost is being determined
	 * @param y The y coordinate of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The heuristic cost assigned to the tile
	 */
	public float getHeuristicCost(final Mover mover, final int x, final int y, final int tx, final int ty) {
		return heuristic.getCost(map, mover, x, y, tx, ty);
	}
	
	/**
	 * Get the cost to move through a given location
	 * 
	 * @param mover The entity that is being moved
	 * @param sx The x coordinate of the tile whose cost is being determined
	 * @param sy The y coordinate of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The cost of movement through the given tile
	 */
	public float getMovementCost(final Mover mover, final int sx, final int sy, final int tx, final int ty) {
		return map.getCost(mover, sx, sy, tx, ty);
	}
	
	/**
	 * Check if the node supplied is in the closed list
	 * 
	 * @param node The node to search for
	 * @return True if the node specified is in the closed list
	 */
	protected boolean inClosedList(final Node node) {
		return closed.contains(node);
	}
	
	/**
	 * Check if a node is in the open list
	 * 
	 * @param node The node to check for
	 * @return True if the node given is in the open list
	 */
	protected boolean inOpenList(final Node node) {
		return open.contains(node);
	}
	
	/**
	 * Check if a given location is valid for the supplied mover
	 * 
	 * @param mover The mover that would hold a given location
	 * @param sx The starting x coordinate
	 * @param sy The starting y coordinate
	 * @param x The x coordinate of the location to check
	 * @param y The y coordinate of the location to check
	 * @return True if the location is valid for the given mover
	 */
	protected boolean isValidLocation(final Mover mover, final int sx, final int sy, final int x, final int y) {
		boolean invalid = (x < 0) || (y < 0) || (x >= map.getWidthInTiles()) || (y >= map.getHeightInTiles());
		
		if ((!invalid) && ((sx != x) || (sy != y))) {
			invalid = map.blocked(mover, x, y);
		}
		
		return !invalid;
	}

	/**
	 * Remove a node from the closed list
	 * 
	 * @param node The node to remove from the closed list
	 */
	protected void removeFromClosed(final Node node) {
		closed.remove(node);
	}
	
	/**
	 * Remove a node from the open list
	 * 
	 * @param node The node to remove from the open list
	 */
	protected void removeFromOpen(final Node node) {
		open.remove(node);
	}
	
	/**
	 * Allow overwrites from the Console
	 * @param distance
	 */
	public void setMaxSearchDistance(final int distance) {
		maxSearchDistance = distance;
	}
}
