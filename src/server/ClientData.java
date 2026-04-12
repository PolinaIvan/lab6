package server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранит данные одного подключённого клиента.
 * Данные могут приходить частями.
 */
public class ClientData {
    private final ByteBuffer buffer;           // буфер для накопления данных
    private final List<Byte> receivedData;     // временное хранилище для сборки объекта

    public ClientData() {
        this.buffer = ByteBuffer.allocate(8192);  // 8 КБ буфер
        this.receivedData = new ArrayList<>();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public List<Byte> getReceivedData() {
        return receivedData;
    }

    /**
     * Очищает накопленные данные (после успешного чтения)
     */
    public void clear() {
        receivedData.clear();
        buffer.clear();
    }
}