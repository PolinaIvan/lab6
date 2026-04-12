import common.CommandRequest;
import common.CommandResponse;
import model.Worker;
import utils.InputHelper;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8090;
        int attempts = 3;
        Scanner scanner = new Scanner(System.in);
        InputHelper inputHelper = new InputHelper(scanner);

        System.out.println("Клиент запущен");

        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                // пытаемся создать сокет и подключиться сразу
                socket = new Socket(host, port);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());

                System.out.println("Подключено!");
                break; // выход из цикла, если успешно
            } catch (Exception e) {
                System.out.println("Попытка " + i + " не удалась...");
                if (i == attempts) {
                    System.err.println("Сервер недоступен. Попробуйте позже.");
                    return; // выход из программы после 3-й неудачи
                } try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }

        String input;
        while (true) {
            System.out.print("> ");
            input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            if (input.equalsIgnoreCase("exit")) {
                try {
                    // отправляем команду save на сервер
                    CommandRequest saveRequest = new CommandRequest("save", "");
                    sendRequest(out, saveRequest);

                    // получаем ответ через receiveResponse
                    CommandResponse saveResponse = receiveResponse(in);
                    System.out.println(saveResponse.getMessage());
                } catch (Exception e) {
                    System.out.println("Не удалось сохранить: " + e.getMessage());
                }
                System.out.println("До свидания!");
                break;
            } else if (input.equalsIgnoreCase("save")) {
                System.out.println("Сохранение произойдёт автоматически при выходе");
                continue;
            }

            String[] parts = input.split(" ", 2);
            String commandName = parts[0];
            String argument = parts.length > 1 ? parts[1] : "";

            CommandRequest request = null;

            if (commandName.equals("insert")) {
                try {
                    Long.parseLong(argument);
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: ключ должен быть числом!");
                    continue;
                }
                // ввод данных работника
                Worker worker = inputHelper.readWorkerForComparison();
                if (worker == null) continue;
                request = new CommandRequest("insert", argument, worker);

            } else if (commandName.equals("update_id")) {
                try {
                    Integer.parseInt(argument);
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: id должен быть числом!");
                    continue;
                }
                // ввод данных работника
                Worker worker = inputHelper.readWorkerForComparison();
                if (worker == null) continue;
                request = new CommandRequest("update_id", argument, worker);
            } else if (commandName.equals("remove_lower")) {
                // ввод эталонного работника
                Worker worker = inputHelper.readWorkerForComparison();
                if (worker == null) continue;
                request = new CommandRequest("remove_lower", "", worker);
            } else if (commandName.equals("replace_if_lower")) {
                // проверка ключа
                try {
                    Long.parseLong(argument);
                } catch (NumberFormatException e) {
                    System.out.println("Ключ должен быть числом!");
                    continue;
                }
                // ввод данных работника
                Worker worker = inputHelper.readWorkerForComparison();
                if (worker == null) continue;
                request = new CommandRequest("replace_if_lower", argument, worker);
            } else {
                request = new CommandRequest(commandName, argument);
            }

            try {
                // отправляем запрос с длиной
                sendRequest(out, request);

                // получаем ответ
                CommandResponse response = receiveResponse(in);
                System.out.println(response.getMessage());

            } catch (Exception e) {
                System.err.println("Ошибка: " + e.getMessage());
            }
        }
    }
    // отправка запроса с указанием длины
    private static void sendRequest(DataOutputStream out, CommandRequest request) throws IOException {
        // Сериализуем объект в байты
        byte[] requestData = serialize(request);
        if (requestData == null) {
            throw new IOException("Failed to serialize request");
        }

        // Отправляем длину (4 байта)
        out.writeInt(requestData.length);
        // Отправляем сами данные
        out.write(requestData);
        out.flush();
    }

    // получение ответа с длиной
    private static CommandResponse receiveResponse(DataInputStream in) throws IOException {
        // Читаем длину ответа (4 байта)
        int responseLength = in.readInt();

        // Читаем данные ответа
        byte[] responseData = new byte[responseLength];
        in.readFully(responseData);  // читаем ровно столько байт, сколько нужно

        // Десериализуем ответ
        return deserialize(responseData);
    }

    private static byte[] serialize(CommandRequest request) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(request);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Ошибка сериализации: " + e.getMessage());
            return null;
        }
    }

    private static CommandResponse deserialize(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (CommandResponse) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка десериализации: " + e.getMessage());
            return null;
        }
    }
}


