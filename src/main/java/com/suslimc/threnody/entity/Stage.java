package com.suslimc.threnody.entity;

public enum Stage {
    STALKER(0),
    CRAWLER(1),
    SQUEEZE(2),
    BREAKER(3),
    HUNTMASTER(4),
    ESCHATON(5);

    private final int id;

    Stage(int id) { this.id = id; }
    public int getId() { return id; }

    public static Stage fromId(int id) {
        for (Stage stage : values()) {
            if (stage.id == id) {
                return stage;
            }
        }
        return STALKER;
    }
}
