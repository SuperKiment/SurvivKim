package com.superkiment.client.graphics.ui;

import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

public class UIElement {
    public Vector2d pos, dim;
    public ShapeModel shapeModel;
    public List<UIElement> children;
    public UIElement parent;

    /**
     * 0 = derrière, 1000 = devant
     */
    public int zIndex = 0;
    public boolean isClickable = false, isHovered = false;

    /**
     *
     * @param pos
     * @param z   doit être positif
     */
    public UIElement(Vector2d pos, int z) {
        this.pos = pos;
        this.dim = new Vector2d(100, 100);
        this.shapeModel = new ShapeModel();
        this.zIndex = z;
        this.children = new ArrayList<>();
    }

    public boolean isClicked(float x, float y) {
        if (!isClickable) return false;

        Vector2d topLeft = new Vector2d(pos.x - dim.x / 2, pos.y - dim.y / 2);
        Vector2d bottomRight = new Vector2d(pos.x + dim.x / 2, pos.y + dim.y / 2);

        return (x > topLeft.x && x < bottomRight.x && y > topLeft.y && y < bottomRight.y);
    }

    public void onClick() {
        System.out.printf("Blank & clickable UIElement on pos %f/%f was clicked !%n", (float) pos.x, (float) pos.y);
    }

    public void addYourselfAndChildrenToThisList(List<UIElement> list) {
        list.add(this);
        for (UIElement child : children) {
            if (child != null)
                child.addYourselfAndChildrenToThisList(list);
        }
    }

    public void addChild(UIElement e) {
        e.setParent(this);
        children.add(e);
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setParent(UIElement p) {
        if (parent == null) parent = p;
    }
}
