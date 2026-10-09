package com.superkiment.common.net;

import com.superkiment.common.Logger;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

/**
 * Associe une classe de packet à son handler.
 * <p>
 * Les threads réseau appellent {@link #post}, la boucle de jeu appelle {@link #drain} :
 * tous les handlers s'exécutent donc sur le thread de jeu, sans concurrence sur l'état du monde.
 *
 * @param <C> contexte passé aux handlers (ex : la connexion d'origine côté serveur)
 */
public class PacketRouter<C> {
    private final Map<Class<?>, BiConsumer<Packet, C>> handlers = new ConcurrentHashMap<>();
    private final Queue<Runnable> inbox = new ConcurrentLinkedQueue<>();

    public <P extends Packet> PacketRouter<C> on(Class<P> type, BiConsumer<P, C> handler) {
        handlers.put(type, (p, ctx) -> handler.accept(type.cast(p), ctx));
        return this;
    }

    public void dispatch(Packet packet, C ctx) {
        BiConsumer<Packet, C> handler = handlers.get(packet.getClass());
        if (handler == null) {
            Logger.warn("Packet sans handler : " + packet.getClass().getSimpleName());
            return;
        }
        handler.accept(packet, ctx);
    }

    public void post(Packet packet, C ctx) {
        inbox.add(() -> dispatch(packet, ctx));
    }

    public void post(Runnable task) {
        inbox.add(task);
    }

    public void drain() {
        Runnable task;
        while ((task = inbox.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException e) {
                Logger.error("Erreur pendant le traitement d'un packet", e);
            }
        }
    }
}
