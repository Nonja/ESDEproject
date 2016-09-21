package fi.oulu.tol.esde08.ohapclient08;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by Jonna on 22.5.2015.
 */
public class IncomingMessage {

    /**
     * The internal buffer. It is reserved in the readExactly() method.
     */
    private byte[] buffer;

    /**
     * The position where the next byte should be taken from.
     */
    private int position;

    /**
     * Character set used to convert strings.
     */
    private final Charset charset = Charset.forName("UTF-8");

    /**
     * Reads the specified amount of bytes from the given InputStream.
     *
     * @param inputStream the InputStream from which the bytes are read
     * @param length the amount of bytes to be read
     * @return the byte array of which length is the given length
     * @throws IOException when the actual read throws an exception
     */
    private static byte[] readExactly(InputStream inputStream, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (length > 0) {
            int got = inputStream.read(bytes, offset, length);
            if (got == -1)
                throw new EOFException("End of message input.");
            offset += got;
            length -= got;
        }
        return bytes;
    }

    public void readFrom(InputStream inputStream) throws IOException {
        buffer = readExactly(inputStream, 2);
        position = 0;
        int size = integer16();
        buffer = readExactly(inputStream, size);
        position = 0;
    }

    public boolean binary8() {
        return integer8() != 0;
    }

    public int integer8() {
        if (position + 1 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        int value = buffer[position];
        position += 1;

        return value;

    }

    public int integer16() {
        if (position + 2 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        int value = (buffer[position] & 0xff) << 8 |
                (buffer[position + 1] & 0xff);
        position += 2;

        return value;
    }

    public int integer32() {
        if (position + 4 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        int a = integer16();
        int b = integer16();

        return (a << 16) | b;

    }

    public double decimal64() {
        if (position + 8 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        long a = integer32();
        long b = integer32();

        long c = (a << 32) | b;
        return Double.longBitsToDouble(c);

    }

    public String text() {
        if (position + 2 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        int i = integer16();
        String value = new String(buffer, position, i, charset);
        position += i;

        return value;
    }
}
