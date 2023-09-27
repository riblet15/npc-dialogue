package com.NpcDialogue.node;

public class PlayerDialogueNode extends DialogueNode
{
	public PlayerDialogueNode(String content)
	{
		super("'''Player:''' " + content);
	}
}
