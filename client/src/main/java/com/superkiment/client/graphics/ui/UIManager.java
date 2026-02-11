package com.superkiment.client.graphics.ui;

import com.superkiment.client.Main;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UIManager {
    public UIElement baseElement;

    public UIManager() {
        baseElement = new UIElement(new Vector2d(0, 0), 0);
    }

    public List<UIElement> getUIElementsSortedByZ() {
        List<UIElement> elements = getAllUIElements();

        elements.sort(Comparator.comparingInt(UIElement::getZIndex));

        return elements;
    }

    public List<UIElement> getAllUIElements() {
        List<UIElement> elements = new ArrayList<>();

        baseElement.addYourselfAndChildrenToThisList(elements);

        return elements;
    }

    public void setup(long window) {

        baseElement.addChild(
                new UIButton(new Vector2d(100, 100), new Vector2d(200, 50), 50) {
                    @Override
                    public void onClick() {
                        System.out.println("coucou");

                        Main.connect();
                    }
                }
        );
    }
}
