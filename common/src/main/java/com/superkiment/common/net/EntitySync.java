package com.superkiment.common.net;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.shapes.Shape;
import org.joml.Vector2d;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sérialisation générique des entités : lit / écrit les champs {@link Sync} par réflexion.
 * Remplace les PacketCreateEntityXxx + EntityFactory : une nouvelle entité n'a besoin que d'un constructeur sans argument.
 */
public final class EntitySync {
    /**
     * Clé réservée : textes des shapes (affichage), synchronisés comme un champ.
     */
    static final String SHAPE_TEXTS = "$shapeTexts";

    private static final ClassValue<List<Field>> SYNC_FIELDS = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!f.isAnnotationPresent(Sync.class) || Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    fields.add(f);
                }
            }
            return fields;
        }
    };

    private EntitySync() {
    }

    public static Map<String, Object> snapshot(Entity entity) {
        Map<String, Object> state = new HashMap<>();
        try {
            for (Field f : SYNC_FIELDS.get(entity.getClass())) {
                state.put(f.getName(), copy(f.get(entity)));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        state.put(SHAPE_TEXTS, entity.shapeModel.shapes.stream().map(s -> s.text).toArray(String[]::new));
        return state;
    }

    public static void apply(Entity entity, Map<String, Object> state) {
        try {
            for (Field f : SYNC_FIELDS.get(entity.getClass())) {
                if (state.containsKey(f.getName())) f.set(entity, copy(state.get(f.getName())));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (state.get(SHAPE_TEXTS) instanceof String[] texts) {
            List<Shape> shapes = entity.shapeModel.shapes;
            for (int i = 0; i < Math.min(texts.length, shapes.size()); i++) shapes.get(i).text = texts[i];
        }
    }

    public static Packets.SpawnEntity spawnPacket(Entity e) {
        return new Packets.SpawnEntity(e.getClass().getName(), e.id,
                e.pos.x, e.pos.y, e.dirLookTarget.x, e.dirLookTarget.y, snapshot(e));
    }

    /**
     * Instancie l'entité décrite par le packet (constructeur sans argument, éventuellement privé).
     */
    public static Entity create(Packets.SpawnEntity p) {
        Entity e;
        try {
            var ctor = Class.forName(p.type()).asSubclass(Entity.class).getDeclaredConstructor();
            ctor.setAccessible(true);
            e = ctor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Entité non instanciable (constructeur sans argument requis) : " + p.type(), ex);
        }
        e.id = p.id();
        e.pos.set(p.x(), p.y());
        e.posLerp.set(e.pos);
        e.dirLookTarget.set(p.dirX(), p.dirY());
        e.dirLookLerp.set(e.dirLookTarget);
        apply(e, p.state());
        return e;
    }

    private static Object copy(Object v) {
        if (v instanceof Vector2d vec) return new Vector2d(vec);
        if (v instanceof Object[] arr) return arr.clone();
        return v;
    }

    /**
     * Mémorise le dernier état connu de chaque entité pour ne propager que ce qui a changé.
     */
    public static final class Tracker {
        private final Map<String, Map<String, Object>> baselines = new ConcurrentHashMap<>();

        /**
         * @return les champs modifiés depuis le dernier appel, ou null si rien (ou première observation).
         */
        public Map<String, Object> diff(Entity e) {
            Map<String, Object> now = snapshot(e);
            Map<String, Object> before = baselines.put(e.id, now);
            if (before == null) return null;

            Map<String, Object> changes = new HashMap<>();
            now.forEach((k, v) -> {
                if (!Objects.deepEquals(v, before.get(k))) changes.put(k, v);
            });
            return changes.isEmpty() ? null : changes;
        }

        /**
         * À appeler après avoir appliqué un état reçu, pour ne pas le renvoyer en écho.
         */
        public void rebase(Entity e) {
            baselines.put(e.id, snapshot(e));
        }

        public void forget(String id) {
            baselines.remove(id);
        }
    }
}
