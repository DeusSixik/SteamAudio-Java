package net.sixik.steamaudio.core;

/**
 * A point or vector in three-dimensional space corresponding to
 * {@code IPLVector3}.
 * <p>
 * Steam Audio uses a right-handed coordinate system: the positive x axis
 * points right, the positive y axis points up, and the negative z axis
 * points forward. Position and direction coordinates obtained from the game
 * engine must be converted before being passed to any Steam Audio function.
 */
public final class Vector3 {

    /** X coordinate. */
    public float x;

    /** Y coordinate. */
    public float y;

    /** Z coordinate. */
    public float z;

    /**
     * Creates a zero vector.
     */
    public Vector3() {
    }

    /**
     * Creates a vector with the given coordinates.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     */
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Sets the vector coordinates.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return this vector (for call chaining)
     */
    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Copies the coordinates of another vector into this one.
     *
     * @param other source vector
     * @return this vector (for call chaining)
     */
    public Vector3 set(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    /**
     * Writes the vector coordinates into an array without allocation
     * (for the hot path).
     *
     * @param out    destination array
     * @param offset index in the array at which the coordinates are written
     */
    public void get(float[] out, int offset) {
        out[offset] = x;
        out[offset + 1] = y;
        out[offset + 2] = z;
    }

    /**
     * Reads the vector coordinates from an array without allocation
     * (for the hot path).
     *
     * @param src    source array
     * @param offset index in the array from which the coordinates are read
     * @return this vector (for call chaining)
     */
    public Vector3 set(float[] src, int offset) {
        this.x = src[offset];
        this.y = src[offset + 1];
        this.z = src[offset + 2];
        return this;
    }

    /**
     * Returns the string representation of the vector.
     *
     * @return string of the form {@code (x, y, z)}
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
