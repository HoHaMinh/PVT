package com.hoaphat.pvt.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@Controller
public class ImageController {

    @Value("${image.folder.path}")
    private String imageFolderPath;

    // Các đuôi file ảnh hợp lệ
    private static final List<String> IMAGE_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @GetMapping("/home/employee/image")
    public String showImage(Model model, HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);

        // Map: tên subfolder → danh sách URL ảnh
        Map<String, List<String>> albums = new LinkedHashMap<>();

        File baseFolder = new File(imageFolderPath);

        if (baseFolder.exists() && baseFolder.isDirectory()) {
            File[] subFolders = baseFolder.listFiles(File::isDirectory);

            if (subFolders != null) {
                // Sắp xếp subfolder theo tên
                Arrays.sort(subFolders, Comparator.comparing(File::getName));

                for (File subFolder : subFolders) {
                    List<String> imageUrls = new ArrayList<>();
                    File[] files = subFolder.listFiles();

                    if (files != null) {
                        Arrays.sort(files, Comparator.comparing(File::getName));
                        for (File file : files) {
                            String ext = getExtension(file.getName()).toLowerCase();
                            if (IMAGE_EXTENSIONS.contains(ext)) {
                                // URL để truy cập ảnh
                                String url = "/img-local/" + subFolder.getName()
                                        + "/" + file.getName();
                                imageUrls.add(url);
                            }
                        }
                    }

                    if (!imageUrls.isEmpty()) {
                        albums.put(subFolder.getName(), imageUrls);
                    }
                }
            }
        }

        model.addAttribute("albums", albums);
        return "images";
    }

    @Value("${video.folder.path}")
    private String videoFolderPath;

    private static final List<String> VIDEO_EXTENSIONS =
            Arrays.asList("mp4", "webm", "ogg", "mov");

    @GetMapping("/home/employee/video")
    public String showVideo(Model model, HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);

        Map<String, List<String>> videoAlbums = new LinkedHashMap<>();
        File baseFolder = new File(videoFolderPath);

        if (baseFolder.exists() && baseFolder.isDirectory()) {
            File[] subFolders = baseFolder.listFiles(File::isDirectory);
            if (subFolders != null) {
                Arrays.sort(subFolders, Comparator.comparing(File::getName));
                for (File subFolder : subFolders) {
                    List<String> videoUrls = new ArrayList<>();
                    File[] files = subFolder.listFiles();
                    if (files != null) {
                        Arrays.sort(files, Comparator.comparing(File::getName));
                        for (File file : files) {
                            String ext = getExtension(file.getName()).toLowerCase();
                            if (VIDEO_EXTENSIONS.contains(ext)) {
                                videoUrls.add("/vid-local/" + subFolder.getName()
                                        + "/" + file.getName());
                            }
                        }
                    }
                    if (!videoUrls.isEmpty()) {
                        videoAlbums.put(subFolder.getName(), videoUrls);
                    }
                }
            }
        }

        model.addAttribute("videoAlbums", videoAlbums);
        return "video";
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }
}