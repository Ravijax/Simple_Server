# Simple HTTP Server in Java

This is a simple HTTP server written in Java that can serve static files and execute PHP scripts.

# Prerequisites

Before running the server, ensure you have the following installed on your system:

- Java Development Kit (JDK)
- PHP (for executing PHP scripts)
- A web browser or an HTTP client for testing the server

# Instructions

Compile the Java code:
- java Main

Stop the server:
- Ctrl + C


You should see the following output:
- Waiting for User
- Server Listening on port 2728   
The server is now running and listening on port 2728.

To access a static file, open a web browser and enter the following URL:
http://127.0.0.1:2728/filename.ext
Replace 'filename.ext' with the actual path to your HTML file within the htdocs directory.

To execute a PHP script, open a web browser and enter the following URL:
http://127.0.0.1:2728/add.php?param1=value1&param2=value2
Replace /add.php with the path to your PHP script within the htdocs directory, and include any query parameters as needed.

The server will respond with the appropriate content or execute the PHP script and return its output.

To stop the server, press Ctrl + C in the terminal where it is running.