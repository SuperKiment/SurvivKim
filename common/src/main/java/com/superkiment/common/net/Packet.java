package com.superkiment.common.net;

import java.io.Serializable;

/**
 * Marqueur pour tout ce qui transite en TCP. Un packet = un record dans {@link Packets}.
 * Le routage se fait par classe via {@link PacketRouter}, pas besoin d'enum.
 */
public interface Packet extends Serializable {
}
