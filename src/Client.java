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
        int port = 8091;
        Scanner scanner = new Scanner(System.in);
        InputHelper inputHelper = new InputHelper(scanner);

        System.out.println("Клиент запущен");

        while (true) {
            Socket socket = null;
            DataOutputStream out = null;
            DataInputStream in = null;

            // пробуем подключиться - 3 раза
            for (int i = 1; i <= 3; i++) {
                try {
                    // пытаемся создать сокет и подключиться сразу
                    socket = new Socket(host, port);
                    out = new DataOutputStream(socket.getOutputStream());
                    in = new DataInputStream(socket.getInputStream());

                    System.out.println("Подключено!");
                    break;// выход из цикла, если успешно
                } catch (Exception e) {
                    System.out.println("Попытка " + i + " из 3 не удалась");
                    if (i == 3) {
                        System.err.println("Сервер недоступен. Попробуйте позже.");
                        return;
                    }
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }

            // работаем с командами
            try {
                while (true) {
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();
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
                        return;
                    }

                    if (input.equalsIgnoreCase("save")) {
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
                            System.out.println("Ключ должен быть числом!");
                            continue;
                        }
                        Worker worker = inputHelper.readWorkerForComparison();
                        if (worker == null) continue;
                        request = new CommandRequest("insert", argument, worker);

                    } else if (commandName.equals("update_id")) {
                        try {
                            Integer.parseInt(argument);
                        } catch (NumberFormatException e) {
                            System.out.println("id должен быть числом!");
                            continue;
                        }
                        Worker worker = inputHelper.readWorkerForComparison();
                        if (worker == null) continue;
                        request = new CommandRequest("update_id", argument, worker);

                    } else if (commandName.equals("remove_lower")) {
                        Worker worker = inputHelper.readWorkerForComparison();
                        if (worker == null) continue;
                        request = new CommandRequest("remove_lower", "", worker);

                    } else if (commandName.equals("replace_if_lower")) {
                        try {
                            Long.parseLong(argument);
                        } catch (NumberFormatException e) {
                            System.out.println("Ключ должен быть числом!");
                            continue;
                        }
                        Worker worker = inputHelper.readWorkerForComparison();
                        if (worker == null) continue;
                        request = new CommandRequest("replace_if_lower", argument, worker);

                    } else {
                        request = new CommandRequest(commandName, argument);
                    }

                    try {
                        sendRequest(out, request);
                        CommandResponse response = receiveResponse(in);
                        System.out.println(response.getMessage());
                    } catch (EOFException e) {
                        // Сервер отключился
                        System.out.println("Соединение с сервером потеряно");
                        break;  // выходим из цикла команд, идём на переподключение
                    } catch (IOException e) {
                        System.err.println("Ошибка: " + e.getMessage());
                        break;
                    }
                }

            } finally {
                // закрываем ресурсы
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException ignored) {}
            }

            // ждем перед подключением
            System.out.println("Переподключение через 10 секунд...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {}
        }
    }


    private static void sendRequest(DataOutputStream out, CommandRequest request) throws IOException {
        byte[] requestData = serialize(request);
        if (requestData == null) {
            throw new IOException("Failed to serialize request");
        }
        out.writeInt(requestData.length);
        out.write(requestData);
        out.flush();
    }

    private static CommandResponse receiveResponse(DataInputStream in) throws IOException {
        int responseLength = in.readInt();
        byte[] responseData = new byte[responseLength];
        in.readFully(responseData);
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

