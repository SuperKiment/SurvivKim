package com.superkiment.client.graphics.ui;

import java.util.ArrayList;
import java.util.List;

public class UIGroup {
    private final List<UIElement> elements;
    private boolean active = true;

    UIGroup() {
        this.elements = new ArrayList<>();
    }

    public void addElement(UIElement element) {
        this.elements.add(element);
    }

    public void enable() {
        this.active = true;
    }

    public void disable() {
        this.active = false;
    }

    public boolean isEnabled() {
        return active;
    }

    public List<UIElement> getElements() {
        return elements;
    }

    public void removeElement(UIElement element) {
        elements.remove(element);
    }
}
