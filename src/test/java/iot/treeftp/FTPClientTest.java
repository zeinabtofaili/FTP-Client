package iot.treeftp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The {@code FTPClientTest} class contains unit tests for the {@code FTPClient}
 * class to make sure it functions correctly in different scenarios. It tests
 * various functionalities such as reading responses from the server, writing to
 * files, as well as authentication.
 * 
 * @see FTPClient
 */
public class FTPClientTest {

	private FTPClient ftpClient;
	private BufferedReader mockedReader;
	private PrintWriter mockedWriter;

	/**
	 * Sets up the testing environment before each unit test.
	 * <p>
	 * This method creates mocks for {@link BufferedReader}, {@link PrintWriter},
	 * and {@link Socket} then instantiates an {@link FTPClient} with the mocked
	 * dependencies. This allows testing the methods without requiring actual
	 * network connections.
	 * 
	 * @throws IOException in case an input/output error occurs during setup
	 */
	@BeforeEach
	void setUp() throws IOException {
		mockedReader = mock(BufferedReader.class);
		mockedWriter = mock(PrintWriter.class);
		Socket mockSocket = mock(Socket.class);
		when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

		ftpClient = new FTPClient("test.server.com", 21, mockSocket, mockedReader, mockedWriter);
	}

	/**
	 * Tests the {@link FTPClient#readSingleLineResponse()} method's ability to
	 * reinstantiate a connection with the server after disconnecting and reading
	 * the response in the following attempt.
	 * 
	 * It first simulates a failing call to {@link BufferedReader#readLine} by
	 * throwing an {@link IOException}. It then returns a mock response in the
	 * second call. To test the proper functioning of the method, it first asserts
	 * that the response is not null and is equal to the mocked response. It also
	 * makes sure that the mocked reader was called twice, once in the failed
	 * attempt when the exception was handled, and once in the successful one after
	 * retrying connection.
	 * 
	 * @throws IOException if an input/output error occurs.
	 */
	@Test
	void testReadSingleResponseWithReconnection() throws IOException {
		when(mockedReader.readLine()).thenThrow(IOException.class).thenReturn("Mocked Response");

		String response = ftpClient.readSingleLineResponse();

		assertNotNull(response);
		assertEquals("Mocked Response", response);
		verify(mockedReader, times(2)).readLine();
	}

	/**
	 * Tests the {@link FTPClient#login(String, String)} method's ability to
	 * successfully login in normal circumstances. It does so by checking that the
	 * method does not throw an error when positive response is received from the
	 * server.
	 * 
	 * @throws IOException if an input/output error occurs while attempting logging
	 *                     in.
	 */
	@Test
	void testLoginSuccess() throws IOException {
		when(mockedReader.readLine()).thenReturn("230 User logged in");

		assertDoesNotThrow(() -> ftpClient.login("validUser", "validPass"));
	}

	/**
	 * Tests the {@link FTPClient#login(String, String)} method's behavior when a
	 * wrong username is provided. The method is expected to raise an exception in
	 * that case.
	 * 
	 * @throws IOException if an input/output error occurs while attempting logging
	 *                     in.
	 */
	@Test
	void testLoginFailInvalidUser() throws IOException {
		when(mockedReader.readLine()).thenReturn("530 Not logged in, user cannot log in");

		assertThrows(IOException.class, () -> ftpClient.login("invalidUser", "validPass"));
	}

	/**
	 * Tests the {@link FTPClient#login(String, String)} method's behavior when a
	 * wrong password is provided even though the username is correct. The method is
	 * expected to raise an exception in that case.
	 * 
	 * @throws IOException if an input/output error occurs while attempting logging
	 *                     in.
	 */
	@Test
	void testLoginFailInvalidPassword() throws IOException {
		when(mockedReader.readLine()).thenReturn("230 User validUser logged in")
				.thenReturn("530 Not logged in, password incorrect");

		assertThrows(IOException.class, () -> ftpClient.login("validUser", "invalidPass"));
	}

