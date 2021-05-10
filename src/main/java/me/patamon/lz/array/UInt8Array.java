package me.patamon.lz.array;

/**
 * @author: yuanlin
 * @date: 2021-05-08 18:02:00
 * @description:
 * The Uint8Array typed array represents an array of 8-bit unsigned integers
 */

import java.io.Serializable;
import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

public class UInt8Array extends TypedArray implements Serializable {

    public UInt8Array(ByteBuffer buffer) {
        super(buffer);
    }

    public UInt8Array(int length) {
        this(ByteBuffer.allocateDirect(length));
    }

    public UInt8Array(short... elements) {
        this(ByteBuffer.allocateDirect(elements.length));
        int index = 0;

        for(int i = 0; i < elements.length; ++i) {
            short item = elements[i];
            index++;
            this.set(index, item);
        }
    }

    /**
     * Returns the 8-bit unsigned integer at the given index.
     *
     * @param index The index at which to return the value.
     * *
     * @return The 8-bit unsigned integer at the index.
     */
    public short get(int index) {
        return (short)(0XFF & this.getBuffer().get(index));
    }

    /**
     * Puts a 8-bit unsigned integer at a particular index.
     *
     * @param index The index at which to place the value.
     * *
     * @param value The 8-bit unsigned integer to put into buffer.
     */
    public void put(int index, short value) {
        this.getBuffer().put(index, (byte)(255 & value));
    }

    public void set(int index, short value) {
        this.put(index, value);
    }

    @Override
    public int length() {
        return this.buffer.limit();
    }

    @Override
    public int getType() {
        return TypedArrayValue.UNSIGNED_INT_8_ARRAY;
    }

    @Override
    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int i = 0;

        for(int var3 = this.length(); i < var3; ++i) {
            sb.append(this.get(i));
            if (i != this.length() - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}
