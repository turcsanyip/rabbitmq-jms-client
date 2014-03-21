/* Copyright (c) 2013 Pivotal Software, Inc. All rights reserved. */
package com.rabbitmq.jms.client.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;

import com.rabbitmq.jms.client.RMQMessage;
import com.rabbitmq.jms.util.RMQByteArrayOutputStream;
import com.rabbitmq.jms.util.RMQJMSException;
import com.rabbitmq.jms.util.RMQMessageFormatException;

/**
 * Implementation of {@link BytesMessage} interface.
 */
public class RMQBytesMessage extends RMQMessage implements BytesMessage {
    /**
     * This variable is set true if we are reading, but not writing the message
     * and false if we are writing but can not read the message
     */
    private volatile boolean reading;

    /** <code>buf</code> stores the byte array payload and we read from it directly */
    private volatile transient byte[] buf;
    /** The position of our read in the byte array <code>buf</code> */
    private volatile transient int pos;

    /** The stream we write structured and unstructured data to */
    private transient RMQByteArrayOutputStream bout;

    public RMQBytesMessage() {
        this(false);
    }

    /**
     * Instantiates a new RMQBytesMessage
     * @param reading - <code>true</code> if this message is in a read state
     */
    public RMQBytesMessage(boolean reading) {
        this.reading = reading;
        if (!reading) {
            /* If we are in Write state, then create the objects to support that state */
            this.bout = new RMQByteArrayOutputStream(DEFAULT_MESSAGE_BODY_SIZE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readBoolean() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_BOOLEAN > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        return Bits.getBoolean(this.buf, this.pos++);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + 1 > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        return this.buf[this.pos++];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedByte() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + 1 > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        return ((int) (this.buf[this.pos++])) & 0xFF;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_SHORT > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        short s = Bits.getShort(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_SHORT;
        return s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedShort() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_SHORT > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        short s = Bits.getShort(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_SHORT;
        return ((int) s) & 0xFFFF;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_CHAR > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        char ch = Bits.getChar(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_CHAR;
        return ch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_INT > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        int i = Bits.getInt(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_INT;
        return i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_LONG > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        long l = Bits.getLong(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_LONG;
        return l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_FLOAT > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        float flt = Bits.getFloat(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_FLOAT;
        return flt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (this.pos + Bits.NUM_BYTES_IN_DOUBLE > this.buf.length)
            throw new MessageEOFException(MSG_EOF);
        double dbl = Bits.getDouble(this.buf, this.pos);
        this.pos += Bits.NUM_BYTES_IN_DOUBLE;
        return dbl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readUTF() throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        int posOfUtfItem = this.pos;
        int lenUtfBytes = readUnsignedShort(); // modifies pos if valid
        this.pos = posOfUtfItem;               // reset in case of failure

        int utfItemLen = Bits.NUM_BYTES_IN_SHORT + lenUtfBytes;
        if (posOfUtfItem + utfItemLen > this.buf.length) {
            throw new MessageFormatException("Not enough bytes in message body for UTF object");
        }
        byte[] utfBuf = new byte[utfItemLen];
        System.arraycopy(this.buf, posOfUtfItem, utfBuf, 0, utfItemLen);

        try {
            String str = new DataInputStream(new ByteArrayInputStream(utfBuf)).readUTF();
            this.pos += utfItemLen;
            return str;
        } catch (IOException ioe) {
            throw new RMQMessageFormatException("UTF String invalid format", ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(byte[] value) throws JMSException {
        return this.readBytes(value, value.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(byte[] value, int length) throws JMSException {
        if (!this.reading)
            throw new MessageNotReadableException(NOT_READABLE);
        if (length<0 || length>value.length) {
            throw new IndexOutOfBoundsException();
        }
        if (this.pos < this.buf.length) {
            int readLen = Math.min(length, this.buf.length - this.pos);
            System.arraycopy(this.buf, this.pos, value, 0, readLen);
            this.pos += readLen;
            return readLen;
        }
        return -1; // means EOF already
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBoolean(boolean value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeBoolean(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(byte value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeByte(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(short value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeShort(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeChar(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeInt(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeLong(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(float value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeFloat(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(double value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeDouble(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTF(String value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        this.bout.writeUTF(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        try {
            this.bout.write(value);
        } catch (IOException e) {
            throw new RMQJMSException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        if (value == null ) {
            throw new MessageFormatException("Null byte array");
        } else if (offset>=value.length || length<0) {
            throw new IndexOutOfBoundsException();
        }
        this.bout.write(value, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object value) throws JMSException {
        if (this.reading || isReadonlyBody())
            throw new MessageNotWriteableException(NOT_WRITEABLE);
        writePrimitiveData(value, this.bout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws JMSException {
        if (this.reading) {
            //if we already are reading, all we want to do is reset to the
            //beginning of the stream
            this.pos = 0;
        } else {
            if (this.bout != null) {
                this.buf = this.bout.toByteArray();
            } else {
                this.buf = new byte[0];
            }
            this.pos = 0;
            this.reading = true;
            this.bout = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getBodyLength() throws JMSException {
        return this.reading ? this.buf.length : this.bout.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearBodyInternal() throws JMSException {
        this.bout = new RMQByteArrayOutputStream(DEFAULT_MESSAGE_BODY_SIZE);
        this.pos = 0;
        this.buf = null;
        this.reading = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeBody(ObjectOutput oOut, ByteArrayOutputStream bout) throws IOException {
        byte[] buf = this.bout.toByteArray();
        bout.write(buf, 0, buf.length);
    }

    @Override
    protected void writeAmqpBody(ByteArrayOutputStream baos) throws IOException {
        byte[] buf = this.bout.toByteArray();
        baos.write(buf);
    }

    /**
     * {@inheritDoc}
     * Structured data (if any) is already read by the time this is called, in which case, for {@link RMQBytesMessage},
     * only a byte array remains.
     */
    @Override
    protected void readBody(ObjectInput inputStream, ByteArrayInputStream bin) throws IOException, ClassNotFoundException {
        int len = bin.available();
        this.buf = new byte[len];
        bin.read(this.buf);
        this.reading = true;
        this.pos = 0;
    }

    @Override
    protected void readAmqpBody(byte[] barr) {
        this.buf = barr;
        this.reading = true;
        this.pos = 0;
    }

    /**
     * Utility method to write an object as a primitive or as an object
     * @param s the object to write
     * @param out the output (normally a stream) to write it to
     * @param allowSerializable true if we allow {@link Serializable} objects
     * @throws IOException from write primitives
     * @throws MessageFormatException if s is not a recognised type for writing
     * @throws NullPointerException if s is null
     */
    private static final void writePrimitiveData(Object s, RMQByteArrayOutputStream out) throws JMSException {
        if(s==null) {
            throw new NullPointerException();
        } else if (s instanceof Boolean) {
            out.writeBoolean(((Boolean) s).booleanValue());
        } else if (s instanceof Byte) {
            out.writeByte(((Byte) s).byteValue());
        } else if (s instanceof Short) {
            out.writeShort((((Short) s).shortValue()));
        } else if (s instanceof Integer) {
            out.writeInt(((Integer) s).intValue());
        } else if (s instanceof Long) {
            out.writeLong(((Long) s).longValue());
        } else if (s instanceof Float) {
            out.writeFloat(((Float) s).floatValue());
        } else if (s instanceof Double) {
            out.writeDouble(((Double) s).doubleValue());
        } else if (s instanceof String) {
            out.writeUTF((String) s);
        } else if (s instanceof Character) {
            out.writeChar(((Character) s).charValue());
        } else if (s instanceof Character) {
            out.writeChar(((Character) s).charValue());
        } else if (s instanceof byte[]) {
            out.write((byte[])s, 0, ((byte[]) s).length);
        } else
            throw new MessageFormatException(s + " is not a recognized writable type.");
    }

    /**
    * Utility methods for unpacking primitive values out of byte arrays
    * using big-endian byte ordering.
    * [A modified copy of java.io.Bits, it being package private :-(]
    */
    private static abstract class Bits { // prevent ourselves creating an instance

        private Bits() {};  // prevent anyone else creating an instance
        /*
         * Methods for unpacking primitive values from byte arrays starting at
         * given offsets.
         */

        public static final int NUM_BYTES_IN_BOOLEAN = 1;
        public static boolean getBoolean(byte[] b, int off) {
            return b[off] != 0;
        }

        public static final int NUM_BYTES_IN_CHAR = 2;
        public static char getChar(byte[] b, int off) {
            return (char) (((b[off + 1] & 0xFF) << 0) +
                           ((b[off + 0]) << 8));
        }

        public static final int NUM_BYTES_IN_SHORT = 2;
        public static short getShort(byte[] b, int off) {
            return (short) (((b[off + 1] & 0xFF) << 0) +
                            ((b[off + 0]) << 8));
        }

        public static final int NUM_BYTES_IN_INT = 4;
        public static int getInt(byte[] b, int off) {
            return ((b[off + 3] & 0xFF) << 0) +
                   ((b[off + 2] & 0xFF) << 8) +
                   ((b[off + 1] & 0xFF) << 16) +
                   ((b[off + 0]) << 24);
        }

        public static final int NUM_BYTES_IN_FLOAT = 4;
        public static float getFloat(byte[] b, int off) {
            return Float.intBitsToFloat(getInt(b, off));
        }

        public static final int NUM_BYTES_IN_LONG = 8;
        public static long getLong(byte[] b, int off) {
            return ((b[off + 7] & 0xFFL) << 0) +
                    ((b[off + 6] & 0xFFL) << 8) +
                    ((b[off + 5] & 0xFFL) << 16) +
                    ((b[off + 4] & 0xFFL) << 24) +
                    ((b[off + 3] & 0xFFL) << 32) +
                    ((b[off + 2] & 0xFFL) << 40) +
                    ((b[off + 1] & 0xFFL) << 48) +
                    (((long) b[off + 0]) << 56);
        }

        public static final int NUM_BYTES_IN_DOUBLE = 8;
        public static double getDouble(byte[] b, int off) {
            return Double.longBitsToDouble(getLong(b, off));
        }
    }

    public static final RMQMessage recreate(BytesMessage msg) throws JMSException {
        RMQBytesMessage rmqBMsg = new RMQBytesMessage();
        RMQMessage.copyAttributes(rmqBMsg, msg);

        return rmqBMsg;
    }
}
