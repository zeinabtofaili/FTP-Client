package iot.treeftp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The {@code FTPClient} class defines a client that connects to an FTP server
 * and retrieves the directory listing in either a BFS or DFS manner.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * FTPClient client = new FTPClient("ftp.example.com", 21);
 * client.login(username, password);
 * // Perform operations like client.showTreeDFS()
 * </pre>
 * 
 */
public class FTPClient implements Client{

	/**
	 * The maximum number of times the client tries reconnecting to the server after
	 * failing
	 */
	private static final int MAX_NUM_RETRIES = 3;

	/**
	 * The time that the client waits after a failed reconnection to attempt
	 * reconnecting again
	 */
	private static final long RETRY_INTERVAL = 5000;

	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;

	private String server;
	private int port;

	/**
	 * Creates an FTP client instance given the server name and the port number.
	 * 
	 * @param server Server hostname
	 * @param port   Port number of the server
	 * @throws UnknownHostException if the host IP address cannot be determined
	 * @throws IOException          if an input/output error occurred
	 */
	public FTPClient(String server, int port) throws UnknownHostException, IOException {
		this.server = server;
		this.port = port;

		socket = new Socket(server, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(socket.getOutputStream(), true);
		System.out.println(readSingleLineResponse());
	}

	/**
	 * Creates an FTP client instance given the server name, port number, socket,
	 * reader, and writer. Only used in testing environment as it decouples the
	 * class from the actual network resources.
	 * 
	 * @param server Server hostname
	 * @param port   Port number of the server
	 * @param socket Socket used for the network connection to the server
	 * @param reader A {@link BufferedReader} that wraps the socket input stream
	 * @param writer A {@link PrintWriter} that wraps the socket output stream
	 * @throws IOException if an input/output error occurred
	 */
	public FTPClient(String server, int port, Socket socket, BufferedReader reader, PrintWriter writer)
			throws IOException {
		this.server = server;
		this.port = port;
		this.socket = socket;
		this.reader = reader;
		this.writer = writer;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public void setReader(BufferedReader reader) {
		this.reader = reader;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}
	
	/**
	 * Logs into the server with the provided username and password.
	 * 
	 * @param username The username of the user
	 * @param password The password of the user
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public void login(String username, String password) throws IOException {
		sendCommandToServer("USER " + username);
		String userResponse = readSingleLineResponse();
		System.out.println(userResponse);
		if (!positiveResponse(userResponse)) {
			throw new IOException("Authentication failed: Invalid credentials");
		}

		sendCommandToServer("PASS " + password);
		String passResponse = readSingleLineResponse();
		System.out.println(passResponse);
		if (!positiveResponse(passResponse)) {
			throw new IOException("Authentication failed: Invalid credentials");
		}
	}

	/**
	 * Checks whether the response received from the server after the login attempt
	 * is positive or not.
	 * 
	 * @param response The response received from the server
	 * @return True if the server did not raise an error, and false otherwise.
	 */
	private boolean positiveResponse(String response) {
		return response != null && (response.startsWith("230") || response.startsWith("331"));
	}

	/**
	 * Reads a single response line from the server. Used when the server is
	 * expected to reply in a single line. Supports temporary disconnection from the
	 * server by attempting reconnection several times.
	 * 
	 * @return A string containing the response from the server
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public String readSingleLineResponse() throws IOException {
		int nbAttempts = 0;
		while (nbAttempts < MAX_NUM_RETRIES) {
			try {
				return reader.readLine();
			} catch (IOException e) {
				System.err.println("Reading response failed; attempting to reconnect...");
				reconnectToServer();
				nbAttempts++;
			}
		}
		throw new IOException("Reading response from server failed after several attempts.");
	}

	/**
	 * Attempts reconnecting to the server after temporary disconnection. It tries
	 * reconnecting three times before giving up.
	 * 
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public void reconnectToServer() throws IOException {
		int nbRetries = 0;
		while (nbRetries < MAX_NUM_RETRIES) {
			try {
				disconnectFromServer();

				socket = new Socket(server, port);
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream(), true);

				break;
			} catch (IOException e) {
				nbRetries++;
				try {
					Thread.sleep(RETRY_INTERVAL);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}

		if (nbRetries == MAX_NUM_RETRIES) {
			System.out.println("Reconnection failed. Exiting application...");
			disconnectFromServer();
		}
	}

	/**
	 * Reads a multiline response from the server. Used when the server is expected
	 * to reply with several lines such as when receiving response from "LIST"
	 * command. Supports temporary disconnection from the server by attempting
	 * reconnection several times.
	 * 
	 * @return A string containing the response from the server
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public String readMultilineResponse() throws IOException {
		int nbAttempts = 0;
		while (nbAttempts < MAX_NUM_RETRIES) {
			try {
				String responseLine, response = "";
				while ((responseLine = reader.readLine()) != null) {
					response += responseLine + "\n";
					if (responseLine.startsWith("226") || responseLine.startsWith("421")) {
						break;
					}
				}
				return response;
			} catch (IOException e) {
				System.err.println("Reading response failed; attempting to reconnect...");
				reconnectToServer();
				nbAttempts++;
			}
		}
		throw new IOException("Reading response from server failed after several attempts.");
	}

	/**
	 * Sends a command to the server.
	 * 
	 * @param command The command to send to the server
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public void sendCommandToServer(String command) throws IOException {
		writer.println(command);
	}

	/**
	 * Closes the connection between the client and the server.
	 * 
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public void disconnectFromServer() throws IOException {
		if (socket != null && !socket.isClosed()) {
			socket.close();
			System.out.println("Disconnected from the FTP server.");
		}
	}

	/**
	 * Initiates a passive connection to the FTP server.
	 *
	 * <p>
	 * This method sends a PASV command to the FTP server and parses its response to
	 * establish a new {@link Socket} connection. It uses the IP address and port
	 * number provided by the response.
	 *
	 * @return A {@link Socket} representing the passive connection, or {@code null}
	 *         in case the server did not accept the PASV command.
	 * @throws IOException If sending the PASV command failed, or if there was an
	 *                     issue in parsing the server's response, or establishing
	 *                     the socket connection.
	 */
	public Socket passiveConnection() throws IOException {
		sendCommandToServer("PASV");
		String response = readSingleLineResponse();
		if (response == null || !response.startsWith("227")) {
			return null;
		} else {
			String[] responseParts = response.split("\\(")[1].split("\\)")[0].split(",");
			String ip = responseParts[0] + "." + responseParts[1] + "." + responseParts[2] + "." + responseParts[3];
			int port = Integer.parseInt(responseParts[4]) * 256 + Integer.parseInt(responseParts[5]);

			return new Socket(ip, port);
		}
	}

	/**
	 * Retrieves the contents of a directory by sending the command "LIST" to the
	 * server and returning its response.
	 * 
	 * @param path the path to the directory to list
	 * @return String containing the direct contents of the directory
	 * @throws IOException if an input/output error occurred
	 */
	@Override
	public String listDirectory(String path) throws IOException {
		Socket s = passiveConnection();
		if (s == null) {
			return "";
		}
		sendCommandToServer("LIST " + path);

		BufferedReader bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String line, result = "";
		while ((line = bf.readLine()) != null) {
			result += line + "\n";
		}
		s.close();
		readMultilineResponse();
		return result;
	}

	/**
	 * Displays the tree structure of a directory and its contents using Depth First
	 * Search starting from the specified path. The traversal depth is limited by
	 * the {@code maxDepth} parameter. The tree is printed in a similar way of the
	 * output of the tree command in Linux.
	 *
	 * @param path     The starting path of the directory to traverse and list.
	 * @param prefix   The prefix to use for indentation and tree formatting.
	 * @param depth    The current depth of the traversal.
	 * @param maxDepth The maximum depth to traverse.
	 * @throws IOException If an I/O error occurs during directory listing.
	 */
	public void showTreeDFS(String path, String prefix, int depth, int maxDepth) throws IOException {
		if (depth > maxDepth)
			return;

		String dirListing = listDirectory(path);
		String[] contents = dirListing.split("\n");

		for (int i = 0; i < contents.length; i++) {
			String item = contents[i];
			String itemName = parseName(item);
			if (itemName == null || itemName.equals(""))
				continue;

			boolean isLastItem = (i == contents.length - 1);
			System.out.println(prefix + (isLastItem ? "`-- " : "|-- ") + itemName);

			if (isDirectory(item)) {
				String childPrefix = prefix + (isLastItem ? "    " : "|   ");
				showTreeDFS(path.equals("/") ? path + itemName : path + "/" + itemName, childPrefix, depth + 1,
						maxDepth);
			}
		}
	}

	/**
	 * Checks if a line resembles a directory or not. Used in other methods to check
	 * whether further exploration in the current directory shall take place.
	 * 
	 * @param line The line to check
	 * @return true if the line resembles a directory and false otherwise.
	 */
	public boolean isDirectory(String line) {
		return line.startsWith("d");
	}

	/**
	 * Retrieves the name of the directory or file from the listing response.
	 * 
	 * @param line The line to get the name from.
	 * @return String containing retrieved name of the directory or file
	 */
	public String parseName(String line) {
		String[] tokens = line.split(" ");
		return tokens[tokens.length - 1];
	}

	/**
	 * Displays the tree structure of a directory and its contents using Breadth
	 * First Search starting from the specified path. The traversal depth is limited
	 * by the {@code maxDepth} parameter.
	 *
	 * @param path     The starting path of the directory to traverse and list.
	 * @param maxDepth The maximum depth to traverse.
	 * @throws IOException If an I/O error occurs during directory listing.
	 */
	public void showTreeBFS(String rootPath, int maxDepth) throws IOException {
		Queue<String> pathsQueue = new LinkedList<>();
		Queue<Integer> depthQueue = new LinkedList<>();
		pathsQueue.add(rootPath);
		depthQueue.add(0);

		while (!pathsQueue.isEmpty()) {
			String currentPath = pathsQueue.poll();
			int currDepth = depthQueue.poll();
			if (currDepth > maxDepth)
				continue;

			String dirListing = listDirectory(currentPath);
			String[] contents = dirListing.split("\n");

			for (String item : contents) {
				String itemName = parseName(item);
				if (itemName == null || itemName.equals(""))
					continue;

				String prefix = "   ".repeat(currDepth) + "|__ ";
				System.out.println(prefix + itemName);

				if (isDirectory(item)) {
					String newPath = currentPath.equals("/") ? currentPath + itemName : currentPath + "/" + itemName;
					pathsQueue.add(newPath);
					depthQueue.add(currDepth + 1);
				}
			}
		}
	}

	/**
	 * Builds a JSON tree representation of the directory structure starting from
	 * the provided path up to the maximum depth specified. This method is useful to
	 * create a JSON file of the tree structure.
	 *
	 * @param path     The starting path of building the JSON tree.
	 * @param depth    The current depth of the traversal.
	 * @param maxDepth The maximum depth to traverse.
	 * @return The root {@link TreeNode} of the constructed JSON tree.
	 * @throws IOException If an I/O error occurs during directory listing.
	 */
	public TreeNode buildJsonTree(String path, int depth, int maxDepth) throws IOException {
		if (depth > maxDepth)
			return null;

		TreeNode node = new TreeNode(parseName(path));
		String dirListing = listDirectory(path);
		String[] contents = dirListing.split("\n");

		for (String item : contents) {
			String itemName = parseName(item);
			if (itemName == null)
				continue;

			if (isDirectory(item)) {
				TreeNode child = buildJsonTree(path.equals("/") ? path + itemName : path + "/" + itemName, depth + 1,
						maxDepth);
				if (child != null) {
					node.addChild(child);
				}
			} else {
				node.addChild(new TreeNode(itemName));
			}
		}

		return node;
	}

	/**
	 * Writes the JSON representation of a tree structure to a file. This method
	 * uses Gson to serialize the tree into a JSON string which is then written to
	 * the specified file.
	 *
	 * <p>
	 * Example usage:
	 * 
	 * <pre>
	 * TreeNode root = buildJsonTree("/path/to/directory", 0, 3);
	 * writeTreeToJson(root, "output.json");
	 * </pre>
	 *
	 * @param root     The root node of the directory tree.
	 * @param filename The name of the file to write the JSON representation into.
	 */
	public void writeTreeToJson(TreeNode root, String filename) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(root);

		try (FileWriter writer = new FileWriter(filename)) {
			System.out.println("Writing the JSON representation to "+filename+"...");
			writer.write(json);
			System.out.println("JSON file written successfully to "+filename);
		}catch (IOException e) {
			System.err.println("An error occurred while writing to the JSON file");
		}
	}

}