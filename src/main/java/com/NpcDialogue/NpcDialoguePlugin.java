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

	private int treeDepth = 1;

    @Override
    public void startUp()
    {
        // Shamelessly copied from NotesPlugin
        panel = injector.getInstance(NpcDialoguePanel.class);
        panel.init();

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
                panel.appendText("<chose " + dialogueOptions[actionParam].getText() + ">");
//                appendText("{{topt|" + dialogueOptions[actionParam].getText() + "}}");
				treeDepth++;
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
            appendText("'''" + npcName + ":''' " + npcText);
        }

        // This should be in WidgetInfo under DialogPlayer, but isn't currently.
        Widget playerDialogueTextWidget = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

        if (playerDialogueTextWidget != null && !playerDialogueTextWidget.getText().equals(lastText)) {
            String playerText = playerDialogueTextWidget.getText();
            lastText = playerText;

            appendText("'''Player:''' " + playerText);
        }

        Widget playerDialogueOptionsWidget = client.getWidget(WidgetID.DIALOG_OPTION_GROUP_ID, 1);
        if (playerDialogueOptionsWidget != null && playerDialogueOptionsWidget.getChildren() != dialogueOptions) {
            dialogueOptions = playerDialogueOptionsWidget.getChildren();
            appendText("{{tselect|" + dialogueOptions[0].getText() + "}}");
            for (int i = 1; i < dialogueOptions.length - 2; i++) {
                appendText("{{topt|" + dialogueOptions[i].getText() + "}}");
            }
        }

        Widget spriteTextWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
        if (spriteTextWidget != null && !spriteTextWidget.getText().equals(lastText)) {
            String spriteText = spriteTextWidget.getText();
            lastText = spriteText;
            Widget spriteWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_SPRITE);
            int id = spriteWidget.getItemId();
            appendText("{{tbox|pic=" + id + " detail.png|" + spriteText + "}}");
        }

        Widget msgTextWidget = client.getWidget(229, 1);
        if (msgTextWidget != null && !msgTextWidget.getText().equals(lastText)) {
            String msgText = msgTextWidget.getText();
            lastText = msgText;
            appendText("{{tbox|" + msgText + "}}");
        }

        Widget doubleSpriteTextWidget = client.getWidget(11, 2);
        if (doubleSpriteTextWidget != null && !doubleSpriteTextWidget.getText().equals(lastText)) {
            String doubleSpriteText = doubleSpriteTextWidget.getText();
            lastText = doubleSpriteText;
            int id1 = client.getWidget(11, 1).getItemId();
            int id2 = client.getWidget(11, 3).getItemId();
            appendText("{{tbox|pic=" + id1 + " detail.png|pic2=" + id2 + " detail.png|" + doubleSpriteText + "}}");
        }

		if (npcDialogueTextWidget == null
			&& playerDialogueTextWidget == null
			&& playerDialogueOptionsWidget == null
			&& spriteTextWidget == null
			&& msgTextWidget == null
			&& doubleSpriteTextWidget == null
			&& treeDepth > 1
		) {
			appendText("{{tact|end}}");
			treeDepth = 1; //Reset depth if no dialog is open
		}
    }

	private void appendText(String text) {
		String indent = "*";
		for(int i = 1; i < treeDepth; i++) {
			indent += "*";
		}
		panel.appendText(indent + " " + text);
	}
}
