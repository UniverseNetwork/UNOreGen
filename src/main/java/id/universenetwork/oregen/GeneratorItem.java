package id.universenetwork.oregen;

public class GeneratorItem {
    /**
     * Name of the material
     */
    private String name;

    /**
     * Damage value, only for mc < 1.13
     */
    private Byte damage;

    /**
     * The chance in double. From 0 to 1.
     */
    private double chance;

    public GeneratorItem(String name, byte damage, double chance) {
        this.name = name;
        this.damage = damage;
        this.chance = chance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getDamage() {
        return damage;
    }

    public void setDamage(Byte damage) {
        this.damage = damage;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }
}