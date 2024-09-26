package iot.treeftp;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code TreeNode} class represents a node in a tree defined by its name
 * and a list of its children. The purpose of this class is to help in
 * manipulating the hierarchical structure of directory contents and thereafter
 * serialize it to JSON.
 * <p>
 * Example usage:
 * 
 * <pre>
 * TreeNode root = new TreeNode("rootDir");
 * TreeNode child1 = new TreeNode("Child 1");
 * TreeNode child2 = new TreeNode("Child 2");
 * 
 * root.addChild(child1);
 * root.addChild(child2);
 * </pre>
 * 
 */
public class TreeNode {
	private String name;
	private List<TreeNode> children;

	/**
	 * Constructs a {@code TreeNode} with the name of the directory or file and
	 * defines a new arraylist of nodes for its children.
	 *
	 * @param name the name of the folder or file to be held in this node.
	 */
	public TreeNode(String name) {
		this.name = name;
		this.children = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TreeNode> getChildren() {
		return children;
	}

	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	/**
	 * Adds a child node to this node.
	 *
	 * @param child the {@code TreeNode} to be added as a child.
	 */
	public void addChild(TreeNode child) {
		this.children.add(child);
	}
}