package com.superkiment.common.net;

import com.superkiment.common.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Canal TCP bidirectionnel de packets, identique côté client et serveur.
 */
public class Connection {
    private final Socket socket;
    private final ObjectOutputStream out;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush(); // envoie l'en-tête pour débloquer l'ObjectInputStream d'en face
    }

    /**
     * Boucle de réception bloquante. Rend la main à la fermeture du socket.
     */
    public void listen(Consumer<Packet> onPacket) {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))) {
            while (!socket.isClosed()) {
                onPacket.accept((Packet) in.readObject());
            }
        } catch (EOFException | java.net.SocketException ignored) {
            // fermeture normale
        } catch (IOException | ClassNotFoundException e) {
            Logger.error("Connexion TCP interrompue", e);
        } finally {
            close();
        }
    }

    public synchronized void send(Packet packet) {
        if (socket.isClosed()) return;
        try {
            out.writeObject(packet);
            out.reset(); // sinon le stream garde une référence à chaque objet envoyé (fuite + états périmés)
            out.flush();
        } catch (IOException e) {
            Logger.error("Échec d'envoi de " + packet.getClass().getSimpleName(), e);
            close();
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public InetAddress getAddress() {
        return socket.getInetAddress();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
