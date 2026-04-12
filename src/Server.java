import common.CommandRequest;
import common.CommandResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ClientData;
import server.HandlerCommand;

/**
 * NIO сервер — неблокирующий, однопоточный.
 * Может обрабатывать много клиентов одновременно.
 */
public class Server {
    // LoggerFactory.getLogger() — связывает логи с этим классом
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static final int port = 8090;
    private static HandlerCommand commandHandler;

    public static void main(String[] args) {
        // Инициализируем обработчик команд (как раньше)
        String fileName = System.getenv("WORKERS");
        if (fileName == null) fileName = "workers.json";
        commandHandler = new HandlerCommand(fileName);

        try {
            // создаем серверный канал:
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port)); // привязываем канал к порту
            serverChannel.configureBlocking(false);  // устанавливаем неблокирующий режим
            // {} — плейсхолдер, port подставится вместо {}
            logger.info("Сервер запущен на порту {}", port);


            // создаем селектор:
            Selector selector = Selector.open();

            // регистрируем серверный канал на селекторе:
            // OP_ACCEPT — ждём новые подключения
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("Ожидание подключений...");

            // хранилище данных клиентов:
            // каждому клиенту соответствует свой буфер для накопления данных
            Map<SocketChannel, ClientData> clientsData = new HashMap<>();

            while (true) {
                // ждем, когда кто-то подключится
                selector.select();

                // возвращает набор событий, которые произошли
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                // удаляет событие после обработки
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();  // удаляем, чтобы не обрабатывать повторно

                    try {
                        if (key.isAcceptable()) {
                            // новое подключение:
                            ServerSocketChannel server = (ServerSocketChannel) key.channel(); // получаем канал
                            SocketChannel clientChannel = server.accept(); // принимаем клиента
                            clientChannel.configureBlocking(false);


                            // регистрируем клиентский канал на чтение
                            // чтобы селектор мог следить за данными от этого клиента
                            clientChannel.register(selector, SelectionKey.OP_READ);

                            // создаём буфер для этого клиента
                            clientsData.put(clientChannel, new ClientData());

                            logger.info("Клиент подключился: {}", clientChannel.getRemoteAddress());

                        } else if (key.isReadable()) {
                            // если есть данные для чтения:
                            SocketChannel clientChannel = (SocketChannel) key.channel();

                            // получаем буфер для этого клиента
                            ClientData clientData = clientsData.get(clientChannel);
                            ByteBuffer buffer = clientData.getBuffer();

                            // читаем данные из канала в буфер
                            int bytesRead = clientChannel.read(buffer);

                            if (bytesRead == -1) {
                                // клиент закрыл соединение
                                logger.info("Клиент отключился");
                                clientChannel.close();
                                clientsData.remove(clientChannel);
                                continue;
                            }

                            // переключаем буфер в режим чтения
                            buffer.flip();

                            // сохраняем все прочитанные байты
                            while (buffer.hasRemaining()) {
                                clientData.getReceivedData().add(buffer.get());
                            }

                            // переключаем буфер обратно в режим записи
                            buffer.clear();

                            // пробуем собрать объект из накопленных байтов
                            // если достаточно данных, то возвращает объект
                            CommandRequest request = tryDeserialize(clientData.getReceivedData());

                            if (request != null) {
                                logger.info("Получена команда: {}", request.getCommandName());

                                // выполняем команду
                                CommandResponse response = commandHandler.executeCommand(request);

                                // превращаем объекты в байты и отправляем ответ клиенту
                                ByteBuffer responseBuffer = serializeResponse(response);
                                clientChannel.write(responseBuffer);

                                // очищаем данные из буфера для следующей команды
                                clientData.clear();
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Ошибка с клиентом: {}", e.getMessage(), e);
                        key.channel().close();
                        clientsData.remove(key.channel());
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Ошибка сервера: {}", e.getMessage());
        }
    }

    /**
     * Пытается собрать объект CommandRequest из накопленных байтов.
     * Возвращает null, если данных недостаточно.
     */
    private static CommandRequest tryDeserialize(List<Byte> bytes) {
        if (bytes.isEmpty()) return null;

        try {
            // превращаем List<Byte> в массив байтов
            byte[] data = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                data[i] = bytes.get(i);
            }

            // десериализация
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(data); // читает байты из массива
                DataInputStream dis = new DataInputStream(bais);  // превращает байты в объект

                // читаем длину сообщения - 4 байта
                int messageLength = dis.readInt();

                // проверяем, достаточно ли данных, если нет то ждем
                if (data.length - 4 < messageLength) {
                    return null;  // еще не все данные получены
                }

                // читаем данные
                byte[] messageData = new byte[messageLength];
                dis.readFully(messageData);

                // десериализуем объект
                ByteArrayInputStream bais2 = new ByteArrayInputStream(messageData);
                ObjectInputStream ois = new ObjectInputStream(bais2);
                return (CommandRequest) ois.readObject();
            } catch (StreamCorruptedException | OptionalDataException e) {
                // поврежденные данные или неправильный формат
                logger.error("Invalid stream format: {}", e.getMessage(), e);
                return null;
            } catch (IOException e) {
                logger.error("IO error during deserialization: {}", e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            // данных недостаточно или объект не полный — возвращаем null
            return null;
        }
    }


    /**
     * Сериализует ответ в ByteBuffer для отправки (БЕЗ ДЛИНЫ)
     */
    private static ByteBuffer serializeResponse(CommandResponse response) {
        ByteBuffer result;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(response);
            oos.flush();

            byte[] data = baos.toByteArray();
            // 👇 СОЗДАЕМ БУФЕР С ДЛИНОЙ (4 байта) + ДАННЫЕ
            ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
            buffer.putInt(data.length);  // сначала записываем длину
            buffer.put(data);            // и затем данные
            buffer.flip();
            result = buffer;
        } catch (IOException E){
            result = null;
        }
        return result;
    }
}