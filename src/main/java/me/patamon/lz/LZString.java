package me.patamon.lz;

/**
 * @author: yuanlin
 * @date: 2021-05-08 17:57:32
 * @description:
 * LZ-based compression algorithm
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Function;

import me.patamon.lz.array.UInt8Array;

public class LZString {
    public LZString(){}
    private final static String keyStrBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    private final static String keyStrUriSafe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$";
    private HashMap<String,HashMap<Character, Integer>> baseReverseDic = new HashMap<>();

    private char getBaseValue (String alphabet, char character) {
        HashMap<Character, Integer> map = baseReverseDic.get(alphabet);
        if(map == null){
            map = new HashMap<>();
            baseReverseDic.put(alphabet,map);
            char[] chars = alphabet.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                map.put(chars[i],i);
            }
        }
        //会有空指针异常
        Integer integer = map.get(character);
        return (char) integer.intValue();
    }

    public String compressToBase64(String input) {
        if (input != null) {
            String res = compress(input, 6, a->keyStrBase64.charAt(a));
            switch(res.length() % 4) {
                case 0:
                    return res;
                case 1:
                    return res + "===";
                case 2:
                    return res + "==";
                case 3:
                    return res + "=";
                default:
                    return res;
            }
        } else {
            return "";
        }
    }

    public String decompressFromBase64(String input) {
        if (input == null) {
            return "";
        }
        if(input == "") {
            return null;
        }
        return decompress(input.length(),32, index ->getBaseValue(keyStrBase64, input.charAt(index)));
    }

    public String compressToUTF16(String input) {
        if(input == null){
            return "";
        }
        return compress(input, 15, a->(char)(a+32))+ " ";
    }

    public String decompressFromUTF16(String compressed) {
        if (compressed == null) {
            return "";
        }
        if(compressed == "") {
            return null;
        }
        return decompress(compressed.length(), 16384, index -> (char)((int)compressed.charAt(index)-32));
    }

    public UInt8Array compressToUint8Array(String input) {
        String compressed = compress(input);
        UInt8Array buf = new UInt8Array(compressed.length() * 2);

        char[] chars = compressed.toCharArray();
        for (int index = 0; index < chars.length; index++) {
            buf.set(index * 2, (short)(chars[index] >>> 8));
            buf.set(index * 2 + 1, (short)(chars[index] % 256));
        }

        return buf;
    }

    public String decompressFromUint8Array(UInt8Array buffer) {
        if (buffer == null) {
            return "";
        }
        if (buffer.length() == 0) {
            return null;
        }
        int totalLen = buffer.length() / 2;
        char[] result = new char[totalLen];

        for(int index = 0; index < totalLen; ++index) {
            result[index] = (char)(buffer.get(index * 2) * 256 + buffer.get(index * 2 + 1));
        }
        return decompress(String.valueOf(result));
    }

//    public String compressToEncodedURIComponent(String input) {
//        return input != null ? INSTANCE._compress(input, 6, (Function1)null.INSTANCE) + "" : "";
//    }
//
//    public String decompressFromEncodedURIComponent(String input) {
//        if (input == null) {
//            return "";
//        } else if (Intrinsics.areEqual(input, "")) {
//            return null;
//        } else {
//            final String reInput = StringsKt.replace$default(input, ' ', '+', false, 4, (Object)null);
//            return INSTANCE._decompress(reInput.length(), 32, (Function1)(new Function1() {
//                // $FF: synthetic method
//                // $FF: bridge method
//                public Object invoke(Object var1) {
//                    return this.invoke(((Number)var1).intValue());
//                }
//
//                public final char invoke(int index) {
//                    return LZString.INSTANCE.getBaseValue(LZString.access$getKeyStrUriSafe$p(LZString.INSTANCE), reInput.charAt(index));
//                }
//            }));
//        }
//    }

    public String compress(String uncompressed) {
        return compress(uncompressed, 16, a->(char)a.intValue());
    }

    private String compress(String uncompressed, int bitsPerChar, Function<Integer,Character> getCharFromInt) {
        if (uncompressed == null) {
            return "";
        }

        HashMap<String,Integer> context_dictionary = new HashMap();
        HashSet<String> context_dictionaryToCreate = new HashSet();
        String context_c = null;
        String context_wc = null;
        String context_w = "";
        // Compensate for the first entry which should not count
        double context_enlargeIn = 2.0D;
        int context_dictSize = 3;
        int context_numBits = 2;
        ArrayList<Character> context_data = new ArrayList(uncompressed.length() / 3);
        int context_data_val = 0;
        int context_data_position = 0;

        int i = 0;
        int value;

        for(int ii = 0; ii < uncompressed.length(); ++ii) {
            context_c = String.valueOf(uncompressed.charAt(ii));
            if (!context_dictionary.containsKey(context_c)) {
                context_dictionary.put(context_c, context_dictSize++);
                context_dictionaryToCreate.add(context_c);
            }

            context_wc = context_w + context_c;
            if (context_dictionary.containsKey(context_wc)) {
                context_w = context_wc;
            } else {
                if (!context_dictionaryToCreate.contains(context_w)) {
                    Integer integer = context_dictionary.get(context_w);
                    if (integer == null) {
                        throw new NullPointerException();
                    }

                    value = integer.intValue();

                    for(i = 0; i < context_numBits; ++i) {
                        context_data_val = context_data_val << 1 | value & 1;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.add(getCharFromInt.apply(context_data_val));
                            context_data_val = 0;
                        } else {
                            ++context_data_position;
                        }

                        value >>= 1;
                    }
                } else {
                    if (context_w.charAt(0) < 256) {
                        for(i = 0; i < context_numBits; ++i) {
                            context_data_val <<= 1;
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.add(getCharFromInt.apply(context_data_val));
                                context_data_val = 0;
                            } else {
                                ++context_data_position;
                            }
                        }

                        value = context_w.charAt(0);

                        for(i = 0; i < 8; ++i) {
                            context_data_val = context_data_val << 1 | value & 1;
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.add(getCharFromInt.apply(context_data_val));
                                context_data_val = 0;
                            } else {
                                ++context_data_position;
                            }
                            value >>= 1;
                        }
                    } else {
                        value = 1;

                        for(i = 0; i < context_numBits; ++i) {
                            context_data_val = context_data_val << 1 | value;
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.add(getCharFromInt.apply(context_data_val));
                                context_data_val = 0;
                            } else {
                                ++context_data_position;
                            }

                            value = 0;
                        }

                        value = context_w.charAt(0);

                        for(i = 0; i < 16; ++i) {
                            context_data_val = context_data_val << 1 | value & 1;
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.add(getCharFromInt.apply(context_data_val));
                                context_data_val = 0;
                            } else {
                                ++context_data_position;
                            }

                            value >>= 1;
                        }
                    }

                    context_enlargeIn += -1.0D;
                    if (context_enlargeIn == 0.0D) {
                        context_enlargeIn = Math.pow(2.0D, (double)context_numBits);
                        ++context_numBits;
                    }

                    context_dictionaryToCreate.remove(context_w);
                }

                context_enlargeIn += -1.0D;
                if (context_enlargeIn == 0.0D) {
                    context_enlargeIn = Math.pow(2.0D, (double)context_numBits);
                    ++context_numBits;
                }

                context_dictionary.put(context_wc, context_dictSize++);
                context_w = context_c;
            }
        }

        // Output the code for w.
        if (context_w.length() != 0) {
            if (!context_dictionaryToCreate.contains(context_w)) {
                Integer integer = context_dictionary.get(context_w);
                if (integer == null) {
                    throw new NullPointerException();
                }

                value = integer.intValue();

                for(i = 0; i < context_numBits; ++i) {
                    context_data_val = context_data_val << 1 | value & 1;
                    if (context_data_position == bitsPerChar - 1) {
                        context_data_position = 0;
                        context_data.add(getCharFromInt.apply(context_data_val));
                        context_data_val = 0;
                    } else {
                        ++context_data_position;
                    }

                    value >>= 1;
                }
            } else {
                if (context_w.charAt(0) < 256) {
                    for(i = 0; i < context_numBits; ++i) {
                        context_data_val <<= 1;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.add(getCharFromInt.apply(context_data_val));
                            context_data_val = 0;
                        } else {
                            ++context_data_position;
                        }
                    }

                    value = context_w.charAt(0);

                    for(i = 0; i < 8; ++i) {
                        context_data_val = context_data_val << 1 | value & 1;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.add(getCharFromInt.apply(context_data_val));
                            context_data_val = 0;
                        } else {
                            ++context_data_position;
                        }

                        value >>= 1;
                    }
                } else {
                    value = 1;

                    for(i = 0; i < context_numBits; ++i) {
                        context_data_val = context_data_val << 1 | value;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.add(getCharFromInt.apply(context_data_val));
                            context_data_val = 0;
                        } else {
                            ++context_data_position;
                        }

                        value = 0;
                    }

                    value = context_w.charAt(0);

                    for(i = 0; i < 16; ++i) {
                        context_data_val = context_data_val << 1 | value & 1;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.add(getCharFromInt.apply(context_data_val));
                            context_data_val = 0;
                        } else {
                            ++context_data_position;
                        }

                        value >>= 1;
                    }
                }

                context_enlargeIn += -1.0D;
                if (context_enlargeIn == 0.0D) {
                    context_enlargeIn = Math.pow(2.0D, (double)context_numBits);
                    ++context_numBits;
                }

                context_dictionaryToCreate.remove(context_w);
            }

            context_enlargeIn += -1.0D;
            if (context_enlargeIn == 0.0D) {
                context_enlargeIn = Math.pow(2.0D, (double)context_numBits);
                ++context_numBits;
            }
        }

        // Mark the end of the stream
        value = 2;

        for(i = 0; i < context_numBits; ++i) {
            context_data_val = context_data_val << 1 | value & 1;
            if (context_data_position == bitsPerChar - 1) {
                context_data_position = 0;
                context_data.add(getCharFromInt.apply(context_data_val));
                context_data_val = 0;
            } else {
                ++context_data_position;
            }

            value >>= 1;
        }

        // Flush the last char
        while(true) {
            context_data_val <<= 1;
            if (context_data_position == bitsPerChar - 1) {
                context_data.add(getCharFromInt.apply(context_data_val));
                return String.valueOf(context_data.toArray());
            }

            ++context_data_position;
        }
    }

    public String decompress(String compressed) {
        if (compressed == null) {
            return "";
        }
        if (compressed == "") {
            return null;
        }
        return decompress(compressed.length(),32768,index->compressed.charAt(index));
    }

    private String decompress(int length, int resetValue, Function<Integer,Character> getNextValue) {
        ArrayList<String> dictionary = new ArrayList();
        double enlargeIn = 4.0D;
        int dictSize = 4;
        int numBits = 3;
        String entry = null;
        ArrayList<String> result = new ArrayList();
        String w = null;
        String c = null;

        //方法内类
        class DecData {
            private char value = ' ';
            private int position = 0;
            private int index = 0;

            public DecData(char value, int position, int index) {
                this.value = value;
                this.position = position;
                this.index = index;
            }

            public char getValue() {
                return value;
            }

            public void setValue(char value) {
                this.value = value;
            }

            public int getPosition() {
                return position;
            }

            public void setPosition(int position) {
                this.position = position;
            }

            public int getIndex() {
                return index;
            }

            public void setIndex(int index) {
                this.index = index;
            }
        }

        DecData data = new DecData(getNextValue.apply(0), resetValue, 1);

        for(int i = 0; i < 3; ++i) {
            dictionary.add(i, String.valueOf((char)i));
        }

        int bits = 0;
        int maxpower = (int)Math.pow(2.0D, 2.0D);

        int cc;
        int resb;
        int power;
        for(power = 1; power != maxpower; power <<= 1) {
            resb = data.getValue() & data.getPosition();
            data.setPosition(data.getPosition() >> 1);
            if (data.getPosition() == 0) {
                data.setPosition(resetValue);
                data.setIndex((cc = data.getIndex()) + 1);
                data.setValue(getNextValue.apply(cc));
            }

            bits |= (resb > 0 ? 1 : 0) * power;
        }

        switch(bits) {
            case 0:
                bits = 0;
                maxpower = (int)Math.pow(2.0D, 8.0D);

                for(power = 1; power != maxpower; power <<= 1) {
                    resb = data.getValue() & data.getPosition();
                    data.setPosition(data.getPosition() >> 1);
                    if (data.getPosition() == 0) {
                        data.setPosition(resetValue);
                        data.setIndex((cc = data.getIndex()) + 1);
                        data.setValue((getNextValue.apply(cc)));
                    }

                    bits |= (resb > 0 ? 1 : 0) * power;
                }

                c = String.valueOf((char)bits);
                break;
            case 1:
                bits = 0;
                maxpower = (int)Math.pow(2.0D, 16.0D);

                for(power = 1; power != maxpower; power <<= 1) {
                    resb = data.getValue() & data.getPosition();
                    data.setPosition(data.getPosition() >> 1);
                    if (data.getPosition() == 0) {
                        data.setPosition(resetValue);
                        data.setIndex((cc = data.getIndex()) + 1);
                        data.setValue(getNextValue.apply(cc));
                    }

                    bits |= (resb > 0 ? 1 : 0) * power;
                }

                c = String.valueOf((char)bits);
                break;
            case 2:
                return "";
            default:
                return "";
        }

        dictionary.add(3, c);
        w = c;
        result.add(c);

        while(data.getIndex() <= length) {
            bits = 0;
            maxpower = (int)Math.pow(2.0D, (double)numBits);

            for(power = 1; power != maxpower; power <<= 1) {
                resb = data.getValue() & data.getPosition();
                data.setPosition(data.getPosition() >> 1);
                if (data.getPosition() == 0) {
                    data.setPosition(resetValue);
                    data.setIndex((cc = data.getIndex()) + 1);
                    data.setValue(getNextValue.apply(cc));
                }

                bits |= (resb > 0 ? 1 : 0) * power;
            }

            cc = bits;
            int var28;
            switch(bits) {
                case 0:
                    bits = 0;
                    maxpower = (int)Math.pow(2.0D, 8.0D);

                    for(power = 1; power != maxpower; power <<= 1) {
                        resb = data.getValue() & data.getPosition();
                        data.setPosition(data.getPosition() >> 1);
                        if (data.getPosition() == 0) {
                            data.setPosition(resetValue);
                            data.setIndex((var28 = data.getIndex()) + 1);
                            data.setValue(getNextValue.apply(var28));
                        }

                        bits |= (resb > 0 ? 1 : 0) * power;
                    }

                    dictionary.add(dictSize++, String.valueOf((char)bits));
                    cc = dictSize - 1;
                    enlargeIn += -1.0D;
                    break;
                case 1:
                    bits = 0;
                    maxpower = (int)Math.pow(2.0D, 16.0D);

                    for(power = 1; power != maxpower; power <<= 1) {
                        resb = data.getValue() & data.getPosition();
                        data.setPosition(data.getPosition() >> 1);
                        if (data.getPosition() == 0) {
                            data.setPosition(resetValue);
                            data.setIndex((var28 = data.getIndex()) + 1);
                            data.setValue(getNextValue.apply(var28));
                        }

                        bits |= (resb > 0 ? 1 : 0) * power;
                    }

                    dictionary.add(dictSize++, String.valueOf((char)bits));
                    cc = dictSize - 1;
                    enlargeIn += -1.0D;
                    break;
                case 2:
                    StringBuffer sb = new StringBuffer(result.size());
                    Iterator var23 = result.iterator();

                    while(var23.hasNext()) {
                        String s = (String)var23.next();
                        sb.append(s);
                    }

                    return sb.toString();
            }

            if (enlargeIn == 0.0D) {
                enlargeIn = Math.pow(2.0D, (double)numBits);
                ++numBits;
            }

            if (cc < dictionary.size() && dictionary.get(cc) != null && !dictionary.get(cc).isEmpty()) {
                entry = dictionary.get(cc);
            }else {
                if (cc == dictSize) {
                    entry = w + w.charAt(0);
                }else {
                    return null;
                }
            }

            result.add(entry);
            // Add w+entry[0] to the dictionary.
            dictionary.add(dictSize++, w + entry.charAt(0));
            enlargeIn += -1.0D;
            w = entry;
            if (enlargeIn == 0.0D) {
                enlargeIn = Math.pow(2.0D, (double)numBits);
                ++numBits;
            }
        }

        return "";
    }
}

