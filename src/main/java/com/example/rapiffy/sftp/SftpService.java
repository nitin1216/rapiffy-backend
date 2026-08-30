package com.example.rapiffy.sftp;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class SftpService {

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.private-key-content}")
    private String privateKeyContent;

    @Value("${sftp.base-path}")
    private String basePath;

    @Value("${sftp.base-url}")
    private String baseUrl;

    /**
     * Uploads a file to Serverbyt via SFTP and returns the public URL.
     *
     * @param file       the image file to upload
     * @param remotePath folder path relative to base-path
     *                   e.g. "categories/3"  |  "products/3/10"  |  "variants/3/10/5"
     * @return public URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String remotePath) {
        Session session = null;
        ChannelSftp channel = null;

        try {
            String fullRemotePath = basePath + "/" + remotePath;

            // Generate unique filename
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID() + ext;

            // Write private key content from properties to a temp file
            Path tempKey = Files.createTempFile("sftp-key-", "");
            tempKey.toFile().deleteOnExit();
            Files.writeString(tempKey, privateKeyContent.replace("\\n", "\n"));

            // Connect session using private key
            JSch jsch = new JSch();
            jsch.addIdentity(tempKey.toAbsolutePath().toString());
            session = jsch.getSession(username, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey");
            session.connect(30000);

            // Open SFTP channel
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            // Create remote directories recursively
            createRemoteDirectories(channel, fullRemotePath);

            // Upload file
            try (InputStream in = file.getInputStream()) {
                channel.put(in, fullRemotePath + "/" + fileName);
            }

            return baseUrl + "/" + remotePath + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("SFTP upload failed: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    /**
     * Recursively creates directories on the remote server if they don't exist.
     */
    private void createRemoteDirectories(ChannelSftp channel, String path) throws SftpException {
        String[] folders = path.split("/");
        StringBuilder current = new StringBuilder();

        for (String folder : folders) {
            if (folder.isEmpty()) {
                current.append("/");
                continue;
            }
            current.append(folder);
            try {
                channel.stat(current.toString());
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    channel.mkdir(current.toString());
                }
            }
            current.append("/");
        }
    }
}
