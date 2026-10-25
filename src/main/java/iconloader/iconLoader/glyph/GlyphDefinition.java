package iconloader.iconLoader.glyph;

public class GlyphDefinition {
    private final String name;
    private final String texturePath;
    private final String character;
    private final String permission;
    private final int ascent;
    private final int height;
    private final int customModelData; // New

    public GlyphDefinition(String name, String texturePath, String character, String permission, int ascent, int height, int customModelData) {
        this.name = name;
        this.texturePath = texturePath;
        this.character = character;
        this.permission = permission;
        this.ascent = ascent;
        this.height = height;
        this.customModelData = customModelData;
    }

    public String getName() { return name; }
    public String getTexturePath() { return texturePath; }
    public String getCharacter() { return character; }
    public String getPermission() { return permission; }
    public int getAscent() { return ascent; }
    public int getHeight() { return height; }
    public int getCustomModelData() { return customModelData; } // New
}