package com.superkiment.common.shapes;

import com.superkiment.common.collisions.Collisionable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Un objet permettant une collection de Shapes et de toutes les render d'un coup, permettant de les bind à une direction.
 */
public class ShapeModel {
    public List<Shape> shapes;

    public ShapeModel() {
        shapes = new CopyOnWriteArrayList<>();
    }

    public void addShape(Shape s) {
        shapes.add(s);
    }

    public void addShapes(List<Shape> newShapes) {
        shapes.addAll(newShapes);
    }

    public void update(Collisionable collisionable) {
    }
}
