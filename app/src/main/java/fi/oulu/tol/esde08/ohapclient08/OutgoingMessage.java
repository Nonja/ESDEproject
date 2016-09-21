package fi.oulu.tol.esde08.ohapclient08;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by Jonna on 21.5.2015.
 */
public class OutgoingMessage {

    /**
     * The internal buffer. It will be grown if the message do not fit to it.
     */
    private byte[] buffer = new byte[256];

    /**
     * The position where the next byte should be appended. The initial position
     * skips the space reserved for the message length.
     */
    private int position = 2;

    /**
     * Character set used to convert strings.
     */
    private final Charset charset = Charset.forName("UTF-8");

    /**
     * Ensures that the internal buffer have room for the specified amount of
     * bytes. Grows the buffer when needed by doubling its size.
     *
     * @param appendLength the amount of bytes to be appended
     */
    private void ensureCapacity(int appendLength) {
        if (position + appendLength < buffer.length)
            return;
        int newLength = buffer.length * 2;
        while (position + appendLength >= newLength)
            newLength *= 2;
        buffer = Arrays.copyOf(buffer, newLength);
    }

    public OutgoingMessage binary8(boolean b) {
        integer8(b ? 1 : 0);
        return this;
    }

    public OutgoingMessage integer8(int value) {
        ensureCapacity(1);

        buffer[position] = (byte)(value);
        position += 1;

        return this;
    }

    public OutgoingMessage integer16(int value) {
        ensureCapacity(2);

        buffer[position] = (byte)(value >> 8);
        buffer[position + 1] = (byte)value;
        position += 2;

        return this;
    }

    public OutgoingMessage integer32(int value) {
        ensureCapacity(4);

        integer16(value >> 16);
        integer16(value);

        return this;

    }

    public OutgoingMessage decimal64(double value) {
        ensureCapacity(8);

        long i = Double.doubleToLongBits(value);

        integer32((int) (i >> 32));
        integer32((int) i);

        return this;
    }

    public OutgoingMessage text(String abc) {

        byte[] b = abc.getBytes(charset);
        int i = b.length;
        ensureCapacity(i + 2);

        integer16(i);
        for (int j = 0; j < i; ++j) {
            buffer[position] = b[j];
            position += 1;
        }


        return this;
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        int count = position - 2;

        buffer[0] = (byte)((count & 0xFF00) >> 8);
        buffer[1] = (byte)(count & 0xFF);

        outputStream.write(buffer, 0, position);

    }
}
