/*
 * Copyright (c) 2018, Woox <https://github.com/wooxsolo>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.NpcDialogue;

import com.NpcDialogue.node.DialogueNode;
import com.NpcDialogue.node.MetaDialogueNode;
import com.NpcDialogue.node.NPCDialogueNode;
import com.NpcDialogue.node.OptionDialogueNode;
import com.NpcDialogue.node.PlayerDialogueNode;
import com.NpcDialogue.node.SelectDialogueNode;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
        name = "NPC dialogue",
        description = "Utility to make it easier to transcribe NPC dialogue for OSRS Wiki."
)

public class NpcDialoguePlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    private String lastText = null;
    private Widget[] dialogueOptions;
    private NpcDialoguePanel panel;
    private NavigationButton navButton;

	private DialogueNode rootNode = new DialogueNode("");
	private DialogueNode curParentNode = rootNode;

    @Override
    public void startUp()
    {
        // Shamelessly copied from NotesPlugin
        panel = injector.getInstance(NpcDialoguePanel.class);
        panel.init(this);

        // Hack to get around not having resources.
        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "dialogue_icon.png");

        navButton = NavigationButton.builder()
                .tooltip("NPC dialogue")
                .icon(icon)
                .priority(100)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        if (menuOptionClicked.getMenuAction() == MenuAction.WIDGET_CONTINUE && menuOptionClicked.getMenuOption().equals("Continue")) {
            int actionParam = menuOptionClicked.getActionParam();
            // if -1, "Click here to continue"
            if (actionParam > 0 && actionParam < dialogueOptions.length) {
				curParentNode = curParentNode.findOption(dialogueOptions[actionParam].getText());
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        Widget npcDialogueTextWidget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);

        if (npcDialogueTextWidget != null && !npcDialogueTextWidget.getText().equals(lastText)) {
            String npcText = npcDialogueTextWidget.getText();
            lastText = npcText;

            String npcName = client.getWidget(WidgetInfo.DIALOG_NPC_NAME).getText();
			curParentNode.addChild(new NPCDialogueNode(npcName, npcText));
			printTree();
        }

        // This should be in WidgetInfo under DialogPlayer, but isn't currently.
        Widget playerDialogueTextWidget = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

        if (playerDialogueTextWidget != null && !playerDialogueTextWidget.getText().equals(lastText)) {
            String playerText = playerDialogueTextWidget.getText();
            lastText = playerText;

			curParentNode.addChild(new PlayerDialogueNode(playerText));
			printTree();
        }

        Widget playerDialogueOptionsWidget = client.getWidget(WidgetID.DIALOG_OPTION_GROUP_ID, 1);
        if (playerDialogueOptionsWidget != null && playerDialogueOptionsWidget.getChildren() != dialogueOptions) {
            dialogueOptions = playerDialogueOptionsWidget.getChildren();

			//Detect loop
			boolean loop = false;
			DialogueNode existingSelectNode = rootNode.findOption(dialogueOptions[0].getText());
			int totalOptions = dialogueOptions.length - 1;
			if(existingSelectNode != null) {
				int matchingOptions = 0;
				for (int i = 0; i < dialogueOptions.length - 1; i++) {
					DialogueNode existingOption = existingSelectNode.findOption(dialogueOptions[i].getText());
					if(existingOption != null) {
						matchingOptions++;
					}
				}
				if(matchingOptions == totalOptions || matchingOptions == totalOptions - 1) {
					curParentNode.addChild(new MetaDialogueNode("{{tact|a previous option menu is displayed}}"));
					loop = true;
				}
			}

			if(loop) {
				curParentNode = existingSelectNode;
			} else {
				DialogueNode selectNode = new SelectDialogueNode(dialogueOptions[0].getText());
				//This lets us rewalk the tree after restarting dialogue
				selectNode = curParentNode.addChild(selectNode);
				curParentNode = selectNode;
				for (int i = 1; i < dialogueOptions.length - 2; i++)
				{
					curParentNode.addChild(new OptionDialogueNode(dialogueOptions[i].getText()));
				}
			}
			printTree();
        }

        Widget spriteTextWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
        if (spriteTextWidget != null && !spriteTextWidget.getText().equals(lastText)) {
            String spriteText = spriteTextWidget.getText();
            lastText = spriteText;
            Widget spriteWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_SPRITE);
			String itemName = "<!--Error-->";
			int itemid = -1;
			if(spriteWidget != null) {
				itemName = client.getItemDefinition(spriteWidget.getItemId()).getName();
				itemid = spriteWidget.getItemId();
			}
			curParentNode.addChild(new DialogueNode("{{tbox|<!--id: "+ itemid + "-->pic=" + itemName + " detail.png|" + spriteText + "}}"));
			printTree();
        }

        Widget msgTextWidget = client.getWidget(229, 1);
        if (msgTextWidget != null && !msgTextWidget.getText().equals(lastText)) {
            String msgText = msgTextWidget.getText();
            lastText = msgText;
			curParentNode.addChild(new DialogueNode("{{tbox|" + msgText + "}}"));
			printTree();
        }

        Widget doubleSpriteTextWidget = client.getWidget(11, 2);
        if (doubleSpriteTextWidget != null && !doubleSpriteTextWidget.getText().equals(lastText)) {
            String doubleSpriteText = doubleSpriteTextWidget.getText();
            lastText = doubleSpriteText;
			Widget widget1 = client.getWidget(11, 1);
			Widget widget2 = client.getWidget(11, 3);
			String itemName1 = "<!--Error-->";
			String itemName2 = "<!--Error-->";
			int itemid1 = -1;
			int itemid2 = -1;
			if (widget1 != null) {
				itemName1 = client.getItemDefinition(widget1.getItemId()).getName();
				itemid1 = widget1.getItemId();
			}
			if (widget2 != null) {
				itemName2 = client.getItemDefinition(widget2.getItemId()).getName();
				itemid2 = widget2.getItemId();
			}
			curParentNode.addChild(new DialogueNode("{{tbox|<!--id: "+ itemid1 + "-->pic=" + itemName1 + " detail.png|<!--id: "+ itemid2 + "-->pic2=" + itemName2 + " detail.png|" + doubleSpriteText + "}}"));
			printTree();
        }

		if (npcDialogueTextWidget == null
			&& playerDialogueTextWidget == null
			&& playerDialogueOptionsWidget == null
			&& spriteTextWidget == null
			&& msgTextWidget == null
			&& doubleSpriteTextWidget == null
			&& lastText != null
		) {

			printTree();
			curParentNode = rootNode;
			lastText = null;

		}
    }

	private void printTree() {
		StringBuilder sb = new StringBuilder();
		rootNode.print(sb, 1);
		String playerName = client.getLocalPlayer().getName();
		panel.setText(sb.toString().replaceAll(playerName, "[player name]"));
	}

	public void reset() {
		rootNode = new DialogueNode("");
		curParentNode = rootNode;
		lastText = null;
		panel.setText("");
	}
}
