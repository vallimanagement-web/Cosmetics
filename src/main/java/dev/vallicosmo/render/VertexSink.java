package dev.vallicosmo.render;

/** Nimmt fertige Vertices entgegen. Die Minecraft-Anbindung steckt in CosmeticFeatureRenderer. */
@FunctionalInterface
public interface VertexSink {
	void vertex(float x, float y, float z, int argb, float nx, float ny, float nz);
}
