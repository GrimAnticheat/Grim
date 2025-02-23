package ac.grim.grimac.platform.api.permission;


public class Permission {

    private String name;
    private PermissionDefault permissionDefault;
    private int opLevel = 0;

    public Permission(String permissionName, PermissionDefault permissionDefault) {
        this.name = permissionName;
        this.permissionDefault = permissionDefault;
    }

    /**
     * Gets the unique name of the permission.
     * @return The permission name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the default state of the permission.
     * @return The default state
     */
    public PermissionDefault getDefault() {
        return this.permissionDefault;
    }

    /**
     * Gets the op level (only relevant if default is OP).
     * @return The op level
     */
    public int getOpLevel() {
        return this.opLevel;
    }

    /**
     * Sets the default state of the permission.
     * @param permissionDefault The new default state
     */
    public void setDefault(PermissionDefault permissionDefault) {
        this.permissionDefault = permissionDefault;
    }

    /**
     * Sets the default state of the permission.
     * @param permissionDefault The new default state
     * @param opLevel The op level (only relevant if default is OP), ignored if new default is not OP
     */
    public void setDefault(PermissionDefault permissionDefault, int opLevel) {
        if (permissionDefault == PermissionDefault.OP) {
            this.opLevel = opLevel;
        } else {
            this.opLevel = 0;
        }
        this.permissionDefault = permissionDefault;
    }
}