	/**
	 * Tests the outcome of {@link FTPClient#isDirectory(String)} method when a
	 * directory is provided as an input. It asserts that the method returns true in
	 * this case.
	 */
	@Test
	public void testIsDirectoryWithDirectoryLine() {
		String line = "drwxr-xr-x 2 user group 4096 Jan 28 10:00 dirName";
		assertTrue(ftpClient.isDirectory(line));
	}

	/**
	 * Tests the outcome of {@link FTPClient#isDirectory(String)} method when a file
	 * and not a folder is provided as an argument. It asserts that the method
	 * returns false in this case.
	 */
	@Test
	public void testIsDirectoryWithFileLine() {
		String line = "-rw-r--r-- 1 user group 1024 Jan 28 11:00 fileName.txt";
		assertFalse(ftpClient.isDirectory(line));
	}

	/**
	 * Tests the outcome of {@link FTPClient#parseName(String)} method. It checks if
	 * the method returns the correct name of the file or directory from a listing
	 * response.
	 */
	@Test
	public void testParseNameWithValidLine() {
		String line = "-rw-r--r-- 1 user group 1024 Jan 28 11:00 fileName.txt";
		String expectedName = "fileName.txt";
		assertEquals(expectedName, ftpClient.parseName(line));
	}

	/**
	 * Tests the {@link FTPClient#writeTreeToJson(TreeNode, String)} method's
	 * ability to write into a file given the root of the tree and the filename in
	 * normal circumstances. It asserts that the file is created successfully and is
	 * not empty proving that it was successful in writing into it.
	 * 
	 * @throws IOException if an input/output error occurs
	 */
	@Test
	void testWriteTreeToJsonWithValidInput() throws IOException {
		TreeNode root = new TreeNode("Root");
		TreeNode child1 = new TreeNode("Child 1");
		TreeNode child2 = new TreeNode("Child 2");
		root.addChild(child1);
		root.addChild(child2);
		String filename = "test.json";
		ftpClient.writeTreeToJson(root, filename);

		File file = new File(filename);
		assertTrue(file.exists());
		String content = new String(Files.readAllBytes(file.toPath()));
		assertFalse(content.isEmpty());

		file.delete();
	}

	/**
	 * Tests if the {@link FTPClient#writeTreeToJson(TreeNode, String)} method
	 * behaves naturally if the provided root is null. It asserts that the method
	 * does not throw an exception causing the program to terminate.
	 * 
	 * @throws IOException if an input/output error occurs
	 */
	@Test
	void testWriteTreeToJsonWithNullTree() {
		String filename = "test.json";

		assertDoesNotThrow(() -> ftpClient.writeTreeToJson(null, filename));

		new File(filename).delete();
	}

	/**
	 * Tests the {@link FTPClient#writeTreeToJson(TreeNode, String)} method's
	 * behavior when an invalid name is provided.
	 * <p>
	 * The original method is designed to catch such an exception and not terminate
	 * the program in order to still be able to show the output of the tree to the
	 * console. This test makes sure that a file with an invalid name will not be
	 * created.
	 */
	@Test
	void testWriteTreeToJsonWithInvalidFilename() {
		TreeNode root = new TreeNode("Root");
		String filename = "\0invalid.json";

		ftpClient.writeTreeToJson(root, filename);

		File file = new File(filename);
		assertFalse(file.exists(), "File should not exist as the filename is invalid");
	}

	/**
	 * Tests the {@link FTPClient#writeTreeToJson(TreeNode, String)} method's
	 * behavior when a read-only file is provided.
	 * 
	 * <p>
	 * Such files do not allow the application to write into them. To test this
	 * behavior, this test creates a new file then sets its permissions to
	 * read-only. After that, it attempts writing into it, then checks that the file
	 * is empty which asserts that the method is functioning as required.
	 * 
	 * @throws IOException if an input/output error occurs
	 */
	@Test
	void testWriteTreeToJsonToReadOnlyFile() throws IOException {
		TreeNode root = new TreeNode("Root");
		String filename = "readonly.json";

		File file = new File(filename);
		file.createNewFile();
		file.setReadOnly();

		ftpClient.writeTreeToJson(root, filename);
		assertEquals(0, file.length(), "File should be empty or unchanged as it is read-only");

		if (!file.delete()) {
			System.err.println("Warning: Failed to delete the test file: " + filename);
		}
	}

}
