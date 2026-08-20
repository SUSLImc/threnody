package com.suslimc.threnody.entity;

/**
 * What Threnody is currently doing. Most of the horror lives in {@link #LURKING}, where it
 * refuses to move while it is being watched and only closes distance behind your back.
 */
public enum ThrenodyState {
    LURKING(0),
    HUNTING(1),
    VANISHING(2);

    private final int id;

    ThrenodyState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ThrenodyState fromId(int id) {
        for (ThrenodyState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return LURKING;
    }
}
