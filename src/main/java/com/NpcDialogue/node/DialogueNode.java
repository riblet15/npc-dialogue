package com.NpcDialogue.node;

import java.util.ArrayList;
import java.util.List;

public class DialogueNode
{
	String content;
	private DialogueNode parent;
	private List<DialogueNode> children = new ArrayList<>();

	public DialogueNode(String content) {
		this.content = content;
	}

	public DialogueNode findOption(String substring) {
		if (this.content.contains(substring)) {
			return this;
		} else {
			for(DialogueNode child : children) {
				DialogueNode downstream = child.findOption(substring);
				if (downstream != null) {
					return downstream;
				}
			}
			return null;
		}
	}

	public DialogueNode getParent() {
		return this.parent;
	}

	public void setParent(DialogueNode parent) {
		this.parent = parent;
	}

	public DialogueNode addChild(DialogueNode node) {
		//Intercept the addChild to inject into previously created tree.
		//This allows us to build a node tree of all options together
		if(node instanceof MetaDialogueNode)
		{
			if(this.children.size() > 0)
			{
				DialogueNode lastChild = this.children.get(this.children.size() - 1);
				if (node.content.equals(lastChild.content))
				{
					return lastChild;
				}
			}
		} else {
			if (this.content.equals(node.content))
			{
				return this;
			}
			for (DialogueNode n : children)
			{
				if (n.content.equals(node.content))
				{
					return n;
				}
			}
		}
		node.setParent(this);
		children.add(node);
		return node;
	}

	public List<DialogueNode> getChildren() {
		return this.children;
	}

	private void printDepth(StringBuilder sb, int depth) {
		for(int i = 0; i < depth; i++) {
			sb.append("*");
		}
	}

	private void printContent(StringBuilder sb) {
		sb.append(this.content);
	}

	public void print(StringBuilder sb, int depth) {
		if(content.length() > 0) {
			this.printDepth(sb, depth);
			this.printContent(sb);
			sb.append("\n");
		}
		if (this.children.size() > 0) {
			if (this instanceof OptionDialogueNode) {
				depth += 1;
			}
			for (DialogueNode n : children) {
				n.print(sb, depth);
			}
			if(content.length() > 0
			&& this instanceof OptionDialogueNode) {
				this.printDepth(sb, depth);
				sb.append("{{tact|end}}\n");
			}
		}
	}
}
