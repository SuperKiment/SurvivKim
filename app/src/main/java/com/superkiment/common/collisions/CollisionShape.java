package com.superkiment.common.collisions;

import com.superkiment.client.graphics.Shape;
import org.joml.Vector2d;

public class CollisionShape extends Shape {
    public final Collisionable parent;

    public CollisionShape(Vector2d pos, Vector2d dim, ShapeType st, Collisionable par) {
        super(pos, dim, st);
        this.parent = par;
    }

    public boolean isInCollisionWith(CollisionShape otherShape) {
        ShapeType thisType = getBaseType(this.shapeType);
        ShapeType otherType = getBaseType(otherShape.shapeType);

        String collisionType = GetCollisionType(this, otherShape);

        switch (collisionType) {
            case "RR" -> {
                return checkRectRect(this, otherShape);
            }
            case "CC" -> {
                return checkCircleCircle(this, otherShape);
            }
            case "RC" -> {
                return checkRectCircle(this, otherShape);
            }
            case "CR" -> {
                return checkRectCircle(otherShape, this);
            }
        }

        return false;
    }

    /**
     * Détection de collision entre deux rectangles avec rotation (OBB)
     */
    private boolean checkRectRect(CollisionShape rect1, CollisionShape rect2) {
        Vector2d dir1 = rect1.parent.getDirection();
        Vector2d dir2 = rect2.parent.getDirection();

        // Utilise SAT (Separating Axis Theorem) pour les rectangles orientés
        Vector2d[] axes = new Vector2d[4];
        axes[0] = new Vector2d(dir1.x, dir1.y);
        axes[1] = new Vector2d(-dir1.y, dir1.x); // Perpendiculaire à dir1
        axes[2] = new Vector2d(dir2.x, dir2.y);
        axes[3] = new Vector2d(-dir2.y, dir2.x); // Perpendiculaire à dir2

        Vector2d[] corners1 = getRotatedRectCorners(rect1);
        Vector2d[] corners2 = getRotatedRectCorners(rect2);

        for (Vector2d axis : axes) {
            axis.normalize();

            double[] proj1 = projectOntoAxis(corners1, axis);
            double[] proj2 = projectOntoAxis(corners2, axis);

            if (proj1[1] < proj2[0] || proj2[1] < proj1[0]) {
                return false; // Séparation trouvée
            }
        }

        return true; // Aucune séparation trouvée = collision
    }

    /**
     * Obtient les 4 coins d'un rectangle avec rotation
     */
    private Vector2d[] getRotatedRectCorners(CollisionShape rect) {
        Vector2d center = rect.getWorldPosition();
        Vector2d dir = rect.parent.getDirection();
        Vector2d perp = new Vector2d(-dir.y, dir.x);

        double halfWidth = rect.dimensions.x / 2;
        double halfHeight = rect.dimensions.y / 2;

        Vector2d[] corners = new Vector2d[4];
        corners[0] = new Vector2d(center).add(new Vector2d(dir).mul(halfWidth)).add(new Vector2d(perp).mul(halfHeight));
        corners[1] = new Vector2d(center).add(new Vector2d(dir).mul(halfWidth)).sub(new Vector2d(perp).mul(halfHeight));
        corners[2] = new Vector2d(center).sub(new Vector2d(dir).mul(halfWidth)).sub(new Vector2d(perp).mul(halfHeight));
        corners[3] = new Vector2d(center).sub(new Vector2d(dir).mul(halfWidth)).add(new Vector2d(perp).mul(halfHeight));

        return corners;
    }

    /**
     * Projette des points sur un axe et retourne [min, max]
     */
    private double[] projectOntoAxis(Vector2d[] points, Vector2d axis) {
        double min = points[0].dot(axis);
        double max = min;

        for (int i = 1; i < points.length; i++) {
            double proj = points[i].dot(axis);
            if (proj < min) min = proj;
            if (proj > max) max = proj;
        }

        return new double[]{min, max};
    }

    /**
     * Détection de collision entre deux cercles
     */
    private boolean checkCircleCircle(CollisionShape circle1, CollisionShape circle2) {
        double dx = circle1.getWorldPosition().x - circle2.getWorldPosition().x;
        double dy = circle1.getWorldPosition().y - circle2.getWorldPosition().y;
        double distanceSquared = dx * dx + dy * dy;

        double radius1 = circle1.dimensions.x;
        double radius2 = circle2.dimensions.x;
        double radiusSum = radius1 + radius2;

        return distanceSquared < radiusSum * radiusSum;
    }

