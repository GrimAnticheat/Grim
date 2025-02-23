package ac.grim.grimac.platform.api.permission;



public enum PermissionDefault {
    TRUE,
    FALSE,
    OP(4);

    final int opLevel;

    private PermissionDefault() {
        this.opLevel = 0;
    }

    private PermissionDefault(int opLevel) {
        this.opLevel = opLevel;
    }
}
