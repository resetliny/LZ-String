package me.patamon.lz.array;

/**
 * @author: yuanlin
 * @date: 2021-05-08 18:00:13
 * @description:
 * abstract typed array
 *
 * refer to https://github.com/eclipsesource/J2V8/blob/master/src/main/java/com/eclipsesource/v8/utils/typedarrays/TypedArray.java
 */
import java.nio.ByteBuffer;

import static me.patamon.lz.array.TypedArrayValue.*;

public abstract class TypedArray {

    protected ByteBuffer buffer;

    protected TypedArray(ByteBuffer buffer) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer");
        }
        if ((buffer.limit() % getStructureSize(this.getType())) != 0) {
            throw new  IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer");
        }
        this.buffer = buffer;
    }

    protected ByteBuffer getBuffer() {
        return this.buffer;
    }

    /**
     * Return the size of this view. The size of the view is determined by the size
     * of the buffer, and the size of the data projected onto it. For example, for a
     * buffer size of 8, and a view representing 16bit integers, the size would be 4.
     *
     * @return The size of this view
     */
    public abstract int length();

    /**
     * Returns the 'Type' of this TypedArray using one of the constants defined in V8Value.
     *
     * @return The 'Type' of this typed array.
     */
    public abstract int getType();

    /**
     * Computes the size of the structures required for each TypedArray variation.
     *
     * @param type The type of the TypeArray
     * @return The size of the structures required
     */
    private int getStructureSize(int type) {
        switch (type){
            case FLOAT_64_ARRAY:
                return 8;
            case INT_32_ARRAY:
            case UNSIGNED_INT_32_ARRAY:
            case FLOAT_32_ARRAY:
                return 4;
            case UNSIGNED_INT_16_ARRAY:
            case INT_16_ARRAY:
                return 2;
            case INT_8_ARRAY:
            case UNSIGNED_INT_8_ARRAY:
            case UNSIGNED_INT_8_CLAMPED_ARRAY:
                return 1;
            default:
                throw new IllegalArgumentException("Cannot create a typed array of the type");

        }
    }

    @Override
    public String toString() {
        return this.buffer.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if(other.getClass() != this.getClass()){
            return false;
        }
        TypedArray typedArray = (TypedArray) other;
        if(buffer != typedArray.buffer){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.buffer.hashCode();
    }
}

