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
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static volatile boolean running = true;

    private static final int port = 8091;
    private static HandlerCommand commandHandler;
    private static Selector selector;


    public static void main(String[] args) {
        // Инициализируем обработчик команд
        String fileName = System.getenv("WORKERS");
        if (fileName == null) fileName = "workers.json";
        commandHandler = new HandlerCommand(fileName);


        try {
            // создаем серверный канал:
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port)); // привязываем канал к порту
            serverChannel.configureBlocking(false); // устанавливаем неблокирующий режим
            // {} — плейсхолдер, port подставится вместо {}
            logger.info("Сервер запущен на порту {}", port);

            // создаем селектор:
            selector = Selector.open();
            // регистрируем серверный канал на селекторе
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("Ожидание подключений...");

            logger.info("Доступные команды сервера: save, exit");

            // хранилище данных клиентов:
            // каждому клиенту соответствует свой буфер для накопления данных
            Map<SocketChannel, ClientData> clientsData = new HashMap<>();

            // добавляем BufferedReader для неблокирующего чтения консоли
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            while (running) {
                // таймаут 100 мс позволяет периодически проверять консольный ввод
                int readyChannels = selector.select(100);

                // вызов проверки консоли в главном цикле - каждые 100 мс или при сетевом событии
                checkConsoleInput(consoleReader);

                // проверка readyChannels > 0, если селектор
                // проснулся по таймауту - пропускаем обработку ключей
                if (readyChannels > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        try {
                            if (key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                                SocketChannel clientChannel = server.accept();
                                clientChannel.configureBlocking(false);

                                clientChannel.register(selector, SelectionKey.OP_READ);
                                clientsData.put(clientChannel, new ClientData());

                                logger.info("Клиент подключился: {}", clientChannel.getRemoteAddress());

                            } else if (key.isReadable()) {
                                SocketChannel clientChannel = (SocketChannel) key.channel();
                                ClientData clientData = clientsData.get(clientChannel);
                                ByteBuffer buffer = clientData.getBuffer();

                                int bytesRead = clientChannel.read(buffer);

                                if (bytesRead == -1) {
                                    logger.info("Клиент отключился");
                                    clientChannel.close();
                                    clientsData.remove(clientChannel);
                                    continue;
                                }

                                buffer.flip();
                                while (buffer.hasRemaining()) {
                                    clientData.getReceivedData().add(buffer.get());
                                }
                                buffer.clear();

                                CommandRequest request = tryDeserialize(clientData.getReceivedData());

                                if (request != null) {
                                    logger.info("Получена команда: {}", request.getCommandName());
                                    CommandResponse response = commandHandler.executeCommand(request);
                                    ByteBuffer responseBuffer = serializeResponse(response);
                                    clientChannel.write(responseBuffer);
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
            }

        } catch (IOException e) {
            running = false;
            logger.error("Ошибка сервера: {}", e.getMessage());
        }
    }

    /**
     * Проверяет консольный ввод
     *
     * @param reader BufferedReader для чтения
     */
    private static void checkConsoleInput(BufferedReader reader) {
        try {
            // ready() возвращает true только если есть данные для чтения
            // поток не будет ждать пока пользователь не введет строку
            if (reader.ready()) {
                String input = reader.readLine().trim().toLowerCase();

                if (input.equals("save")) {
                    // так как всё в одном потоке - не нужны блокировки
                    commandHandler.saveCollection();
                    System.out.println("Коллекция сохранена");
                    logger.info("Команда save выполнена на сервере");

                } else if (input.equals("exit")) {
                    System.out.println("Завершение работы сервера...");
                    logger.info("Команда exit выполнена сервером");
                    commandHandler.saveCollection();
                    running = false; // цикл завершится при следующей итерации
                } else if (!input.isEmpty()) {
                    System.out.println("Неизвестная команда. Доступны: save, exit");
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка чтения консоли: {}", e.getMessage());
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
            // создаем буффер длиной 4 байта и отправляем
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