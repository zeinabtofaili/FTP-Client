package iot.treeftp;

import java.io.IOException;

/**
 * The {@code Client} interface defines the contract for an FTP client that
 * interacts with an FTP server. It encapsulates all the necessary
 * functionalities to interact with an FTP server.
 *
 * <p>
 * Implementations of this interface should handle various FTP operations such
 * as:
 * <ul>
 * <li>Logging into the FTP server</li>
 * <li>Listing directory contents</li>
 * <li>Reading single and multiline responses from the server</li>
 * <li>Reconnecting to the server in case a temporary disconnection
 * occurred</li>
 * <li>Sending commands to the server</li>
 * <li>Disconnecting from the server</li>
 * </ul>
 * 
 * @see FTPClient
 *
 */
public interface Client {
	/**
	 * Logs into the server with the provided username and password.
	 * 
	 * @param username The username of the user
	 * @param password The password of the user
	 * @throws IOException if an input/output error occurred
	 */
	void login(String username, String password) throws IOException;

	/**
	 * Attempts reconnecting to the server after temporary disconnection.
	 * 
	 * @throws IOException if an input/output error occurred
	 */
	void reconnectToServer() throws IOException;

	/**
	 * Reads a single line response from the server.
	 * 
	 * @return A string containing the response from the server
	 * @throws IOException if an input/output error occurred
	 */
	String readSingleLineResponse() throws IOException;

	/**
	 * Reads a multiline response from the server.
	 * 
	 * @return A string containing the response from the server
	 * @throws IOException if an input/output error occurred
	 */
	String readMultilineResponse() throws IOException;

	/**
	 * Sends a command to the FTP server.
	 * 
	 * @param command The command to send
	 * @throws IOException if an input/output error occurred
	 */
	void sendCommandToServer(String command) throws IOException;

	/**
	 * Closes the active connection between the client and the server.
	 * 
	 * @throws IOException if an input/output error occurred
	 */
	void disconnectFromServer() throws IOException;

	/**
	 * Retrieves the contents of a directory on the server.
	 * 
	 * @param path the path to the directory to list
	 * @return String containing the direct contents of the directory
	 * @throws IOException if an input/output error occurred
	 */
	String listDirectory(String path) throws IOException;
}
