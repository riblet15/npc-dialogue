package com.NpcDialogue.node;

public class NPCDialogueNode extends DialogueNode
{
	public NPCDialogueNode(String npcName, String content)
	{
		super("'''" + npcName + ":''' " + content);
	}
}
