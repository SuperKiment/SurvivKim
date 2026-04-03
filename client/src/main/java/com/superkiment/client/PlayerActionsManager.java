package com.superkiment.client;

public class PlayerActionsManager {
    private static PlayerActionsManager instance = null;

    public boolean isBlockGround = false;

    private PlayerActionsManager() {}

    public static PlayerActionsManager getInstance() {
        if (instance == null) instance = new PlayerActionsManager();
        return instance;
    }

    public boolean toggleGroundWallBlock() {
        isBlockGround = !isBlockGround;
        return  isBlockGround;
    }
}
