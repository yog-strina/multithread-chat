package ChatServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * A chat server that delivers public and private message
 */
public class Server {
    // The server socket
    private static ServerSocket serverSocket = null;
    // The client socket
    private static Socket clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String args[]) {
        // The default port number
        int portNumber = 2222;
        if (args.length < 1) {
            System.out.println(
                    "Usage: java Server <portNumber>\n"
                    + "Now using port number= " + portNumber
            );
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

        /*
         * Opens a server socket on the portNumber (default 2222).
         * Note that we can not choose a port less thatn 1023 if we are not privileged users root
         */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

        /*
         * Creates a client socket for each connection an pass it to a new client thread
         */
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                int i;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream outputStream = new PrintStream(clientSocket.getOutputStream());
                    outputStream.println("Server too busy. Try later.");
                    outputStream.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, asks the client's name, informs all the clients conencted to
 * the server about the fact that a new client has joined the chat room, and as long
 * as it receives data, echos that data back to all other clients.
 * When a client leaves the chat room, this thread informs also all the clients about that
 * and terminates.
 */
class clientThread extends Thread {
    private DataInputStream inputStream = null;
    private PrintStream outputStream = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {
            /*
             * Creates input and output streams for this client.
             */
            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new PrintStream(clientSocket.getOutputStream());
            outputStream.println("Enter your name.");
            String name = inputStream.readLine().trim();
            outputStream.println(
                    "Hello " + name + " to our chat room.\nTo leave enter /quit in a new line"
            );
            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i] != this) {
                    threads[i].outputStream.println(
                            "*** A new user " + name + " entered the chat room !!! ***"
                    );
                }
            }
            while (true) {
                String line = inputStream.readLine();
                if (line.startsWith("/quit")) {
                    break;
                }
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null) {
                        threads[i].outputStream.println(
                                "<" + name + "> " + line
                        );
                    }
                }
            }
            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i] != this) {
                    threads[i].outputStream.println(
                            "*** The user " + name + " is leaving the chat room !!! ***"
                    );
                }
            }
            outputStream.println("*** Bye " + name + " ***");

            /*
             * Cleans up. Sets the current thread variable to null so that a new client
             * could be accepted by the server
             */
            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] == this) {
                    threads[i] = null;
                }
            }

            /*
             * Closes the output stream, the input stream, and the socket
             */
            inputStream.close();
            outputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}