package com.superkiment.client.graphics.ui.shapemodels;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.shapes.ShapeModel;

import java.util.ArrayList;
import java.util.List;

abstract public class ShapeModel_UI extends ShapeModel {
    public static List<Class<?>> applyToClass;
    public static List<Class<?>> doNotApplyToClass;

    public static List<Class<?>> uiShapeModels = new ArrayList<>();

    static {
        uiShapeModels.add(SMHealthBar.class);
    }

    public static void AddDynamicUIElementsToEntity(Entity entity) {
        for (Class<?> shapeModelClass : uiShapeModels) {
            try {
                List<Class<?>> applyTo = (List<Class<?>>) shapeModelClass.getField("applyToClass").get(null);
                List<Class<?>> doNotApplyTo = (List<Class<?>>) shapeModelClass.getField("doNotApplyToClass").get(null);

                boolean isApplied = false;
                for (Class<?> appliedClass : applyTo) {
                    if (appliedClass.isInstance(entity)) {
                        isApplied = true;
                        break;
                    }
                }
                for (Class<?> exceptionClass : doNotApplyTo) {
                    if (exceptionClass.isInstance(entity)) {
                        isApplied = false;
                        break;
                    }
                }

                if (isApplied) {
                    ShapeModel_UI instance = (ShapeModel_UI) shapeModelClass.getDeclaredConstructor().newInstance();
                    entity.uiShapeModels.add(instance);
                }

            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}
