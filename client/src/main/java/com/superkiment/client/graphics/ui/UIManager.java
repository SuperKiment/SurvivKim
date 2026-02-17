package com.superkiment.client.graphics.ui;

import com.superkiment.client.Main;
import com.superkiment.client.graphics.Renderer;
import org.joml.Vector2d;

import java.util.*;

public class UIManager {
    public UIElement baseElement;
    public Map<String, UIGroup> uiGroups;

    public UIManager() {
        baseElement = new UIElement(new Vector2d(0, 0), 0);
        uiGroups = new HashMap<>();
    }

    public List<UIElement> getUIElementsSortedByZ() {
        List<UIElement> elements = getEnabledElements();

        elements.sort(Comparator.comparingInt(UIElement::getZIndex));

        return elements;
    }

    public List<UIElement> getAllUIElements() {
        List<UIElement> elements = new ArrayList<>();

        baseElement.addYourselfAndChildrenToThisList(elements);

        return elements;
    }

    public List<UIElement> getEnabledElements() {
        List<UIElement> enabledElements = new ArrayList<>();

        for (UIGroup uiGroup : uiGroups.values()) {
            if (!uiGroup.isEnabled()) continue;

            enabledElements.addAll(uiGroup.getElements());
        }

        return enabledElements;
    }

    public void setup(long window) {
        Vector2d winSize = Renderer.GetCurrentWindowSize(window);

        uiGroups.put("title-menu", new UIGroup());
        uiGroups.put("options-menu", new UIGroup());
        uiGroups.put("credits-menu", new UIGroup());
        uiGroups.put("connect-menu", new UIGroup());

        UIButton connectButton = new UIButton(new Vector2d(winSize.x / 2, winSize.y / 2), new Vector2d(400, 100), 50) {
            @Override
            public void onClick() {
                Main.connect();
                uiGroups.get("title-menu").disable();
                uiGroups.get("credits-menu").disable();
                uiGroups.get("options-menu").disable();
                uiGroups.get("connect-menu").disable();
            }
        };
        connectButton.shapeModel.shapes.getFirst().text = "Connect local";

        UIButton playButton = new UIButton(new Vector2d(winSize.x / 2, winSize.y / 2), new Vector2d(400, 100), 50) {
            @Override
            public void onClick() {
                uiGroups.get("title-menu").disable();
                uiGroups.get("credits-menu").disable();
                uiGroups.get("options-menu").disable();
                uiGroups.get("connect-menu").enable();
            }
        };
        playButton.shapeModel.shapes.getFirst().text = "Play";

        uiGroups.get("connect-menu").addElement(connectButton);
        uiGroups.get("title-menu").addElement(playButton);

        baseElement.addChild(connectButton);
        baseElement.addChild(playButton);

        uiGroups.get("title-menu").enable();
        uiGroups.get("credits-menu").disable();
        uiGroups.get("options-menu").disable();
        uiGroups.get("connect-menu").disable();
    }
}
