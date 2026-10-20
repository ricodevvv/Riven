package dev.ricodevvv.riven.npc;

/**
 * Texture and signature of a Mojang skin.
 */
public class Skin {

    private final String textureData;
    private final String textureSignature;

    public Skin(String textureData, String textureSignature) {
        this.textureData = textureData;
        this.textureSignature = textureSignature;
    }

    public String getTextureData() {
        return this.textureData;
    }

    public String getTextureSignature() {
        return this.textureSignature;
    }

}