    /**
     * Détection de collision entre un rectangle et un cercle
     */
    private boolean checkRectCircle(CollisionShape rect, CollisionShape circle) {
        Vector2d circlePos = circle.getWorldPosition();
        Vector2d rectCenter = rect.getWorldPosition();
        Vector2d dir = rect.parent.getDirection();
        Vector2d perp = new Vector2d(-dir.y, dir.x);

        // Transforme le cercle dans l'espace local du rectangle
        Vector2d localCircle = new Vector2d(circlePos).sub(rectCenter);
        double localX = localCircle.dot(dir);
        double localY = localCircle.dot(perp);

        // Trouve le point le plus proche
        double halfWidth = rect.dimensions.x / 2;
        double halfHeight = rect.dimensions.y / 2;

        double closestX = Math.max(-halfWidth, Math.min(localX, halfWidth));
        double closestY = Math.max(-halfHeight, Math.min(localY, halfHeight));

        // Calcule la distance
        double dx = localX - closestX;
        double dy = localY - closestY;
        double distanceSquared = dx * dx + dy * dy;

        double radius = circle.dimensions.x;
        return distanceSquared < radius * radius;
    }

    public Vector2d getWorldPosition() {
        return new Vector2d(position.x + parent.getWorldPosition().x, position.y + parent.getWorldPosition().y);
    }

    public static String GetCollisionType(CollisionShape shape1, CollisionShape shape2) {
        String collisionType = "";

        switch (shape1.getBaseType(shape1.shapeType)) {
            case RECT -> collisionType += "R";
            case CIRCLE -> collisionType += "C";
            case TRIANGLE -> collisionType += "T";
        }
        switch (shape2.getBaseType(shape2.shapeType)) {
            case RECT -> collisionType += "R";
            case CIRCLE -> collisionType += "C";
            case TRIANGLE -> collisionType += "T";
        }

        return collisionType;
    }

    /**
     * Calcule le vecteur de pénétration minimal (MTV - Minimum Translation Vector)
     * pour séparer deux rectangles en collision
     */
    public Vector2d calculateRectRectMTV(CollisionShape other) {
        Vector2d dir1 = this.parent.getDirection();
        Vector2d dir2 = other.parent.getDirection();

        Vector2d[] axes = new Vector2d[4];
        axes[0] = new Vector2d(dir1.x, dir1.y);
        axes[1] = new Vector2d(-dir1.y, dir1.x);
        axes[2] = new Vector2d(dir2.x, dir2.y);
        axes[3] = new Vector2d(-dir2.y, dir2.x);

        Vector2d[] corners1 = getRotatedRectCorners(this);
        Vector2d[] corners2 = getRotatedRectCorners(other);

        double minOverlap = Double.MAX_VALUE;
        Vector2d minAxis = null;

        for (Vector2d axis : axes) {
            axis.normalize();

            double[] proj1 = projectOntoAxis(corners1, axis);
            double[] proj2 = projectOntoAxis(corners2, axis);

            double overlap = Math.min(proj1[1], proj2[1]) - Math.max(proj1[0], proj2[0]);

            if (overlap < minOverlap) {
                minOverlap = overlap;
                minAxis = new Vector2d(axis);
            }
        }

        // S'assure que le MTV pointe de other vers this
        Vector2d centerDiff = new Vector2d(this.getWorldPosition()).sub(other.getWorldPosition());
        if (minAxis.dot(centerDiff) < 0) {
            minAxis.mul(-1);
        }

        return minAxis.mul(minOverlap);
    }

    /**
     * Calcule le vecteur de pénétration pour deux cercles
     */
    public Vector2d calculateCircleCircleMTV(CollisionShape other) {
        Vector2d center1 = this.getWorldPosition();
        Vector2d center2 = other.getWorldPosition();

        Vector2d direction = new Vector2d(center1).sub(center2);
        double distance = direction.length();

        double radius1 = this.dimensions.x;
        double radius2 = other.dimensions.x;
        double penetration = (radius1 + radius2) - distance;

        if (distance > 0) {
            direction.normalize();
        } else {
            direction.set(1, 0); // Direction par défaut si les centres sont identiques
        }

        return direction.mul(penetration);
    }
}