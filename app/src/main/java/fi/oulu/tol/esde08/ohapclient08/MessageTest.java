/**
 * Created by Jonna on 21.5.2015.
 */
package fi.oulu.tol.esde08.ohapclient08;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MessageTest {
    public static boolean failed;

    public static void main(String[] args) throws IOException {
        // Sending
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        OutgoingMessage outgoingMessage = new OutgoingMessage();
        outgoingMessage.integer8(1).integer16(1 << 8).integer32((1 << 24) + 1).decimal64(-1970.2015)
                .text("abc").binary8(true).writeTo(outputStream);

        //.binary8(true)

        // Check the byte layout
        byte[] bytes = outputStream.toByteArray();
        byte[] expected = { 0x00, 0x15, 0x01, 0x01, 0x00, 0x01, 0x00, 0x00, 0x01,
                (byte)0xc0, (byte)0x9e, (byte)0xc8, (byte)0xce,
                0x56, 0x04, 0x18, (byte)0x93,
                0x00, 0x03, 0x61, 0x62, 0x63,
                0x01};
        test(Arrays.equals(bytes, expected), "Arrays.equals(bytes, expected)");

        // Receiving
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        IncomingMessage incomingMessage = new IncomingMessage();
        incomingMessage.readFrom(inputStream);
        test(incomingMessage.integer8() == 1, "incomingMessage.integer8() == 1");
        test(incomingMessage.integer16() == 1 << 8, "incomingMessage.integer16() == 1 << 8");
        test(incomingMessage.integer32() == (1 << 24) + 1, "incomingMessage.integer32() == (1 << 24) + 1");
        test(incomingMessage.decimal64() == -1970.2015, "incomingMessage.decimal64() == -1970.2015");
        test(incomingMessage.text().equals("abc"), "incomingMessage.text().equals(\"abc\")");
        test(incomingMessage.binary8() == true, "incomingMessage.binary8() == true");

        // Final judgement
        if (!failed)
            System.out.println("Tests OK.");
    }

    private static void test(boolean condition, String message) {
        if (!condition) {
            System.out.println("Test failed: " + message);
            failed = true;
        }
    }
}