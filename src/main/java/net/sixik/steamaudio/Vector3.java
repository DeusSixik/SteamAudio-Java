package net.sixik.steamaudio;

/**
 * Точка или вектор в трехмерном пространстве, соответствующая
 * {@code IPLVector3}.
 * <p>
 * Steam Audio использует правую систему координат: положительная ось x
 * указывает вправо, положительная ось y — вверх, отрицательная ось z —
 * вперед. Координаты позиций и направлений, полученные из игрового движка,
 * должны быть преобразованы перед передачей в любую функцию Steam Audio.
 */
public final class Vector3 {

    /** Координата x. */
    public float x;

    /** Координата y. */
    public float y;

    /** Координата z. */
    public float z;

    /**
     * Создает нулевой вектор.
     */
    public Vector3() {
    }

    /**
     * Создает вектор с заданными координатами.
     *
     * @param x координата x
     * @param y координата y
     * @param z координата z
     */
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Задает координаты вектора.
     *
     * @param x координата x
     * @param y координата y
     * @param z координата z
     * @return этот вектор (для построения цепочек вызовов)
     */
    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Копирует координаты другого вектора в этот.
     *
     * @param other вектор-источник
     * @return этот вектор (для построения цепочек вызовов)
     */
    public Vector3 set(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    /**
     * Записывает координаты вектора в массив без аллокаций (для хот-паса).
     *
     * @param out    массив назначения
     * @param offset индекс в массиве, с которого записываются координаты
     */
    public void get(float[] out, int offset) {
        out[offset] = x;
        out[offset + 1] = y;
        out[offset + 2] = z;
    }

    /**
     * Читает координаты вектора из массива без аллокаций (для хот-паса).
     *
     * @param src    массив-источник
     * @param offset индекс в массиве, с которого читаются координаты
     * @return этот вектор (для построения цепочек вызовов)
     */
    public Vector3 set(float[] src, int offset) {
        this.x = src[offset];
        this.y = src[offset + 1];
        this.z = src[offset + 2];
        return this;
    }

    /**
     * Возвращает строковое представление вектора.
     *
     * @return строка вида {@code (x, y, z)}
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
