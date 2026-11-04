package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class TrashController {
    private final IMonthEventService monthEventService;
    private final NotificationService notificationService;

    public TrashController(IMonthEventService monthEventService, NotificationService notificationService) {
        this.monthEventService = monthEventService;
        this.notificationService = notificationService;
    }

    @GetMapping("/home/manager/trash")
    public String showTrash(Model model,
                            @RequestParam("page") Optional<Integer> page,
                            @RequestParam("size") Optional<Integer> size,
                            @RequestParam(value = "search", required = false) String search,
                            HttpServletRequest request) {
        int currentPage = page.orElse(1);
        String normalizedSearch = search == null ? "" : search.trim();
        Page<MonthEvent> deletedEvents = monthEventService.searchDeletedMonthEvents(
                PageRequest.of(Math.max(0, currentPage - 1), size.orElse(11),
                        Sort.by("monthEventDeadline").ascending()), normalizedSearch);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("search", normalizedSearch);
        model.addAttribute("monthEventList", deletedEvents);
        if (deletedEvents.getTotalPages() > 1) {
            model.addAttribute("pageNumbers", IntStream.rangeClosed(1, deletedEvents.getTotalPages())
                    .boxed().collect(Collectors.toList()));
        }
        model.addAttribute("username", request.getSession().getAttribute("username"));
        return "trash";
    }

    @PostMapping("/home/manager/trash/move/{id}")
    public String moveToTrash(@PathVariable Integer id,
                              @RequestParam(value = "source", defaultValue = "task") String source,
                              @RequestParam(value = "selectedValue", required = false) String selectedValue,
                              RedirectAttributes redirectAttributes) {
        try {
            monthEventService.moveToTrash(id);
            redirectAttributes.addFlashAttribute("mess", "Đã chuyển công việc vào Thư mục đã xóa.");
            notificationService.sendRefreshSignal();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToSource(source, selectedValue, redirectAttributes);
    }

    @PostMapping("/home/manager/trash/restore/{id}")
    public String restore(@PathVariable Integer id,
                          @RequestParam(value = "search", required = false) String search,
                          @RequestParam(value = "page", defaultValue = "1") int page,
                          @RequestParam(value = "size", defaultValue = "11") int size,
                          RedirectAttributes redirectAttributes) {
        try {
            monthEventService.restoreFromTrash(id);
            redirectAttributes.addFlashAttribute("mess", "Khôi phục công việc thành công và đã chuyển sang trạng thái Hiển thị.");
            notificationService.sendRefreshSignal();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        addTrashViewState(search, page, size, redirectAttributes);
        return "redirect:/home/manager/trash";
    }

    @PostMapping("/home/manager/trash/delete/{id}")
    public String permanentlyDelete(@PathVariable Integer id,
                                    @RequestParam(value = "search", required = false) String search,
                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "size", defaultValue = "11") int size,
                                    RedirectAttributes redirectAttributes) {
        try {
            monthEventService.permanentlyDelete(id);
            redirectAttributes.addFlashAttribute("mess", "Đã xóa hẳn công việc và toàn bộ phản hồi liên quan.");
            notificationService.sendRefreshSignal();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        addTrashViewState(search, page, size, redirectAttributes);
        return "redirect:/home/manager/trash";
    }

    @PostMapping("/home/manager/trash/empty")
    public String emptyTrash(RedirectAttributes redirectAttributes) {
        int deletedCount = monthEventService.emptyTrash();
        String message = deletedCount == 0
                ? "Thư mục đã xóa đang trống."
                : "Đã xóa hẳn " + deletedCount + " công việc và toàn bộ phản hồi liên quan.";
        redirectAttributes.addFlashAttribute("mess", message);
        if (deletedCount > 0) {
            notificationService.sendRefreshSignal();
        }
        return "redirect:/home/manager/trash";
    }

    private String redirectToSource(String source, String selectedValue,
                                    RedirectAttributes redirectAttributes) {
        if ("weekly".equals(source)) {
            return "redirect:/home/manager/weeklyTask";
        }
        if ("private".equals(source)) {
            redirectAttributes.addAttribute("selectedValue", selectedValue == null ? "" : selectedValue);
            return "redirect:/home/employee/private";
        }
        return "redirect:/home/manager/task";
    }

    private void addTrashViewState(String search, int page, int size,
                                   RedirectAttributes redirectAttributes) {
        if (search != null && !search.trim().isEmpty()) {
            redirectAttributes.addAttribute("search", search.trim());
        }
        if (page > 1) redirectAttributes.addAttribute("page", page);
        if (size != 11) redirectAttributes.addAttribute("size", size);
    }
}
