package com.aoaojiao.rpc.common.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo 二进制序列化器
 * 高性能二进制序列化，比 Hessian 更快
 */
public class KryoSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Output output = new Output(bos)) {
            kryo.writeClassAndObject(output, obj);
            output.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Kryo serialize failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) {
            return null;
        }

        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             Input input = new Input(bis)) {
            return (T) kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new IllegalStateException("Kryo deserialize failed", e);
        }
    }

    @Override
    public SerializationType type() {
        return SerializationType.KRYO;
    }
}