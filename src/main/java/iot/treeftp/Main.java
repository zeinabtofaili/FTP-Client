package iot.treeftp;

import java.util.Arrays;

/**
 * This class is the driver class of the FTPClient. It initiates a connection
 * between the client and the FTP server and sends commands to the server based
 * on the command line arguments provided by the user. It allows for the
 * following functionalities:
 * 
 * <li>Logging in using a username and a password.</li>
 * <li>Selecting the maximum depth of the directory tree to be retrieved.</li>
 * <li>Displaying the tree in DFS or BFS format.</li>
 * <li>Creating a JSON representation file for the tree.</li>
 * 
 * <p>
 * Example command-line usage:
 * 
 * <pre>
 *     java -jar target/tree-ftp-1.0-SNAPSHOT.jar ftp.ubuntu.com anonymous anonymous@example.com 2 dfs
 *     or
 *     java -jar target/tree-ftp-1.0-SNAPSHOT.jar ftp.ubuntu.com anonymous anonymous@example.com 2 --json
 * 
 * </pre>
 */
public class Main {
	/**
	 * This method resembles the program entry point. It parses the command line
	 * arguments and connects with the FTP server in order to list its directory
	 * tree.
	 *
	 * <p>
	 * If the --json flag is not included, the directory structure is simply printed
	 * to standard output. Otherwise, it is also saved to 'directory_structure.json'
	 * file. The application supports 'dfs' and 'bfs' methods for directory
	 * traversal. If an invalid method is specified, it defaults to 'dfs'. If no
	 * maximum depth of traversal is specified, the application will traverse the
	 * tree as deep as possible.
	 * <p>
	 * Usage example:
	 * 
	 * <pre>
	 *   java -jar TreeFtp.jar ftp.example.com user pass 3 dfs --json
	 * </pre>
	 * 
	 * @param args [0] The server that the client wishes to connect to
	 * @param args [1] The username used to login (optional)
	 * @param args [2] The password used for authentication (optional)
	 * @param args [3] Maximum depth of the directory tree (optional)
	 * @param args [4] Tree format (dfs or bfs) (optional)
	 * @param args [5] --json flag to output the directory structure in JSON format
	 *             (optional)
	 * 
	 */
	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				System.out.println(
						"Usage: java -jar TreeFtp.jar <ftp-server-address> [username] [password] [max-depth] [dfs/bfs] [--json]");
				return;
			}
			if (Arrays.asList(args).contains("--help")) {
				printHelp();
				return;
			}
			String server = args[0];

			String username = args.length > 1 ? args[1] : "anonymous";
			String password = args.length > 2 ? args[2] : "anonymous@example.com";
			int maxDepth = args.length > 3 ? Integer.parseInt(args[3]) : Integer.MAX_VALUE;
			String method = args.length > 4 ? args[4].toLowerCase() : "dfs";
			if (method == null || (!method.equals("bfs") && !method.equals("dfs"))) {
				method = "dfs";
			}

			boolean jsonOutput = Arrays.asList(args).contains("--json");

			FTPClient ftpClient = new FTPClient(server, 21);
			ftpClient.login(username, password);

			if (jsonOutput) {
				TreeNode root = ftpClient.buildJsonTree("/", 0, maxDepth);
				ftpClient.writeTreeToJson(root, "directory_structure.json");
			}
			if (method.equals("dfs")) {
				ftpClient.showTreeDFS("/", "", 0, maxDepth);
			} else if (method.equals("bfs")) {
				ftpClient.showTreeBFS("/", maxDepth);
			} else {
				System.out.println("Invalid traversal method. Please choose 'dfs' or 'bfs'.");
			}

			ftpClient.disconnectFromServer();
		} catch (Exception e) {
			System.err.println("An unexpected error occurred. The application will close: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * This method is provides necessary information on how to use the application.
	 * It provides details about each argument and option.
	 */
	private static void printHelp() {
		System.out.println(
				"Usage: java -jar TreeFtp.jar <ftp-server-address> [username] [password] [max-depth] [dfs/bfs] [--json]");
		System.out.println("Options:");
		System.out.println("  <ftp-server-address>\tThe address of the FTP server to connect to.");
		System.out.println("  [username]\t\tUsername for the FTP server. Default is 'anonymous'.");
		System.out.println("  [password]\t\tPassword for the FTP server. Default is 'anonymous@example.com'.");
		System.out.println("  [max-depth]\t\tThe maximum depth for tree traversal.");
		System.out.println("  [dfs/bfs]\t\tThe method of tree traversal: Depth-first (dfs) or Breadth-first (bfs).");
		System.out.println("  --json\t\tIf included, outputs the tree in JSON format.");
	}
}
