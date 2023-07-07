package id.universenetwork.oregen.hooks;

/**
 * Stores all existing hooks for sky block plugins
 *
 * @author MasterCake
 */
public enum HookInfo {
    BentoBox(HookBentoBox.class), Vanilla(HookVanilla.class);

    private Class<?> hookClass;

    HookInfo(Class<?> hookClass) {
        this.hookClass = hookClass;
    }

    public Class<?> getHookClass() {
        return this.hookClass;
    }
}
