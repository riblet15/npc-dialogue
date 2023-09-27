package com.NpcDialogue.node;

public class SelectDialogueNode extends DialogueNode
{
	public SelectDialogueNode(String content)
	{
		super("{{tselect|" + content + "}}");
	}
}
