package com.superkiment.common.packets;

import com.superkiment.common.entities.Entity;

import java.util.ArrayList;
import java.util.List;

public class PacketPositionsBulk extends Packet {

    public List<String> ids;
    public List<Double> x;
    public List<Double> y;

    public PacketPositionsBulk(List<Entity> entities) {
        ids = new ArrayList<>();
        x = new ArrayList<>();
        y = new ArrayList<>();

        for (Entity entity : entities) {
            ids.add(entity.id);
            x.add(entity.pos.x);
            y.add(entity.pos.y);
        }
    }

    @Override
    public PacketType getType() {
        return PacketType.BULK_POSITION;
    }

    @Override
    public String toString() {
        return "PacketPositionsBulk{ids=" + ids.size() + ", xs=" + x.size() + ", ys=" + y.size() + "}";
    }
}
