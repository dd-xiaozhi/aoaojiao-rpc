package com.aoaojiao.rpc.common.serialization;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Hessian2Output output = new Hessian2Output(bos);
            output.writeObject(obj);
            output.flush();
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Hessian serialize failed", ex);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            Hessian2Input input = new Hessian2Input(bis);
            Object obj = input.readObject(clazz);
            return clazz.cast(obj);
        } catch (Exception ex) {
            throw new IllegalStateException("Hessian deserialize failed", ex);
        }
    }

    @Override
    public SerializationType type() {
        return SerializationType.HESSIAN;
    }
}
