package me.patamon.lz;

import me.patamon.lz.array.UInt8Array;

/**
 * @author: yuanlin
 * @date: 2021-05-09 09:33:24
 * @description:
 */

public class Test {
    public static void main(String[] args) {
        LZString lzString = new LZString();
        String s = "Hello world!";
        System.out.println(s);
        UInt8Array uInt8Array = lzString.compressToUint8Array(s);
        String s1 = lzString.decompressFromUint8Array(uInt8Array);
        System.out.println(s1);
    }
}
