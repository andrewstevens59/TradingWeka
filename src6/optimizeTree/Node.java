package optimizeTree;

public class Node<Key extends Comparable<Key>, Value> {

	public Key key;           // key
	public Value val;         // associated data
	public Node<Key, Value> left, right;  // links to left and right subtrees
	public boolean color;     // color of parent link
	public int N;             // subtree count

    public Node(Key key, Value val, boolean color, int N) {
        this.key = key;
        this.val = val;
        this.color = color;
        this.N = N;
    }

}
